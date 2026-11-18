package sv.ledgerflow.api.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import sv.ledgerflow.api.entidad.Moneda;

import java.util.List;
import java.util.Optional;

public interface MonedaRepositorio extends JpaRepository<Moneda, Long> {
    Optional<Moneda> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);

    @Query(value = "SELECT m FROM Moneda m WHERE m.activo = TRUE")
    List<Moneda> findAllActive();
}
