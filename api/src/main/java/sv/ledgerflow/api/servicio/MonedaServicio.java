package sv.ledgerflow.api.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import sv.ledgerflow.api.entidad.Moneda;
import sv.ledgerflow.api.dtos.respuesta.MonedaDto;
import sv.ledgerflow.api.dtos.solicitud.ActualizarMonedaDto;
import sv.ledgerflow.api.dtos.solicitud.CrearMonedaDto;
import sv.ledgerflow.api.excepciones.RecursoNoEncontradoExcepcion;
import sv.ledgerflow.api.excepciones.ReglaNegocioException;
import sv.ledgerflow.api.repositorio.MonedaRepositorio;

import java.util.List;

@Service
public class MonedaServicio {

    private static final Logger log = LoggerFactory.getLogger(MonedaServicio.class);
    private final MonedaRepositorio repositorio;

    public MonedaServicio(MonedaRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    public MonedaDto crearMoneda(CrearMonedaDto monedaDto) {
        if (repositorio.existsByCodigo(monedaDto.codigo())) {
            throw new ReglaNegocioException("Una moneda con el código " + monedaDto.codigo() + " ya existe");
        }

        Moneda moneda = new Moneda(monedaDto.codigo(), monedaDto.nombre(), monedaDto.simbolo(), true);
        repositorio.save(moneda);
        log.info("Moneda {} creada", moneda.getCodigo());
        return new MonedaDto(moneda.getCodigo(), moneda.getNombre(), moneda.getSimbolo());
    }

    public MonedaDto actualizarMoneda(String codigo, ActualizarMonedaDto monedaDto) {
        Moneda moneda = obtenerMonedaPorCodigo(codigo);
        if (monedaDto.nombre() != null) moneda.setNombre(monedaDto.nombre());
        if (monedaDto.simbolo() != null) moneda.setSimbolo(monedaDto.simbolo());

        repositorio.save(moneda);
        log.info("Moneda {} actualizada", codigo);

        return new MonedaDto(moneda.getCodigo(), moneda.getNombre(), moneda.getSimbolo());
    }

    public MonedaDto obtenerMoneda(String codigo) {
        Moneda moneda = obtenerMonedaPorCodigo(codigo);
        return new MonedaDto(moneda.getCodigo(), moneda.getNombre(), moneda.getSimbolo());
    }

    public List<MonedaDto> obtenerMonedasActivas() {
        List<Moneda> monedas = repositorio.findAllActive();
        return monedas.stream()
                .map(m -> new MonedaDto(m.getCodigo(), m.getNombre(), m.getSimbolo())).toList();
    }

    public List<MonedaDto> obtenerMonedas() {
        List<Moneda> monedas = repositorio.findAll();
        return monedas.stream()
                .map(m -> new MonedaDto(m.getCodigo(), m.getNombre(), m.getSimbolo())).toList();
    }

    public MonedaDto activarMoneda(String codigo) {
        Moneda moneda = obtenerMonedaPorCodigo(codigo);
        moneda.setActivo(true);
        log.info("Moneda {} activada", codigo);
        repositorio.save(moneda);
        return new MonedaDto(moneda.getCodigo(), moneda.getNombre(), moneda.getSimbolo());
    }

    public MonedaDto desactivarMoneda(String codigo) {
        Moneda moneda = obtenerMonedaPorCodigo(codigo);
        moneda.setActivo(false);
        log.info("Moneda {} desactivada", codigo);
        repositorio.save(moneda);
        return new MonedaDto(moneda.getCodigo(), moneda.getNombre(), moneda.getSimbolo());
    }

    private Moneda obtenerMonedaPorCodigo(String codigo) {
        return repositorio.findByCodigo(codigo)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("No se encontró una moneda con el código: " + codigo));
    }
}
