package sv.ledgerflow.api.repositorio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import sv.ledgerflow.api.entidad.Bitacora;
import sv.ledgerflow.api.entidad.enums.AccionBitacora;
import sv.ledgerflow.api.entidad.enums.ResultadoBitacora;

import java.time.LocalDateTime;

@Repository
public interface BitacoraRepositorio extends JpaRepository<Bitacora, Long> {
    //Para estos resultados utilizaré paginación, ya que son muchos
    //Campos que se deberían de recibir, entonces es más fácil de digerír de esta manera
    Page<Bitacora> findByUsuarioId(Long usuarioId, Pageable pageable);
    Page<Bitacora> findByAccion(AccionBitacora accion, Pageable pageable);
    Page<Bitacora> findByResultado(ResultadoBitacora resultadoBitacora, Pageable pageable);
    Page<Bitacora> findByEntidadAndEntidadId(String entidad, Long entidadId, Pageable pageable);

    @Query("""
    SELECT b FROM Bitacora b
    WHERE (:usuarioId IS NULL OR b.usuario.id = :usuarioId)
    AND (:accion IS NULL OR b.accion = :accion)
    AND (:resultado IS NULL OR b.resultado = :resultado)
    AND b.fecha >= :desde and b.fecha < :hasta
    """)
    Page<Bitacora> buscar(Long usuarioId, AccionBitacora accion, ResultadoBitacora resultado, LocalDateTime desde, LocalDateTime hasta, Pageable pageable);
}
