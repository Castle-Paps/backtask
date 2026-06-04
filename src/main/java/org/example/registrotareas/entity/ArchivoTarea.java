package org.example.registrotareas.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Metadatos de archivos almacenados en Google Drive vinculados a una tarea.
 * La base de datos NO guarda el binario — solo la referencia al archivo en Drive.
 */
@Entity
@Table(name = "archivos_tarea")
@Data
public class ArchivoTarea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID del archivo en Google Drive */
    @Column(nullable = false, length = 200)
    private String driveFileId;

    @Column(nullable = false, length = 255)
    private String nombreArchivo;

    /** MIME type: image/jpeg, application/pdf, etc. */
    @Column(nullable = false, length = 100)
    private String mimeType;

    private Long tamanoBytes;

    /** URL directa para visualizar/descargar desde Drive */
    @Column(nullable = false, length = 1000)
    private String urlArchivo;

    @CreationTimestamp
    private LocalDateTime fechaSubida;

    @Column(length = 100)
    private String usuarioCreacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tarea_id", nullable = false)
    private Tarea tarea;
}
