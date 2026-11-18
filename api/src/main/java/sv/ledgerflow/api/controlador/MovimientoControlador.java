package sv.ledgerflow.api.controlador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sv.ledgerflow.api.dtos.respuesta.ComprobanteDto;
import sv.ledgerflow.api.dtos.solicitud.MovimientoDto;
import sv.ledgerflow.api.dtos.solicitud.ReversionDto;
import sv.ledgerflow.api.dtos.solicitud.TransferenciaDto;
import sv.ledgerflow.api.seguridad.UsuarioAutenticado;
import sv.ledgerflow.api.servicio.TransferenciaServicio;

@Tag(name = "Movimientos", description = "Ingresos, egresos, transferencias y reversiones")
@RestController
@RequestMapping("/api/movimientos")
public class MovimientoControlador {

    private final TransferenciaServicio servicio;
    private final UsuarioAutenticado usuarioAutenticado;

    public MovimientoControlador(TransferenciaServicio servicio, UsuarioAutenticado usuarioAutenticado) {
        this.servicio = servicio;
        this.usuarioAutenticado = usuarioAutenticado;
    }

    @Operation(summary = "Registrar ingreso")
    @PostMapping("/ingresos")
    public ResponseEntity<ComprobanteDto> registrarIngreso(@RequestParam(required = false) Long usuarioId,
                                                           @Valid @RequestBody MovimientoDto solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicio.registrarIngreso(usuarioAutenticado.resolver(usuarioId), solicitud));
    }

    @Operation(summary = "Registrar egreso")
    @PostMapping("/egresos")
    public ResponseEntity<ComprobanteDto> registrarEgreso(@RequestParam(required = false) Long usuarioId,
                                                          @Valid @RequestBody MovimientoDto solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicio.registrarEgreso(usuarioAutenticado.resolver(usuarioId), solicitud));
    }

    @Operation(summary = "Transferir entre cuentas")
    @PostMapping("/transferencias")
    public ResponseEntity<ComprobanteDto> transferir(@RequestParam(required = false) Long usuarioId,
                                                     @Valid @RequestBody TransferenciaDto solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicio.transferir(usuarioAutenticado.resolver(usuarioId), solicitud));
    }

    @Operation(summary = "Revertir una transacción")
    @PostMapping("/reversiones")
    public ResponseEntity<ComprobanteDto> revertir(@RequestParam(required = false) Long usuarioId,
                                                   @Valid @RequestBody ReversionDto solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicio.revertir(usuarioAutenticado.resolver(usuarioId), solicitud.transaccionId()));
    }
}
