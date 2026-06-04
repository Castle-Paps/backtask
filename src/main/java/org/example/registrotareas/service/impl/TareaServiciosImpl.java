package org.example.registrotareas.service.impl;

import org.example.registrotareas.dto.*;
import org.example.registrotareas.dto.whatsapp.WhatsAppEnvioResponse;
import org.example.registrotareas.entity.*;
import org.example.registrotareas.exception.ResourceNotFoundException;
import org.example.registrotareas.repository.ArchivoSeguimientoRepositorio;
import org.example.registrotareas.repository.ArchivoTareaRepositorio;
import org.example.registrotareas.repository.SeguimientoRepositorio;
import org.example.registrotareas.repository.TareaRepositorio;
import org.example.registrotareas.service.DriveService;
import org.example.registrotareas.service.MensajeWhatsappService;
import org.example.registrotareas.service.TareaServicios;
import org.example.registrotareas.util.TelefonoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class TareaServiciosImpl implements TareaServicios {

    private static final Logger log = LoggerFactory.getLogger(TareaServiciosImpl.class);

    private static final long MAX_ARCHIVO_BYTES = 50 * 1024 * 1024; // 50 MB por archivo
    private static final List<String> TIPOS_PERMITIDOS = List.of(
            "image/jpeg", "image/png", "image/webp",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "video/mp4", "video/quicktime"
    );

    private final TareaRepositorio tareaRepository;
    private final ArchivoTareaRepositorio archivoTareaRepositorio;
    private final ArchivoSeguimientoRepositorio archivoSeguimientoRepositorio;
    private final SeguimientoRepositorio seguimientoRepositorio;
    private final DriveService driveService;
    private final MensajeWhatsappService mensajeWhatsappService;

    public TareaServiciosImpl(
            TareaRepositorio tareaRepository,
            ArchivoTareaRepositorio archivoTareaRepositorio,
            ArchivoSeguimientoRepositorio archivoSeguimientoRepositorio,
            SeguimientoRepositorio seguimientoRepositorio,
            DriveService driveService,
            MensajeWhatsappService mensajeWhatsappService
    ) {
        this.tareaRepository = tareaRepository;
        this.archivoTareaRepositorio = archivoTareaRepositorio;
        this.archivoSeguimientoRepositorio = archivoSeguimientoRepositorio;
        this.seguimientoRepositorio = seguimientoRepositorio;
        this.driveService = driveService;
        this.mensajeWhatsappService = mensajeWhatsappService;
    }

    // ─── CREAR ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public TareaResponse crearTarea(TareaRequest request,
                                    List<MultipartFile> archivos) {
        validarEvidenciaInicial(archivos);

        Tarea tarea = new Tarea();
        tarea.setNombreCliente(request.nombreCliente());
        tarea.setTelefono(TelefonoUtil.normalizarTelefonoPeru(request.telefono()));
        tarea.setMotivo(request.motivo());
        tarea.setMonto(request.monto());
        tarea.setAdelanto(request.adelanto() != null ? request.adelanto() : BigDecimal.ZERO);
        tarea.setTiempoDias(request.tiempoDias());
        tarea.setEstado(Estado.PENDIENTE);
        tarea.setUsuarioRegistro(obtenerUsuarioActual());

        Tarea tareaGuardada = tareaRepository.saveAndFlush(tarea);
        tareaGuardada.setNumeroTicket("TK-" + String.format("%06d", tareaGuardada.getId()));
        tareaGuardada.setFechaVencimiento(
                calcularFechaVencimiento(tareaGuardada.getFechaRegistro(), request.tiempoDias()));
        tareaRepository.save(tareaGuardada);

        // Subir imágenes a Drive (carpeta "problema")
        if (archivos != null) {
            for (MultipartFile archivo : archivos) {
                subirArchivoATarea(tareaGuardada, archivo);
            }
        }

        registrarHistorial(tareaGuardada, TipoHistorial.CREACION,
                "Tarea registrada por " + tareaGuardada.getUsuarioRegistro(),
                tareaGuardada.getUsuarioRegistro());

        log.info("Tarea creada: {} - {}", tareaGuardada.getNumeroTicket(), tareaGuardada.getNombreCliente());
        return convertirAResponse(tareaGuardada);
    }

    // ─── LISTAR ───────────────────────────────────────────────────────────────

    @Override @Transactional(readOnly = true)
    public List<TareaResponse> listarTareas() {
        return tareaRepository.findAll().stream().map(this::convertirAResponse).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<TareaResponse> listarTareasPendientes() {
        return tareaRepository.findByEstadoOrdenadoPorUrgencia(Estado.PENDIENTE).stream()
                .map(this::convertirAResponse).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<TareaResponse> listarTareasCompletadas() {
        return tareaRepository.findByEstadoOrderByFechaRegistroDesc(Estado.COMPLETADO).stream()
                .map(this::convertirAResponse).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<TareaResponse> listarTareasCanceladas() {
        return tareaRepository.findByEstadoOrderByFechaRegistroDesc(Estado.CANCELADO).stream()
                .map(this::convertirAResponse).toList();
    }

    @Override @Transactional(readOnly = true)
    public TareaResponse buscarPorId(Long id) {
        return convertirAResponse(obtenerTareaPorId(id));
    }

    @Override @Transactional(readOnly = true)
    public List<TareaResponse> buscar(String q) {
        if (q == null || q.isBlank()) return listarTareas();
        List<Tarea> resultado = tareaRepository.buscar(q.trim());
        if (resultado.isEmpty())
            throw new ResourceNotFoundException("Sin resultados para: " + q);
        return resultado.stream().map(this::convertirAResponse).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<TareaResponse> buscarPorNombre(String nombre) {
        List<Tarea> tareas = tareaRepository.buscarPorNombre(nombre);
        if (tareas.isEmpty()) throw new ResourceNotFoundException("No se encontraron tareas: " + nombre);
        return tareas.stream().map(this::convertirAResponse).toList();
    }

    // ─── ACTUALIZAR ───────────────────────────────────────────────────────────

    @Override @Transactional
    public TareaResponse actualizarTarea(Long id, TareaRequest request) {
        Tarea tarea = obtenerTareaPorId(id);

        List<String> cambios = new ArrayList<>();
        if (!Objects.equals(tarea.getNombreCliente(), request.nombreCliente()))
            cambios.add("nombreCliente");
        if (!Objects.equals(tarea.getMotivo(), request.motivo()))
            cambios.add("motivo");
        if (!Objects.equals(tarea.getMonto(), request.monto()))
            cambios.add("monto (S/" + tarea.getMonto() + " → S/" + request.monto() + ")");
        if (!Objects.equals(tarea.getTiempoDias(), request.tiempoDias()))
            cambios.add("tiempo (" + tarea.getTiempoDias() + " → " + request.tiempoDias() + " días)");

        tarea.setNombreCliente(request.nombreCliente());
        tarea.setTelefono(TelefonoUtil.normalizarTelefonoPeru(request.telefono()));
        tarea.setMotivo(request.motivo());
        tarea.setMonto(request.monto());
        tarea.setAdelanto(request.adelanto() != null ? request.adelanto() : BigDecimal.ZERO);
        tarea.setTiempoDias(request.tiempoDias());
        tarea.setFechaVencimiento(calcularFechaVencimiento(tarea.getFechaRegistro(), request.tiempoDias()));
        tarea.setFechaEdicion(LocalDateTime.now());

        Tarea guardada = tareaRepository.save(tarea);

        if (!cambios.isEmpty()) {
            registrarHistorial(guardada, TipoHistorial.EDICION,
                    "Campos modificados: " + String.join(", ", cambios),
                    obtenerUsuarioActual());
        }
        return convertirAResponse(guardada);
    }

    // ─── COMPLETAR ────────────────────────────────────────────────────────────

    @Override @Transactional
    public CambioEstadoResponse completarTarea(Long id, String descripcion,
                                               List<MultipartFile> archivos) {
        Tarea tarea = obtenerTareaPorId(id);
        if (tarea.getEstado() != Estado.PENDIENTE)
            throw new IllegalArgumentException("Solo se pueden completar tareas PENDIENTE");

        if (archivos == null || archivos.isEmpty())
            throw new IllegalArgumentException("Debes adjuntar al menos una imagen como evidencia");

        Seguimiento entrada = new Seguimiento();
        entrada.setTarea(tarea);
        entrada.setTipo(TipoHistorial.COMPLETADO);
        entrada.setDescripcion(descripcion);
        entrada.setUsuario(obtenerUsuarioActual());
        Seguimiento guardado = seguimientoRepositorio.saveAndFlush(entrada);

        // Subir evidencia a Drive (carpeta "seguimiento")
        for (MultipartFile archivo : archivos) {
            subirArchivoASeguimiento(guardado, tarea.getNumeroTicket(), archivo);
        }

        tarea.setEstado(Estado.COMPLETADO);
        tarea.setFechaFinalizacion(LocalDateTime.now());
        Tarea tareaActualizada = tareaRepository.save(tarea);

        List<String> urlsEvidencia = guardado.getArchivos().stream()
                .map(ArchivoSeguimiento::getUrlArchivo).toList();

        String mensaje = mensajeWhatsappService.construirMensajeCompletado(
                tareaActualizada, descripcion, urlsEvidencia);
        String whatsappUrl = mensajeWhatsappService.construirUrlWhatsApp(
                telefonoNormalizado(tareaActualizada.getTelefono()), mensaje);

        return new CambioEstadoResponse(convertirAResponse(tareaActualizada), whatsappUrl, mensaje);
    }

    // ─── CANCELAR ─────────────────────────────────────────────────────────────

    @Override @Transactional
    public CambioEstadoResponse cancelarTarea(Long id, String descripcion,
                                              List<MultipartFile> archivos) {
        Tarea tarea = obtenerTareaPorId(id);
        if (tarea.getEstado() != Estado.PENDIENTE)
            throw new IllegalArgumentException("Solo se pueden cancelar tareas PENDIENTE");

        Seguimiento entrada = new Seguimiento();
        entrada.setTarea(tarea);
        entrada.setTipo(TipoHistorial.CANCELACION);
        entrada.setDescripcion(descripcion);
        entrada.setUsuario(obtenerUsuarioActual());
        Seguimiento guardado = seguimientoRepositorio.saveAndFlush(entrada);

        if (archivos != null) {
            for (MultipartFile archivo : archivos) {
                subirArchivoASeguimiento(guardado, tarea.getNumeroTicket(), archivo);
            }
        }

        tarea.setEstado(Estado.CANCELADO);
        tarea.setFechaFinalizacion(LocalDateTime.now());
        Tarea tareaActualizada = tareaRepository.save(tarea);

        String mensaje = mensajeWhatsappService.construirMensajeCancelado(tareaActualizada, descripcion);
        String whatsappUrl = mensajeWhatsappService.construirUrlWhatsApp(
                telefonoNormalizado(tareaActualizada.getTelefono()), mensaje);

        return new CambioEstadoResponse(convertirAResponse(tareaActualizada), whatsappUrl, mensaje);
    }

    // ─── AVANCE ───────────────────────────────────────────────────────────────

    @Override @Transactional
    public SeguimientoResponse agregarAvance(Long tareaId, String descripcion,
                                             List<MultipartFile> archivos) {
        Tarea tarea = obtenerTareaPorId(tareaId);

        if (archivos == null || archivos.isEmpty())
            throw new IllegalArgumentException("Debes adjuntar al menos una imagen en el avance");

        Seguimiento avance = new Seguimiento();
        avance.setTarea(tarea);
        avance.setTipo(TipoHistorial.AVANCE);
        avance.setDescripcion(descripcion);
        avance.setUsuario(obtenerUsuarioActual());
        Seguimiento guardado = seguimientoRepositorio.saveAndFlush(avance);

        for (MultipartFile archivo : archivos) {
            subirArchivoASeguimiento(guardado, tarea.getNumeroTicket(), archivo);
        }

        return convertirSeguimientoAResponse(guardado);
    }

    // ─── HISTORIAL ────────────────────────────────────────────────────────────

    @Override @Transactional(readOnly = true)
    public List<SeguimientoResponse> listarHistorial(Long tareaId) {
        return seguimientoRepositorio.findByTareaIdOrderByFechaCreacionAsc(tareaId).stream()
                .map(this::convertirSeguimientoAResponse).toList();
    }

    // ─── ARCHIVOS ─────────────────────────────────────────────────────────────

    @Override @Transactional
    public TareaResponse agregarImagenes(Long tareaId, List<MultipartFile> archivos) {
        if (archivos == null || archivos.isEmpty())
            throw new IllegalArgumentException("Debes enviar al menos un archivo");
        Tarea tarea = obtenerTareaPorId(tareaId);
        for (MultipartFile archivo : archivos) {
            subirArchivoATarea(tarea, archivo);
        }
        return convertirAResponse(tareaRepository.save(tarea));
    }

    @Override @Transactional
    public TareaResponse eliminarImagen(Long tareaId, Long archivoId) {
        Tarea tarea = obtenerTareaPorId(tareaId);
        ArchivoTarea archivo = archivoTareaRepositorio.findByIdAndTareaId(archivoId, tareaId)
                .orElseThrow(() -> new ResourceNotFoundException("Archivo no encontrado"));

        try {
            driveService.eliminar(archivo.getDriveFileId());
        } catch (Exception e) {
            log.warn("No se pudo eliminar de Drive (ID: {}): {}", archivo.getDriveFileId(), e.getMessage());
        }

        tarea.getArchivos().remove(archivo);
        archivoTareaRepositorio.delete(archivo);
        return convertirAResponse(tareaRepository.save(tarea));
    }

    // ─── WHATSAPP ─────────────────────────────────────────────────────────────

    @Override @Transactional
    public WhatsAppEnvioResponse generarWhatsappCreacion(Long tareaId) {
        Tarea tarea = obtenerTareaPorId(tareaId);
        String telefono = telefonoNormalizado(tarea.getTelefono());
        String mensaje = mensajeWhatsappService.construirResumenCreacion(tarea);
        String url = mensajeWhatsappService.construirUrlWhatsApp(telefono, mensaje);
        return new WhatsAppEnvioResponse(tarea.getId(), telefono, url, mensaje,
                "LINK_PRELLENADO", "Abre este enlace para enviar el resumen al cliente.");
    }

    // ─── DRIVE HELPERS ────────────────────────────────────────────────────────

    private void subirArchivoATarea(Tarea tarea, MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) return;
        validarArchivo(archivo);
        try {
            DriveService.DriveArchivoInfo info = driveService.subir(
                    archivo,
                    tarea.getNombreCliente(),   // nivel 1 → cliente
                    tarea.getNumeroTicket(),    // nivel 2 → ticket
                    "problema",                // nivel 3 → imágenes iniciales del problema
                    obtenerUsuarioActual());

            ArchivoTarea entidad = new ArchivoTarea();
            entidad.setTarea(tarea);
            entidad.setDriveFileId(info.driveFileId());
            entidad.setNombreArchivo(info.nombreArchivo());
            entidad.setMimeType(info.mimeType());
            entidad.setTamanoBytes(info.tamanoBytes());
            entidad.setUrlArchivo(info.urlArchivo());
            entidad.setUsuarioCreacion(obtenerUsuarioActual());
            tarea.getArchivos().add(archivoTareaRepositorio.save(entidad));

        } catch (Exception e) {
            log.error("Error subiendo archivo '{}' a Drive: {}", archivo.getOriginalFilename(), e.getMessage());
            throw new RuntimeException("Error al subir archivo a Google Drive: " + e.getMessage(), e);
        }
    }

    private void subirArchivoASeguimiento(Seguimiento seguimiento, String ticket, MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) return;
        validarArchivo(archivo);
        try {
            DriveService.DriveArchivoInfo info = driveService.subir(
                    archivo,
                    seguimiento.getTarea().getNombreCliente(),  // nivel 1 → cliente
                    ticket,                                     // nivel 2 → ticket
                    "seguimiento",                              // nivel 3 → avances/completar/cancelar
                    obtenerUsuarioActual());

            ArchivoSeguimiento entidad = new ArchivoSeguimiento();
            entidad.setSeguimiento(seguimiento);
            entidad.setDriveFileId(info.driveFileId());
            entidad.setNombreArchivo(info.nombreArchivo());
            entidad.setMimeType(info.mimeType());
            entidad.setTamanoBytes(info.tamanoBytes());
            entidad.setUrlArchivo(info.urlArchivo());
            entidad.setUsuarioCreacion(obtenerUsuarioActual());
            seguimiento.getArchivos().add(archivoSeguimientoRepositorio.save(entidad));

        } catch (Exception e) {
            log.error("Error subiendo evidencia '{}' a Drive: {}", archivo.getOriginalFilename(), e.getMessage());
            throw new RuntimeException("Error al subir evidencia a Google Drive: " + e.getMessage(), e);
        }
    }

    private void validarArchivo(MultipartFile archivo) {
        if (archivo.getSize() > MAX_ARCHIVO_BYTES)
            throw new IllegalArgumentException("El archivo supera el límite de 50 MB");
        String tipo = archivo.getContentType();
        if (tipo == null || !TIPOS_PERMITIDOS.contains(tipo))
            throw new IllegalArgumentException(
                    "Tipo de archivo no permitido: " + tipo +
                    ". Permitidos: JPG, PNG, WEBP, PDF, Word, Excel, MP4");
    }

    private void validarEvidenciaInicial(List<MultipartFile> archivos) {
        if (archivos == null || archivos.isEmpty())
            throw new IllegalArgumentException("Debes adjuntar al menos una imagen");
    }

    // ─── HELPERS GENERALES ────────────────────────────────────────────────────

    private void registrarHistorial(Tarea tarea, TipoHistorial tipo,
                                    String descripcion, String usuario) {
        Seguimiento entrada = new Seguimiento();
        entrada.setTarea(tarea);
        entrada.setTipo(tipo);
        entrada.setDescripcion(descripcion);
        entrada.setUsuario(usuario);
        seguimientoRepositorio.save(entrada);
    }

    private String obtenerUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()))
            return auth.getName();
        return "sistema";
    }

    private String telefonoNormalizado(String telefono) {
        try { return TelefonoUtil.normalizarTelefonoPeru(telefono); }
        catch (Exception e) { return telefono != null ? telefono.replaceAll("\\D", "") : ""; }
    }

    private LocalDate calcularFechaVencimiento(LocalDateTime fechaRegistro, Integer tiempoDias) {
        if (fechaRegistro == null || tiempoDias == null || tiempoDias <= 0) return null;
        return fechaRegistro.toLocalDate().plusDays(tiempoDias);
    }

    private String calcularColorUrgencia(Tarea tarea) {
        if (tarea.getEstado() != Estado.PENDIENTE || tarea.getFechaVencimiento() == null) return null;
        long dias = ChronoUnit.DAYS.between(LocalDate.now(), tarea.getFechaVencimiento());
        if (dias > 1) return "VERDE";
        if (dias == 1) return "AMARILLO";
        return "ROJO";
    }

    private Integer calcularDiasRestantes(Tarea tarea) {
        if (tarea.getFechaVencimiento() == null) return null;
        return (int) ChronoUnit.DAYS.between(LocalDate.now(), tarea.getFechaVencimiento());
    }

    private Tarea obtenerTareaPorId(Long id) {
        return tareaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarea no encontrada: " + id));
    }

    // ─── CONVERSIÓN ───────────────────────────────────────────────────────────

    private TareaResponse convertirAResponse(Tarea tarea) {
        BigDecimal monto   = tarea.getMonto()   != null ? tarea.getMonto()   : BigDecimal.ZERO;
        BigDecimal adelanto= tarea.getAdelanto() != null ? tarea.getAdelanto(): BigDecimal.ZERO;

        List<ArchivoResponse> archivos = tarea.getArchivos() != null
                ? tarea.getArchivos().stream().map(this::convertirArchivoTareaAResponse).toList()
                : List.of();

        List<SeguimientoResponse> historial = tarea.getHistorial() != null
                ? tarea.getHistorial().stream().map(this::convertirSeguimientoAResponse).toList()
                : List.of();

        String resumen = mensajeWhatsappService.construirResumenCreacion(tarea);
        String whatsappUrl = mensajeWhatsappService.construirUrlWhatsApp(
                telefonoNormalizado(tarea.getTelefono()), resumen);

        return new TareaResponse(
                tarea.getId(),
                tarea.getNumeroTicket(),
                tarea.getNombreCliente(),
                tarea.getTelefono(),
                tarea.getMotivo(),
                monto,
                adelanto,
                monto.subtract(adelanto),
                tarea.getEstado() != null ? tarea.getEstado().name() : null,
                tarea.getUsuarioRegistro(),
                tarea.getFechaRegistro(),
                tarea.getFechaEdicion(),
                tarea.getFechaFinalizacion(),
                tarea.getFechaVencimiento(),
                tarea.getTiempoDias(),
                calcularDiasRestantes(tarea),
                calcularColorUrgencia(tarea),
                archivos,
                whatsappUrl,
                resumen,
                historial
        );
    }

    private SeguimientoResponse convertirSeguimientoAResponse(Seguimiento s) {
        List<ArchivoResponse> archivos = s.getArchivos() != null
                ? s.getArchivos().stream().map(this::convertirArchivoSeguimientoAResponse).toList()
                : List.of();

        return new SeguimientoResponse(
                s.getId(),
                s.getTarea() != null ? s.getTarea().getId() : null,
                s.getTipo() != null ? s.getTipo().name() : null,
                s.getDescripcion(),
                s.getUsuario(),
                s.getFechaCreacion(),
                archivos
        );
    }

    private ArchivoResponse convertirArchivoTareaAResponse(ArchivoTarea a) {
        return new ArchivoResponse(a.getId(), a.getDriveFileId(), a.getNombreArchivo(),
                a.getMimeType(), a.getTamanoBytes(), a.getUrlArchivo(),
                a.getFechaSubida(), a.getUsuarioCreacion());
    }

    private ArchivoResponse convertirArchivoSeguimientoAResponse(ArchivoSeguimiento a) {
        return new ArchivoResponse(a.getId(), a.getDriveFileId(), a.getNombreArchivo(),
                a.getMimeType(), a.getTamanoBytes(), a.getUrlArchivo(),
                a.getFechaSubida(), a.getUsuarioCreacion());
    }
}
