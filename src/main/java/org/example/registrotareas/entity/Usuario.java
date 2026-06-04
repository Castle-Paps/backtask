package org.example.registrotareas.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "usuarios")
@Data
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 80)
    private String apellido;

    @Column(nullable = false, unique = true, length = 60)
    private String usuario;

    @Column(nullable = false, length = 120)
    private String contrasena;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RolUsuario rol = RolUsuario.ROLE_USER;

    @Column(nullable = false)
    private boolean enabled = true;
}
