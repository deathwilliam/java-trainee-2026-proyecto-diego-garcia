package sv.ledgerflow.api.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.ledgerflow.api.dtos.respuesta.CategoriaDto;
import sv.ledgerflow.api.dtos.solicitud.ActualizarCategoriaDto;
import sv.ledgerflow.api.dtos.solicitud.CrearCategoriaDto;
import sv.ledgerflow.api.entidad.Categoria;
import sv.ledgerflow.api.entidad.Usuario;
import sv.ledgerflow.api.entidad.enums.TipoCategoria;
import sv.ledgerflow.api.excepciones.ConflictoNegocioException;
import sv.ledgerflow.api.excepciones.RecursoNoEncontradoExcepcion;
import sv.ledgerflow.api.excepciones.ReglaNegocioException;
import sv.ledgerflow.api.repositorio.*;

import java.util.List;

@Service
public class CategoriaServicio {

    private static final Logger log = LoggerFactory.getLogger(CategoriaServicio.class);

    private final CategoriaRepositorio categoriaRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final TransaccionRepositorio transaccionRepositorio;
    private final PresupuestoRepositorio presupuestoRepositorio;
    private final TransaccionRecurrenteRepositorio transaccionRecurrenteRepositorio;

    public CategoriaServicio(CategoriaRepositorio categoriaRepositorio, UsuarioRepositorio usuarioRepositorio, TransaccionRepositorio transaccionRepositorio, PresupuestoRepositorio presupuestoRepositorio, TransaccionRecurrenteRepositorio transaccionRecurrenteRepositorio) {
        this.categoriaRepositorio = categoriaRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.transaccionRepositorio = transaccionRepositorio;
        this.presupuestoRepositorio = presupuestoRepositorio;
        this.transaccionRecurrenteRepositorio = transaccionRecurrenteRepositorio;
    }

    @Transactional
    public CategoriaDto crearCategoria(Long usuarioId, CrearCategoriaDto categoriaDto) {
        if (categoriaRepositorio.existsByUsuarioIdAndNombre(usuarioId, categoriaDto.nombre())) {
            throw new ReglaNegocioException("Una categoría con este nombre ya existe");
        }

        Usuario usuario = usuarioRepositorio.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("Un usuario con este Id no existe"));

        Categoria categoria = new Categoria(
                categoriaDto.tipo(),
                categoriaDto.nombre(),
                usuario
        );

        categoriaRepositorio.save(categoria);
        log.info("Categoría {} creada (usuario {}, tipo {})", categoria.getId(), usuarioId, categoria.getTipo());

        return new CategoriaDto(categoria.getId(), categoria.getNombre(), categoria.getTipo());
    }

    @Transactional
    public CategoriaDto actualizarCategoria(Long usuarioId, Long categoriaId, ActualizarCategoriaDto categoriaDto) {
        Categoria categoria = propia(usuarioId, categoriaId);

        String nombre = categoriaDto.nombre().trim();
        if (categoriaRepositorio.existsByUsuarioIdAndNombreAndIdNot(usuarioId, nombre, categoriaId)) {
            throw new ReglaNegocioException("Ya existe una categoría con ese nombre");
        }

        if (categoria.getTipo() != categoriaDto.tipo() && enUso(categoriaId)) {
            log.warn("Cambio de tipo rechazado: la categoría {} está en uso (usuario {})", categoriaId, usuarioId);
            throw new ConflictoNegocioException(
                    "No se puede cambiar el tipo de una categoría que ya está en uso");
        }

        categoria.setNombre(nombre);
        categoria.setTipo(categoriaDto.tipo());
        log.info("Categoría {} actualizada (usuario {})", categoriaId, usuarioId);
        return new CategoriaDto(categoria.getId(), categoria.getNombre(), categoria.getTipo());
    }

    @Transactional(readOnly = true)
    public List<CategoriaDto> listar(Long usuarioId, TipoCategoria tipo) {
        List<Categoria> categorias = (tipo == null)
                ? categoriaRepositorio.findByUsuarioIdOrderByNombre(usuarioId)
                : categoriaRepositorio.findByUsuarioIdAndTipoOrderByNombre(usuarioId, tipo);
        return categorias.stream().map(c -> new CategoriaDto(c.getId(), c.getNombre(), c.getTipo())).toList();
    }

    @Transactional(readOnly = true)
    public CategoriaDto obtener(Long usuarioId, Long id) {
        Categoria categoria = propia(usuarioId, id);
        return new CategoriaDto(categoria.getId(), categoria.getNombre(), categoria.getTipo());
    }

    @Transactional
    public void eliminar(Long usuarioId, Long id) {
        Categoria categoria = propia(usuarioId, id);
        if (enUso(id)) {
            log.warn("Eliminación rechazada: la categoría {} tiene datos relacionados (usuario {})", id, usuarioId);
            throw new ConflictoNegocioException("No se puede eliminar una categoría que tiene datos relacionados");
        }
        categoriaRepositorio.delete(categoria);
        log.info("Categoría {} eliminada (usuario {})", id, usuarioId);
    }

    private Categoria propia(Long usuarioId, Long id) {
        return categoriaRepositorio.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("La categoría no existe para este usuario"));
    }

    private boolean enUso(Long categoriaId) {
        return transaccionRepositorio.existsByCategoriaId(categoriaId)
                || presupuestoRepositorio.existsByCategoriaId(categoriaId)
                || transaccionRecurrenteRepositorio.existsByCategoriaId(categoriaId);
    }
}
