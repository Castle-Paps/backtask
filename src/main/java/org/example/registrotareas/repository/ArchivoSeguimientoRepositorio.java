package org.example.registrotareas.repository;

import org.example.registrotareas.entity.ArchivoSeguimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArchivoSeguimientoRepositorio extends JpaRepository<ArchivoSeguimiento, Long> {

    List<ArchivoSeguimiento> findBySeguimientoIdOrderByFechaSubidaAsc(Long seguimientoId);
}
