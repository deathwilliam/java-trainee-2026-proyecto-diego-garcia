package sv.ledgerflow.api.repositorio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import sv.ledgerflow.api.entidad.DetalleTransaccion;
import sv.ledgerflow.api.entidad.enums.TipoMovimiento;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface DetalleTransaccionRepositorio extends JpaRepository<DetalleTransaccion, Long> {
    List<DetalleTransaccion> findByTransaccionId(Long id);
    List<Long> findCuenta_IdByTransaccion_Id(Long transaccionId);
    Optional<DetalleTransaccion> findFirstByCuenta_IdOrderByIdDesc(Long cuentaId);
    @Query("""
    SELECT COALESCE(SUM(
    CASE WHEN d.tipoMovimiento = :credito THEN d.monto ELSE -d.monto END
    ), 0)
    FROM DetalleTransaccion d
    WHERE d.cuenta.id = :cuentaId
    """)
    BigDecimal calcularSaldo(Long cuentaId, TipoMovimiento credito);
    @EntityGraph(attributePaths = {"transaccion", "transaccion.categoria"})
    Page<DetalleTransaccion> findByCuentaId(Long cuentaId, Pageable pageable);
}
