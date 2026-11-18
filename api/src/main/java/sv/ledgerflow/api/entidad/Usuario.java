package sv.ledgerflow.api.entidad;

import jakarta.persistence.*;
import sv.ledgerflow.api.entidad.enums.EstadoUsuario;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, unique = true, length = 10)
    private String dui;

    @Column(name = "clave_hash", nullable = false)
    private String password;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private EstadoUsuario estado;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private final LocalDateTime creadoEn;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "usuario_rol",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_id")
    )
    private final Set<Rol> roles = new HashSet<>();

    protected Usuario() {
        this.creadoEn = LocalDateTime.now();
    }

    public Usuario(String nombre, String apellido, String email, String dui, String password, EstadoUsuario estado ) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.dui = dui;
        this.password = password;
        this.estado = estado;
        this.creadoEn = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void agregarRol(Rol rol) {
        roles.add(rol);
    }

    public Set<Rol> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getEmail() {
        return email;
    }

    public String getDui() {
        return dui;
    }

    public String getPassword() {
        return password;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public boolean estaActivo() {
        return EstadoUsuario.ACTIVO.equals(estado);
    }
}
