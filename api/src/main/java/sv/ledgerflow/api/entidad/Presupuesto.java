package sv.ledgerflow.api.entidad;

import jakarta.persistence.*;
import sv.ledgerflow.api.entidad.enums.EstadoPresupuesto;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
public class Presupuesto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(name = "monto_limite", precision = 15, scale = 2, nullable = false, check = @CheckConstraint(constraint = "monto_limite > 0"))
    private BigDecimal montoLimite;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPresupuesto estado;

    public Presupuesto() {}

    public Presupuesto(Categoria categoria, BigDecimal montoLimite, LocalDate fechaInicio, LocalDate fechaFin, EstadoPresupuesto estado) {
        this.categoria = categoria;
        this.montoLimite = montoLimite;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.estado = estado;
    }

    public boolean estaActivo() {
        return estado.equals(EstadoPresupuesto.ACTIVO);
    }

    public boolean estaVigente(LocalDate fecha) {
        return !fecha.isBefore(fechaInicio) && !fecha.isAfter(fechaFin);
    }

    public boolean superaLimite(BigDecimal totalConEgreso) {
        return totalConEgreso.compareTo(montoLimite) > 0;
    }

    public Long getId() {
        return id;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public BigDecimal getMontoLimite() {
        return montoLimite;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public EstadoPresupuesto getEstado() {
        return estado;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public void setMontoLimite(BigDecimal montoLimite) {
        this.montoLimite = montoLimite;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public void setEstado(EstadoPresupuesto estado) {
        this.estado = estado;
    }
}
