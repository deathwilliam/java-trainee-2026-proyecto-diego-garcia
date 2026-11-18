package sv.ledgerflow.api.controlador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sv.ledgerflow.api.dtos.respuesta.BitacoraDto;
import sv.ledgerflow.api.entidad.enums.AccionBitacora;
import sv.ledgerflow.api.entidad.enums.ResultadoBitacora;
import sv.ledgerflow.api.servicio.BitacoraServicio;

import java.time.LocalDate;

@Tag(name = "Bitácora", description = "Consulta paginada del historial de auditoría")
@RestController
@RequestMapping("/api/bitacora")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'AUDITOR')")
public class BitacoraControlador {

    private final BitacoraServicio servicio;

    public BitacoraControlador(BitacoraServicio servicio) {
        this.servicio = servicio;
    }

    @Operation(summary = "Buscar registros de bitácora",
            description = "Permite filtrar por usuario, acción, resultado y rango de fechas. " +
                    "Por defecto consulta los últimos 30 días.")
    @GetMapping
    public ResponseEntity<Page<BitacoraDto>> buscar(
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) AccionBitacora accion,
            @RequestParam(required = false) ResultadoBitacora resultado,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(servicio.buscar(usuarioId, accion, resultado, desde, hasta, pageable));
    }

    @Operation(summary = "Consultar historial de una entidad")
    @GetMapping("/entidad/{entidad}/{entidadId}")
    public ResponseEntity<Page<BitacoraDto>> historialDeEntidad(
            @PathVariable String entidad,
            @PathVariable Long entidadId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(servicio.historialDeEntidad(entidad, entidadId, pageable));
    }
}
