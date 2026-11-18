package sv.ledgerflow.api.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import sv.ledgerflow.api.entidad.TipoCuenta;
import sv.ledgerflow.api.dtos.respuesta.TipoCuentaDto;
import sv.ledgerflow.api.dtos.solicitud.ActualizarTipoCuentaDto;
import sv.ledgerflow.api.dtos.solicitud.CrearTipoCuentaDto;
import sv.ledgerflow.api.excepciones.RecursoNoEncontradoExcepcion;
import sv.ledgerflow.api.excepciones.ReglaNegocioException;
import sv.ledgerflow.api.repositorio.TipoCuentaRepositorio;

import java.util.List;

@Service
public class TipoCuentaServicio {

    private static final Logger log = LoggerFactory.getLogger(TipoCuentaServicio.class);

    private final TipoCuentaRepositorio repositorio;

    public TipoCuentaServicio(TipoCuentaRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    public TipoCuentaDto crearTipoCuenta(CrearTipoCuentaDto tipoCuentaDto) {
        if (repositorio.existsByNombre(tipoCuentaDto.nombre())) {
            throw new ReglaNegocioException("Un tipo de cuenta llamado '" + tipoCuentaDto.nombre() + "' ya existe");
        }

        TipoCuenta tipoCuenta = new TipoCuenta(tipoCuentaDto.nombre(), tipoCuentaDto.descripcion(), true);
        repositorio.save(tipoCuenta);
        log.info("Tipo de cuenta '{}' creado", tipoCuenta.getNombre());

        return new TipoCuentaDto(tipoCuenta.getNombre(), tipoCuenta.getDescripcion());
    }

    public TipoCuentaDto actualizarTipoCuenta(String nombre, ActualizarTipoCuentaDto tipoCuentaDto) {
        TipoCuenta tipo = obtenerTipoCuentaPorNombre(nombre);

        if (tipoCuentaDto.nombre() != null && !tipoCuentaDto.nombre().equals(nombre)) {
            if (repositorio.existsByNombre(tipoCuentaDto.nombre())) {
                throw new ReglaNegocioException("Un tipo de cuenta llamado '" + tipoCuentaDto.nombre() + "' ya existe");
            }

            tipo.setNombre(tipoCuentaDto.nombre());
        }
        if (tipoCuentaDto.descripcion() != null) {
            tipo.setDescripcion(tipoCuentaDto.descripcion());
        }

        repositorio.save(tipo);
        log.info("Tipo de cuenta '{}' actualizado", nombre);

        return new TipoCuentaDto(tipo.getNombre(), tipo.getDescripcion());
    }

    public TipoCuentaDto obtenerTipoCuenta(String nombre) {
        TipoCuenta tipoCuenta = obtenerTipoCuentaPorNombre(nombre);
        return new TipoCuentaDto(tipoCuenta.getNombre(), tipoCuenta.getDescripcion());
    }

    public List<TipoCuentaDto> obtenerTiposCuentaActivos() {
        List<TipoCuenta> tipoCuentas = repositorio.findAllActive();
        return tipoCuentas.stream().map(t -> new TipoCuentaDto(t.getNombre(), t.getDescripcion())).toList();
    }

    public List<TipoCuentaDto> obtenerTiposCuenta() {
        List<TipoCuenta> tipoCuentas = repositorio.findAll();
        return tipoCuentas.stream().map(t -> new TipoCuentaDto(t.getNombre(), t.getDescripcion())).toList();
    }

    public TipoCuentaDto activarTipoCuenta(String nombre) {
        TipoCuenta tipoCuenta = obtenerTipoCuentaPorNombre(nombre);
        tipoCuenta.setActivo(true);
        log.info("Tipo de cuenta '{}' activado", nombre);
        repositorio.save(tipoCuenta);
        return new TipoCuentaDto(tipoCuenta.getNombre(), tipoCuenta.getDescripcion());
    }

    public TipoCuentaDto desactivarTipoCuenta(String nombre) {
        TipoCuenta tipoCuenta = obtenerTipoCuentaPorNombre(nombre);
        tipoCuenta.setActivo(false);
        log.info("Tipo de cuenta '{}' desactivado", nombre);
        repositorio.save(tipoCuenta);
        return new TipoCuentaDto(tipoCuenta.getNombre(), tipoCuenta.getDescripcion());
    }

    private TipoCuenta obtenerTipoCuentaPorNombre(String nombre) {
        return repositorio.findByNombre(nombre)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("No se encontró un tipo de cuenta con el nombre: " + nombre));
    }
}
