package org.example.registrotareas.repository;

import org.example.registrotareas.entity.Estado;
import org.example.registrotareas.entity.Tarea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TareaRepositorio extends JpaRepository<Tarea, Long> {

    /** Búsqueda por nombre del cliente O teléfono (parcial, sin distinción de mayúsculas). */
    @Query("""
            SELECT t FROM Tarea t
            WHERE LOWER(t.nombreCliente) LIKE LOWER(CONCAT('%', :q, '%'))
               OR t.telefono LIKE CONCAT('%', :q, '%')
            ORDER BY t.fechaRegistro DESC
            """)
    List<Tarea> buscar(@Param("q") String q);

    /** Mantiene la firma anterior por compatibilidad. */
    @Query("SELECT t FROM Tarea t WHERE LOWER(t.nombreCliente) LIKE LOWER(CONCAT('%', :nombre, '%'))")
    List<Tarea> buscarPorNombre(@Param("nombre") String nombre);

    @Query("""
            SELECT t FROM Tarea t WHERE t.estado = :estado
            ORDER BY CASE WHEN t.fechaVencimiento IS NULL THEN 1 ELSE 0 END ASC,
                     t.fechaVencimiento ASC
            """)
    List<Tarea> findByEstadoOrdenadoPorUrgencia(@Param("estado") Estado estado);

    List<Tarea> findByEstadoOrderByFechaRegistroDesc(Estado estado);
}
