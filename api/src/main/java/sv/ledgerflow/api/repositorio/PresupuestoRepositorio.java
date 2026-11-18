package sv.ledgerflow.api.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import sv.ledgerflow.api.entidad.Presupuesto;
import sv.ledgerflow.api.entidad.enums.EstadoPresupuesto;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PresupuestoRepositorio extends JpaRepository<Presupuesto, Long> {
    @Query("""
    SELECT p FROM Presupuesto p
    WHERE p.categoria.id = :categoriaId AND p.estado = 'ACTIVO'
    AND :fecha between p.fechaInicio and p.fechaFin
    """)
    List<Presupuesto> findVigente(Long categoriaId, LocalDate fecha);
    Optional<Presupuesto> findByIdAndCategoriaUsuarioId(Long id, Long usuarioId);
    boolean existsByCategoriaId(Long categoriaId);

    List<Presupuesto> findByCategoriaUsuarioIdOrderByFechaInicioDesc(Long usuarioId);
    List<Presupuesto> findByCategoriaUsuarioIdAndEstadoOrderByFechaInicioDesc(Long usuarioId, EstadoPresupuesto estado);
}
