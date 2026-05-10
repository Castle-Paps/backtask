package org.example.registrotareas.Servicios;


import org.example.registrotareas.Dto.TareaRequest;
import org.example.registrotareas.Dto.TareaResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TareaServicios {

    TareaResponse crearTarea(TareaRequest request);

    TareaResponse crearTareaConImagenes(TareaRequest request, List<MultipartFile> imagenes);

    List<TareaResponse> listarTareas();

    TareaResponse buscarPorId(Long id);

    List<TareaResponse> buscarPorNombre(String nombre);

    TareaResponse actualizarTarea(Long id, TareaRequest request);

    TareaResponse agregarImagenes(Long tareaId, List<MultipartFile> imagenes);

    TareaResponse eliminarImagen(Long tareaId, Long imagenId);

    void eliminarTarea(Long id);

}
