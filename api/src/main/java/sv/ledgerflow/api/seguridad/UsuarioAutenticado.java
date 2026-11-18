package sv.ledgerflow.api.seguridad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class UsuarioAutenticado {

    private static final Logger log = LoggerFactory.getLogger(UsuarioAutenticado.class);

    private static final Set<String> ROLES_OPERACION = Set.of("ROLE_ADMINISTRADOR");
    private static final Set<String> ROLES_CONSULTA = Set.of("ROLE_ADMINISTRADOR", "ROLE_AUDITOR");

    /**
     * Devuelve el usuario sobre el que se opera. Sin usuario solicitado es el dueño del token.
     * Solo un administrador puede indicar un usuario distinto al suyo.
     */
    public Long resolver(Long usuarioSolicitado) {
        return resolver(usuarioSolicitado, ROLES_OPERACION);
    }

    /**
     * Igual que {@link #resolver(Long)}, para endpoints de solo lectura: aquí un auditor
     * también puede indicar un usuario distinto al suyo.
     */
    public Long resolverConsulta(Long usuarioSolicitado) {
        return resolver(usuarioSolicitado, ROLES_CONSULTA);
    }

    private Long resolver(Long usuarioSolicitado, Set<String> rolesConAccesoAOtros) {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null
                || !(autenticacion.getPrincipal() instanceof Jwt jwt)
                || !(jwt.getClaims().get("idUsuario") instanceof Number numero)) {
            log.warn("Token sin el id de usuario");
            throw new AccessDeniedException("El token no contiene el id del usuario");
        }

        Long idEnToken = numero.longValue();
        if (usuarioSolicitado == null || usuarioSolicitado.equals(idEnToken)) {
            return idEnToken;
        }

        boolean tieneAcceso = autenticacion.getAuthorities().stream()
                .anyMatch(autoridad -> rolesConAccesoAOtros.contains(autoridad.getAuthority()));
        if (!tieneAcceso) {
            log.warn("Usuario {} intentó acceder a los datos del usuario {}", idEnToken, usuarioSolicitado);
            throw new AccessDeniedException("No puede acceder a los datos de otro usuario");
        }
        return usuarioSolicitado;
    }
}
