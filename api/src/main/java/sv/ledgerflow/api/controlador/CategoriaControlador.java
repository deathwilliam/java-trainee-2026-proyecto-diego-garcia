package sv.ledgerflow.api.controlador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sv.ledgerflow.api.dtos.respuesta.CategoriaDto;
import sv.ledgerflow.api.dtos.solicitud.ActualizarCategoriaDto;
import sv.ledgerflow.api.dtos.solicitud.CrearCategoriaDto;
import sv.ledgerflow.api.entidad.enums.TipoCategoria;
import sv.ledgerflow.api.seguridad.UsuarioAutenticado;
import sv.ledgerflow.api.servicio.CategoriaServicio;

import java.util.List;

@Tag(name = "Categorías", description = "Categorías personales para clasificar movimientos")
@RestController
@RequestMapping("/api/categorias")
public class CategoriaControlador {

    private final CategoriaServicio servicio;
    private final UsuarioAutenticado usuarioAutenticado;

    public CategoriaControlador(CategoriaServicio servicio, UsuarioAutenticado usuarioAutenticado) {
        this.servicio = servicio;
        this.usuarioAutenticado = usuarioAutenticado;
    }

    @Operation(summary = "Crear categoría para un usuario")
    @PostMapping
    public ResponseEntity<CategoriaDto> crear(@RequestParam(required = false) Long usuarioId,
                                              @Valid @RequestBody CrearCategoriaDto solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicio.crearCategoria(usuarioAutenticado.resolver(usuarioId), solicitud));
    }

    @Operation(summary = "Actualizar categoría")
    @PutMapping("/{categoriaId}")
    public ResponseEntity<CategoriaDto> actualizar(@RequestParam(required = false) Long usuarioId,
                                                   @PathVariable Long categoriaId,
                                                   @Valid @RequestBody ActualizarCategoriaDto solicitud) {
        return ResponseEntity.ok(servicio.actualizarCategoria(usuarioAutenticado.resolver(usuarioId), categoriaId, solicitud));
    }

    @Operation(summary = "Listar categorías de un usuario")
    @GetMapping
    public ResponseEntity<List<CategoriaDto>> listar(@RequestParam(required = false) Long usuarioId,
                                                     @RequestParam(required = false) TipoCategoria tipo) {
        return ResponseEntity.ok(servicio.listar(usuarioAutenticado.resolverConsulta(usuarioId), tipo));
    }

    @Operation(summary = "Obtener categoría")
    @GetMapping("/{categoriaId}")
    public ResponseEntity<CategoriaDto> obtener(@RequestParam(required = false) Long usuarioId,
                                                @PathVariable Long categoriaId) {
        return ResponseEntity.ok(servicio.obtener(usuarioAutenticado.resolverConsulta(usuarioId), categoriaId));
    }

    @Operation(summary = "Eliminar categoría sin dependencias")
    @DeleteMapping("/{categoriaId}")
    public ResponseEntity<Void> eliminar(@RequestParam(required = false) Long usuarioId,
                                         @PathVariable Long categoriaId) {
        servicio.eliminar(usuarioAutenticado.resolver(usuarioId), categoriaId);
        return ResponseEntity.noContent().build();
    }
}
