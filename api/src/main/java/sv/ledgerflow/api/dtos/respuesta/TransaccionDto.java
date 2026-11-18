package sv.ledgerflow.api.dtos.respuesta;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import sv.ledgerflow.api.entidad.enums.TipoTransaccion;

import java.math.BigDecimal;

public record TransaccionDto(
        String categoria,
        TipoTransaccion transaccion,
        BigDecimal monto,
        String descripcion,
        String contraparte
) {

}
