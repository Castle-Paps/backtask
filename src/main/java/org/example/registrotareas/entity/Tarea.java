package org.example.registrotareas.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tareas")
@Getter
@Setter
public class Tarea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 20)
    private String numeroTicket;

    @Column(nullable = false, length = 100)
    private String nombreCliente;

    @Column(nullable = false, length = 20)
    private String telefono;

    @Column(nullable = false, length = 500)
    private String motivo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal adelanto = BigDecimal.ZERO;

    private Integer tiempoDias;

    private LocalDate fechaVencimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado = Estado.PENDIENTE;

    @CreationTimestamp
    private LocalDateTime fechaRegistro;

    private LocalDateTime fechaEdicion;

    private LocalDateTime fechaFinalizacion;

    @Column(length = 100)
    private String usuarioRegistro;

    /** Archivos/imágenes almacenados en Google Drive */
    @OneToMany(mappedBy = "tarea", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fechaSubida ASC")
    private List<ArchivoTarea> archivos = new ArrayList<>();

    @OneToMany(mappedBy = "tarea", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fechaCreacion ASC")
    private List<Seguimiento> historial = new ArrayList<>();
}
