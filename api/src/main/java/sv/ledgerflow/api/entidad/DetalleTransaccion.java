package sv.ledgerflow.api.entidad;

import jakarta.persistence.*;
import sv.ledgerflow.api.entidad.enums.TipoMovimiento;

import java.math.BigDecimal;

@Entity
@Table(name = "detalle_transaccion")
public class DetalleTransaccion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaccion_id", nullable = false)
    private Transaccion transaccion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimiento", nullable = false, length = 20)
    private TipoMovimiento tipoMovimiento;

    @Column(nullable = false, precision = 15, scale = 2, check = @CheckConstraint(constraint = "monto > 0"))
    private BigDecimal monto;

    @Column(name = "saldo_resultante", nullable = false, precision = 15, scale = 2)
    private BigDecimal saldoResultante;

    public DetalleTransaccion() {
    }

    public DetalleTransaccion(Transaccion transaccion, Cuenta cuenta, TipoMovimiento tipoMovimiento, BigDecimal monto, BigDecimal saldoResultante) {
        this.transaccion = transaccion;
        this.cuenta = cuenta;
        this.tipoMovimiento = tipoMovimiento;
        this.monto = monto;
        this.saldoResultante = saldoResultante;
    }

    public Long getId() {
        return id;
    }

    public Transaccion getTransaccion() {
        return transaccion;
    }

    public Cuenta getCuenta() {
        return cuenta;
    }

    public TipoMovimiento getTipoMovimiento() {
        return tipoMovimiento;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public BigDecimal getSaldoResultante() {
        return saldoResultante;
    }

    public void setTransaccion(Transaccion transaccion) {
        this.transaccion = transaccion;
    }

    public void setCuenta(Cuenta cuenta) {
        this.cuenta = cuenta;
    }

    public void setTipoMovimiento(TipoMovimiento tipoMovimiento) {
        this.tipoMovimiento = tipoMovimiento;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public void setSaldoResultante(BigDecimal saldoResultante) {
        this.saldoResultante = saldoResultante;
    }
}
