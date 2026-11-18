package sv.ledgerflow.api.repositorio;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import sv.ledgerflow.api.entidad.TransaccionRecurrente;
import sv.ledgerflow.api.entidad.enums.EstadoRecurrente;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransaccionRecurrenteRepositorio extends JpaRepository<TransaccionRecurrente, Long> {
    Optional<TransaccionRecurrente> findByIdAndCuentaUsuarioId(Long id, Long usuarioId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM TransaccionRecurrente r WHERE r.id = :id AND r.cuenta.usuario.id = :usuarioId")
    Optional<TransaccionRecurrente> buscarParaEjecutar(Long id, Long usuarioId);
    boolean existsByCategoriaId(Long categoriaId);

    List<TransaccionRecurrente> findByCuentaUsuarioId(Long usuarioId);
    List<TransaccionRecurrente> findByCuentaUsuarioIdAndEstado(Long usuarioId, EstadoRecurrente estado);
    @Query("""
    select r from TransaccionRecurrente r
    where r.cuenta.usuario.id = :usuarioId and r.estado = :estado
      and r.proximaEjecucion <= :hoy
    order by r.proximaEjecucion
    """)
    List<TransaccionRecurrente> pendientes(Long usuarioId, EstadoRecurrente estado, LocalDate hoy);
}
