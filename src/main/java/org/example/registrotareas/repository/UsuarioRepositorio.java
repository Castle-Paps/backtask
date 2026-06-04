package org.example.registrotareas.repository;

import org.example.registrotareas.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {

    /** Búsqueda sin distinción de mayúsculas — "Pedro", "pedro" y "PEDRO" son lo mismo. */
    Optional<Usuario> findByUsuarioIgnoreCase(String usuario);

    boolean existsByUsuarioIgnoreCase(String usuario);
}
