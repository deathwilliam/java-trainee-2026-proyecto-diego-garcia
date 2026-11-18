package sv.ledgerflow.api.dtos.respuesta;

import sv.ledgerflow.api.entidad.enums.EstadoPresupuesto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PresupuestoDto(
        Long id,
        Long categoriaId,
        String categoria,
        BigDecimal montoLimite,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        EstadoPresupuesto estado
) {
}
