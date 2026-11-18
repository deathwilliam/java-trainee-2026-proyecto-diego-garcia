package sv.ledgerflow.api.entidad;

import jakarta.persistence.*;
import sv.ledgerflow.api.entidad.enums.EstadoRecurrente;
import sv.ledgerflow.api.entidad.enums.Frecuencia;
import sv.ledgerflow.api.entidad.enums.TipoRecurrente;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transaccion_recurrente")
public class TransaccionRecurrente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoRecurrente tipo;

    @Column(precision = 15, scale = 2, nullable = false, check = @CheckConstraint(constraint = "monto > 0"))
    private BigDecimal monto;

    private String descripcion;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Frecuencia frecuencia;

    @Column(name = "proxima_ejecucion", nullable = false)
    private LocalDate proximaEjecucion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoRecurrente estado;

    public TransaccionRecurrente() {
    }

    public TransaccionRecurrente(Cuenta cuenta, Categoria categoria,
                                 TipoRecurrente tipo, BigDecimal monto,
                                 String descripcion, Frecuencia frecuencia,
                                 LocalDate proximaEjecucion, EstadoRecurrente estado) {
        this.cuenta = cuenta;
        this.categoria = categoria;
        this.tipo = tipo;
        this.monto = monto;
        this.descripcion = descripcion;
        this.frecuencia = frecuencia;
        this.proximaEjecucion = proximaEjecucion;
        this.estado = estado;
    }

    public boolean puedeEjecutarse(LocalDate hoy) {
        return !proximaEjecucion.isAfter(hoy) && EstadoRecurrente.ACTIVA.equals(estado);
    }

    public void avanzarProximaEjecucion() {
        this.proximaEjecucion = frecuencia.siguiente(proximaEjecucion);
    }

    public Long getId() {
        return id;
    }

    public Cuenta getCuenta() {
        return cuenta;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public TipoRecurrente getTipo() {
        return tipo;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Frecuencia getFrecuencia() {
        return frecuencia;
    }

    public LocalDate getProximaEjecucion() {
        return proximaEjecucion;
    }

    public EstadoRecurrente getEstado() {
        return estado;
    }

    public void setCuenta(Cuenta cuenta) {
        this.cuenta = cuenta;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public void setTipo(TipoRecurrente tipo) {
        this.tipo = tipo;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setFrecuencia(Frecuencia frecuencia) {
        this.frecuencia = frecuencia;
    }

    public void setProximaEjecucion(LocalDate proximaEjecucion) {
        this.proximaEjecucion = proximaEjecucion;
    }

    public void setEstado(EstadoRecurrente estado) {
        this.estado = estado;
    }
}
