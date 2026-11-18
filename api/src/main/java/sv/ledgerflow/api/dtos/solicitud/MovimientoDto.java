package sv.ledgerflow.api.dtos.solicitud;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MovimientoDto(
        @NotNull
        Long cuentaId,
        Long categoriaId,
        @NotNull
        @Positive
        @Digits(integer = 15, fraction = 2)
        BigDecimal monto,
        @Size(max = 255)
        String descripcion,
        @Size(max = 100)
        String contraparte
) { }
