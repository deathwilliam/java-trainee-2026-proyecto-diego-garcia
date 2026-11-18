package sv.ledgerflow.api.entidad;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Moneda {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 3)
    private String codigo;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(length = 5)
    private String simbolo;

    @Column(nullable = false)
    private Boolean activo;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private final LocalDateTime creadoEn;

    public Moneda() {
        this.creadoEn = LocalDateTime.now();
    }

    public Moneda(String codigo, String nombre, String simbolo, Boolean activo) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.simbolo = simbolo;
        this.activo = activo;
        this.creadoEn = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getSimbolo() {
        return simbolo;
    }

    public Boolean getActivo() {
        return activo;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setSimbolo(String simbolo) {
        this.simbolo = simbolo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
