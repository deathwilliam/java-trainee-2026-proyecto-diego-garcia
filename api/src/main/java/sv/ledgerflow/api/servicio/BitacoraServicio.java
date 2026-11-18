package sv.ledgerflow.api.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import sv.ledgerflow.api.dtos.respuesta.BitacoraDto;
import sv.ledgerflow.api.entidad.Bitacora;
import sv.ledgerflow.api.entidad.enums.AccionBitacora;
import sv.ledgerflow.api.entidad.enums.ResultadoBitacora;
import sv.ledgerflow.api.excepciones.ReglaNegocioException;
import sv.ledgerflow.api.repositorio.BitacoraRepositorio;
import sv.ledgerflow.api.repositorio.UsuarioRepositorio;

import java.time.LocalDate;

@Service
public class BitacoraServicio {

    private static final Logger log = LoggerFactory.getLogger(BitacoraServicio.class);
    private final BitacoraRepositorio bitacoraRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private static final int TAMANO_MAXIMO = 100;
    private static final int DIAS_POR_DEFECTO = 30;

    public BitacoraServicio(BitacoraRepositorio bitacoraRepositorio, UsuarioRepositorio usuarioRepositorio) {
        this.bitacoraRepositorio = bitacoraRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
    }

    @Transactional
    public void registrarExitoso(Long usuarioId, AccionBitacora accion, String entidad, Long entidadId, String detalle) {
        guardar(usuarioId, accion, ResultadoBitacora.EXITOSO, entidad, entidadId, detalle);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarFallido(Long usuarioId, AccionBitacora accion, String entidad, Long entidadId, String detalle) {
        guardar(usuarioId, accion, ResultadoBitacora.FALLIDO, entidad, entidadId, detalle);
    }

    private void guardar(Long usuarioId, AccionBitacora accion, ResultadoBitacora resultado,
    String entidad, Long entidadId, String detalle) {
        Bitacora b = new Bitacora();
        b.setUsuario(usuarioId == null ? null : usuarioRepositorio.getReferenceById(usuarioId));
        b.setAccion(accion);
        b.setResultado(resultado);
        b.setEntidad(entidad);
        b.setEntidadId(entidadId);
        b.setDetalle(detalle);
        bitacoraRepositorio.save(b);
        log.debug("Bitácora: {} {} sobre {} {} (usuario {})", resultado, accion, entidad, entidadId, usuarioId);
    }

    @Transactional(readOnly = true)
    public Page<BitacoraDto> buscar(Long usuarioId, AccionBitacora accion, ResultadoBitacora resultado,
                                    LocalDate desde, LocalDate hasta, Pageable pageable) {
        LocalDate fin = (hasta != null) ? hasta : LocalDate.now();
        LocalDate inicio = (desde != null) ? desde : fin.minusDays(DIAS_POR_DEFECTO);
        if (inicio.isAfter(fin)) {
            throw new ReglaNegocioException("La fecha de inicio no puede ser posterior a la de fin");
        }

        log.debug("Consulta de bitácora: usuario={}, accion={}, resultado={}, {} a {}", usuarioId, accion, resultado, inicio, fin);
        return bitacoraRepositorio
                .buscar(usuarioId, accion, resultado,
                        inicio.atStartOfDay(), fin.plusDays(1).atStartOfDay(),
                        paginaSegura(pageable))
                .map(b -> new BitacoraDto(
                        b.getId(),
                        b.getUsuario() == null ? null : b.getUsuario().getId(),
                        b.getAccion(), b.getResultado(), b.getEntidad(), b.getEntidadId(),
                        b.getDetalle(), b.getFecha()));
    }


    @Transactional(readOnly = true)
    public Page<BitacoraDto> historialDeEntidad(String entidad, Long entidadId, Pageable pageable) {
        return bitacoraRepositorio
                .findByEntidadAndEntidadId(entidad, entidadId, paginaSegura(pageable))
                .map(b -> new BitacoraDto(
                        b.getId(),
                        b.getUsuario() == null ? null : b.getUsuario().getId(),
                        b.getAccion(), b.getResultado(), b.getEntidad(), b.getEntidadId(),
                        b.getDetalle(), b.getFecha())
                );
    }

    private Pageable paginaSegura(Pageable pageable) {
        return PageRequest.of(
                pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), TAMANO_MAXIMO),
                Sort.by(Sort.Direction.DESC, "fecha", "id"));
    }
}
