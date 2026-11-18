package sv.ledgerflow.api.dtos.solicitud;

import sv.ledgerflow.api.entidad.Categoria;
import sv.ledgerflow.api.entidad.enums.AccionBitacora;
import sv.ledgerflow.api.entidad.enums.TipoTransaccion;

import java.math.BigDecimal;
import java.util.List;

public record SolicitudMovimiento(
        TipoTransaccion tipo,
        Categoria categoria,
        BigDecimal monto,
        String descripcion,
        String contraparte,
        List<Afectacion> afectaciones,
        Long usuarioId,
        AccionBitacora accion,
        String detalleExtra
) {
}
