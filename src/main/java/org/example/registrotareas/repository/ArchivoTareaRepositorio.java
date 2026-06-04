package org.example.registrotareas.repository;

import org.example.registrotareas.entity.ArchivoTarea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArchivoTareaRepositorio extends JpaRepository<ArchivoTarea, Long> {

    List<ArchivoTarea> findByTareaIdOrderByFechaSubidaAsc(Long tareaId);

    Optional<ArchivoTarea> findByIdAndTareaId(Long id, Long tareaId);

    List<ArchivoTarea> findByDriveFileId(String driveFileId);
}
