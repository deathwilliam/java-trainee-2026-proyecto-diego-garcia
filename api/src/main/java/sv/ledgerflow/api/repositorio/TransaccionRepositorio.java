package sv.ledgerflow.api.repositorio;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import sv.ledgerflow.api.entidad.Cuenta;
import sv.ledgerflow.api.entidad.Transaccion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TransaccionRepositorio extends JpaRepository<Transaccion, Long> {
    boolean existsByReferencia(String ref);
    @Query("""
    SELECT coalesce(sum(t.monto), 0) FROM Transaccion t
    WHERE t.categoria.id = :categoriaId
    AND t.tipo = 'EGRESO' AND t.estado = 'COMPLETADA'
    AND t.fecha >= :desde AND t.fecha < :hasta
    """)
    BigDecimal sumarEgresosCompletados(Long categoriaId, LocalDateTime desde, LocalDateTime hasta);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transaccion t WHERE t.id = :id")
    Optional<Transaccion> findByIdForUpdate(Long id);
    boolean existsByCategoriaId(Long categoriaId);
}
