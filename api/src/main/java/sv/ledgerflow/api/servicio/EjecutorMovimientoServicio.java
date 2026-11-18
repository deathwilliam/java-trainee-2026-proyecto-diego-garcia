package sv.ledgerflow.api.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.ledgerflow.api.entidad.*;
import sv.ledgerflow.api.dtos.solicitud.Afectacion;
import sv.ledgerflow.api.dtos.solicitud.SolicitudMovimiento;
import sv.ledgerflow.api.dtos.respuesta.ComprobanteDto;
import sv.ledgerflow.api.dtos.solicitud.MovimientoDto;
import sv.ledgerflow.api.dtos.solicitud.TransferenciaDto;
import sv.ledgerflow.api.entidad.enums.*;
import sv.ledgerflow.api.excepciones.RecursoNoEncontradoExcepcion;
import sv.ledgerflow.api.excepciones.ReglaNegocioException;
import sv.ledgerflow.api.repositorio.*;
import sv.ledgerflow.api.util.MotorMovimiento;

import java.util.*;

@Service
public class EjecutorMovimientoServicio {

    private static final Logger log = LoggerFactory.getLogger(EjecutorMovimientoServicio.class);
    private final CuentaRepositorio cuentaRepositorio;
    private final CategoriaRepositorio categoriaRepositorio;
    private final TransaccionRepositorio transaccionRepositorio;
    private final DetalleTransaccionRepositorio detalleTransaccionRepositorio;
    private final MotorMovimiento motorMovimiento;

    public EjecutorMovimientoServicio(CuentaRepositorio cuentaRepositorio, CategoriaRepositorio categoriaRepositorio, TransaccionRepositorio transaccionRepositorio, DetalleTransaccionRepositorio detalleTransaccionRepositorio, MotorMovimiento motorMovimiento) {
        this.cuentaRepositorio = cuentaRepositorio;
        this.categoriaRepositorio = categoriaRepositorio;
        this.transaccionRepositorio = transaccionRepositorio;
        this.detalleTransaccionRepositorio = detalleTransaccionRepositorio;
        this.motorMovimiento = motorMovimiento;
    }

    @Transactional
    public ComprobanteDto ingreso(Long usuarioId, MovimientoDto movimientoDto) {
        return movimiento(usuarioId, movimientoDto, TipoTransaccion.INGRESO);
    }

    @Transactional
    public ComprobanteDto egreso(Long usuarioId, MovimientoDto movimientoDto) {
        return movimiento(usuarioId, movimientoDto, TipoTransaccion.EGRESO);
    }

    //Método para realización de movimientos (ingresos y egresos)
    private ComprobanteDto movimiento(Long usuarioId, MovimientoDto movimientoDto, TipoTransaccion tipoTransaccion) {
        //Búsqueda de la cuenta
        Cuenta cuenta = cuentaRepositorio.findByIdForUpdate(movimientoDto.cuentaId())
                .filter(c -> c.perteneceA(usuarioId))
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("La cuenta no existe para este usuario"));

        //Comprobación de las categorías
        Categoria categoria = null;
        if (movimientoDto.categoriaId() != null) {
            categoria = categoriaRepositorio.findByIdAndUsuarioId(movimientoDto.categoriaId(), usuarioId)
                    .orElseThrow(() -> new RecursoNoEncontradoExcepcion("El recurso no pertenece al usuario"));
        }

        //Verificación del tipo de transacción
        boolean esEgreso = tipoTransaccion == TipoTransaccion.EGRESO;

        //Ejecución de la transacción
        Transaccion tx = motorMovimiento.aplicar(new SolicitudMovimiento(
                tipoTransaccion, categoria, movimientoDto.monto(), movimientoDto.descripcion(),
                movimientoDto.contraparte(), List.of(new Afectacion(cuenta,
                    esEgreso ? TipoMovimiento.DEBITO : TipoMovimiento.CREDITO)),
                usuarioId, AccionBitacora.REGISTRAR_MOVIMIENTO, null
        ));

        //Devolución del comprobante
        return new ComprobanteDto(tx.getReferencia(), cuenta.getSaldo(), null);
    }

    @Transactional
    public ComprobanteDto transferir(Long usuarioId, TransferenciaDto transferenciaDto) {
        //Comprobación que la transacción sea entre cuentas distintas
        if (transferenciaDto.cuentaOrigenId().equals(transferenciaDto.cuentaDestinoId())) {
            log.warn("Transferencia rechazada: origen y destino iguales (cuenta {}, usuario {})", transferenciaDto.cuentaOrigenId(), usuarioId);
            throw new ReglaNegocioException("Las cuentas de origen y destino deben ser distintas");
        }

        //Bloqueo de las cuentas afectadas
        Map<Long, Cuenta> cuentas = bloquearPropias(usuarioId, List.of(transferenciaDto.cuentaOrigenId(), transferenciaDto.cuentaDestinoId()));

        //Asignación de las cuentas
        Cuenta origen = cuentas.get(transferenciaDto.cuentaOrigenId());
        Cuenta destino = cuentas.get(transferenciaDto.cuentaDestinoId());

        //Verificación que posean la misma moneda
        if (!origen.tieneMismaMoneda(destino)) {
            log.warn("Transferencia rechazada: monedas distintas (cuentas {} y {}, usuario {})", origen.getId(), destino.getId(), usuarioId);
            throw new ReglaNegocioException("No se puede transferir entre " + origen.getMoneda().getCodigo() + " y "
            +destino.getMoneda().getCodigo());
        }

        //Se efectúa la transferencia entre las dos cuentas
        Transaccion tx = motorMovimiento.aplicar(new SolicitudMovimiento(
                TipoTransaccion.TRANSFERENCIA,
                null,
                transferenciaDto.monto(),
                transferenciaDto.descripcion(),
                null,
                List.of(new Afectacion(origen, TipoMovimiento.DEBITO),
                        new Afectacion(destino, TipoMovimiento.CREDITO)),
                usuarioId,
                AccionBitacora.TRANSFERIR,
                null
        ));

        //Devolución del comprobante
        return new ComprobanteDto(tx.getReferencia(), origen.getSaldo(), null);
    }

    @Transactional
    public ComprobanteDto revertir(Long usuarioId, Long transaccionId) {
        //Verificación de la existencia de la transacción a revertir
        Transaccion original = transaccionRepositorio.findByIdForUpdate(transaccionId)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("El recurso no pertenece al usuario"));

        //Obtención de las cuentas afectadas en el movimiento
        List<Long> cuentaIds = detalleTransaccionRepositorio.findCuenta_IdByTransaccion_Id(transaccionId);
        Map<Long, Cuenta> cuentas = bloquearPropias(usuarioId, cuentaIds);

        //Verificación que no se hayan revertido antes
        if (!original.puedeRevertirse()) {
            log.warn("Reversión rechazada: la transacción {} ya fue revertida (usuario {})", transaccionId, usuarioId);
            throw new ReglaNegocioException("La transacción ya fue REVERTIDA");
        }

        //Obtención de los detalles para bitácora
        List<DetalleTransaccion> detalles = original.getDetalles().stream()
                .sorted(Comparator.comparing(DetalleTransaccion::getId))
                .toList();
        List<Afectacion> inversas = detalles.stream()
                .map(d -> new Afectacion(cuentas.get(d.getCuenta().getId()),
                        d.getTipoMovimiento() == TipoMovimiento.DEBITO ? TipoMovimiento.CREDITO : TipoMovimiento.DEBITO))
                .toList();

        TipoTransaccion tipoReversion = switch (original.getTipo()) {
            case INGRESO -> TipoTransaccion.EGRESO;
            case EGRESO -> TipoTransaccion.INGRESO;
            case TRANSFERENCIA -> TipoTransaccion.TRANSFERENCIA;
        };

        //Se efectúa la reversión
        Transaccion reversion = motorMovimiento.aplicar(new SolicitudMovimiento(
                tipoReversion,
                null,
                original.getMonto(),
                "Reversión de " + original.getReferencia(),
                original.getContraparte(),
                inversas,
                usuarioId,
                AccionBitacora.REVERTIR_TRANSACCION,
                "Revierte " + original.getReferencia()
        ));

        //Se marca la transacción como revertida
        original.marcarRevertida();
        log.info("Transacción {} ({}) revertida por la {} (usuario {})", original.getId(), original.getReferencia(), reversion.getReferencia(), usuarioId);

        //Se devuelve el comprobante de transacción
        Cuenta principal = cuentas.get(detalles.getFirst().getCuenta().getId());
        return new ComprobanteDto(reversion.getReferencia(), principal.getSaldo(), null);
    }

    //Permite bloquear las cuentas en orden de Id, de esta manera no hay deadlocks provocados por carga de cuentas por otros medios
    private Map<Long, Cuenta> bloquearPropias(Long usuarioId, Collection<Long> ids) {
        Map<Long, Cuenta> cuentas = new LinkedHashMap<>();
        for (Long id : new TreeSet<>(ids)) {
            Cuenta cuenta = cuentaRepositorio.findByIdForUpdate(id)
                    .filter(c -> c.perteneceA(usuarioId))
                    .orElseThrow(() -> new RecursoNoEncontradoExcepcion("El recurso no pertenece al usuario"));
            cuentas.put(id, cuenta);
        }
        return cuentas;
    }
}
