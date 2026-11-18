package sv.ledgerflow.api.controlador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sv.ledgerflow.api.dtos.respuesta.AvancePresupuestoDto;
import sv.ledgerflow.api.dtos.respuesta.PresupuestoDto;
import sv.ledgerflow.api.dtos.solicitud.ActualizarPresupuestoDto;
import sv.ledgerflow.api.dtos.solicitud.CrearPresupuestoDto;
import sv.ledgerflow.api.entidad.enums.EstadoPresupuesto;
import sv.ledgerflow.api.seguridad.UsuarioAutenticado;
import sv.ledgerflow.api.servicio.PresupuestoServicio;

import java.util.List;

@Tag(name = "Presupuestos", description = "Presupuestos por categoría y período")
@RestController
@RequestMapping("/api/presupuestos")
public class PresupuestoControlador {

    private final PresupuestoServicio servicio;
    private final UsuarioAutenticado usuarioAutenticado;

    public PresupuestoControlador(PresupuestoServicio servicio, UsuarioAutenticado usuarioAutenticado) {
        this.servicio = servicio;
        this.usuarioAutenticado = usuarioAutenticado;
    }

    @Operation(summary = "Crear presupuesto")
    @PostMapping
    public ResponseEntity<PresupuestoDto> crear(@RequestParam(required = false) Long usuarioId,
                                                @Valid @RequestBody CrearPresupuestoDto solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(usuarioAutenticado.resolver(usuarioId), solicitud));
    }

    @Operation(summary = "Listar presupuestos de un usuario")
    @GetMapping
    public ResponseEntity<List<PresupuestoDto>> listar(@RequestParam(required = false) Long usuarioId,
                                                       @RequestParam(required = false) EstadoPresupuesto estado) {
        return ResponseEntity.ok(servicio.listar(usuarioAutenticado.resolverConsulta(usuarioId), estado));
    }

    @Operation(summary = "Obtener presupuesto")
    @GetMapping("/{presupuestoId}")
    public ResponseEntity<PresupuestoDto> obtener(@RequestParam(required = false) Long usuarioId,
                                                  @PathVariable Long presupuestoId) {
        return ResponseEntity.ok(servicio.obtener(usuarioAutenticado.resolverConsulta(usuarioId), presupuestoId));
    }

    @Operation(summary = "Actualizar presupuesto")
    @PutMapping("/{presupuestoId}")
    public ResponseEntity<PresupuestoDto> actualizar(@RequestParam(required = false) Long usuarioId,
                                                     @PathVariable Long presupuestoId,
                                                     @Valid @RequestBody ActualizarPresupuestoDto solicitud) {
        return ResponseEntity.ok(servicio.actualizar(usuarioAutenticado.resolver(usuarioId), presupuestoId, solicitud));
    }

    @Operation(summary = "Consultar avance del presupuesto")
    @GetMapping("/{presupuestoId}/avance")
    public ResponseEntity<AvancePresupuestoDto> avance(@RequestParam(required = false) Long usuarioId,
                                                       @PathVariable Long presupuestoId) {
        return ResponseEntity.ok(servicio.consultarAvance(usuarioAutenticado.resolverConsulta(usuarioId), presupuestoId));
    }

    @Operation(summary = "Activar presupuesto")
    @PatchMapping("/{presupuestoId}/activacion")
    public ResponseEntity<PresupuestoDto> activar(@RequestParam(required = false) Long usuarioId,
                                                  @PathVariable Long presupuestoId) {
        return ResponseEntity.ok(servicio.activar(usuarioAutenticado.resolver(usuarioId), presupuestoId));
    }

    @Operation(summary = "Inactivar presupuesto")
    @PatchMapping("/{presupuestoId}/inactivacion")
    public ResponseEntity<PresupuestoDto> inactivar(@RequestParam(required = false) Long usuarioId,
                                                    @PathVariable Long presupuestoId) {
        return ResponseEntity.ok(servicio.inactivar(usuarioAutenticado.resolver(usuarioId), presupuestoId));
    }
}
