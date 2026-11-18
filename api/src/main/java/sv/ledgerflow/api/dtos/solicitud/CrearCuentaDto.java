package sv.ledgerflow.api.dtos.solicitud;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import sv.ledgerflow.api.entidad.enums.EstadoCuenta;

import java.math.BigDecimal;

public record CrearCuentaDto(
        @NotNull
        Long tipoCuentaId,
        @NotNull
        Long monedaId,
        @Positive
        BigDecimal saldoInicial
){}
