package sv.ledgerflow.api.dtos.respuesta;

import sv.ledgerflow.api.entidad.enums.EstadoRecurrente;
import sv.ledgerflow.api.entidad.enums.Frecuencia;
import sv.ledgerflow.api.entidad.enums.TipoRecurrente;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurrenteDto(
        Long id,
        Long cuentaId,
        Long categoriaId,
        TipoRecurrente tipo,
        BigDecimal monto,
        String descripcion,
        Frecuencia frecuencia,
        LocalDate proximaEjecucion,
        EstadoRecurrente estado
) {
}
