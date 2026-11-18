package sv.ledgerflow.api.dtos.respuesta;

import sv.ledgerflow.api.entidad.enums.TipoCategoria;

public record CategoriaDto(
        Long id,
        String nombre,
        TipoCategoria tipo
) {
}
