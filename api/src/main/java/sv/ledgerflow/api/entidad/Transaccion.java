package sv.ledgerflow.api.entidad;

import jakarta.persistence.*;
import sv.ledgerflow.api.entidad.enums.EstadoTransaccion;
import sv.ledgerflow.api.entidad.enums.TipoTransaccion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
public class Transaccion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Column(nullable = false, unique = true, length = 30)
    private String referencia;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoTransaccion tipo;

    @Column(precision = 15, scale = 2, nullable = false, check = @CheckConstraint(constraint = "monto > 0"))
    private BigDecimal monto;

    private String descripcion;

    @Column(length = 100)
    private String contraparte;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoTransaccion estado;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fecha;

    @OneToMany(mappedBy = "transaccion", cascade = CascadeType.ALL)
    private final List<DetalleTransaccion> detalles = new ArrayList<>();

    public Transaccion() {
    }

    public Transaccion(Categoria categoria, String referencia, BigDecimal monto, TipoTransaccion tipo, String descripcion, String contraparte, EstadoTransaccion estado, LocalDateTime fecha) {
        this.categoria = categoria;
        this.referencia = referencia;
        this.monto = monto;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.contraparte = contraparte;
        this.estado = estado;
        this.fecha = fecha;
    }

    public void agregarDetalle(DetalleTransaccion detalle) {
        detalles.add(detalle);
        detalle.setTransaccion(this);
    }

    public boolean puedeRevertirse() {
        return EstadoTransaccion.COMPLETADA.equals(estado);
    }

    public void marcarRevertida() {
        if (!puedeRevertirse()) {
            throw new IllegalStateException("Solo se puede revertir una transaccion completada una vez");
        }
        this.estado = EstadoTransaccion.REVERTIDA;
    }

    public Long getId() {
        return id;
    }

    public List<DetalleTransaccion> getDetalles() {
        return Collections.unmodifiableList(detalles);
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public String getReferencia() {
        return referencia;
    }

    public TipoTransaccion getTipo() {
        return tipo;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getContraparte() {
        return contraparte;
    }

    public EstadoTransaccion getEstado() {
        return estado;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public void setTipo(TipoTransaccion tipo) {
        this.tipo = tipo;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public void setEstado(EstadoTransaccion estado) {
        this.estado = estado;
    }

    public void setContraparte(String contraparte) {
        this.contraparte = contraparte;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
}
