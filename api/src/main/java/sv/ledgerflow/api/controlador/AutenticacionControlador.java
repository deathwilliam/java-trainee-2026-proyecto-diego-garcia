package sv.ledgerflow.api.controlador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sv.ledgerflow.api.dtos.respuesta.TokenDto;
import sv.ledgerflow.api.dtos.solicitud.LoginDto;
import sv.ledgerflow.api.servicio.AutenticacionServicio;

@Tag(name = "Autenticación", description = "Inicio de sesión y emisión de token")
@RestController
@RequestMapping("/api/auth")
public class AutenticacionControlador {

    private final AutenticacionServicio servicio;

    public AutenticacionControlador(AutenticacionServicio servicio) {
        this.servicio = servicio;
    }

    @Operation(summary = "Iniciar sesión", description = "Devuelve un token JWT para las credenciales validas")
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<TokenDto> iniciarSesion(@Valid @RequestBody LoginDto solicitud) {
        return ResponseEntity.ok(servicio.iniciarSesion(solicitud));
    }
}
