package sv.ledgerflow.api.dtos.solicitud;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CrearPresupuestoDto(
        @NotNull
        Long categoriaId,
        @NotNull @Positive @Digits(integer = 13, fraction = 2)
        BigDecimal montoLimite,
        @NotNull
        LocalDate fechaInicio,
        @NotNull
        LocalDate fechaFin
) {
}
