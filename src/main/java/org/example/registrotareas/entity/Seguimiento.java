package org.example.registrotareas.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "historial")
@Getter
@Setter
public class Seguimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tarea_id", nullable = false)
    private Tarea tarea;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoHistorial tipo;

    @Column(nullable = false, length = 2000)
    private String descripcion;

    @Column(length = 100)
    private String usuario;

    @CreationTimestamp
    private LocalDateTime fechaCreacion;

    /** Archivos de evidencia almacenados en Google Drive */
    @OneToMany(mappedBy = "seguimiento", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fechaSubida ASC")
    private List<ArchivoSeguimiento> archivos = new ArrayList<>();
}
