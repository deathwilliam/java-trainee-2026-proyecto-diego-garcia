package sv.ledgerflow.api.repositorio;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import sv.ledgerflow.api.entidad.Cuenta;

import java.util.List;
import java.util.Optional;

@Repository
public interface CuentaRepositorio extends JpaRepository<Cuenta, Long> {
    Optional<Cuenta> findByIdAndUsuarioId(Long id, Long usuarioId);
    boolean existsByNumeroCuenta(String numero);
    List<Cuenta> findByUsuarioId(Long usuarioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    //Evita errores si llega a existir concurrencia en una cuenta
    @Query("SELECT c FROM Cuenta c WHERE c.id = :id")
    Optional<Cuenta> findByIdForUpdate(Long id);
}
