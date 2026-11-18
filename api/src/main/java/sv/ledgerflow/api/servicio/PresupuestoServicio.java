package sv.ledgerflow.api.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.ledgerflow.api.dtos.respuesta.AvancePresupuestoDto;
import sv.ledgerflow.api.dtos.respuesta.PresupuestoDto;
import sv.ledgerflow.api.dtos.solicitud.ActualizarPresupuestoDto;
import sv.ledgerflow.api.dtos.solicitud.CrearPresupuestoDto;
import sv.ledgerflow.api.entidad.Categoria;
import sv.ledgerflow.api.entidad.Presupuesto;
import sv.ledgerflow.api.entidad.enums.EstadoPresupuesto;
import sv.ledgerflow.api.entidad.enums.TipoCategoria;
import sv.ledgerflow.api.excepciones.RecursoNoEncontradoExcepcion;
import sv.ledgerflow.api.excepciones.ReglaNegocioException;
import sv.ledgerflow.api.repositorio.CategoriaRepositorio;
import sv.ledgerflow.api.repositorio.PresupuestoRepositorio;
import sv.ledgerflow.api.repositorio.TransaccionRepositorio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class PresupuestoServicio {

    private static final Logger log = LoggerFactory.getLogger(PresupuestoServicio.class);

    private final PresupuestoRepositorio presupuestoRepository;
    private final CategoriaRepositorio categoriaRepository;
    private final TransaccionRepositorio transaccionRepository;

    public PresupuestoServicio(PresupuestoRepositorio presupuestoRepository, CategoriaRepositorio categoriaRepository, TransaccionRepositorio transaccionRepository) {
        this.presupuestoRepository = presupuestoRepository;
        this.categoriaRepository = categoriaRepository;
        this.transaccionRepository = transaccionRepository;
    }

    @Transactional
    public PresupuestoDto crear(Long usuarioId, CrearPresupuestoDto req) {

        Categoria categoria = categoriaRepository.findByIdAndUsuarioId(req.categoriaId(), usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("La categoría no existe para este usuario"));

        if (categoria.getTipo() != TipoCategoria.EGRESO) {
            throw new ReglaNegocioException("Solo se pueden presupuestar categorías de EGRESO");
        }
        validarPeriodo(req.fechaInicio(), req.fechaFin());

        Presupuesto p = new Presupuesto();
        p.setCategoria(categoria);
        p.setMontoLimite(req.montoLimite());
        p.setFechaInicio(req.fechaInicio());
        p.setFechaFin(req.fechaFin());
        p.setEstado(EstadoPresupuesto.ACTIVO);
        presupuestoRepository.save(p);
        log.info("Presupuesto {} creado (usuario {}, categoría {})", p.getId(), usuarioId, categoria.getId());
        return new PresupuestoDto(
                p.getId(),
                p.getCategoria().getId(),
                p.getCategoria().getNombre(),
                p.getMontoLimite(),
                p.getFechaInicio(),
                p.getFechaFin(),
                p.getEstado()
        );
    }

    @Transactional(readOnly = true)
    public List<PresupuestoDto> listar(Long usuarioId, EstadoPresupuesto estado) {
        List<Presupuesto> lista = (estado == null)
                ? presupuestoRepository.findByCategoriaUsuarioIdOrderByFechaInicioDesc(usuarioId)
                : presupuestoRepository.findByCategoriaUsuarioIdAndEstadoOrderByFechaInicioDesc(usuarioId, estado);
        return lista.stream().map(p -> new PresupuestoDto(
                p.getId(),
                p.getCategoria().getId(),
                p.getCategoria().getNombre(),
                p.getMontoLimite(),
                p.getFechaInicio(),
                p.getFechaFin(),
                p.getEstado()
        )).toList();
    }

    @Transactional(readOnly = true)
    public PresupuestoDto obtener(Long usuarioId, Long id) {
        Presupuesto p = propio(usuarioId, id);
        return new PresupuestoDto(
                p.getId(),
                p.getCategoria().getId(),
                p.getCategoria().getNombre(),
                p.getMontoLimite(),
                p.getFechaInicio(),
                p.getFechaFin(),
                p.getEstado()
        );
    }

    @Transactional
    public PresupuestoDto actualizar(Long usuarioId, Long id, ActualizarPresupuestoDto req) {
        Presupuesto p = propio(usuarioId, id);
        validarPeriodo(req.fechaInicio(), req.fechaFin());

        p.setMontoLimite(req.montoLimite());
        p.setFechaInicio(req.fechaInicio());
        p.setFechaFin(req.fechaFin());
        log.info("Presupuesto {} actualizado (usuario {})", id, usuarioId);
        return new PresupuestoDto(
                p.getId(),
                p.getCategoria().getId(),
                p.getCategoria().getNombre(),
                p.getMontoLimite(),
                p.getFechaInicio(),
                p.getFechaFin(),
                p.getEstado()
        );
    }

    @Transactional
    public PresupuestoDto inactivar(Long usuarioId, Long id) {
        return cambiarEstado(usuarioId, id, EstadoPresupuesto.INACTIVO);
    }

    @Transactional
    public PresupuestoDto activar(Long usuarioId, Long id) {
        return cambiarEstado(usuarioId, id, EstadoPresupuesto.ACTIVO);
    }

    @Transactional(readOnly = true)
    public AvancePresupuestoDto consultarAvance(Long usuarioId, Long id) {
        Presupuesto p = propio(usuarioId, id);

        BigDecimal gastado = transaccionRepository.sumarEgresosCompletados(
                p.getCategoria().getId(),
                p.getFechaInicio().atStartOfDay(),
                p.getFechaFin().plusDays(1).atStartOfDay());

        log.debug("Avance del presupuesto {}: gastado {} de {}", id, gastado, p.getMontoLimite());
        BigDecimal restante = p.getMontoLimite().subtract(gastado);
        BigDecimal porcentaje = gastado.multiply(BigDecimal.valueOf(100))
                .divide(p.getMontoLimite(), 2, RoundingMode.HALF_UP);

        return new AvancePresupuestoDto(p.getId(), p.getMontoLimite(), gastado, restante, porcentaje,
                p.superaLimite(gastado));
    }

    private PresupuestoDto cambiarEstado(Long usuarioId, Long id, EstadoPresupuesto nuevo) {
        Presupuesto p = propio(usuarioId, id);
        if (p.getEstado() == nuevo) {
            log.warn("Cambio de estado rechazado: el presupuesto {} ya está {} (usuario {})", id, nuevo, usuarioId);
            throw new ReglaNegocioException("El presupuesto ya está " + nuevo);
        }
        p.setEstado(nuevo);
        log.info("Presupuesto {} pasó a {} (usuario {})", id, nuevo, usuarioId);
        return new PresupuestoDto(
                p.getId(),
                p.getCategoria().getId(),
                p.getCategoria().getNombre(),
                p.getMontoLimite(),
                p.getFechaInicio(),
                p.getFechaFin(),
                p.getEstado()
        );
    }

    private Presupuesto propio(Long usuarioId, Long id) {
        return presupuestoRepository.findByIdAndCategoriaUsuarioId(id, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("El presupuesto no existe para este usuario"));
    }

    private void validarPeriodo(LocalDate inicio, LocalDate fin) {
        if (fin.isBefore(inicio)) {
            throw new ReglaNegocioException("La fecha de fin no puede ser anterior a la de inicio");
        }
    }
}