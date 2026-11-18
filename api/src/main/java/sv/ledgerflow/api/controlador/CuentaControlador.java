package sv.ledgerflow.api.controlador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sv.ledgerflow.api.dtos.respuesta.CuentaDto;
import sv.ledgerflow.api.dtos.respuesta.MovimientoCuentaDto;
import sv.ledgerflow.api.dtos.solicitud.CrearCuentaDto;
import sv.ledgerflow.api.seguridad.UsuarioAutenticado;
import sv.ledgerflow.api.servicio.CuentaServicio;

import java.util.List;

@Tag(name = "Cuentas", description = "Creación y consulta de cuentas")
@RestController
@RequestMapping("/api/cuentas")
public class CuentaControlador {

    private final CuentaServicio servicio;
    private final UsuarioAutenticado usuarioAutenticado;

    public CuentaControlador(CuentaServicio servicio, UsuarioAutenticado usuarioAutenticado) {
        this.servicio = servicio;
        this.usuarioAutenticado = usuarioAutenticado;
    }

    @Operation(summary = "Crear cuenta")
    @PostMapping
    public ResponseEntity<CuentaDto> crear(@RequestParam(required = false) Long usuarioId,
                                           @Valid @RequestBody CrearCuentaDto solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicio.crear(usuarioAutenticado.resolver(usuarioId), solicitud));
    }

    @Operation(summary = "Listar cuentas de un usuario")
    @GetMapping
    public ResponseEntity<List<CuentaDto>> listar(@RequestParam(required = false) Long usuarioId) {
        return ResponseEntity.ok(servicio.listarCuentasPropias(usuarioAutenticado.resolverConsulta(usuarioId)));
    }

    @Operation(summary = "Obtener una cuenta de un usuario")
    @GetMapping("/{cuentaId}")
    public ResponseEntity<CuentaDto> obtener(@PathVariable Long cuentaId,
                                             @RequestParam(required = false) Long usuarioId) {
        return ResponseEntity.ok(servicio.obtenerCuenta(usuarioAutenticado.resolverConsulta(usuarioId), cuentaId));
    }

    @Operation(summary = "Bloquear cuenta")
    @PatchMapping("/{cuentaId}/bloqueo")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<CuentaDto> bloquear(@PathVariable Long cuentaId) {
        return ResponseEntity.ok(servicio.bloquearCuenta(cuentaId));
    }

    @Operation(summary = "Desbloquear cuenta")
    @PatchMapping("/{cuentaId}/desbloqueo")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<CuentaDto> desbloquear(@PathVariable Long cuentaId) {
        return ResponseEntity.ok(servicio.desbloquearCuenta(cuentaId));
    }

    @Operation(summary = "Cerrar cuenta")
    @PatchMapping("/{cuentaId}/cierre")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<CuentaDto> cerrar(@PathVariable Long cuentaId) {
        return ResponseEntity.ok(servicio.cerrarCuenta(cuentaId));
    }

    @Operation(summary = "Obtener historial de movimientos")
    @GetMapping("/{cuentaId}/movimientos")
    public ResponseEntity<Page<MovimientoCuentaDto>> historial(
            @RequestParam(required = false) Long usuarioId,
            @PathVariable Long cuentaId,
            @PageableDefault(size = 20) Pageable pageable
            ){
        return ResponseEntity.ok(servicio.historial(usuarioAutenticado.resolverConsulta(usuarioId), cuentaId, pageable));
    }
}
