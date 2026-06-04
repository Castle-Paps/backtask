package org.example.registrotareas.controller;

import jakarta.validation.Valid;
import org.example.registrotareas.dto.CrearUsuarioSimpleRequest;
import org.example.registrotareas.dto.UsuarioResponse;
import org.example.registrotareas.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * Crea un usuario operativo con solo usuario+contraseña.
     * El admin accede desde el menú lateral de la app.
     */
    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody CrearUsuarioSimpleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(usuarioService.crearUsuarioSimple(request));
    }

    /** Lista todos los usuarios — solo ROLE_ADMIN (aplicado en SecurityConfig). */
    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(usuarioService.listarUsuarios());
    }

    /**
     * Habilita o deshabilita el acceso de un usuario.
     * El admin no puede deshabilitarse a sí mismo ni al último admin activo.
     */
    @PutMapping("/{id}/acceso")
    public ResponseEntity<UsuarioResponse> toggleAcceso(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.toggleAcceso(id));
    }

    /** Elimina permanentemente un usuario. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        usuarioService.eliminarUsuario(id);
        return ResponseEntity.noContent().build();
    }
}
