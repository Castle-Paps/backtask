package org.example.registrotareas.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.example.registrotareas.dto.*;
import org.example.registrotareas.dto.whatsapp.WhatsAppEnvioResponse;
import org.example.registrotareas.service.TareaServicios;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/tareas")
public class TareaController {

    private static final Logger log = LoggerFactory.getLogger(TareaController.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TareaServicios tareaServicios;

    public TareaController(TareaServicios tareaServicios) {
        this.tareaServicios = tareaServicios;
    }

    // ===================== CREAR =====================

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TareaResponse> crearTarea(
            @RequestPart("tarea") String tareaJson,
            @RequestPart(value = "imagenes", required = false) List<MultipartFile> imagenes
    ) {
        TareaRequest request = parsearTarea(tareaJson);
        log.info("POST /tareas - cliente: {}", request.nombreCliente());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tareaServicios.crearTarea(request, imagenes));
    }

    // ===================== LISTAR =====================

    @GetMapping
    public ResponseEntity<List<TareaResponse>> listarTodas() {
        return ResponseEntity.ok(tareaServicios.listarTareas());
    }

    @GetMapping("/pendientes")
    public ResponseEntity<List<TareaResponse>> listarPendientes() {
        return ResponseEntity.ok(tareaServicios.listarTareasPendientes());
    }

    @GetMapping("/completadas")
    public ResponseEntity<List<TareaResponse>> listarCompletadas() {
        return ResponseEntity.ok(tareaServicios.listarTareasCompletadas());
    }

    @GetMapping("/canceladas")
    public ResponseEntity<List<TareaResponse>> listarCanceladas() {
        return ResponseEntity.ok(tareaServicios.listarTareasCanceladas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TareaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(tareaServicios.buscarPorId(id));
    }

    /** Busca por nombre del cliente O teléfono. Ej: GET /tareas/buscar?q=juan */
    @GetMapping("/buscar")
    public ResponseEntity<List<TareaResponse>> buscar(@RequestParam String q) {
        return ResponseEntity.ok(tareaServicios.buscar(q));
    }

    // ===================== ACTUALIZAR =====================

    @PutMapping("/{id}")
    public ResponseEntity<TareaResponse> actualizarTarea(
            @PathVariable Long id,
            @Valid @RequestBody TareaRequest request
    ) {
        return ResponseEntity.ok(tareaServicios.actualizarTarea(id, request));
    }

    // ===================== COMPLETAR / CANCELAR =====================

    @PutMapping(value = "/{id}/completar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CambioEstadoResponse> completarTarea(
            @PathVariable Long id,
            @RequestParam("descripcion") String descripcion,
            @RequestParam(value = "imagenes", required = false) List<MultipartFile> imagenes
    ) {
        log.info("PUT /tareas/{}/completar", id);
        return ResponseEntity.ok(tareaServicios.completarTarea(id, descripcion, imagenes));
    }

    @PutMapping(value = "/{id}/cancelar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CambioEstadoResponse> cancelarTarea(
            @PathVariable Long id,
            @RequestParam("descripcion") String descripcion,
            @RequestParam(value = "imagenes", required = false) List<MultipartFile> imagenes
    ) {
        log.info("PUT /tareas/{}/cancelar", id);
        return ResponseEntity.ok(tareaServicios.cancelarTarea(id, descripcion, imagenes));
    }

    // ===================== HISTORIAL / AVANCE =====================

    @PostMapping(value = "/{id}/avance", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SeguimientoResponse> agregarAvance(
            @PathVariable Long id,
            @RequestParam("descripcion") String descripcion,
            @RequestParam(value = "imagenes", required = false) List<MultipartFile> imagenes
    ) {
        log.info("POST /tareas/{}/avance", id);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tareaServicios.agregarAvance(id, descripcion, imagenes));
    }

    @GetMapping("/{id}/historial")
    public ResponseEntity<List<SeguimientoResponse>> listarHistorial(@PathVariable Long id) {
        return ResponseEntity.ok(tareaServicios.listarHistorial(id));
    }

    // ===================== IMAGENES =====================

    @PostMapping(value = "/{id}/imagenes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TareaResponse> agregarImagenes(
            @PathVariable Long id,
            @RequestParam("imagenes") List<MultipartFile> imagenes
    ) {
        return ResponseEntity.ok(tareaServicios.agregarImagenes(id, imagenes));
    }

    @DeleteMapping("/{id}/imagenes/{imagenId}")
    public ResponseEntity<TareaResponse> eliminarImagen(
            @PathVariable Long id,
            @PathVariable Long imagenId
    ) {
        return ResponseEntity.ok(tareaServicios.eliminarImagen(id, imagenId));
    }

    // ===================== WHATSAPP =====================

    @GetMapping("/{id}/whatsapp")
    public ResponseEntity<WhatsAppEnvioResponse> whatsappCreacion(@PathVariable Long id) {
        return ResponseEntity.ok(tareaServicios.generarWhatsappCreacion(id));
    }

    // ===================== UTIL =====================

    private TareaRequest parsearTarea(String tareaJson) {
        try {
            return objectMapper.readValue(tareaJson, TareaRequest.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException(
                    "El campo 'tarea' debe ser JSON válido. " +
                    "Ejemplo: {\"nombreCliente\":\"Juan\",\"telefono\":\"987654321\"," +
                    "\"motivo\":\"Pantalla rota\",\"monto\":250.00,\"adelanto\":100.00,\"tiempoDias\":3}"
            );
        }
    }
}
