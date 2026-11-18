package sv.ledgerflow.api.dtos.solicitud;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearTipoCuentaDto(
        @NotBlank
        @Size(max = 50)
        String nombre,
        @Size(max = 200)
        String descripcion
) {
}
