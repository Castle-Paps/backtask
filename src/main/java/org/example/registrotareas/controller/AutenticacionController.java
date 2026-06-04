package org.example.registrotareas.controller;

import jakarta.validation.Valid;
import org.example.registrotareas.dto.LoginRequest;
import org.example.registrotareas.dto.LoginResponse;
import org.example.registrotareas.dto.RegistroUsuarioRequest;
import org.example.registrotareas.dto.UsuarioResponse;
import org.example.registrotareas.entity.Usuario;
import org.example.registrotareas.security.JwtService;
import org.example.registrotareas.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AutenticacionController {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final UsuarioService usuarioService;

    public AutenticacionController(
            AuthenticationManager authManager,
            JwtService jwtService,
            UsuarioService usuarioService
    ) {
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.usuario(), request.contrasena())
            );
        } catch (BadCredentialsException e) {
            throw new IllegalArgumentException("Usuario o contraseña incorrectos");
        }

        Usuario usuario = usuarioService.buscarPorNombreUsuario(request.usuario());
        String token = jwtService.generarToken(usuario.getUsuario());
        String nombre = usuario.getNombre() + " " + usuario.getApellido();

        return ResponseEntity.ok(new LoginResponse(token, usuario.getUsuario(), nombre));
    }

    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponse> registro(@Valid @RequestBody RegistroUsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.registrarUsuario(request));
    }

    /**
     * Indica si el sistema ya tiene al menos un usuario registrado.
     * La app Android lo usa en el primer arranque para decidir si
     * mostrar el registro completo o la pantalla de login.
     */
    @GetMapping("/estado")
    public ResponseEntity<Map<String, Object>> estado() {
        long total = usuarioService.totalUsuarios();
        return ResponseEntity.ok(Map.of(
                "tieneUsuarios", total > 0,
                "totalUsuarios", total
        ));
    }
}
