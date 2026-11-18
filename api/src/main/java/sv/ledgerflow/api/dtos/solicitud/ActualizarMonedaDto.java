package sv.ledgerflow.api.dtos.solicitud;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ActualizarMonedaDto(
        @Pattern(regexp = ".*\\S.*", message = "El nombre no puede estar vacío")
        @Size(max = 50)
        String nombre,
        @Pattern(regexp = ".*\\S.*", message = "El símbolo no puede estar vacío")
        @Size(max = 5)
        String simbolo
) {
}
