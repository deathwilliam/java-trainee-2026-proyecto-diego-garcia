package sv.ledgerflow.api.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.ledgerflow.api.dtos.respuesta.ComprobanteDto;
import sv.ledgerflow.api.dtos.respuesta.RecurrenteDto;
import sv.ledgerflow.api.dtos.solicitud.ActualizarRecurrenteDto;
import sv.ledgerflow.api.dtos.solicitud.CrearRecurrenteDto;
import sv.ledgerflow.api.entidad.Categoria;
import sv.ledgerflow.api.entidad.Cuenta;
import sv.ledgerflow.api.entidad.TransaccionRecurrente;
import sv.ledgerflow.api.entidad.enums.AccionBitacora;
import sv.ledgerflow.api.entidad.enums.EstadoRecurrente;
import sv.ledgerflow.api.entidad.enums.TipoRecurrente;
import sv.ledgerflow.api.excepciones.RecursoNoEncontradoExcepcion;
import sv.ledgerflow.api.excepciones.ReglaNegocioException;
import sv.ledgerflow.api.repositorio.CategoriaRepositorio;
import sv.ledgerflow.api.repositorio.CuentaRepositorio;
import sv.ledgerflow.api.repositorio.TransaccionRecurrenteRepositorio;
import sv.ledgerflow.api.util.OperacionAuditada;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
public class RecurrenteServicio {

    private static final Logger log = LoggerFactory.getLogger(RecurrenteServicio.class);
    private final EjecutorRecurrenteServicio ejecutor;
    private final TransaccionRecurrenteRepositorio recurrenteRepositorio;
    private final CategoriaRepositorio categoriaRepositorio;
    private final CuentaRepositorio cuentaRepositorio;
    private final OperacionAuditada operacionAuditada;

    public RecurrenteServicio(EjecutorRecurrenteServicio ejecutor, TransaccionRecurrenteRepositorio recurrenteRepositorio, CategoriaRepositorio categoriaRepositorio, CuentaRepositorio cuentaRepositorio, OperacionAuditada operacionAuditada) {
        this.ejecutor = ejecutor;
        this.recurrenteRepositorio = recurrenteRepositorio;
        this.categoriaRepositorio = categoriaRepositorio;
        this.cuentaRepositorio = cuentaRepositorio;
        this.operacionAuditada = operacionAuditada;
    }

    public ComprobanteDto ejecutar(Long recurrenteId, Long usuarioId) {
        return operacionAuditada.ejecutar(
                usuarioId,
                AccionBitacora.EJECUTAR_RECURRENTE,
                "transaccion_recurrente",
                recurrenteId,
                () -> ejecutor.ejecutar(recurrenteId, usuarioId)
        );
    }

    @Transactional
    public RecurrenteDto crear(Long usuarioId, CrearRecurrenteDto req) {

        Cuenta cuenta = cuentaRepositorio.findByIdAndUsuarioId(req.cuentaId(), usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("La cuenta no existe para este usuario"));


        if (!cuenta.estaActiva()) {
            throw new ReglaNegocioException(
                    "La cuenta " + cuenta.getNumeroCuenta() + " está " + cuenta.getEstado());
        }

        Categoria categoria = categoriaCompatible(usuarioId, req.categoriaId(), req.tipo()); // R4 + R7

        if (req.proximaEjecucion().isBefore(LocalDate.now())) {
            throw new ReglaNegocioException("La próxima ejecución no puede estar en el pasado");
        }

        TransaccionRecurrente r = new TransaccionRecurrente();
        r.setCuenta(cuenta);
        r.setCategoria(categoria);
        r.setTipo(req.tipo());
        r.setMonto(req.monto());
        r.setDescripcion(req.descripcion());
        r.setFrecuencia(req.frecuencia());
        r.setProximaEjecucion(req.proximaEjecucion());
        r.setEstado(EstadoRecurrente.ACTIVA);

        recurrenteRepositorio.save(r);
        log.info("Recurrente {} creada (usuario {}, {}, {})", r.getId(), usuarioId, r.getTipo(), r.getFrecuencia());

        return new RecurrenteDto(
                r.getId(), r.getCuenta().getId(),
                r.getCategoria() == null ? null : r.getCategoria().getId(),
                r.getTipo(), r.getMonto(), r.getDescripcion(), r.getFrecuencia(),
                r.getProximaEjecucion(), r.getEstado()
        );
    }


    @Transactional(readOnly = true)
    public List<RecurrenteDto> listar(Long usuarioId, EstadoRecurrente estado) {
        List<TransaccionRecurrente> lista = (estado == null)
                ? recurrenteRepositorio.findByCuentaUsuarioId(usuarioId)
                : recurrenteRepositorio.findByCuentaUsuarioIdAndEstado(usuarioId, estado);
        return lista.stream().map(r -> new RecurrenteDto(
                r.getId(), r.getCuenta().getId(),
                r.getCategoria() == null ? null : r.getCategoria().getId(),
                r.getTipo(), r.getMonto(), r.getDescripcion(), r.getFrecuencia(),
                r.getProximaEjecucion(), r.getEstado()
        )).toList();
    }


    @Transactional(readOnly = true)
    public List<RecurrenteDto> listarPendientes(Long usuarioId) {
        return recurrenteRepositorio
                .pendientes(usuarioId, EstadoRecurrente.ACTIVA, LocalDate.now())
                .stream().map(r -> new RecurrenteDto(
                r.getId(), r.getCuenta().getId(),
                r.getCategoria() == null ? null : r.getCategoria().getId(),
                r.getTipo(), r.getMonto(), r.getDescripcion(), r.getFrecuencia(),
                r.getProximaEjecucion(), r.getEstado()
        )).toList();
    }

    @Transactional(readOnly = true)
    public RecurrenteDto obtener(Long usuarioId, Long id) {
        TransaccionRecurrente r = recurrenteRepositorio.findByIdAndCuentaUsuarioId(id, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("La transacción recurrente no existe para este usuario"));

        return new RecurrenteDto(
                r.getId(), r.getCuenta().getId(),
                r.getCategoria() == null ? null : r.getCategoria().getId(),
                r.getTipo(), r.getMonto(), r.getDescripcion(), r.getFrecuencia(),
                r.getProximaEjecucion(), r.getEstado()
        );
    }

    @Transactional
    public RecurrenteDto actualizar(Long usuarioId, Long id, ActualizarRecurrenteDto req) {
        TransaccionRecurrente r = bloquearPropia(usuarioId, id);

        if (r.getEstado() == EstadoRecurrente.FINALIZADA) {
            throw new ReglaNegocioException("No se puede modificar una recurrente FINALIZADA");
        }

        // Una recurrente atrasada conserva su fecha; solo una fecha NUEVA no puede ser pasada
        if (!req.proximaEjecucion().equals(r.getProximaEjecucion())
                && req.proximaEjecucion().isBefore(LocalDate.now())) {
            throw new ReglaNegocioException("La próxima ejecución no puede estar en el pasado");
        }

        r.setCategoria(categoriaCompatible(usuarioId, req.categoriaId(), r.getTipo())); // R4 + R7
        r.setMonto(req.monto());
        r.setDescripcion(req.descripcion());
        r.setFrecuencia(req.frecuencia());
        r.setProximaEjecucion(req.proximaEjecucion());
        log.info("Recurrente {} actualizada (usuario {})", id, usuarioId);

        return new RecurrenteDto(
                r.getId(), r.getCuenta().getId(),
                r.getCategoria() == null ? null : r.getCategoria().getId(),
                r.getTipo(), r.getMonto(), r.getDescripcion(), r.getFrecuencia(),
                r.getProximaEjecucion(), r.getEstado()
        );
    }

    @Transactional
    public RecurrenteDto pausar(Long usuarioId, Long id) {
        return transicion(usuarioId, id, Set.of(EstadoRecurrente.ACTIVA), EstadoRecurrente.PAUSADA);
    }

    @Transactional
    public RecurrenteDto reanudar(Long usuarioId, Long id) {
        return transicion(usuarioId, id, Set.of(EstadoRecurrente.PAUSADA), EstadoRecurrente.ACTIVA);
    }

    /** Estado final: una FINALIZADA no vuelve a ejecutarse ni a editarse. */
    @Transactional
    public RecurrenteDto finalizar(Long usuarioId, Long id) {
        return transicion(usuarioId, id,
                Set.of(EstadoRecurrente.ACTIVA, EstadoRecurrente.PAUSADA), EstadoRecurrente.FINALIZADA);
    }

    private RecurrenteDto transicion(Long usuarioId, Long id,
                                 Set<EstadoRecurrente> permitidos, EstadoRecurrente nuevo) {
        TransaccionRecurrente r = bloquearPropia(usuarioId, id);
        if (!permitidos.contains(r.getEstado())) {
            log.warn("Transición rechazada: recurrente {} de {} a {} (usuario {})", id, r.getEstado(), nuevo, usuarioId);
            throw new ReglaNegocioException(
                    "No se puede pasar una recurrente de " + r.getEstado() + " a " + nuevo);
        }
        r.setEstado(nuevo);
        log.info("Recurrente {} pasó a {} (usuario {})", id, nuevo, usuarioId);
        return new RecurrenteDto(
                r.getId(), r.getCuenta().getId(),
                r.getCategoria() == null ? null : r.getCategoria().getId(),
                r.getTipo(), r.getMonto(), r.getDescripcion(), r.getFrecuencia(),
                r.getProximaEjecucion(), r.getEstado()
        );
    }

    private TransaccionRecurrente bloquearPropia(Long usuarioId, Long id) {
        return recurrenteRepositorio.buscarParaEjecutar(id, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion(
                        "La transacción recurrente no existe para este usuario"));
    }

    private Categoria categoriaCompatible(Long usuarioId, Long categoriaId, TipoRecurrente tipo) {
        if (categoriaId == null) {
            return null;
        }
        Categoria cat = categoriaRepositorio.findByIdAndUsuarioId(categoriaId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("La categoría no existe para este usuario"));
        if (!cat.getTipo().name().equals(tipo.name())) {
            throw new ReglaNegocioException("La categoría no corresponde al tipo de transacción");
        }
        return cat;
    }
}
