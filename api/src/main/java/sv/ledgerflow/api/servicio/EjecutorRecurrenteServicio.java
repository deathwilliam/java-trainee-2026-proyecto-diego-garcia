package sv.ledgerflow.api.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.ledgerflow.api.entidad.*;
import sv.ledgerflow.api.dtos.solicitud.Afectacion;
import sv.ledgerflow.api.dtos.solicitud.SolicitudMovimiento;
import sv.ledgerflow.api.dtos.respuesta.ComprobanteDto;
import sv.ledgerflow.api.entidad.enums.*;
import sv.ledgerflow.api.excepciones.RecursoNoEncontradoExcepcion;
import sv.ledgerflow.api.excepciones.ReglaNegocioException;
import sv.ledgerflow.api.repositorio.*;
import sv.ledgerflow.api.util.MotorMovimiento;
import sv.ledgerflow.api.util.ReferenciaGenerator;

import java.time.LocalDate;
import java.util.List;

@Service
public class EjecutorRecurrenteServicio {

    private static final Logger log = LoggerFactory.getLogger(EjecutorRecurrenteServicio.class);

    private final TransaccionRecurrenteRepositorio recurrenteRepositorio;
    private final CuentaRepositorio cuentaRepositorio;
    private final MotorMovimiento motorMovimiento;

    public EjecutorRecurrenteServicio(TransaccionRecurrenteRepositorio transaccionRecurrenteRepositorio, CuentaRepositorio cuentaRepositorio, PresupuestoRepositorio presupuestoRepositorio, TransaccionRepositorio transaccionRepositorio, MetaAhorroRepositorio metaAhorroRepositorio, BitacoraServicio bitacoraServicio, ReferenciaGenerator referenciaGenerator, MotorMovimiento motorMovimiento) {
        this.recurrenteRepositorio = transaccionRecurrenteRepositorio;
        this.cuentaRepositorio = cuentaRepositorio;
        this.motorMovimiento = motorMovimiento;
    }

    @Transactional
    public ComprobanteDto ejecutar(Long recurrenteId, Long usuarioId) {
        LocalDate hoy = LocalDate.now();
        log.debug("Ejecutando recurrente {} (usuario {})", recurrenteId, usuarioId);

        TransaccionRecurrente rec = recurrenteRepositorio
                .buscarParaEjecutar(recurrenteId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("El recurso no pertenece al usuario"));

        if(rec.getEstado() != EstadoRecurrente.ACTIVA) {
            throw new ReglaNegocioException("La transacción recurrente está " + rec.getEstado());
        }

        if (rec.getProximaEjecucion().isAfter(hoy)) {
            throw new ReglaNegocioException("Aún no le toca ejecutarse");
        }

        Cuenta cuenta = cuentaRepositorio.findByIdForUpdate(rec.getCuenta().getId())
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("El recurso no pertenece al usuario"));

        if (!cuenta.estaActiva()) {
            throw new ReglaNegocioException("La cuenta " + cuenta.getNumeroCuenta() + " está " +
                    cuenta.getEstado());
        }

        boolean esEgreso = rec.getTipo().equals(TipoRecurrente.EGRESO);

        Transaccion tx = motorMovimiento.aplicar(new SolicitudMovimiento(
                esEgreso ? TipoTransaccion.EGRESO : TipoTransaccion.INGRESO,
                rec.getCategoria(),
                rec.getMonto(),
                rec.getDescripcion(),
                null,
                List.of(new Afectacion(cuenta,
                        esEgreso ? TipoMovimiento.DEBITO : TipoMovimiento.CREDITO)),
                usuarioId,
                AccionBitacora.EJECUTAR_RECURRENTE,
                "Recurrente " + rec.getId()
        ));

        rec.avanzarProximaEjecucion();

        log.info("Recurrente {} ejecutada: {} ref {} (usuario {}), próxima ejecución {}",
                rec.getId(), rec.getTipo(), tx.getReferencia(), usuarioId, rec.getProximaEjecucion());
        return new ComprobanteDto(tx.getReferencia(), cuenta.getSaldo(), rec.getProximaEjecucion());
    }
}

