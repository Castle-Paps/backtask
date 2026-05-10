package org.example.registrotareas.Repositorios;

import org.example.registrotareas.Entidades.Tarea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TareaRepositorio extends JpaRepository<Tarea, Long> {

    List<Tarea> findByNombreCliente(String nombreCliente);
}
