package sv.ledgerflow.api.dtos.solicitud;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearMonedaDto(
        @NotBlank(message = "El código de la moneda es obligatorio")
        @Size(max = 3)
        String codigo,
        @NotBlank(message = "El nombre de la moneda es obligatorio")
        @Size(max = 50)
        String nombre,
        @Size(max = 5)
        String simbolo
) { }
