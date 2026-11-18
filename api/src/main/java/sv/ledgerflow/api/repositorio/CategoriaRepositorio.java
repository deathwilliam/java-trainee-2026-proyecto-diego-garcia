package sv.ledgerflow.api.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sv.ledgerflow.api.entidad.Categoria;
import sv.ledgerflow.api.entidad.enums.TipoCategoria;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriaRepositorio extends JpaRepository<Categoria, Long> {
    Optional<Categoria> findByIdAndUsuarioId(Long id, Long usuarioId);
    List<Categoria> findByUsuarioId(Long usuarioId);
    boolean existsByUsuarioIdAndNombre(Long usuarioId, String nombre);
    boolean existsByUsuarioIdAndNombreAndIdNot(Long usuarioId, String nombre, Long id);
    List<Categoria> findByUsuarioIdOrderByNombre(Long usuarioId);
    List<Categoria> findByUsuarioIdAndTipoOrderByNombre(Long usuarioId, TipoCategoria tipo);

}
