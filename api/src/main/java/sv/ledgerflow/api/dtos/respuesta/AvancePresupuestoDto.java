package sv.ledgerflow.api.dtos.respuesta;

import java.math.BigDecimal;

public record AvancePresupuestoDto(
        Long presupuestoId,
        BigDecimal montoLimite,
        BigDecimal gastado,
        BigDecimal restante,
        BigDecimal porcentaje,
        boolean superado
) {
}
