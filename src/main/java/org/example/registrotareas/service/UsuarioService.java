package org.example.registrotareas.service;

import org.example.registrotareas.dto.CrearUsuarioSimpleRequest;
import org.example.registrotareas.dto.RegistroUsuarioRequest;
import org.example.registrotareas.dto.UsuarioResponse;
import org.example.registrotareas.entity.RolUsuario;
import org.example.registrotareas.entity.Usuario;
import org.example.registrotareas.exception.ResourceNotFoundException;
import org.example.registrotareas.repository.UsuarioRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepositorio usuarioRepositorio;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepositorio usuarioRepositorio, PasswordEncoder passwordEncoder) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.passwordEncoder = passwordEncoder;
    }

    // ─── Spring Security ──────────────────────────────────────────────────────

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepositorio.findByUsuarioIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        if (!usuario.isEnabled()) {
            throw new UsernameNotFoundException("Cuenta deshabilitada. Contacta al administrador.");
        }

        return User.builder()
                .username(usuario.getUsuario())
                .password(usuario.getContrasena())
                .authorities(usuario.getRol().name())
                .build();
    }

    // ─── Registro ─────────────────────────────────────────────────────────────

    /**
     * Reglas de registro:
     *  - Si no hay ningún usuario en el sistema → el primero se crea como ROLE_ADMIN (acceso sin auth).
     *  - Si ya existen usuarios → solo un ROLE_ADMIN autenticado puede registrar nuevos usuarios.
     *  - Los nuevos usuarios creados por el admin reciben ROLE_USER y quedan habilitados.
     */
    @Transactional
    public UsuarioResponse registrarUsuario(RegistroUsuarioRequest request) {
        long totalUsuarios = usuarioRepositorio.count();

        if (totalUsuarios > 0) {
            // Ya hay usuarios: verificar que el solicitante sea ROLE_ADMIN
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean esAdmin = auth != null && auth.isAuthenticated()
                    && !"anonymousUser".equals(auth.getPrincipal())
                    && auth.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (!esAdmin) {
                throw new IllegalArgumentException(
                        "Solo el administrador puede registrar nuevos usuarios. " +
                        "Inicia sesión como administrador.");
            }
        }

        if (usuarioRepositorio.existsByUsuarioIgnoreCase(request.usuario())) {
            throw new IllegalArgumentException("El usuario '" + request.usuario() + "' ya existe");
        }

        // Primer usuario → ROLE_ADMIN; los demás → ROLE_USER
        RolUsuario rol = totalUsuarios == 0 ? RolUsuario.ROLE_ADMIN : RolUsuario.ROLE_USER;

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre().trim());
        usuario.setApellido(request.apellido().trim());
        usuario.setUsuario(request.usuario().trim().toLowerCase()); // siempre minúsculas
        usuario.setContrasena(passwordEncoder.encode(request.contrasena()));
        usuario.setRol(rol);
        usuario.setEnabled(true);

        Usuario guardado = usuarioRepositorio.save(usuario);
        log.info("Usuario '{}' creado con rol {}", guardado.getUsuario(), guardado.getRol());

        return convertirAResponse(guardado);
    }

    // ─── Consultas ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarUsuarios() {
        return usuarioRepositorio.findAll().stream()
                .map(this::convertirAResponse)
                .toList();
    }

    /**
     * Crea un usuario operativo (ROLE_USER) sin requerir nombre ni apellido.
     * Solo el ROLE_ADMIN puede llamar a este método (aplicado en SecurityConfig).
     */
    @Transactional
    public UsuarioResponse crearUsuarioSimple(CrearUsuarioSimpleRequest request) {
        verificarEsAdmin();   // defensa extra además de SecurityConfig
        if (usuarioRepositorio.existsByUsuarioIgnoreCase(request.usuario())) {
            throw new IllegalArgumentException("El usuario '" + request.usuario() + "' ya existe");
        }
        Usuario usuario = new Usuario();
        usuario.setNombre(request.usuario().trim());  // nombre = mismo que el usuario
        usuario.setApellido("");
        usuario.setUsuario(request.usuario().trim().toLowerCase()); // siempre minúsculas
        usuario.setContrasena(passwordEncoder.encode(request.contrasena()));
        usuario.setRol(RolUsuario.ROLE_USER);
        usuario.setEnabled(true);

        Usuario guardado = usuarioRepositorio.save(usuario);
        log.info("Usuario simple '{}' creado con ROLE_USER", guardado.getUsuario());
        return convertirAResponse(guardado);
    }

    public long totalUsuarios() {
        return usuarioRepositorio.count();
    }

    /** Lanza excepción si el usuario autenticado no es ROLE_ADMIN. */
    private void verificarEsAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean esAdmin = auth != null && auth.isAuthenticated()
                && auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!esAdmin) {
            throw new IllegalArgumentException("Solo el administrador puede gestionar usuarios.");
        }
    }

    public Usuario buscarPorNombreUsuario(String username) {
        return usuarioRepositorio.findByUsuarioIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));
    }

    // ─── Habilitar / Deshabilitar ─────────────────────────────────────────────

    /**
     * El admin puede habilitar o deshabilitar cualquier usuario, excepto a sí mismo.
     * Tampoco puede deshabilitar al único admin que queda.
     */
    @Transactional
    public UsuarioResponse toggleAcceso(Long id) {
        Usuario usuario = usuarioRepositorio.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // No puede deshabilitarse a sí mismo
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && usuario.getUsuario().equals(auth.getName())) {
            throw new IllegalArgumentException("No puedes deshabilitar tu propia cuenta");
        }

        // Si es admin y va a ser deshabilitado, verificar que no sea el último admin
        if (usuario.getRol() == RolUsuario.ROLE_ADMIN && usuario.isEnabled()) {
            long adminsActivos = usuarioRepositorio.findAll().stream()
                    .filter(u -> u.getRol() == RolUsuario.ROLE_ADMIN && u.isEnabled() && !u.getId().equals(id))
                    .count();
            if (adminsActivos == 0) {
                throw new IllegalArgumentException(
                        "No puedes deshabilitar al único administrador activo");
            }
        }

        usuario.setEnabled(!usuario.isEnabled());
        Usuario actualizado = usuarioRepositorio.save(usuario);

        log.info("Acceso de '{}' cambiado a {}", actualizado.getUsuario(),
                actualizado.isEnabled() ? "HABILITADO" : "DESHABILITADO");

        return convertirAResponse(actualizado);
    }

    // ─── Eliminar ─────────────────────────────────────────────────────────────

    @Transactional
    public void eliminarUsuario(Long id) {
        if (!usuarioRepositorio.existsById(id)) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + id);
        }
        usuarioRepositorio.deleteById(id);
    }

    // ─── Conversión ───────────────────────────────────────────────────────────

    private UsuarioResponse convertirAResponse(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNombre(), u.getApellido(),
                u.getUsuario(), u.getRol().name(), u.isEnabled());
    }
}
