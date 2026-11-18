package sv.ledgerflow.api.dtos.respuesta;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ComprobanteDto(
        String referencia,
        BigDecimal saldoResultante,
        LocalDate proximaEjecucion
) { }
