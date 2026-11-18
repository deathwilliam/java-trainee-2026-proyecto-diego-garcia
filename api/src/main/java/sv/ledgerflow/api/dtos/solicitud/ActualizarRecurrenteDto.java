package sv.ledgerflow.api.dtos.solicitud;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import sv.ledgerflow.api.entidad.enums.Frecuencia;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ActualizarRecurrenteDto(
        Long categoriaId,                    // opcional
        @NotNull @Positive @Digits(integer = 13, fraction = 2)
        BigDecimal monto,
        @Size(max = 255)
        String descripcion,
        @NotNull
        Frecuencia frecuencia,
        @NotNull
        LocalDate proximaEjecucion
) {
}
