package sv.ledgerflow.api.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sv.ledgerflow.api.entidad.MetaAhorro;
import sv.ledgerflow.api.entidad.enums.EstadoMeta;

import java.util.List;
import java.util.Optional;

@Repository
public interface MetaAhorroRepositorio extends JpaRepository<MetaAhorro, Long> {
    List<MetaAhorro> findByCuentaIdAndEstado(Long cuentaId, EstadoMeta estado);
    Optional<MetaAhorro> findByIdAndCuentaUsuarioId(Long id, Long usuarioId);

    List<MetaAhorro> findByCuentaUsuarioId(Long usuarioId);

    List<MetaAhorro> findByCuentaUsuarioIdAndEstado(Long usuarioId, EstadoMeta estado);
}
