package sv.ledgerflow.api.dtos.solicitud;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TransferenciaDto(
        @NotNull
        Long cuentaOrigenId,
        @NotNull
        Long cuentaDestinoId,
        @NotNull
        @Positive
        @Digits(integer = 13, fraction = 2)
        BigDecimal monto,
        @Size(max = 255)
        String descripcion
) {
}
