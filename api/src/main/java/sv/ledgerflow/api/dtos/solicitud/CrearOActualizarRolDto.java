package sv.ledgerflow.api.dtos.solicitud;

import jakarta.validation.constraints.NotBlank;

public record CrearOActualizarRolDto(
        @NotBlank
        String nombre
) {
}
