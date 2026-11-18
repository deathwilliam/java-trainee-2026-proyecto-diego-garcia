package sv.ledgerflow.api.servicio;

import org.springframework.stereotype.Service;
import sv.ledgerflow.api.dtos.respuesta.ComprobanteDto;
import sv.ledgerflow.api.dtos.solicitud.MovimientoDto;
import sv.ledgerflow.api.dtos.solicitud.TransferenciaDto;
import sv.ledgerflow.api.entidad.enums.AccionBitacora;
import sv.ledgerflow.api.util.OperacionAuditada;

@Service
public class TransferenciaServicio {

    private final EjecutorMovimientoServicio ejecutorMovimientoServicio;
    private final OperacionAuditada operacionAuditada;

    public TransferenciaServicio(EjecutorMovimientoServicio ejecutorMovimientoServicio, BitacoraServicio bitacoraServicio, OperacionAuditada operacionAuditada) {
        this.ejecutorMovimientoServicio = ejecutorMovimientoServicio;
        this.operacionAuditada = operacionAuditada;
    }

    public ComprobanteDto registrarIngreso(Long usuarioId, MovimientoDto movimientoDto) {
        return operacionAuditada.ejecutar(
                usuarioId,
                AccionBitacora.REGISTRAR_MOVIMIENTO,
                "cuenta",
                movimientoDto.cuentaId(),
                () -> ejecutorMovimientoServicio.ingreso(usuarioId, movimientoDto)
        );
    }

    public ComprobanteDto registrarEgreso(Long usuarioId, MovimientoDto movimientoDto) {
        return operacionAuditada.ejecutar(
                usuarioId,
                AccionBitacora.REGISTRAR_MOVIMIENTO,
                "cuenta",
                movimientoDto.cuentaId(),
                () -> ejecutorMovimientoServicio.egreso(usuarioId, movimientoDto)
        );
    }

    public ComprobanteDto transferir(Long usuarioId, TransferenciaDto transferenciaDto) {
        return operacionAuditada.ejecutar(
                usuarioId,
                AccionBitacora.TRANSFERIR,
                "cuenta",
                transferenciaDto.cuentaOrigenId(),
                () -> ejecutorMovimientoServicio.transferir(usuarioId, transferenciaDto)
        );
    }

    public ComprobanteDto revertir(Long usuarioId, Long transaccionId) {
        return operacionAuditada.ejecutar(
                usuarioId,
                AccionBitacora.REVERTIR_TRANSACCION,
                "transaccion",
                transaccionId,
                () -> ejecutorMovimientoServicio.revertir(usuarioId, transaccionId)
        );
    }
}
