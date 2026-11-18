package sv.ledgerflow.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/ledgerflow-test",
        "jwt.clave=0123456789abcdef0123456789abcdef"
})
@AutoConfigureMockMvc
class LedgerFlowApplicationTests {

    private static final String ADMINISTRADOR = "diego.garcia@example.com";
    private static final String AUDITOR = "ana.lopez@example.com";
    private static final String CLIENTE = "carlos.mendez@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    void loginDevuelveUnicamenteUnTokenJwtValido() throws Exception {
        String respuesta = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"diego.garcia@example.com","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.idUsuario").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Matcher cuerpo = Pattern.compile("^\\{\"token\":\"([^\"]+)\"}$").matcher(respuesta);
        assertThat(cuerpo.matches()).isTrue();
        assertThat(jwtDecoder.decode(cuerpo.group(1)).getSubject())
                .isEqualTo("diego.garcia@example.com");
    }

    @Test
    void sinTokenResponde401ConProblemDetail() throws Exception {
        mockMvc.perform(post("/api/ejecuciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idTransaccion\":1}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(esProblemDetail(401, "No autenticado"));
    }

    @Test
    void tokenInvalidoResponde401ConProblemDetail() throws Exception {
        mockMvc.perform(get("/api/cuentas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(esProblemDetail(401, "No autenticado"));
    }

    @Test
    void rolAuditorPuedeLeerPeroNoEjecutar() throws Exception {
        String token = iniciarSesion(AUDITOR);

        mockMvc.perform(get("/api/cuentas").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/ejecuciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idTransaccion\":1}"))
                .andExpect(status().isForbidden())
                .andExpect(esProblemDetail(403, "Acceso denegado"));
    }

    @Test
    void clienteNoPuedeIndicarOtroUsuarioEnLaPeticion() throws Exception {
        String token = iniciarSesion(CLIENTE);

        mockMvc.perform(post("/api/ejecuciones")
                        .param("usuarioId", String.valueOf(idUsuario(ADMINISTRADOR)))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idTransaccion\":" + idRecurrente(ADMINISTRADOR) + "}"))
                .andExpect(status().isForbidden())
                .andExpect(esProblemDetail(403, "Acceso denegado"));
    }

    @Test
    void otroUsuarioEnElJsonSeIgnoraYNoAfectaSusDatos() throws Exception {
        String token = iniciarSesion(CLIENTE);
        Long idDuenio = idUsuario(ADMINISTRADOR);
        Long recurrenteAjena = idRecurrente(ADMINISTRADOR);
        BigDecimal saldoAntes = saldoDeLaRecurrente(recurrenteAjena);

        mockMvc.perform(post("/api/ejecuciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":" + idDuenio + ",\"idUsuario\":" + idDuenio
                                + ",\"idTransaccion\":" + recurrenteAjena + "}"))
                .andExpect(status().isNotFound());

        assertThat(saldoDeLaRecurrente(recurrenteAjena)).isEqualByComparingTo(saldoAntes);
    }

    @Test
    void clienteNoPuedeVerUnaCuentaAjena() throws Exception {
        mockMvc.perform(get("/api/cuentas/" + idCuenta(ADMINISTRADOR))
                        .header(HttpHeaders.AUTHORIZATION, bearer(iniciarSesion(CLIENTE))))
                .andExpect(status().isNotFound());
    }

    @Test
    void elUsuarioSaleDelToken() throws Exception {
        mockMvc.perform(get("/api/cuentas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(iniciarSesion(CLIENTE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].titular").value("Carlos Mendez"));
    }

    @Test
    void administradorPuedeConsultarYOperarSobreOtroUsuario() throws Exception {
        String token = iniciarSesion(ADMINISTRADOR);
        Long idAuditor = idUsuario(AUDITOR);

        mockMvc.perform(get("/api/cuentas")
                        .param("usuarioId", String.valueOf(idAuditor))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titular").value("Ana Lopez"));

        mockMvc.perform(post("/api/ejecuciones")
                        .param("usuarioId", String.valueOf(idAuditor))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idTransaccion\":" + idRecurrente(AUDITOR) + "}"))
                .andExpect(status().isCreated());
    }

    @Test
    void auditorPuedeConsultarDatosDeOtroUsuario() throws Exception {
        String token = iniciarSesion(AUDITOR);
        String idDuenio = String.valueOf(idUsuario(ADMINISTRADOR));

        mockMvc.perform(get("/api/cuentas")
                        .param("usuarioId", idDuenio)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titular").value("Diego Garcia"));

        mockMvc.perform(get("/api/cuentas/" + idCuenta(ADMINISTRADOR) + "/movimientos")
                        .param("usuarioId", idDuenio)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void auditorPuedeConsultarLaBitacora() throws Exception {
        mockMvc.perform(get("/api/bitacora")
                        .header(HttpHeaders.AUTHORIZATION, bearer(iniciarSesion(AUDITOR))))
                .andExpect(status().isOk());
    }

    @Test
    void auditorNoPuedeModificarDatosDeOtroUsuario() throws Exception {
        mockMvc.perform(post("/api/ejecuciones")
                        .param("usuarioId", String.valueOf(idUsuario(ADMINISTRADOR)))
                        .header(HttpHeaders.AUTHORIZATION, bearer(iniciarSesion(AUDITOR)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idTransaccion\":" + idRecurrente(ADMINISTRADOR) + "}"))
                .andExpect(status().isForbidden())
                .andExpect(esProblemDetail(403, "Acceso denegado"));
    }

    @Test
    void clienteNoPuedeConsultarLaBitacoraNiDatosDeOtroUsuario() throws Exception {
        String token = iniciarSesion(CLIENTE);

        mockMvc.perform(get("/api/bitacora").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(esProblemDetail(403, "Acceso denegado"));

        mockMvc.perform(get("/api/cuentas")
                        .param("usuarioId", String.valueOf(idUsuario(ADMINISTRADOR)))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(esProblemDetail(403, "Acceso denegado"));
    }

    private ResultMatcher esProblemDetail(int estado, String titulo) {
        return resultado -> {
            content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON).match(resultado);
            jsonPath("$.status").value(estado).match(resultado);
            jsonPath("$.title").value(titulo).match(resultado);
            jsonPath("$.detail").isNotEmpty().match(resultado);
        };
    }

    private String iniciarSesion(String email) throws Exception {
        String respuesta = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(respuesta, "$.token");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private Long idUsuario(String email) {
        return jdbcTemplate.queryForObject("SELECT id FROM usuario WHERE email = ?", Long.class, email);
    }

    private Long idCuenta(String email) {
        return jdbcTemplate.queryForObject("""
                SELECT MIN(c.id) FROM cuenta c JOIN usuario u ON u.id = c.usuario_id WHERE u.email = ?
                """, Long.class, email);
    }

    private Long idRecurrente(String email) {
        return jdbcTemplate.queryForObject("""
                SELECT MIN(r.id) FROM transaccion_recurrente r
                JOIN cuenta c ON c.id = r.cuenta_id
                JOIN usuario u ON u.id = c.usuario_id
                WHERE u.email = ?
                """, Long.class, email);
    }

    private BigDecimal saldoDeLaRecurrente(Long recurrenteId) {
        return jdbcTemplate.queryForObject("""
                SELECT c.saldo FROM cuenta c
                JOIN transaccion_recurrente r ON r.cuenta_id = c.id
                WHERE r.id = ?
                """, BigDecimal.class, recurrenteId);
    }
}
