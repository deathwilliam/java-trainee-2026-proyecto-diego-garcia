package sv.ledgerflow.api.dtos.respuesta;

import sv.ledgerflow.api.entidad.enums.AccionBitacora;
import sv.ledgerflow.api.entidad.enums.ResultadoBitacora;

import java.time.LocalDateTime;

public record BitacoraDto(
        Long id,
        Long usuarioId,
        AccionBitacora accion,
        ResultadoBitacora resultado,
        String entidad,
        Long entidadId,
        String detalle,
        LocalDateTime fecha
) {
}
