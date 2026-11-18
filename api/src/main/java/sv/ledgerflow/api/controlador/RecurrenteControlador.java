package sv.ledgerflow.api.controlador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sv.ledgerflow.api.dtos.respuesta.RecurrenteDto;
import sv.ledgerflow.api.dtos.solicitud.ActualizarRecurrenteDto;
import sv.ledgerflow.api.dtos.solicitud.CrearRecurrenteDto;
import sv.ledgerflow.api.entidad.enums.EstadoRecurrente;
import sv.ledgerflow.api.seguridad.UsuarioAutenticado;
import sv.ledgerflow.api.servicio.RecurrenteServicio;

import java.util.List;

@Tag(name = "Transacciones recurrentes", description = "Administración de movimientos programados")
@RestController
@RequestMapping("/api/recurrentes")
public class RecurrenteControlador {

    private final RecurrenteServicio servicio;
    private final UsuarioAutenticado usuarioAutenticado;

    public RecurrenteControlador(RecurrenteServicio servicio, UsuarioAutenticado usuarioAutenticado) {
        this.servicio = servicio;
        this.usuarioAutenticado = usuarioAutenticado;
    }

    @Operation(summary = "Crear transacción recurrente")
    @PostMapping
    public ResponseEntity<RecurrenteDto> crear(@RequestParam(required = false) Long usuarioId,
                                               @Valid @RequestBody CrearRecurrenteDto solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(usuarioAutenticado.resolver(usuarioId), solicitud));
    }

    @Operation(summary = "Listar transacciones recurrentes de un usuario")
    @GetMapping
    public ResponseEntity<List<RecurrenteDto>> listar(@RequestParam(required = false) Long usuarioId,
                                                      @RequestParam(required = false) EstadoRecurrente estado) {
        return ResponseEntity.ok(servicio.listar(usuarioAutenticado.resolverConsulta(usuarioId), estado));
    }

    @Operation(summary = "Listar transacciones recurrentes pendientes")
    @GetMapping("/pendientes")
    public ResponseEntity<List<RecurrenteDto>> pendientes(@RequestParam(required = false) Long usuarioId) {
        return ResponseEntity.ok(servicio.listarPendientes(usuarioAutenticado.resolverConsulta(usuarioId)));
    }

    @Operation(summary = "Obtener transacción recurrente")
    @GetMapping("/{recurrenteId}")
    public ResponseEntity<RecurrenteDto> obtener(@RequestParam(required = false) Long usuarioId,
                                                 @PathVariable Long recurrenteId) {
        return ResponseEntity.ok(servicio.obtener(usuarioAutenticado.resolverConsulta(usuarioId), recurrenteId));
    }

    @Operation(summary = "Actualizar transacción recurrente")
    @PutMapping("/{recurrenteId}")
    public ResponseEntity<RecurrenteDto> actualizar(@RequestParam(required = false) Long usuarioId,
                                                    @PathVariable Long recurrenteId,
                                                    @Valid @RequestBody ActualizarRecurrenteDto solicitud) {
        return ResponseEntity.ok(servicio.actualizar(usuarioAutenticado.resolver(usuarioId), recurrenteId, solicitud));
    }

    @Operation(summary = "Pausar transacción recurrente")
    @PatchMapping("/{recurrenteId}/pausa")
    public ResponseEntity<RecurrenteDto> pausar(@RequestParam(required = false) Long usuarioId,
                                                @PathVariable Long recurrenteId) {
        return ResponseEntity.ok(servicio.pausar(usuarioAutenticado.resolver(usuarioId), recurrenteId));
    }

    @Operation(summary = "Reanudar transacción recurrente")
    @PatchMapping("/{recurrenteId}/reanudar")
    public ResponseEntity<RecurrenteDto> reanudar(@RequestParam(required = false) Long usuarioId,
                                                  @PathVariable Long recurrenteId) {
        return ResponseEntity.ok(servicio.reanudar(usuarioAutenticado.resolver(usuarioId), recurrenteId));
    }

    @Operation(summary = "Finalizar transacción recurrente")
    @PatchMapping("/{recurrenteId}/finalizacion")
    public ResponseEntity<RecurrenteDto> finalizar(@RequestParam(required = false) Long usuarioId,
                                                   @PathVariable Long recurrenteId) {
        return ResponseEntity.ok(servicio.finalizar(usuarioAutenticado.resolver(usuarioId), recurrenteId));
    }
}
