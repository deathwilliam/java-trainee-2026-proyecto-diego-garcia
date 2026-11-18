package sv.ledgerflow.api.dtos.solicitud;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import sv.ledgerflow.api.entidad.enums.TipoCategoria;

public record CrearCategoriaDto(
        @NotBlank
        @Size(max = 80)
        String nombre,
        @NotNull
        TipoCategoria tipo
) {
}
