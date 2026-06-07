package co.empresa.ticket_service.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad idéntica al auth-service.
 *
 * Keycloak guarda los roles en:
 *   token.realm_access.roles = ["ROLE_CLIENT", "ROLE_ORGANIZER", "ROLE_ADMIN"]
 *
 * Este converter los extrae y los pone como GrantedAuthority para que
 * @PreAuthorize("hasRole('ROLE_ORGANIZER')") funcione correctamente.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth

                .requestMatchers("/actuator/health").permitAll()
                //lo demás requiere token JWT válido de Keycloak
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );

        return http.build();
    }
    /* OJOOOO CON ESTO, DEBERÁ SER IGUAL PARA TODO SECURITYCONFIG
    Extrae roles de Keycloak desde realm_access.roles.
    Keycloak emite los roles SIN prefijo ("ORGANIZER", "CLIENT", "ADMIN").
    Este converter agrega "ROLE_" para que hasAuthority('ROLE_X') funcione.
    */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");

            if (realmAccess == null || !realmAccess.containsKey("roles")) {
                return List.of();
            }

            @SuppressWarnings("unchecked")
            Collection<String> roles = (Collection<String>) realmAccess.get("roles");

            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority(
                            role.startsWith("ROLE_") ? role : "ROLE_" + role))  // ← única línea cambiada
                    .collect(Collectors.toList());
        });

        return converter;
    }
}
