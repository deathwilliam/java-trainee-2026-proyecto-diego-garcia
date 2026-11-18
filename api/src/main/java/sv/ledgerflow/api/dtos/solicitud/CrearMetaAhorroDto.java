package sv.ledgerflow.api.dtos.solicitud;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CrearMetaAhorroDto(
        @NotNull
        Long cuentaId,
        @NotBlank @Size(max = 100)
        String nombre,
        @NotNull @Positive @Digits(integer = 13, fraction = 2)
        BigDecimal montoObjetivo,
        @FutureOrPresent
        LocalDate fechaLimite
) {
}
