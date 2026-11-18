package sv.ledgerflow.api.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.ledgerflow.api.dtos.respuesta.MovimientoCuentaDto;
import sv.ledgerflow.api.entidad.*;
import sv.ledgerflow.api.dtos.respuesta.CuentaDto;
import sv.ledgerflow.api.dtos.solicitud.CrearCuentaDto;
import sv.ledgerflow.api.dtos.solicitud.MovimientoDto;
import sv.ledgerflow.api.entidad.enums.EstadoCuenta;
import sv.ledgerflow.api.excepciones.RecursoNoEncontradoExcepcion;
import sv.ledgerflow.api.excepciones.ReglaNegocioException;
import sv.ledgerflow.api.repositorio.*;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;

@Service
public class CuentaServicio {

    private static final Logger log = LoggerFactory.getLogger(CuentaServicio.class);

    private final CuentaRepositorio cuentaRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final TipoCuentaRepositorio tipoCuentaRepositorio;
    private final MonedaRepositorio monedaRepositorio;
    private final DetalleTransaccionRepositorio detalleTransaccionRepositorio;
    private final TransferenciaServicio transferenciaServicio;
    private static final SecureRandom RANDOM = new SecureRandom();

    public CuentaServicio(CuentaRepositorio cuentaRepositorio, UsuarioRepositorio usuarioRepositorio, TipoCuentaRepositorio tipoCuentaRepositorio, MonedaRepositorio monedaRepositorio, DetalleTransaccionRepositorio detalleTransaccionRepositorio, TransferenciaServicio transferenciaServicio) {
        this.cuentaRepositorio = cuentaRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.tipoCuentaRepositorio = tipoCuentaRepositorio;
        this.monedaRepositorio = monedaRepositorio;
        this.detalleTransaccionRepositorio = detalleTransaccionRepositorio;
        this.transferenciaServicio = transferenciaServicio;
    }

    @Transactional
    public CuentaDto crear(Long usuarioId, CrearCuentaDto crearCuentaDto) {
        Usuario usuario = usuarioRepositorio.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("Un usuario con id " + usuarioId + " no existe"));
        TipoCuenta tipo = tipoCuentaRepositorio.findById(crearCuentaDto.tipoCuentaId())
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("Un tipo con id " + crearCuentaDto.tipoCuentaId() + " no existe"));
        if (!tipo.getActivo()) {
            throw new ReglaNegocioException("El tipo de cuenta debe estar activo");
        }
        Moneda moneda = monedaRepositorio.findById(crearCuentaDto.monedaId())
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("Una moneda con id " + crearCuentaDto.tipoCuentaId() + " mp existe"));
        if (!moneda.getActivo()) {
            throw new ReglaNegocioException("La moneda debe estar activa");
        }

        Cuenta cuenta = new Cuenta(
                usuario,
                tipo,
                moneda,
                generarNumeroCuenta(),
                BigDecimal.ZERO,
                EstadoCuenta.ACTIVA
        );

        cuentaRepositorio.save(cuenta);
        log.info("Cuenta {} creada (usuario {}, moneda {})", cuenta.getId(), usuarioId, moneda.getCodigo());

        BigDecimal inicial = crearCuentaDto.saldoInicial();
        if (inicial != null && inicial.signum() > 0) {
            inicial = transferenciaServicio.registrarIngreso(usuarioId,
                    new MovimientoDto(cuenta.getId(), null, inicial, "Saldo inicial", null)).saldoResultante();
        } else inicial = BigDecimal.ZERO;
        
        return new CuentaDto(
                cuenta.getNumeroCuenta(),
                inicial,
                usuario.getNombre() + " " + usuario.getApellido()
        );
    }

    @Transactional
    public List<CuentaDto> listarCuentasPropias(Long usuarioId) {
        Usuario usuario = usuarioRepositorio.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("Un usuario con id " + usuarioId + " no existe"));
        List<Cuenta> cuentas = cuentaRepositorio.findByUsuarioId(usuarioId);
        return cuentas.stream().map(c -> new CuentaDto(
                c.getNumeroCuenta(),
                c.getSaldo(),
                usuario.getNombre() + " " + usuario.getApellido()
        )).toList();
    }

    @Transactional
    public CuentaDto obtenerCuenta(Long usuarioId, Long cuentaId) {
        Usuario usuario = usuarioRepositorio.getReferenceById(usuarioId);
        Cuenta cuenta = cuentaRepositorio.findByIdAndUsuarioId(cuentaId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("La cuenta no existe para este usuario"));
        return new CuentaDto(cuenta.getNumeroCuenta(), cuenta.getSaldo(), usuario.getNombre() + " " + usuario.getApellido());
    }

    @Transactional(readOnly = true)
    public Page<MovimientoCuentaDto> historial(Long usuarioId, Long cuentaId, Pageable pageable) {
        cuentaRepositorio.findByIdAndUsuarioId(cuentaId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("La cuenta no existe para este usuario"));

        Pageable seguro = PageRequest.of(
                pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), 100),
                Sort.by(Sort.Direction.DESC, "id")
        );

        return detalleTransaccionRepositorio.findByCuentaId(cuentaId, seguro).map(d -> {
            Transaccion t = d.getTransaccion();
            return new MovimientoCuentaDto(
                    t.getFecha(),
                    t.getReferencia(),
                    t.getTipo(),
                    t.getEstado(),
                    d.getTipoMovimiento(),
                    d.getMonto(),
                    d.getSaldoResultante(),
                    t.getDescripcion(),
                    t.getContraparte(),
                    t.getCategoria() == null ? null : t.getCategoria().getNombre()
            );
        });
    }

    @Transactional
    public CuentaDto bloquearCuenta(Long cuentaId) {
        Cuenta cuenta = cuentaRepositorio.findByIdForUpdate(cuentaId)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("No se encontró una cuenta con ese Id"));
        Usuario usuario = usuarioRepositorio.getReferenceById(cuenta.getUsuario().getId());
        cuenta.setEstado(EstadoCuenta.BLOQUEADA);
        log.info("Cuenta {} bloqueada", cuentaId);
        return new CuentaDto(cuenta.getNumeroCuenta(), cuenta.getSaldo(), usuario.getNombre() + " " + usuario.getApellido());
    }

    @Transactional
    public CuentaDto desbloquearCuenta(Long cuentaId) {
        Cuenta cuenta = cuentaRepositorio.findByIdForUpdate(cuentaId)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("No se encontró una cuenta con ese Id"));
        Usuario usuario = usuarioRepositorio.getReferenceById(cuenta.getUsuario().getId());
        cuenta.setEstado(EstadoCuenta.ACTIVA);
        log.info("Cuenta {} desbloqueada", cuentaId);
        return new CuentaDto(cuenta.getNumeroCuenta(), cuenta.getSaldo(), usuario.getNombre() + " " + usuario.getApellido());
    }

    @Transactional
    public CuentaDto cerrarCuenta(Long cuentaId) {
        Cuenta cuenta = cuentaRepositorio.findByIdForUpdate(cuentaId)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("No se encontró una cuenta con ese Id"));
        if (cuenta.getSaldo().compareTo(BigDecimal.ZERO) > 0) {
            throw new ReglaNegocioException("La cuenta no debe tener fondos para poder cerrarse");
        }
        Usuario usuario = usuarioRepositorio.getReferenceById(cuenta.getUsuario().getId());
        cuenta.setEstado(EstadoCuenta.CERRADA);
        log.info("Cuenta {} cerrada", cuentaId);
        return new CuentaDto(cuenta.getNumeroCuenta(), cuenta.getSaldo(), usuario.getNombre() + " " + usuario.getApellido());
    }

    private String generarNumeroCuenta() {
        for (int intento = 0; intento < 5; intento++) {
            String numero = String.format("%012d", RANDOM.nextLong(1_000_000_000_000L));
            if (!cuentaRepositorio.existsByNumeroCuenta(numero)) {
                return numero;
            }
        }

        log.error("No se pudo generar un número de cuenta único tras 5 intentos");
        throw new IllegalStateException("No se pudo generar un número de cuenta único");
    }
}
