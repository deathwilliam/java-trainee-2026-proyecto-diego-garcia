package sv.ledgerflow.api.dtos.respuesta;

import java.math.BigDecimal;

public record CuentaDto(
        String numeroCuenta,
        BigDecimal saldo,
        String titular
) {
}
