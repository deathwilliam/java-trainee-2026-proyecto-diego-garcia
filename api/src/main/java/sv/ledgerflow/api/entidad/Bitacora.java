package sv.ledgerflow.api.entidad;

import jakarta.persistence.*;
import sv.ledgerflow.api.entidad.enums.AccionBitacora;
import sv.ledgerflow.api.entidad.enums.ResultadoBitacora;

import java.time.LocalDateTime;

@Entity
public class Bitacora {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AccionBitacora accion;

    @Column(nullable = false, length = 50)
    private String entidad;

    @Column(name = "entidad_id")
    private Long entidadId;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResultadoBitacora resultado;

    @Column(columnDefinition = "text")
    private String detalle;

    @Column(nullable = false, updatable = false)
    private final LocalDateTime fecha;

    public Bitacora() {
        this.fecha = LocalDateTime.now();
    }

    public Bitacora(Usuario usuario, AccionBitacora accion, String entidad, Long entidadId, ResultadoBitacora resultado, String detalle) {
        this.usuario = usuario;
        this.accion = accion;
        this.entidad = entidad;
        this.entidadId = entidadId;
        this.resultado = resultado;
        this.detalle = detalle;
        this.fecha = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public AccionBitacora getAccion() {
        return accion;
    }

    public ResultadoBitacora getResultado() {
        return resultado;
    }

    public String getEntidad() {
        return entidad;
    }

    public Long getEntidadId() {
        return entidadId;
    }

    public String getDetalle() {
        return detalle;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public void setAccion(AccionBitacora accion) {
        this.accion = accion;
    }

    public void setResultado(ResultadoBitacora resultado) {
        this.resultado = resultado;
    }

    public void setEntidad(String entidad) {
        this.entidad = entidad;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public void setEntidadId(Long entidadId) {
        this.entidadId = entidadId;
    }
}
