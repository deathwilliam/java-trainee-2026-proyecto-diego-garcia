package sv.ledgerflow.api.seguridad;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsuarioAutenticadoTest {

    private final UsuarioAutenticado usuarioAutenticado = new UsuarioAutenticado();

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticar(String rol, Long idUsuario) {
        Jwt.Builder token = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("usuario@example.com")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60));
        if (idUsuario != null) {
            token.claim("idUsuario", idUsuario);
        }
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(token.build(), List.of(new SimpleGrantedAuthority(rol))));
    }

    @Test
    void sinUsuarioSolicitadoDevuelveElIdDelToken() {
        autenticar("ROLE_CLIENTE", 7L);

        assertThat(usuarioAutenticado.resolver(null)).isEqualTo(7L);
    }

    @Test
    void clientePuedeIndicarSuPropioId() {
        autenticar("ROLE_CLIENTE", 7L);

        assertThat(usuarioAutenticado.resolver(7L)).isEqualTo(7L);
    }

    @Test
    void clienteNoPuedeOperarSobreOtroUsuario() {
        autenticar("ROLE_CLIENTE", 7L);

        assertThatThrownBy(() -> usuarioAutenticado.resolver(8L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void administradorPuedeOperarSobreOtroUsuario() {
        autenticar("ROLE_ADMINISTRADOR", 1L);

        assertThat(usuarioAutenticado.resolver(8L)).isEqualTo(8L);
    }

    @Test
    void administradorSinUsuarioSolicitadoOperaSobreSusDatos() {
        autenticar("ROLE_ADMINISTRADOR", 1L);

        assertThat(usuarioAutenticado.resolver(null)).isEqualTo(1L);
    }

    @Test
    void auditorPuedeConsultarOtroUsuario() {
        autenticar("ROLE_AUDITOR", 2L);

        assertThat(usuarioAutenticado.resolverConsulta(8L)).isEqualTo(8L);
    }

    @Test
    void auditorNoPuedeOperarSobreOtroUsuario() {
        autenticar("ROLE_AUDITOR", 2L);

        assertThatThrownBy(() -> usuarioAutenticado.resolver(8L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void clienteNoPuedeConsultarOtroUsuario() {
        autenticar("ROLE_CLIENTE", 7L);

        assertThatThrownBy(() -> usuarioAutenticado.resolverConsulta(8L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void tokenSinIdUsuarioEsRechazado() {
        autenticar("ROLE_ADMINISTRADOR", null);

        assertThatThrownBy(() -> usuarioAutenticado.resolver(8L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void sinAutenticacionEsRechazado() {
        assertThatThrownBy(() -> usuarioAutenticado.resolver(null))
                .isInstanceOf(AccessDeniedException.class);
    }
}
