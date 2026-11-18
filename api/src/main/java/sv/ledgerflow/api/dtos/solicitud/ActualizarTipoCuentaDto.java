package sv.ledgerflow.api.dtos.solicitud;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ActualizarTipoCuentaDto(
        @Pattern(regexp = ".*\\S.*", message = "El nombre no puede estar vacío")
        @Size(max = 50)
        String nombre,
        @Pattern(regexp = ".*\\S.*", message = "La descripción no puede estar vacía")
        @Size(max = 200)
        String descripcion
) {
}
