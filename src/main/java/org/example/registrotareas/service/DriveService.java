package org.example.registrotareas.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.UserCredentials;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Accede a Google Drive usando OAuth 2.0 con Refresh Token.
 *
 * Las credenciales se leen de variables de entorno al arrancar:
 *   GOOGLE_CLIENT_ID     → OAuth Client ID (Desktop Application)
 *   GOOGLE_CLIENT_SECRET → OAuth Client Secret
 *   GOOGLE_REFRESH_TOKEN → Refresh Token obtenido una sola vez via OAuth Playground
 *
 * El Access Token se renueva automáticamente — no expira nunca.
 *
 * Estructura de carpetas en Drive:
 *   RegistroTareas/
 *     {NombreCliente}/    ← nivel "proyecto"
 *       {TK-000001}/      ← nivel "tarea"
 *         archivo.jpg
 */
@Service
public class DriveService {

    private static final Logger log = LoggerFactory.getLogger(DriveService.class);
    private static final String APP_NAME    = "RegistroTareas";
    private static final String FOLDER_MIME = "application/vnd.google-apps.folder";

    @Value("${google.oauth.client-id:}")
    private String clientId;

    @Value("${google.oauth.client-secret:}")
    private String clientSecret;

    @Value("${google.oauth.refresh-token:}")
    private String refreshToken;

    @Value("${google.drive.root-folder:RegistroTareas}")
    private String rootFolderName;

    private boolean disponible = false;

    /** Caché de IDs de carpetas — evita llamadas innecesarias a Drive */
    private final Map<String, String> carpetaCache = new ConcurrentHashMap<>();
    private String rootFolderId;

    // ─── Inicialización ───────────────────────────────────────────────────────

    @PostConstruct
    public void inicializar() {
        if (clientId.isBlank() || clientSecret.isBlank() || refreshToken.isBlank()) {
            log.warn("⚠ Google Drive NO configurado. " +
                     "Define GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET y GOOGLE_REFRESH_TOKEN.");
            disponible = false;
            return;
        }
        try {
            Drive drive = construirDrive();
            rootFolderId = obtenerOCrearCarpeta(drive, rootFolderName, null);
            disponible = true;
            log.info("✅ Google Drive conectado. Carpeta raíz: '{}' ({})",
                    rootFolderName, rootFolderId);
        } catch (Exception e) {
            disponible = false;
            log.error("❌ Error conectando Google Drive: {}", e.getMessage());
        }
    }

    // ─── API pública ──────────────────────────────────────────────────────────

    /**
     * Sube un archivo a Drive en la carpeta correcta.
     *
     * @param archivo  Archivo multipart recibido del cliente Android
     * @param cliente  Nombre del cliente → subcarpeta "proyecto"
     * @param ticket   Número de ticket (TK-000001) → subcarpeta "tarea"
     * @param usuario  Usuario que sube (para log)
     */
    /**
     * @param subcarpeta "problema" para las imágenes iniciales de la tarea,
     *                   "seguimiento" para evidencias de avances/completar/cancelar.
     */
    public DriveArchivoInfo subir(MultipartFile archivo,
                                  String cliente,
                                  String ticket,
                                  String subcarpeta,
                                  String usuario) throws IOException {
        verificarDisponibilidad();
        Drive drive = construirDrive();

        // Nivel 1 — cliente (proyecto)
        String clienteId = obtenerOCrearCarpeta(
                drive, sanitizar(cliente != null ? cliente : "General"), rootFolderId);

        // Nivel 2 — ticket
        String ticketId = obtenerOCrearCarpeta(
                drive, sanitizar(ticket != null ? ticket : "sin-ticket"), clienteId);

        // Nivel 3 — problema | seguimiento
        String subcarpetaId = obtenerOCrearCarpeta(
                drive, sanitizar(subcarpeta != null ? subcarpeta : "archivos"), ticketId);

        String mimeType = archivo.getContentType() != null
                ? archivo.getContentType() : "application/octet-stream";

        File metadata = new File();
        metadata.setName(archivo.getOriginalFilename());
        metadata.setParents(Collections.singletonList(subcarpetaId));

        InputStreamContent content = new InputStreamContent(mimeType, archivo.getInputStream());
        content.setLength(archivo.getSize());

        File resultado = drive.files().create(metadata, content)
                .setFields("id, name, mimeType, size")
                .execute();

        // Hacer el archivo accesible con enlace (sin login de Google en el móvil)
        hacerPublico(drive, resultado.getId());

        String urlArchivo = urlDirecta(resultado.getId(), mimeType);

        log.info("📤 '{}' subido por '{}'. ID Drive: {}",
                archivo.getOriginalFilename(), usuario, resultado.getId());

        return new DriveArchivoInfo(
                resultado.getId(),
                resultado.getName(),
                mimeType,
                archivo.getSize(),
                urlArchivo
        );
    }

    /**
     * Elimina un archivo de Drive.
     * Si ya no existe, lo ignora silenciosamente.
     */
    public void eliminar(String driveFileId) throws IOException {
        verificarDisponibilidad();
        try {
            construirDrive().files().delete(driveFileId).execute();
            log.info("🗑 Archivo {} eliminado de Drive", driveFileId);
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404) {
                log.warn("Archivo {} no encontrado en Drive", driveFileId);
            } else {
                throw e;
            }
        }
    }

    /**
     * Lista archivos en la carpeta de un ticket.
     */
    public List<File> listar(String cliente, String ticket) throws IOException {
        verificarDisponibilidad();
        Drive drive = construirDrive();
        String clienteId = obtenerOCrearCarpeta(drive, sanitizar(cliente), rootFolderId);
        String ticketId  = obtenerOCrearCarpeta(drive, sanitizar(ticket), clienteId);

        return drive.files().list()
                .setQ("'" + ticketId + "' in parents and trashed = false")
                .setFields("files(id, name, mimeType, size, createdTime)")
                .execute()
                .getFiles();
    }

    public boolean isDisponible() { return disponible; }

    public void limpiarCache() {
        carpetaCache.clear();
        rootFolderId = null;
    }

    // ─── Drive helpers ────────────────────────────────────────────────────────

    private Drive construirDrive() throws IOException {
        try {
            UserCredentials credentials = UserCredentials.newBuilder()
                    .setClientId(clientId)
                    .setClientSecret(clientSecret)
                    .setRefreshToken(refreshToken)
                    .build();

            return new Drive.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials)
            ).setApplicationName(APP_NAME).build();

        } catch (Exception e) {
            throw new IOException("Error construyendo cliente Drive: " + e.getMessage(), e);
        }
    }

    private String obtenerOCrearCarpeta(Drive drive, String nombre, String parentId)
            throws IOException {
        String key = (parentId != null ? parentId : "root") + "/" + nombre;
        if (carpetaCache.containsKey(key)) return carpetaCache.get(key);

        String query = "mimeType='" + FOLDER_MIME + "'"
                + " and name='" + nombre.replace("'", "\\'") + "'"
                + " and trashed=false";
        if (parentId != null) query += " and '" + parentId + "' in parents";

        FileList found = drive.files().list().setQ(query).setFields("files(id)").execute();

        String id;
        if (!found.getFiles().isEmpty()) {
            id = found.getFiles().get(0).getId();
        } else {
            File folder = new File();
            folder.setName(nombre);
            folder.setMimeType(FOLDER_MIME);
            if (parentId != null) folder.setParents(Collections.singletonList(parentId));
            id = drive.files().create(folder).setFields("id").execute().getId();
            log.info("📁 Carpeta '{}' creada en Drive: {}", nombre, id);
        }

        carpetaCache.put(key, id);
        return id;
    }

    private void hacerPublico(Drive drive, String fileId) {
        try {
            com.google.api.services.drive.model.Permission p =
                    new com.google.api.services.drive.model.Permission();
            p.setType("anyone");
            p.setRole("reader");
            drive.permissions().create(fileId, p).execute();
        } catch (Exception e) {
            log.warn("No se pudo hacer público el archivo {}: {}", fileId, e.getMessage());
        }
    }

    /** URL directa según tipo: imágenes → vista previa, resto → descarga. */
    private String urlDirecta(String fileId, String mimeType) {
        return mimeType != null && mimeType.startsWith("image/")
                ? "https://drive.google.com/uc?export=view&id=" + fileId
                : "https://drive.google.com/uc?export=download&id=" + fileId;
    }

    /** Elimina caracteres problemáticos para nombres de carpeta en Drive. */
    private String sanitizar(String nombre) {
        return nombre.replaceAll("[/\\\\:*?\"<>|]", "-").trim();
    }

    private void verificarDisponibilidad() {
        if (!disponible) {
            throw new IllegalStateException(
                "Google Drive no está configurado. " +
                "Define GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET y GOOGLE_REFRESH_TOKEN.");
        }
    }

    // ─── DTO de resultado ─────────────────────────────────────────────────────

    public record DriveArchivoInfo(
            String driveFileId,
            String nombreArchivo,
            String mimeType,
            long tamanoBytes,
            String urlArchivo
    ) {}
}
