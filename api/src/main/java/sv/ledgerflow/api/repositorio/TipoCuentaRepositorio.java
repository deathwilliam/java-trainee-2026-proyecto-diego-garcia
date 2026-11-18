package sv.ledgerflow.api.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import sv.ledgerflow.api.entidad.TipoCuenta;

import java.util.List;
import java.util.Optional;

public interface TipoCuentaRepositorio extends JpaRepository<TipoCuenta, Long> {
    Optional<TipoCuenta> findByNombre(String nombre);
    boolean existsByNombre(String nombre);

    @Query(value = "SELECT t FROM TipoCuenta t WHERE t.activo = FALSE")
    List<TipoCuenta> findAllActive();
}
