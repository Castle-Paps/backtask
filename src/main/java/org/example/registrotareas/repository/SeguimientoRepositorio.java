package org.example.registrotareas.repository;

import org.example.registrotareas.entity.Seguimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeguimientoRepositorio extends JpaRepository<Seguimiento, Long> {

    List<Seguimiento> findByTareaIdOrderByFechaCreacionAsc(Long tareaId);
}
