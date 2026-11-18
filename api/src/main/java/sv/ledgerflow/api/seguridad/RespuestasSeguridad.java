package sv.ledgerflow.api.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

/**
 * Responde el 401 y el 403 con ProblemDetail. Estos rechazos ocurren en los filtros de seguridad,
 * antes de llegar a los controladores, por eso ManejadorErrores no los puede atender.
 */
@Component
public class RespuestasSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(RespuestasSeguridad.class);

    private final JsonMapper jsonMapper;

    public RespuestasSeguridad(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException e) throws IOException {
        log.warn("401 en {} {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        escribir(response, HttpStatus.UNAUTHORIZED, "No autenticado",
                "Falta el token o no es valido");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException e) throws IOException {
        log.warn("403 en {} {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        escribir(response, HttpStatus.FORBIDDEN, "Acceso denegado",
                "No tiene permiso para realizar esta operacion");
    }

    private void escribir(HttpServletResponse response, HttpStatus estado, String titulo,
                          String detalle) throws IOException {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(titulo);

        response.setStatus(estado.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonMapper.writeValueAsString(problema));
    }
}
