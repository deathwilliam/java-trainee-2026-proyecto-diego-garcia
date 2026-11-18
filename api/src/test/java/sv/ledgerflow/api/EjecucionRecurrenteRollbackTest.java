package sv.ledgerflow.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import sv.ledgerflow.api.entidad.*;
import sv.ledgerflow.api.entidad.enums.*;
import sv.ledgerflow.api.repositorio.*;
import sv.ledgerflow.api.servicio.RecurrenteServicio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/ledgerflow-test",
        "jwt.clave=0123456789abcdef0123456789abcdef"
})
class EjecucionRecurrenteRollbackTest {

    @Autowired
    private RecurrenteServicio recurrenteServicio;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Autowired
    private TipoCuentaRepositorio tipoCuentaRepositorio;

    @Autowired
    private MonedaRepositorio monedaRepositorio;

    @Autowired
    private CategoriaRepositorio categoriaRepositorio;

    @Autowired
    private CuentaRepositorio cuentaRepositorio;

    @Autowired
    private TransaccionRecurrenteRepositorio recurrenteRepositorio;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private BitacoraRepositorio bitacoraRepositorio;

    private Usuario usuario;
    private Categoria categoria;
    private Cuenta cuenta;
    private TipoCuenta tipoCuenta;
    private Moneda moneda;
    private TransaccionRecurrente recurrente;
    private LocalDate proximaEjecucionOriginal;

    @BeforeEach
    void prepararDatos() {
        String identificador = UUID.randomUUID().toString().replace("-", "");
        String duiDigitos = identificador.replaceAll("\\D", "");

        usuario = usuarioRepositorio.save(new Usuario(
                "Prueba",
                "Rollback",
                identificador + "@test.local",
                duiDigitos.substring(0, 8) + "-" + duiDigitos.charAt(8),
                "test-password",
                EstadoUsuario.ACTIVO
        ));
        tipoCuenta = tipoCuentaRepositorio.save(
                new TipoCuenta("Tipo " + identificador, "Cuenta de prueba", true));
        moneda = monedaRepositorio.save(
                new Moneda("T" + identificador.substring(0, 2).toUpperCase(), "Moneda prueba", "$", true));
        categoria = categoriaRepositorio.save(
                new Categoria(TipoCategoria.INGRESO, "Ingreso " + identificador, usuario));
        cuenta = cuentaRepositorio.save(new Cuenta(
                usuario,
                tipoCuenta,
                moneda,
                "TEST-" + identificador.substring(0, 15),
                new BigDecimal("100.00"),
                EstadoCuenta.ACTIVA
        ));

        proximaEjecucionOriginal = LocalDate.now().minusDays(1);
        recurrente = recurrenteRepositorio.save(new TransaccionRecurrente(
                cuenta,
                categoria,
                TipoRecurrente.INGRESO,
                new BigDecimal("25.00"),
                "Ingreso recurrente de prueba",
                Frecuencia.DIARIA,
                proximaEjecucionOriginal,
                EstadoRecurrente.ACTIVA
        ));
    }

    @AfterEach
    void limpiarDatos() {
        if (cuenta != null && cuenta.getId() != null) {
            jdbcTemplate.update("DELETE FROM detalle_transaccion WHERE cuenta_id = ?", cuenta.getId());
        }
        if (categoria != null && categoria.getId() != null) {
            jdbcTemplate.update("DELETE FROM detalle_transaccion WHERE transaccion_id IN " +
                    "(SELECT id FROM transaccion WHERE categoria_id = ?)", categoria.getId());
            jdbcTemplate.update("DELETE FROM transaccion WHERE categoria_id = ?", categoria.getId());
        }
        if (recurrente != null && recurrente.getId() != null) {
            jdbcTemplate.update("DELETE FROM transaccion_recurrente WHERE id = ?", recurrente.getId());
        }
        if (cuenta != null && cuenta.getId() != null) {
            jdbcTemplate.update("DELETE FROM cuenta WHERE id = ?", cuenta.getId());
        }
        if (categoria != null && categoria.getId() != null) {
            jdbcTemplate.update("DELETE FROM categoria WHERE id = ?", categoria.getId());
        }
        if (usuario != null && usuario.getId() != null) {
            jdbcTemplate.update("DELETE FROM usuario WHERE id = ?", usuario.getId());
        }
        if (tipoCuenta != null && tipoCuenta.getId() != null) {
            jdbcTemplate.update("DELETE FROM tipo_cuenta WHERE id = ?", tipoCuenta.getId());
        }
        if (moneda != null && moneda.getId() != null) {
            jdbcTemplate.update("DELETE FROM moneda WHERE id = ?", moneda.getId());
        }
    }

    @Test
    void revierteLaTransaccionSiFallaElGuardadoDeLaBitacoraExitosa() {
        RuntimeException falloBitacora = new IllegalStateException("Fallo intencional de bitácora");
        doAnswer(invocacion -> {
            Bitacora bitacora = invocacion.getArgument(0);
            if (bitacora.getResultado() == ResultadoBitacora.EXITOSO) {
                throw falloBitacora;
            }
            return bitacora;
        }).when(bitacoraRepositorio).save(any(Bitacora.class));

        Throwable error = catchThrowable(() ->
                recurrenteServicio.ejecutar(recurrente.getId(), usuario.getId()));

        Long transaccionesPersistidas = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transaccion WHERE categoria_id = ?",
                Long.class,
                categoria.getId());
        BigDecimal saldoPersistido = jdbcTemplate.queryForObject(
                "SELECT saldo FROM cuenta WHERE id = ?",
                BigDecimal.class,
                cuenta.getId());
        LocalDate proximaEjecucionPersistida = jdbcTemplate.queryForObject(
                "SELECT proxima_ejecucion FROM transaccion_recurrente WHERE id = ?",
                LocalDate.class,
                recurrente.getId());

        assertThat(transaccionesPersistidas).isZero();
        assertThat(saldoPersistido).isEqualByComparingTo("100.00");
        assertThat(proximaEjecucionPersistida).isEqualTo(proximaEjecucionOriginal);
        assertThat(error).as("La ejecución debe alcanzar el guardado exitoso de bitácora")
                .isSameAs(falloBitacora);
        verify(bitacoraRepositorio).save(argThat((Bitacora bitacora) ->
                bitacora.getResultado() == ResultadoBitacora.EXITOSO));
    }
}
