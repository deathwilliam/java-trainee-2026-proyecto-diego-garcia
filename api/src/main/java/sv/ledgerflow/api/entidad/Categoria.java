package sv.ledgerflow.api.entidad;

import jakarta.persistence.*;
import sv.ledgerflow.api.entidad.enums.TipoCategoria;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_categoria_usuario_nombre", columnNames = {"usuario_id", "nombre"}))
public class Categoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TipoCategoria tipo;

    public Categoria() {}

    public Categoria(TipoCategoria tipo, String nombre, Usuario usuario) {
        this.tipo = tipo;
        this.nombre = nombre;
        this.usuario = usuario;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public TipoCategoria getTipo() {
        return tipo;
    }

    public void setTipo(TipoCategoria tipo) {
        this.tipo = tipo;
    }
}
