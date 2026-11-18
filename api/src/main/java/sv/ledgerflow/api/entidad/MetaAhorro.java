package sv.ledgerflow.api.entidad;

import jakarta.persistence.*;
import sv.ledgerflow.api.entidad.enums.EstadoMeta;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
public class MetaAhorro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "monto_objetivo", precision = 15, scale = 2, nullable = false, check = @CheckConstraint(constraint = "monto_objetivo > 0"))
    private BigDecimal montoObjetivo;

    @Column(name = "fecha_limite")
    private LocalDate fechaLimite;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoMeta estado;

    public MetaAhorro() {}

    public MetaAhorro(Cuenta cuenta, String nombre, BigDecimal montoObjetivo, LocalDate fechaLimite) {
        this.cuenta = cuenta;
        this.nombre = nombre;
        this.montoObjetivo = montoObjetivo;
        this.fechaLimite = fechaLimite;
        this.estado = EstadoMeta.ACTIVO;
    }

    public BigDecimal calcularAvance() {
        return cuenta.getSaldo()
                .multiply(BigDecimal.valueOf(100))
                .divide(montoObjetivo, 2, java.math.RoundingMode.HALF_UP)
                .min(BigDecimal.valueOf(100));
    }

    public boolean haAlcanzadoObjetivo() {
        return cuenta.getSaldo().compareTo(montoObjetivo) >= 0;
    }

    public void completar() {
        this.estado = EstadoMeta.COMPLETADO;
    }

    public Long getId() {
        return id;
    }

    public Cuenta getCuenta() {
        return cuenta;
    }

    public String getNombre() {
        return nombre;
    }

    public BigDecimal getMontoObjetivo() {
        return montoObjetivo;
    }

    public LocalDate getFechaLimite() {
        return fechaLimite;
    }

    public EstadoMeta getEstado() {
        return estado;
    }

    public void setCuenta(Cuenta cuenta) {
        this.cuenta = cuenta;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setMontoObjetivo(BigDecimal montoObjetivo) {
        this.montoObjetivo = montoObjetivo;
    }

    public void setFechaLimite(LocalDate fechaLimite) {
        this.fechaLimite = fechaLimite;
    }

    public void setEstado(EstadoMeta estado) {
        this.estado = estado;
    }
}
