package sv.ledgerflow.api.entidad;

import jakarta.persistence.*;
import sv.ledgerflow.api.entidad.enums.EstadoCuenta;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class Cuenta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_cuenta_id", nullable = false)
    private TipoCuenta tipoCuenta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "moneda_id", nullable = false)
    private Moneda moneda;

    @Column(name = "numero_cuenta", nullable = false, unique = true, length = 20)
    private String numeroCuenta;

    @Column(precision = 15, scale = 2, nullable = false, check = @CheckConstraint(constraint = "saldo >= 0"))
    private BigDecimal saldo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCuenta estado;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private final LocalDateTime creadoEn;

    protected Cuenta() {
        this.creadoEn = LocalDateTime.now();
    }

    public Cuenta(Usuario usuario, TipoCuenta tipoCuenta, Moneda moneda, String numeroCuenta, BigDecimal saldo, EstadoCuenta estado) {
        this.usuario = usuario;
        this.tipoCuenta = tipoCuenta;
        this.moneda = moneda;
        this.numeroCuenta = numeroCuenta;
        this.saldo = saldo;
        this.estado = estado;
        this.creadoEn = LocalDateTime.now();
    }

    public boolean perteneceA(Long usuarioId) {
        return usuario.getId().equals(usuarioId);
    }

    public boolean tieneMismaMoneda(Cuenta cuenta) {
        return moneda.getCodigo().equals(cuenta.moneda.getCodigo());
    }

    public boolean tieneSaldoSuficiente(BigDecimal monto) {
        return saldo.compareTo(monto) >= 0;
    }

    public boolean estaActiva() {
        return estado.equals(EstadoCuenta.ACTIVA);
    }

    public void debitar(BigDecimal monto) {
        this.saldo = this.saldo.subtract(monto);
    }

    public void acreditar(BigDecimal monto) {
        this.saldo = this.saldo.add(monto);
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public TipoCuenta getTipoCuenta() {
        return tipoCuenta;
    }

    public Moneda getMoneda() {
        return moneda;
    }

    public String getNumeroCuenta() {
        return numeroCuenta;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public EstadoCuenta getEstado() {
        return estado;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setTipoCuenta(TipoCuenta tipoCuenta) {
        this.tipoCuenta = tipoCuenta;
    }

    public void setMoneda(Moneda moneda) {
        this.moneda = moneda;
    }

    public void setEstado(EstadoCuenta estado) {
        this.estado = estado;
    }
}
