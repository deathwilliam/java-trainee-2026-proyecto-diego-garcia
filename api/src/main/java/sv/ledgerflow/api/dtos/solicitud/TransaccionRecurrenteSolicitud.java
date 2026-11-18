package sv.ledgerflow.api.dtos.solicitud;

import jakarta.validation.constraints.NotNull;

public record TransaccionRecurrenteSolicitud(
        @NotNull(message = "El id de la transacción es obligatorio")
        Long idTransaccion
) {
}
