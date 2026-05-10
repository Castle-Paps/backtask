package org.example.registrotareas.Servicios.ImplementacionS;

import jakarta.transaction.Transactional;
import org.example.registrotareas.Dto.ImagenResponse;
import org.example.registrotareas.Dto.TareaRequest;
import org.example.registrotareas.Dto.TareaResponse;
import org.example.registrotareas.Entidades.Estado;
import org.example.registrotareas.Entidades.ImagenTarea;
import org.example.registrotareas.Entidades.Tarea;
import org.example.registrotareas.Repositorios.ImagenRepositorio;
import org.example.registrotareas.Repositorios.TareaRepositorio;
import org.example.registrotareas.Exceptions.ResourceNotFoundException;
import org.example.registrotareas.ResumenWhatsappUtil;
import org.example.registrotareas.Servicios.TareaServicios;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class TareaServiciosImpl implements TareaServicios {

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String UPLOAD_DIR = "uploads/tareas";

    private final TareaRepositorio tareaRepository;
    private final ImagenRepositorio imagenRepositorio;

    public TareaServiciosImpl(TareaRepositorio tareaRepository, ImagenRepositorio imagenRepositorio) {
        this.tareaRepository = tareaRepository;
        this.imagenRepositorio = imagenRepositorio;
    }

    @Override
    public TareaResponse crearTarea(TareaRequest request) {
        Tarea tarea = crearTareaDesdeRequest(request);
        Tarea tareaGuardada = tareaRepository.save(tarea);
        return convertirAResponse(tareaGuardada);
    }

    @Override
    @Transactional
    public TareaResponse crearTareaConImagenes(TareaRequest request, List<MultipartFile> imagenes) {
        validarListaDeImagenes(imagenes);
        
        List<Path> archivosGuardados = new ArrayList<>();
        
        try {
            Tarea tarea = crearTareaDesdeRequest(request);
            Tarea tareaGuardada = tareaRepository.saveAndFlush(tarea);
            
            guardarImagenesEnTarea(tareaGuardada, imagenes, archivosGuardados);
            Tarea tareaFinal = tareaRepository.save(tareaGuardada);
            
            return convertirAResponse(tareaFinal);
            
        } catch (Exception e) {
            borrarArchivosGuardados(archivosGuardados);
            throw new RuntimeException("No se pudo crear la tarea con imágenes: " + e.getMessage(), e);
        }
    }

    @Override
    public List<TareaResponse> listarTareas() {
        return tareaRepository.findAll()
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    @Override
    public TareaResponse buscarPorId(Long id) {
        Tarea tarea = obtenerTareaPorId(id);
        return convertirAResponse(tarea);
    }

    @Override
    public List<TareaResponse> buscarPorNombre(String nombre) {
        List<Tarea> tareas = tareaRepository.findByNombreCliente(nombre);
        
        if (tareas.isEmpty()) {
            throw new ResourceNotFoundException("No se encontraron tareas para el nombre: " + nombre);
        }
        
        return tareas.stream()
                .map(this::convertirAResponse)
                .toList();
    }

    @Override
    public TareaResponse actualizarTarea(Long id, TareaRequest request) {
        Tarea tarea = obtenerTareaPorId(id);
        actualizarTareaDesdeRequest(tarea, request);
        
        Tarea tareaActualizada = tareaRepository.save(tarea);
        return convertirAResponse(tareaActualizada);
    }

    @Override
    @Transactional
    public TareaResponse agregarImagenes(Long tareaId, List<MultipartFile> imagenes) {
        validarListaDeImagenes(imagenes);
        
        Tarea tarea = obtenerTareaPorId(tareaId);
        List<Path> archivosGuardados = new ArrayList<>();
        
        try {
            guardarImagenesEnTarea(tarea, imagenes, archivosGuardados);
            Tarea tareaActualizada = tareaRepository.save(tarea);
            
            return convertirAResponse(tareaActualizada);
            
        } catch (Exception e) {
            borrarArchivosGuardados(archivosGuardados);
            throw new RuntimeException("No se pudieron agregar las imágenes: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public TareaResponse eliminarImagen(Long tareaId, Long imagenId) {
        Tarea tarea = obtenerTareaPorId(tareaId);

        ImagenTarea imagen = imagenRepositorio.findByIdAndTareaId(imagenId, tareaId)
                .orElseThrow(() -> new ResourceNotFoundException("Imagen no encontrada para esta tarea"));

        Path rutaArchivo = obtenerRutaDesdeUrl(imagen.getUrlImagen());

        tarea.getImagenes().remove(imagen);

        imagenRepositorio.delete(imagen);

        try {
            Files.deleteIfExists(rutaArchivo);
        } catch (IOException e) {
            throw new RuntimeException("Se eliminó el registro, pero no se pudo borrar el archivo físico", e);
        }

        Tarea tareaActualizada = tareaRepository.save(tarea);

        return convertirAResponse(tareaActualizada);
    }

    @Override
    @Transactional
    public void eliminarTarea(Long id) {
        Tarea tarea = obtenerTareaPorId(id);
        tareaRepository.delete(tarea);

        Path carpetaTarea = Paths.get(UPLOAD_DIR + "/" + id);

        try {
            if (Files.exists(carpetaTarea)) {
                Files.walk(carpetaTarea)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException ignored) {
                            }
                        });
            }
        } catch (IOException e) {
            throw new RuntimeException("La tarea se eliminó, pero no se pudo borrar la carpeta de imágenes", e);
        }
    }

    private void guardarImagenesEnTarea(
            Tarea tarea,
            List<MultipartFile> imagenes,
            List<Path> archivosGuardados
    ) throws IOException {

        Path carpetaTarea = Paths.get(UPLOAD_DIR + "/" + tarea.getId());
        Files.createDirectories(carpetaTarea);

        for (MultipartFile archivo : imagenes) {
            validarImagen(archivo);

            String nombreOriginal = archivo.getOriginalFilename();
            String extension = obtenerExtension(nombreOriginal);
            String nombreArchivoGuardado = UUID.randomUUID() + extension;

            Path rutaArchivo = carpetaTarea.resolve(nombreArchivoGuardado);

            Files.copy(
                    archivo.getInputStream(),
                    rutaArchivo,
                    StandardCopyOption.REPLACE_EXISTING
            );

            archivosGuardados.add(rutaArchivo);

            String urlImagen = ServletUriComponentsBuilder
                    .fromCurrentContextPath()
                    .path("/uploads/tareas/")
                    .path(String.valueOf(tarea.getId()))
                    .path("/")
                    .path(nombreArchivoGuardado)
                    .toUriString();

            ImagenTarea imagen = new ImagenTarea();
            imagen.setNombreArchivo(nombreOriginal);
            imagen.setUrlImagen(urlImagen);
            imagen.setTamanoBytes(archivo.getSize());
            imagen.setTarea(tarea);

            tarea.getImagenes().add(imagen);
        }
    }

    private void validarImagen(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Una imagen está vacía");
        }

        String tipoContenido = archivo.getContentType();
        if (tipoContenido == null || !tipoContenido.startsWith("image/")) {
            throw new IllegalArgumentException("Solo se permiten archivos de imagen");
        }

        if (archivo.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("La imagen no debe superar los 5MB");
        }
    }

    private String obtenerExtension(String nombreArchivo) {
        if (nombreArchivo == null || !nombreArchivo.contains(".")) {
            return "";
        }

        return nombreArchivo.substring(nombreArchivo.lastIndexOf("."));
    }

    private void borrarArchivosGuardados(List<Path> archivosGuardados) {
        for (Path archivo : archivosGuardados) {
            try {
                Files.deleteIfExists(archivo);
            } catch (IOException ignored) {
            }
        }
    }

    private Path obtenerRutaDesdeUrl(String urlImagen) {
        String marcador = "/uploads/";
        int index = urlImagen.indexOf(marcador);

        if (index == -1) {
            throw new RuntimeException("URL de imagen inválida: " + urlImagen);
        }

        String rutaRelativa = urlImagen.substring(index + 1);

        return Paths.get(rutaRelativa);
    }

    private Tarea obtenerTareaPorId(Long id) {
        return tareaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarea no encontrada con ID: " + id));
    }

    private TareaResponse convertirAResponse(Tarea tarea) {
        List<ImagenResponse> imagenes = tarea.getImagenes()
                .stream()
                .map(this::convertirImagenAResponse)
                .toList();

        String resumenWhatsapp = ResumenWhatsappUtil.generarResumen(tarea);

        return new TareaResponse(
                tarea.getId(),
                tarea.getNombreCliente(),
                tarea.getTelefono(),
                tarea.getMotivo(),
                tarea.getTotal(),
                tarea.getAdelanto(),
                tarea.getEstado().name(),
                tarea.getMetodoPago().name(),
                tarea.getFechaRegistro(),
                tarea.getFechaFinalizacion(),
                imagenes,
                resumenWhatsapp
        );
    }

    private ImagenResponse convertirImagenAResponse(ImagenTarea imagen) {
        return new ImagenResponse(
                imagen.getId(),
                imagen.getNombreArchivo(),
                imagen.getUrlImagen(),
                imagen.getTamanoBytes()
        );
    }

    private Tarea crearTareaDesdeRequest(TareaRequest request) {
        Tarea tarea = new Tarea();
        tarea.setNombreCliente(request.nombreCliente());
        tarea.setTelefono(request.telefono());
        tarea.setMotivo(request.motivo());
        tarea.setTotal(request.total());
        tarea.setAdelanto(request.adelanto());
        tarea.setMetodoPago(request.metodoPago());
        tarea.setEstado(Estado.PROCESO);
        return tarea;
    }

    private void actualizarTareaDesdeRequest(Tarea tarea, TareaRequest request) {
        tarea.setNombreCliente(request.nombreCliente());
        tarea.setTelefono(request.telefono());
        tarea.setMotivo(request.motivo());
        tarea.setTotal(request.total());
        tarea.setAdelanto(request.adelanto());
        tarea.setMetodoPago(request.metodoPago());
    }

    private void validarListaDeImagenes(List<MultipartFile> imagenes) {
        if (imagenes == null || imagenes.isEmpty()) {
            throw new IllegalArgumentException("Debes enviar al menos una imagen");
        }
    }
}