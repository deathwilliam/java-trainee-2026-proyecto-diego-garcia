package sv.ledgerflow.api.seguridad;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableMethodSecurity
public class ConfiguracionSeguridad {

    private final SecretKey clave;

    public ConfiguracionSeguridad(@Value("${jwt.clave}") String clave) {
        this.clave = new SecretKeySpec(clave.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    public SecurityFilterChain filtros(HttpSecurity http, RespuestasSeguridad respuestas) throws Exception {
        http
                // CSRF protege formularios de navegador con sesion. Una API con token no tiene
                // sesion ni formularios: se apaga. Sin esto, todo POST da 401/403.
                .csrf(AbstractHttpConfigurer::disable)
                // STATELESS: el servidor no guarda sesion. Cada peticion trae su token y se valida sola.
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reglas -> reglas
                        // abierto: el login (para pedir el token) y Swagger
                        .requestMatchers("/api/auth/login", "/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs/**", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/**")
                        .hasAnyRole("AUDITOR", "CLIENTE", "ADMINISTRADOR")
                        .requestMatchers("/api/**").hasAnyRole("CLIENTE", "ADMINISTRADOR")
                        .anyRequest().authenticated())
                // "resource server": cada peticion trae Authorization: Bearer <token>, y Spring lo
                // valida con el JwtDecoder de abajo (firma y vencimiento)
                // Peticiones sin token o rechazadas por su rol
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler(respuestas))
                // Tambien aqui: un token invalido o vencido lo rechaza el filtro del resource server
                .oauth2ResourceServer(o -> o
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler(respuestas)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(clave));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(clave).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }

}
