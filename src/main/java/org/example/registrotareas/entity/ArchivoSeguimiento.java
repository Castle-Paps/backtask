package org.example.registrotareas.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Metadatos de archivos de evidencia en Drive vinculados a un seguimiento/historial.
 */
@Entity
@Table(name = "archivos_seguimiento")
@Data
public class ArchivoSeguimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String driveFileId;

    @Column(nullable = false, length = 255)
    private String nombreArchivo;

    @Column(nullable = false, length = 100)
    private String mimeType;

    private Long tamanoBytes;

    @Column(nullable = false, length = 1000)
    private String urlArchivo;

    @CreationTimestamp
    private LocalDateTime fechaSubida;

    @Column(length = 100)
    private String usuarioCreacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seguimiento_id", nullable = false)
    private Seguimiento seguimiento;
}
