package org.example.registrotareas.service;

import org.example.registrotareas.dto.*;
import org.example.registrotareas.dto.whatsapp.WhatsAppEnvioResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TareaServicios {

    TareaResponse crearTarea(TareaRequest request, List<MultipartFile> imagenes);

    List<TareaResponse> listarTareas();

    List<TareaResponse> listarTareasPendientes();

    List<TareaResponse> listarTareasCompletadas();

    List<TareaResponse> listarTareasCanceladas();

    TareaResponse buscarPorId(Long id);

    List<TareaResponse> buscar(String q);
    List<TareaResponse> buscarPorNombre(String nombre);

    TareaResponse actualizarTarea(Long id, TareaRequest request);

    CambioEstadoResponse completarTarea(Long id, String descripcion, List<MultipartFile> imagenes);

    CambioEstadoResponse cancelarTarea(Long id, String descripcion, List<MultipartFile> imagenes);

    SeguimientoResponse agregarAvance(Long tareaId, String descripcion, List<MultipartFile> imagenes);

    List<SeguimientoResponse> listarHistorial(Long tareaId);

    TareaResponse agregarImagenes(Long tareaId, List<MultipartFile> imagenes);

    TareaResponse eliminarImagen(Long tareaId, Long imagenId);

    WhatsAppEnvioResponse generarWhatsappCreacion(Long tareaId);
}
