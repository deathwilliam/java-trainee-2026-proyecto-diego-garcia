package sv.ledgerflow.api.dtos.respuesta;

import sv.ledgerflow.api.entidad.enums.EstadoTransaccion;
import sv.ledgerflow.api.entidad.enums.TipoMovimiento;
import sv.ledgerflow.api.entidad.enums.TipoTransaccion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimientoCuentaDto(
        LocalDateTime fecha,
        String referencia,
        TipoTransaccion tipoTransaccion,
        EstadoTransaccion estado,
        TipoMovimiento tipoMovimiento,
        BigDecimal monto,
        BigDecimal saldoResultante,
        String descripcion,
        String contraparte,
        String categoria
) {
}
