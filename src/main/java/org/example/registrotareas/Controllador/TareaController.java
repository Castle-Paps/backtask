package org.example.registrotareas.Controllador;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.example.registrotareas.Dto.TareaRequest;
import org.example.registrotareas.Dto.TareaResponse;
import org.example.registrotareas.Servicios.TareaServicios;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/tareas")
public class TareaController {

    private final TareaServicios tareaServicios;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TareaController(TareaServicios tareaServicios) {
        this.tareaServicios = tareaServicios;
    }

    @PostMapping("/nuevaTarea")
    public ResponseEntity<TareaResponse> crearTarea(@Valid @RequestBody TareaRequest request) {
        return ResponseEntity.ok(tareaServicios.crearTarea(request));
    }

    @PostMapping(
            value = "/nuevaTarea-con-imagenes",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<TareaResponse> crearTareaConImagenes(
            @RequestParam("tarea") String tareaJson,
            @RequestParam("imagenes") List<MultipartFile> imagenes
    ) {
        TareaRequest request = parseJsonToTareaRequest(tareaJson);
        return ResponseEntity.ok(tareaServicios.crearTareaConImagenes(request, imagenes));
    }

    @GetMapping
    public ResponseEntity<List<TareaResponse>> listarTareas() {
        return ResponseEntity.ok(tareaServicios.listarTareas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TareaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(tareaServicios.buscarPorId(id));
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<TareaResponse>> buscarPorNombre(@RequestParam String nombre) {
        return ResponseEntity.ok(tareaServicios.buscarPorNombre(nombre));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TareaResponse> actualizarTarea(
            @PathVariable Long id,
            @Valid @RequestBody TareaRequest request
    ) {
        return ResponseEntity.ok(tareaServicios.actualizarTarea(id, request));
    }

    @PostMapping(
            value = "/{id}/imagenes",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarTarea(@PathVariable Long id) {
        tareaServicios.eliminarTarea(id);
        return ResponseEntity.noContent().build();
    }

    private TareaRequest parseJsonToTareaRequest(String json) {
        try {
            return objectMapper.readValue(json, TareaRequest.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Formato JSON inválido para la tarea: " + e.getMessage(), e);
        }
    }
}