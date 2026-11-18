package sv.ledgerflow.api.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import sv.ledgerflow.api.entidad.*;
import sv.ledgerflow.api.dtos.solicitud.Afectacion;
import sv.ledgerflow.api.dtos.solicitud.SolicitudMovimiento;
import sv.ledgerflow.api.entidad.enums.EstadoMeta;
import sv.ledgerflow.api.entidad.enums.EstadoTransaccion;
import sv.ledgerflow.api.entidad.enums.TipoMovimiento;
import sv.ledgerflow.api.entidad.enums.TipoTransaccion;
import sv.ledgerflow.api.excepciones.ReglaNegocioException;
import sv.ledgerflow.api.repositorio.MetaAhorroRepositorio;
import sv.ledgerflow.api.repositorio.PresupuestoRepositorio;
import sv.ledgerflow.api.repositorio.TransaccionRepositorio;
import sv.ledgerflow.api.servicio.BitacoraServicio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class MotorMovimiento {

    private static final Logger log = LoggerFactory.getLogger(MotorMovimiento.class);

    private final TransaccionRepositorio transaccionRepositorio;
    private final PresupuestoRepositorio presupuestoRepositorio;
    private final MetaAhorroRepositorio metaAhorroRepositorio;
    private final BitacoraServicio bitacoraServicio;
    private final ReferenciaGenerator referenciaGenerator;

    public MotorMovimiento(TransaccionRepositorio transaccionRepositorio, PresupuestoRepositorio presupuestoRepositorio, MetaAhorroRepositorio metaAhorroRepositorio, BitacoraServicio bitacoraServicio, ReferenciaGenerator referenciaGenerator) {
        this.transaccionRepositorio = transaccionRepositorio;
        this.presupuestoRepositorio = presupuestoRepositorio;
        this.metaAhorroRepositorio = metaAhorroRepositorio;
        this.bitacoraServicio = bitacoraServicio;
        this.referenciaGenerator = referenciaGenerator;
    }

    //Método que evita la repetición de código al momento de realizar transacciones
    //La propagación es mandatoria, es decir que DEBE de ser llamada dentro de una transacción, sino da error.
    @Transactional(propagation = Propagation.MANDATORY)
    public Transaccion aplicar(SolicitudMovimiento s) {
        //Valida la solicitud
        log.debug("Aplicando {} por {} sobre {} cuenta(s)", s.tipo(), s.monto(), s.afectaciones() == null ? 0 : s.afectaciones().size());
        validar(s);

        //Genera la transacción
        Transaccion tx = new Transaccion();
        tx.setCategoria(s.categoria());
        tx.setReferencia(referenciaGenerator.nueva());
        tx.setTipo(s.tipo());
        tx.setMonto(s.monto());
        tx.setDescripcion(s.descripcion());
        tx.setContraparte(s.contraparte());
        tx.setEstado(EstadoTransaccion.COMPLETADA);
        tx.setFecha(LocalDateTime.now());

        //Por cada afectación se agrega un detalle
        for (Afectacion a : s.afectaciones()) {
            DetalleTransaccion detalleTransaccion = getDetalleTransaccion(s, a);
            tx.agregarDetalle(detalleTransaccion);
        }

        //Se guarda la transacción
        transaccionRepositorio.save(tx);

        //Por cada una de las cuentas afectadas, si se ha alcanzado el objetivo de ahorro, la marca como completado
        s.afectaciones().stream()
                .map(a -> a.cuenta().getId())
                .distinct()
                .forEach(cuentaId ->
                        metaAhorroRepositorio.findByCuentaIdAndEstado(cuentaId, EstadoMeta.ACTIVO)
                                .stream()
                                .filter(MetaAhorro::haAlcanzadoObjetivo)
                                .forEach(meta -> {
                                    meta.completar();
                                    log.info("Meta {} completada (cuenta {})", meta.getId(), cuentaId);
                                }));


        //Genera el detalle para la bitácora, si ya traía un detalle la solicitud, simplemente lo agrega.
        String detalle = s.tipo() + " ref " + tx.getReferencia() + " monto " + s.monto() +
                (s.detalleExtra() == null ? "" : " - " + s.detalleExtra());
        bitacoraServicio.registrarExitoso(s.usuarioId(), s.accion(), "transaccion", tx.getId(), detalle);

        log.info("Transacción {} aplicada: {} ref {} (usuario {})", tx.getId(), s.tipo(), tx.getReferencia(), s.usuarioId());
        return tx;
    }

    //Generación de detalles
    private static DetalleTransaccion getDetalleTransaccion(SolicitudMovimiento s, Afectacion a) {
        Cuenta cuenta = a.cuenta();
        if (a.tipoMovimiento() == TipoMovimiento.DEBITO) {
            cuenta.debitar(s.monto());
        } else {
            cuenta.acreditar(s.monto());
        }

        DetalleTransaccion detalleTransaccion = new DetalleTransaccion();
        detalleTransaccion.setCuenta(cuenta);
        detalleTransaccion.setTipoMovimiento(a.tipoMovimiento());
        detalleTransaccion.setMonto(s.monto());
        detalleTransaccion.setSaldoResultante(cuenta.getSaldo());
        return detalleTransaccion;
    }

    //Validaciones recurrentes
    private void validar(SolicitudMovimiento s) {
        //Verificar que el monto a transferir sea mayor a 0
        if (s.monto() == null || s.monto().signum() <= 0) {
            throw new ReglaNegocioException("El monto debe ser mayor que cero");
        }

        //Verifica si hay movimientos a realizar, si no, ¿para qué vas a transferir?
        if (s.afectaciones() == null || s.afectaciones().isEmpty()) {
            throw new IllegalArgumentException("Un movimiento necesita al menos una afectación");
        }

        //Las transferencias no llevan categoría y los demás movimientos deben de ser del mismo
        //tipo de categoría que la transacción
        Categoria cat = s.categoria();
        if (s.tipo() == TipoTransaccion.TRANSFERENCIA) {
            if (cat != null) {
                throw new ReglaNegocioException("La categoría no corresponde al tipo de transacción");
            }
        } else if (cat != null && !cat.getTipo().name().equals(s.tipo().name())) {
            throw new ReglaNegocioException("La categoría no corresponde al tipo de transacción");
        }

        for (Afectacion a : s.afectaciones()) {
            Cuenta cuenta = a.cuenta();
            //Comprueba que las cuentas estén activas (si no, no se puede hacer el movimiento)
            if (!cuenta.estaActiva()) {
                throw new ReglaNegocioException(
                        "La cuenta " + cuenta.getNumeroCuenta() + " está " + cuenta.getEstado());

            }
            //Comprueba que si se va a realizar un débito, que tenga el saldo suficiente
            if (a.tipoMovimiento() == TipoMovimiento.DEBITO && !cuenta.tieneSaldoSuficiente(s.monto())) {
                throw new ReglaNegocioException("Saldo insuficiente");
            }
        }

        //Si se va a realizar un egreso, se tiene que verificar que cumpla con los presupuestos (si hay)
        if (s.tipo() == TipoTransaccion.EGRESO && cat != null) {
            validarPresupuesto(cat, s.monto(), LocalDate.now());
        }
    }

    //Validación de los presupuestos
    private void validarPresupuesto(Categoria categoria, BigDecimal monto, LocalDate hoy) {
        //Recorre todos los presupuestos relacionados con la categoría
        for (Presupuesto p : presupuestoRepositorio.findVigente(categoria.getId(), hoy)) {
            //Obtiene el gasto acumulado
            BigDecimal acumulado = transaccionRepositorio.sumarEgresosCompletados(
                    categoria.getId(),
                    p.getFechaInicio().atStartOfDay(),
                    p.getFechaFin().plusDays(1).atStartOfDay()
            );
            //Comprueba que el gasto no exceda el presupuesto
            if (p.superaLimite(acumulado.add(monto))) {
                throw new ReglaNegocioException(
                        "El egreso supero el presupuesto de " + categoria.getNombre()
                );
            }
        }
    }
}
