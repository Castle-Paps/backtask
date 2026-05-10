package org.example.registrotareas.Entidades;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "imagenesTareas")
@Data
public class ImagenTarea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String nombreArchivo;

    @Column(nullable = false, length = 500)
    private String urlImagen;

    private Long tamanoBytes;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Tarea tarea;
}