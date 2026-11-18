package sv.ledgerflow.api.controlador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sv.ledgerflow.api.dtos.respuesta.RolDto;
import sv.ledgerflow.api.dtos.solicitud.CrearOActualizarRolDto;
import sv.ledgerflow.api.servicio.RolServicio;

import java.util.List;

@Tag(name = "Roles")
@RestController
@RequestMapping("/api/roles")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class RolControlador {

    private final RolServicio servicio;

    public RolControlador(RolServicio servicio) {
        this.servicio = servicio;
    }

    @Operation(summary = "Crear rol")
    @PostMapping
    public ResponseEntity<RolDto> crear(@Valid @RequestBody CrearOActualizarRolDto solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crearRol(solicitud));
    }

    @Operation(summary = "Actualizar rol")
    @PutMapping("/{nombre}")
    public ResponseEntity<RolDto> actualizar(@PathVariable String nombre,
                                             @Valid @RequestBody CrearOActualizarRolDto solicitud) {
        return ResponseEntity.ok(servicio.actualizarRol(nombre, solicitud));
    }

    @Operation(summary = "Listar roles")
    @GetMapping
    public ResponseEntity<List<RolDto>> listar() {
        return ResponseEntity.ok(servicio.obtenerRoles());
    }

    @Operation(summary = "Activar rol")
    @PatchMapping("/{nombre}/activacion")
    public ResponseEntity<RolDto> activar(@PathVariable String nombre) {
        return ResponseEntity.ok(servicio.activarRol(nombre));
    }

    @Operation(summary = "Desactivar rol")
    @PatchMapping("/{nombre}/desactivacion")
    public ResponseEntity<RolDto> desactivar(@PathVariable String nombre) {
        return ResponseEntity.ok(servicio.desactivarRol(nombre));
    }
}
