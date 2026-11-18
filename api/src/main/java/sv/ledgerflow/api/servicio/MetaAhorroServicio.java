package sv.ledgerflow.api.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.ledgerflow.api.dtos.respuesta.MetaAhorroDto;
import sv.ledgerflow.api.dtos.solicitud.ActualizarMetaAhorroDto;
import sv.ledgerflow.api.dtos.solicitud.CrearMetaAhorroDto;
import sv.ledgerflow.api.entidad.Cuenta;
import sv.ledgerflow.api.entidad.MetaAhorro;
import sv.ledgerflow.api.entidad.enums.EstadoMeta;
import sv.ledgerflow.api.excepciones.RecursoNoEncontradoExcepcion;
import sv.ledgerflow.api.excepciones.ReglaNegocioException;
import sv.ledgerflow.api.repositorio.CuentaRepositorio;
import sv.ledgerflow.api.repositorio.MetaAhorroRepositorio;

import java.util.List;

@Service
public class MetaAhorroServicio {

    private static final Logger log = LoggerFactory.getLogger(MetaAhorroServicio.class);

    private final MetaAhorroRepositorio metaRepository;
    private final CuentaRepositorio cuentaRepository;

    public MetaAhorroServicio(MetaAhorroRepositorio metaRepository, CuentaRepositorio cuentaRepository) {
        this.metaRepository = metaRepository;
        this.cuentaRepository = cuentaRepository;
    }

    @Transactional
    public MetaAhorroDto crear(Long usuarioId, CrearMetaAhorroDto req) {
        Cuenta cuenta = cuentaRepository.findByIdAndUsuarioId(req.cuentaId(), usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("La cuenta no existe para este usuario"));

        if (!cuenta.estaActiva()) {
            throw new ReglaNegocioException(
                    "La cuenta " + cuenta.getNumeroCuenta() + " está " + cuenta.getEstado());
        }

        MetaAhorro m = new MetaAhorro();
        m.setCuenta(cuenta);
        m.setNombre(req.nombre().trim());
        m.setMontoObjetivo(req.montoObjetivo());
        m.setFechaLimite(req.fechaLimite());
        m.setEstado(EstadoMeta.ACTIVO);

        // Si la cuenta ya tiene saldo suficiente, nace COMPLETADO (si no, esperaría
        // a la próxima operación para completarse)
        if (m.haAlcanzadoObjetivo()) {
            m.completar();
        }
        metaRepository.save(m);
        log.info("Meta {} creada (usuario {}, cuenta {}, estado {})", m.getId(), usuarioId, cuenta.getId(), m.getEstado());
        return new MetaAhorroDto(
                m.getId(),
                m.getCuenta().getId(),
                m.getNombre(),
                m.getMontoObjetivo(),
                m.getFechaLimite(),
                m.getEstado(),
                m.getCuenta().getSaldo(),
                m.calcularAvance()
        );
    }

    @Transactional(readOnly = true)
    public List<MetaAhorroDto> listar(Long usuarioId, EstadoMeta estado) {
        List<MetaAhorro> metas = (estado == null)
                ? metaRepository.findByCuentaUsuarioId(usuarioId)
                : metaRepository.findByCuentaUsuarioIdAndEstado(usuarioId, estado);
        return metas.stream().map(m -> new MetaAhorroDto(
                m.getId(),
                m.getCuenta().getId(),
                m.getNombre(),
                m.getMontoObjetivo(),
                m.getFechaLimite(),
                m.getEstado(),
                m.getCuenta().getSaldo(),
                m.calcularAvance()
        )).toList();
    }

    @Transactional(readOnly = true)
    public MetaAhorroDto obtener(Long usuarioId, Long id) {
        MetaAhorro m = propia(usuarioId, id);
        return new MetaAhorroDto(
                m.getId(),
                m.getCuenta().getId(),
                m.getNombre(),
                m.getMontoObjetivo(),
                m.getFechaLimite(),
                m.getEstado(),
                m.getCuenta().getSaldo(),
                m.calcularAvance()
        );
    }

    @Transactional
    public MetaAhorroDto actualizar(Long usuarioId, Long id, ActualizarMetaAhorroDto req) {
        MetaAhorro m = propia(usuarioId, id);
        exigirActiva(m, "modificar");

        m.setNombre(req.nombre().trim());
        m.setMontoObjetivo(req.montoObjetivo());
        m.setFechaLimite(req.fechaLimite());

        // Si se bajó el objetivo por debajo del saldo actual, se completa de inmediato
        if (m.haAlcanzadoObjetivo()) {
            m.completar();
        }
        log.info("Meta {} actualizada (usuario {}, estado {})", id, usuarioId, m.getEstado());

        return new MetaAhorroDto(
                m.getId(),
                m.getCuenta().getId(),
                m.getNombre(),
                m.getMontoObjetivo(),
                m.getFechaLimite(),
                m.getEstado(),
                m.getCuenta().getSaldo(),
                m.calcularAvance()
        );
    }

    @Transactional
    public MetaAhorroDto cancelar(Long usuarioId, Long id) {
        MetaAhorro m = propia(usuarioId, id);
        exigirActiva(m, "cancelar");
        m.setEstado(EstadoMeta.CANCELADO);
        log.info("Meta {} cancelada (usuario {})", id, usuarioId);
        return new MetaAhorroDto(
                m.getId(),
                m.getCuenta().getId(),
                m.getNombre(),
                m.getMontoObjetivo(),
                m.getFechaLimite(),
                m.getEstado(),
                m.getCuenta().getSaldo(),
                m.calcularAvance()
        );
    }


    private MetaAhorro propia(Long usuarioId, Long id) {
        return metaRepository.findByIdAndCuentaUsuarioId(id, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("La meta no existe para este usuario"));
    }

    private void exigirActiva(MetaAhorro m, String accion) {
        if (m.getEstado() != EstadoMeta.ACTIVO) {
            log.warn("Meta {} no se puede {}: está {}", m.getId(), accion, m.getEstado());
            throw new ReglaNegocioException(
                    "No se puede " + accion + " una meta que está " + m.getEstado());
        }
    }
}
