package sv.ledgerflow.api.controlador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sv.ledgerflow.api.dtos.respuesta.ComprobanteDto;
import sv.ledgerflow.api.dtos.solicitud.TransaccionRecurrenteSolicitud;
import sv.ledgerflow.api.seguridad.UsuarioAutenticado;
import sv.ledgerflow.api.servicio.RecurrenteServicio;

@Tag(name = "Transacción Recurrente", description = "La operación principal: ejecutar una transacción recurrente")
@RestController
@RequestMapping("/api/ejecuciones")
public class TransaccionRecurrenteControlador {

    private final RecurrenteServicio servicio;
    private final UsuarioAutenticado usuarioAutenticado;

    public TransaccionRecurrenteControlador(RecurrenteServicio servicio, UsuarioAutenticado usuarioAutenticado) {
        this.servicio = servicio;
        this.usuarioAutenticado = usuarioAutenticado;
    }

    @Operation(summary = "Ejecución de una transacción recurrente",
    description = "201 si se ejecuto correctamente; 400 si una regla de negocio no se cumplió;" +
            " 404 si un recurso no se encuentra o no es un recurso del usuario")
    @PostMapping
    public ResponseEntity<ComprobanteDto> ejecutarRecurrente(@RequestParam(required = false) Long usuarioId,
                                                             @Valid @RequestBody TransaccionRecurrenteSolicitud req) {
        ComprobanteDto comprobante = servicio.ejecutar(req.idTransaccion(), usuarioAutenticado.resolver(usuarioId));
        return ResponseEntity.status(HttpStatus.CREATED).body(comprobante);
    }


}
