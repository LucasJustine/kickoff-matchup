package fr.kickoffmatchup.api_gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.http.HttpStatus;
import fr.kickoffmatchup.api_gateway.jwt.JwtHeaderInjectionFilter;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtHeaderInjectionFilter jwtHeaderInjectionFilter;

    public SecurityConfig(JwtHeaderInjectionFilter jwtHeaderInjectionFilter) {
        this.jwtHeaderInjectionFilter = jwtHeaderInjectionFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {


        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers("/login", "/providers"))
                .logout(logout -> logout
                    .logoutUrl("/logout")
                    .invalidateHttpSession(true)
                    .clearAuthentication(true)
                    .deleteCookies("AUTH_SESSION_ID")
                    .logoutSuccessHandler((req, res, auth) -> res.setStatus(HttpServletResponse.SC_NO_CONTENT))
                )
                 .securityContext(securityContext -> securityContext
                         .securityContextRepository(new RequestAttributeSecurityContextRepository()))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login/**",
                                "/oauth2/**",
                                "/providers",
                                "/error",
                                "/favicon.ico",
                                "/assets/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(sessionTokenResolver())
                        .jwt(Customizer.withDefaults())
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                        )
                )
                .addFilterBefore(
                        jwtHeaderInjectionFilter,
                        BearerTokenAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    BearerTokenResolver sessionTokenResolver() {
        return request -> {
            Object attributeToken = request.getAttribute(JwtHeaderInjectionFilter.ACCESS_TOKEN_ATTRIBUTE);
            if (attributeToken instanceof String accessToken) {
                return accessToken;
            }

            var session = request.getSession(false);
            if (session == null) {
                return null;
            }
            Object token = session.getAttribute("ACCESS_TOKEN");
            return token instanceof String accessToken ? accessToken : null;
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}