package sv.ledgerflow.api.controlador;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import sv.ledgerflow.api.excepciones.RecursoNoEncontradoExcepcion;
import sv.ledgerflow.api.excepciones.ReglaNegocioException;
import sv.ledgerflow.api.excepciones.CredencialesInvalidasExcepcion;
import sv.ledgerflow.api.excepciones.ConflictoNegocioException;

import java.util.Map;
import java.util.TreeMap;

@RestControllerAdvice
public class ManejadorErrores extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    // Lo que NADIE esperaba: un null, la base caída, un error de programación.
    // Al cliente, un 500 sin detalles; al log, todo, con la traza completa.
    @ExceptionHandler(Exception.class)
    public ProblemDetail inesperado(Exception e) throws Exception {
        // Seguridad se resuelve en su propia cadena (401/403): no es un error inesperado.
        if (e instanceof AccessDeniedException || e instanceof AuthenticationException) {
            throw e;
        }
        log.error("Error inesperado: {}", e.getMessage(), e);
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado. Intente de nuevo o contacte a soporte.");
        problema.setTitle("Error interno");
        return problema;
    }

    @ExceptionHandler(CredencialesInvalidasExcepcion.class)
    public ProblemDetail credencialesInvalidas(CredencialesInvalidasExcepcion e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
        problem.setTitle("Credenciales invalidas");
        return problem;
    }

    @ExceptionHandler(RecursoNoEncontradoExcepcion.class)
    public ProblemDetail noEncontrado(RecursoNoEncontradoExcepcion e){
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problem.setTitle("Recurso no encontrado");
        return problem;
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ProblemDetail noCumpleRegla(ReglaNegocioException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problem.setTitle("Regla de negocio incumplida");
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail argumentoInvalido(IllegalArgumentException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problema.setTitle("Datos no validos");
        return problema;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail conflicto(DataIntegrityViolationException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "La operacion choca con datos relacionados que ya existen");
        problema.setTitle("Conflicto con los datos");
        return problema;
    }

    @ExceptionHandler(ConflictoNegocioException.class)
    public ProblemDetail conflictoNegocio(ConflictoNegocioException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problema.setTitle("Conflicto con los datos");
        return problema;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errores = new TreeMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(error -> errores.put(error.getField(), error.getDefaultMessage()));

        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Hay datos que no cumplen las validaciones");
        problema.setTitle("Datos no validos");
        problema.setProperty("errores", errores);
        return ResponseEntity.badRequest().body(problema);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException e,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "El cuerpo no es un JSON valido, o tiene un valor que no se puede convertir");
        problema.setTitle("Cuerpo ilegible");
        return ResponseEntity.badRequest().body(problema);
    }
}
