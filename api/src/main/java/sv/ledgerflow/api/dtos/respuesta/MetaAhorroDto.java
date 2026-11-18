package sv.ledgerflow.api.dtos.respuesta;

import sv.ledgerflow.api.entidad.enums.EstadoMeta;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MetaAhorroDto(
        Long id,
        Long cuentaId,
        String nombre,
        BigDecimal montoObjetivo,
        LocalDate fechaLimite,
        EstadoMeta estado,
        BigDecimal saldoActual,
        BigDecimal avance
) {
}
