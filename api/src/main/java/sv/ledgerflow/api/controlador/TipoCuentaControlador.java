package sv.ledgerflow.api.controlador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sv.ledgerflow.api.dtos.respuesta.TipoCuentaDto;
import sv.ledgerflow.api.dtos.solicitud.ActualizarTipoCuentaDto;
import sv.ledgerflow.api.dtos.solicitud.CrearTipoCuentaDto;
import sv.ledgerflow.api.servicio.TipoCuentaServicio;

import java.util.List;

@Tag(name = "Tipos de cuenta")
@RestController
@RequestMapping("/api/tipos-cuenta")
public class TipoCuentaControlador {

    private final TipoCuentaServicio servicio;

    public TipoCuentaControlador(TipoCuentaServicio servicio) {
        this.servicio = servicio;
    }

    @Operation(summary = "Crear tipo de cuenta")
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<TipoCuentaDto> crear(@Valid @RequestBody CrearTipoCuentaDto solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crearTipoCuenta(solicitud));
    }

    @Operation(summary = "Actualizar tipo de cuenta")
    @PutMapping("/{nombre}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<TipoCuentaDto> actualizar(@PathVariable String nombre,
                                                    @Valid @RequestBody ActualizarTipoCuentaDto solicitud) {
        return ResponseEntity.ok(servicio.actualizarTipoCuenta(nombre, solicitud));
    }

    @Operation(summary = "Obtener tipo de cuenta")
    @GetMapping("/{nombre}")
    public ResponseEntity<TipoCuentaDto> obtener(@PathVariable String nombre) {
        return ResponseEntity.ok(servicio.obtenerTipoCuenta(nombre));
    }

    @Operation(summary = "Listar tipos de cuenta")
    @GetMapping
    public ResponseEntity<List<TipoCuentaDto>> listar(@RequestParam(defaultValue = "true") boolean activas) {
        return ResponseEntity.ok(activas ? servicio.obtenerTiposCuentaActivos() : servicio.obtenerTiposCuenta());
    }

    @Operation(summary = "Activar tipo de cuenta")
    @PatchMapping("/{nombre}/activacion")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<TipoCuentaDto> activar(@PathVariable String nombre) {
        return ResponseEntity.ok(servicio.activarTipoCuenta(nombre));
    }

    @Operation(summary = "Desactivar tipo de cuenta")
    @PatchMapping("/{nombre}/desactivacion")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<TipoCuentaDto> desactivar(@PathVariable String nombre) {
        return ResponseEntity.ok(servicio.desactivarTipoCuenta(nombre));
    }
}
