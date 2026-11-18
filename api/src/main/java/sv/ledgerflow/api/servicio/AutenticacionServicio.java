package sv.ledgerflow.api.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.ledgerflow.api.dtos.respuesta.TokenDto;
import sv.ledgerflow.api.dtos.solicitud.LoginDto;
import sv.ledgerflow.api.entidad.Usuario;
import sv.ledgerflow.api.excepciones.CredencialesInvalidasExcepcion;
import sv.ledgerflow.api.repositorio.UsuarioRepositorio;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AutenticacionServicio {

    private static final Logger log = LoggerFactory.getLogger(AutenticacionServicio.class);

    private final UsuarioRepositorio usuarioRepositorio;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    public AutenticacionServicio(UsuarioRepositorio usuarioRepositorio, PasswordEncoder passwordEncoder,
                                 JwtEncoder jwtEncoder) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
    }

    @Transactional(readOnly = true)
    public TokenDto iniciarSesion(LoginDto solicitud) {
        Usuario usuario = usuarioRepositorio.findByEmail(solicitud.email())
                .filter(Usuario::estaActivo)
                .filter(encontrado -> passwordEncoder.matches(solicitud.password(), encontrado.getPassword()))
                .orElseThrow(() -> {
                    // WARN: intento fallido. Nunca se escribe la clave ni el token.
                    log.warn("Inicio de sesión rechazado para el correo {}", solicitud.email());
                    return new CredencialesInvalidasExcepcion();
                });

        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("ledgerflow")
                .issuedAt(ahora)
                .expiresAt(ahora.plus(1, ChronoUnit.HOURS))
                .subject(usuario.getEmail())
                .claim("idUsuario", usuario.getId())
                .claim("roles", usuario.getRoles().stream().map(rol -> rol.getNombre()).toList())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();
        log.info("Inicio de sesión correcto: usuario {}", usuario.getId());
        return new TokenDto(token);
    }
}
