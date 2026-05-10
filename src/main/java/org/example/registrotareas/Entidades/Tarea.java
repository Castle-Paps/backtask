package org.example.registrotareas.Entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.Null;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tareas")
@Data
public class Tarea {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombreCliente;

    @Column(nullable = false)
    private String telefono;

    @Column(nullable = false)
    private String motivo;

    @Column(nullable = false)
    private BigDecimal  adelanto;

    @Column(nullable = false)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado;

    @CreationTimestamp
    private LocalDateTime fechaRegistro;


    private LocalDateTime fechaFinalizacion;

    @Enumerated(EnumType.STRING)
    private MetodoPago metodoPago;

    @OneToMany(
            mappedBy = "tarea",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ImagenTarea> imagenes = new ArrayList<>();

}
