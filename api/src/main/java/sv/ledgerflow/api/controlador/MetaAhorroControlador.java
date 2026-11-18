package sv.ledgerflow.api.controlador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sv.ledgerflow.api.dtos.respuesta.MetaAhorroDto;
import sv.ledgerflow.api.dtos.solicitud.ActualizarMetaAhorroDto;
import sv.ledgerflow.api.dtos.solicitud.CrearMetaAhorroDto;
import sv.ledgerflow.api.entidad.enums.EstadoMeta;
import sv.ledgerflow.api.seguridad.UsuarioAutenticado;
import sv.ledgerflow.api.servicio.MetaAhorroServicio;

import java.util.List;

@Tag(name = "Metas de ahorro", description = "Administración y consulta del avance de metas")
@RestController
@RequestMapping("/api/metas-ahorro")
public class MetaAhorroControlador {

    private final MetaAhorroServicio servicio;
    private final UsuarioAutenticado usuarioAutenticado;

    public MetaAhorroControlador(MetaAhorroServicio servicio, UsuarioAutenticado usuarioAutenticado) {
        this.servicio = servicio;
        this.usuarioAutenticado = usuarioAutenticado;
    }

    @Operation(summary = "Crear meta de ahorro")
    @PostMapping
    public ResponseEntity<MetaAhorroDto> crear(@RequestParam(required = false) Long usuarioId,
                                               @Valid @RequestBody CrearMetaAhorroDto solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(usuarioAutenticado.resolver(usuarioId), solicitud));
    }

    @Operation(summary = "Listar metas de ahorro de un usuario")
    @GetMapping
    public ResponseEntity<List<MetaAhorroDto>> listar(@RequestParam(required = false) Long usuarioId,
                                                      @RequestParam(required = false) EstadoMeta estado) {
        return ResponseEntity.ok(servicio.listar(usuarioAutenticado.resolverConsulta(usuarioId), estado));
    }

    @Operation(summary = "Obtener meta de ahorro")
    @GetMapping("/{metaId}")
    public ResponseEntity<MetaAhorroDto> obtener(@RequestParam(required = false) Long usuarioId,
                                                 @PathVariable Long metaId) {
        return ResponseEntity.ok(servicio.obtener(usuarioAutenticado.resolverConsulta(usuarioId), metaId));
    }

    @Operation(summary = "Actualizar meta de ahorro")
    @PutMapping("/{metaId}")
    public ResponseEntity<MetaAhorroDto> actualizar(@RequestParam(required = false) Long usuarioId,
                                                    @PathVariable Long metaId,
                                                    @Valid @RequestBody ActualizarMetaAhorroDto solicitud) {
        return ResponseEntity.ok(servicio.actualizar(usuarioAutenticado.resolver(usuarioId), metaId, solicitud));
    }

    @Operation(summary = "Cancelar meta de ahorro")
    @PatchMapping("/{metaId}/cancelacion")
    public ResponseEntity<MetaAhorroDto> cancelar(@RequestParam(required = false) Long usuarioId,
                                                  @PathVariable Long metaId) {
        return ResponseEntity.ok(servicio.cancelar(usuarioAutenticado.resolver(usuarioId), metaId));
    }
}
