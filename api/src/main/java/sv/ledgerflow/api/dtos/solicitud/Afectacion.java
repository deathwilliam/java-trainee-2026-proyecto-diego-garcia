package sv.ledgerflow.api.dtos.solicitud;

import sv.ledgerflow.api.entidad.Cuenta;
import sv.ledgerflow.api.entidad.enums.TipoMovimiento;

public record Afectacion(
        Cuenta cuenta,
        TipoMovimiento tipoMovimiento
) {
}
