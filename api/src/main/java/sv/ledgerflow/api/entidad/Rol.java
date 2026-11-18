package sv.ledgerflow.api.entidad;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Rol {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private final LocalDateTime creadoEn;

    public Rol() {
        this.creadoEn = LocalDateTime.now();
    }

    public Rol(String nombre, boolean activo) {
        this.nombre = nombre;
        this.activo = activo;
        this.creadoEn = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }
}
