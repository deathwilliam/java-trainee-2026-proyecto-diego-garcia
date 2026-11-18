package sv.ledgerflow.api.controlador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sv.ledgerflow.api.dtos.respuesta.MonedaDto;
import sv.ledgerflow.api.dtos.solicitud.ActualizarMonedaDto;
import sv.ledgerflow.api.dtos.solicitud.CrearMonedaDto;
import sv.ledgerflow.api.servicio.MonedaServicio;

import java.util.List;

@Tag(name = "Monedas")
@RestController
@RequestMapping("/api/monedas")
public class MonedaControlador {

    private final MonedaServicio servicio;

    public MonedaControlador(MonedaServicio servicio) {
        this.servicio = servicio;
    }

    @Operation(summary = "Crear moneda")
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<MonedaDto> crear(@Valid @RequestBody CrearMonedaDto solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crearMoneda(solicitud));
    }

    @Operation(summary = "Actualizar moneda")
    @PutMapping("/{codigo}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<MonedaDto> actualizar(@PathVariable String codigo,
                                                @Valid @RequestBody ActualizarMonedaDto solicitud) {
        return ResponseEntity.ok(servicio.actualizarMoneda(codigo, solicitud));
    }

    @Operation(summary = "Obtener moneda")
    @GetMapping("/{codigo}")
    public ResponseEntity<MonedaDto> obtener(@PathVariable String codigo) {
        return ResponseEntity.ok(servicio.obtenerMoneda(codigo));
    }

    @Operation(summary = "Listar monedas activas")
    @GetMapping
    public ResponseEntity<List<MonedaDto>> listar(@RequestParam(defaultValue = "true") boolean activas) {
        return ResponseEntity.ok(activas ? servicio.obtenerMonedasActivas() : servicio.obtenerMonedas());
    }

    @Operation(summary = "Activar moneda")
    @PatchMapping("/{codigo}/activacion")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<MonedaDto> activar(@PathVariable String codigo) {
        return ResponseEntity.ok(servicio.activarMoneda(codigo));
    }

    @Operation(summary = "Desactivar moneda")
    @PatchMapping("/{codigo}/desactivacion")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<MonedaDto> desactivar(@PathVariable String codigo) {
        return ResponseEntity.ok(servicio.desactivarMoneda(codigo));
    }
}
