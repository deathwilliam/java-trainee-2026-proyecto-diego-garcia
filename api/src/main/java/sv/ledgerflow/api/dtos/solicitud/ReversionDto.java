package sv.ledgerflow.api.dtos.solicitud;

import jakarta.validation.constraints.NotNull;

public record ReversionDto(
        @NotNull Long transaccionId
) {
}
