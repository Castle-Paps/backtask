package org.example.registrotareas.Repositorios;

import org.example.registrotareas.Entidades.ImagenTarea;
import org.example.registrotareas.Entidades.Tarea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImagenRepositorio extends JpaRepository<ImagenTarea, Long> {


    Optional<ImagenTarea> findByIdAndTareaId(Long imagenId, Long tareaId);
}
