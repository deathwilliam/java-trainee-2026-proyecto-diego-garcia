package sv.ledgerflow.api.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import sv.ledgerflow.api.entidad.enums.AccionBitacora;
import sv.ledgerflow.api.servicio.BitacoraServicio;

import java.util.function.Supplier;

@Component
public class OperacionAuditada {

    // El log de esta clase: su nombre indica de dónde salió cada línea.
    private static final Logger log = LoggerFactory.getLogger(OperacionAuditada.class);

    private final BitacoraServicio bitacoraServicio;

    public OperacionAuditada(BitacoraServicio bitacoraServicio) {
        this.bitacoraServicio = bitacoraServicio;
    }

    public <T> T ejecutar(Long usuarioId, AccionBitacora accion, String entidad, Long entidadId, Supplier<T> operacion) {
        try {
            T resultado = operacion.get();
            log.info("Operación realizada: {} sobre {} {} (usuario {})", accion, entidad, entidadId, usuarioId);
            return resultado;
        } catch (RuntimeException e) {
            // WARN: el sistema manejó el fallo (regla de negocio, recurso ajeno...). Sin traza.
            log.warn("Operación rechazada: {} sobre {} {} (usuario {}): {}", accion, entidad, entidadId, usuarioId, e.getMessage());
            try {
                bitacoraServicio.registrarFallido(usuarioId, accion, entidad, entidadId, e.getMessage());
            } catch (RuntimeException ex) {
                // ERROR: no se pudo escribir la bitácora, alguien debe investigarlo.
                log.error("No se pudo registrar en la bitácora la operación {} (usuario {}): {}", accion, usuarioId, ex.getMessage(), ex);
                e.addSuppressed(ex);
            }
            throw e;
        }
    }
}
