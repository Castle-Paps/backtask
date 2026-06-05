package org.example.registrotareas.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Almacena imágenes en el filesystem del servidor (carpeta "uploads/").
 * Sustituye a Google Drive: las imágenes se sirven por URL estática
 * (ver WebConfig → /uploads/**) y la BD solo guarda la ruta relativa.
 *
 * La carpeta "uploads" se monta como volumen Docker para que los archivos
 * persistan entre reinicios del contenedor.
 */
@Service
public class LocalStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    @Value("${app.uploads-dir:uploads}")
    private String uploadsDir;

    private Path raiz;

    @PostConstruct
    public void init() {
        raiz = Paths.get(uploadsDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(raiz);
            log.info("📁 Almacenamiento local listo: {}", raiz);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear la carpeta de uploads: " + raiz, e);
        }
    }

    /**
     * Guarda un archivo y devuelve sus metadatos.
     *
     * @param archivo    archivo multipart recibido del cliente
     * @param subcarpeta "tareas" | "seguimientos" (organización simple)
     */
    public ArchivoLocalInfo guardar(MultipartFile archivo, String subcarpeta) {
        try {
            Path carpeta = raiz.resolve(subcarpeta).normalize();
            Files.createDirectories(carpeta);

            String ext = extension(archivo.getOriginalFilename());
            String nombreFisico = UUID.randomUUID().toString().replace("-", "") + ext;
            Path destino = carpeta.resolve(nombreFisico);

            try (var in = archivo.getInputStream()) {
                Files.copy(in, destino, StandardCopyOption.REPLACE_EXISTING);
            }

            // Ruta relativa que el cliente concatena al dominio: uploads/tareas/xxx.jpg
            String rutaRelativa = "uploads/" + subcarpeta + "/" + nombreFisico;

            log.info("📤 Imagen guardada: {} ({} bytes)", rutaRelativa, archivo.getSize());

            return new ArchivoLocalInfo(
                    nombreFisico,
                    archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : nombreFisico,
                    archivo.getContentType() != null ? archivo.getContentType() : "image/jpeg",
                    archivo.getSize(),
                    rutaRelativa
            );

        } catch (IOException e) {
            log.error("Error guardando archivo: {}", e.getMessage());
            throw new RuntimeException("No se pudo guardar la imagen: " + e.getMessage(), e);
        }
    }

    /** Elimina un archivo por su ruta relativa (uploads/tareas/xxx.jpg). */
    public void eliminar(String rutaRelativa) {
        if (rutaRelativa == null || rutaRelativa.isBlank()) return;
        try {
            // Quitar el prefijo "uploads/" porque raiz ya apunta a esa carpeta
            String sinPrefijo = rutaRelativa.startsWith("uploads/")
                    ? rutaRelativa.substring("uploads/".length())
                    : rutaRelativa;
            Path archivo = raiz.resolve(sinPrefijo).normalize();
            // Seguridad: que no se salga de la carpeta raíz
            if (archivo.startsWith(raiz)) {
                Files.deleteIfExists(archivo);
                log.info("🗑 Imagen eliminada: {}", rutaRelativa);
            }
        } catch (IOException e) {
            log.warn("No se pudo eliminar {}: {}", rutaRelativa, e.getMessage());
        }
    }

    private String extension(String nombre) {
        if (nombre == null) return ".jpg";
        int dot = nombre.lastIndexOf('.');
        return (dot >= 0) ? nombre.substring(dot) : ".jpg";
    }

    // ── DTO ──────────────────────────────────────────────────────────────────
    public record ArchivoLocalInfo(
            String nombreFisico,
            String nombreOriginal,
            String mimeType,
            long tamanoBytes,
            String rutaRelativa   // "uploads/tareas/xxx.jpg"
    ) {}
}
