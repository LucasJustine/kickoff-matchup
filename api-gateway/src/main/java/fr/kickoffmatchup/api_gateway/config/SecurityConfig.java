package fr.kickoffmatchup.api_gateway.config;

import fr.kickoffmatchup.api_gateway.jwt.JwtHeaderInjectionFilter;
import fr.kickoffmatchup.api_gateway.security.SecurityConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/bff/providers/**",
            "/error",
            "/favicon.ico",
            "/assets/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/public/**"
    };

    private static final String[] AUTH_ENTRY_PATHS = {
            "/bff/auth/login",
            "/bff/auth/mfa/verify",
            "/bff/auth/mfa/resend",
            "/oauth2/**",
            "/login/oauth2/code/**",
            "/bff/auth/register",
            "/bff/auth/verify-email",
            "/bff/auth/resend-verification",
            "/bff/auth/confirm-email-change",
            "/bff/auth/forgot-password",
            "/bff/auth/reset-password"
    };

    private static final String[] MOBILE_ENTRY_PATHS = {
            "/mobile/auth/login",
            "/mobile/auth/mfa/verify",
            "/mobile/auth/mfa/resend",
            "/mobile/auth/refresh",
            "/mobile/auth/logout",
            "/mobile/auth/social/**",
            "/mobile/auth/register",
            "/mobile/auth/verify-email",
            "/mobile/auth/resend-verification",
            "/mobile/auth/confirm-email-change",
            "/mobile/auth/forgot-password",
            "/mobile/auth/reset-password"
    };

    private static final String BEARER_PREFIX = "Bearer ";

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final JwtHeaderInjectionFilter jwtHeaderInjectionFilter;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final UserRateLimitFilter userRateLimitFilter;

    public SecurityConfig(JwtHeaderInjectionFilter jwtHeaderInjectionFilter,
            OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
            TokenBucketLimiter tokenBucketLimiter) {
        this.userRateLimitFilter = new UserRateLimitFilter(tokenBucketLimiter);
        this.jwtHeaderInjectionFilter = jwtHeaderInjectionFilter;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
    }

    @Bean
    @Order(1)
    SecurityFilterChain mobileSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(SecurityConfig::isMobileRequest)
            .csrf(AbstractHttpConfigurer::disable)
            .logout(logout -> logout.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(MOBILE_ENTRY_PATHS).permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                .bearerTokenResolver(mobileTokenResolver())
                .jwt(Customizer.withDefaults()))
            .addFilterAfter(userRateLimitFilter, BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .logout(logout -> logout.disable())
                .securityContext(securityContext -> securityContext
                        .securityContextRepository(new RequestAttributeSecurityContextRepository()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(AUTH_ENTRY_PATHS).permitAll()
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(sessionTokenResolver())
                        .jwt(Customizer.withDefaults()))
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2LoginSuccessHandler)
                        .failureHandler(oAuth2LoginSuccessHandler))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtHeaderInjectionFilter, BearerTokenAuthenticationFilter.class)
                .addFilterAfter(userRateLimitFilter, BearerTokenAuthenticationFilter.class)
                .addFilterBefore(new ReturnUrlCaptureFilter(), OAuth2AuthorizationRequestRedirectFilter.class)
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class);

        return http.build();
    }


    private static BearerTokenResolver sessionTokenResolver() {
        return request -> {
            if (isPublicOrAuthEntry(request.getRequestURI())) {
                return null;
            }
            Object token = request.getAttribute(SecurityConstants.REQUEST_ACCESS_TOKEN_ATTRIBUTE);
            return token instanceof String accessToken ? accessToken : null;
        };
    }

    private static boolean isMobileRequest(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        return request.getRequestURI().startsWith("/mobile/")
                || (authorization != null
                        && authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length()));
    }

    private static BearerTokenResolver mobileTokenResolver() {
        DefaultBearerTokenResolver headerResolver = new DefaultBearerTokenResolver();
        return request -> isMobileEntry(request.getRequestURI()) ? null : headerResolver.resolve(request);
    }

    private static boolean isMobileEntry(String path) {
        for (String pattern : MOBILE_ENTRY_PATHS) {
            if (PATH_MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPublicOrAuthEntry(String path) {
        for (String pattern : PUBLIC_PATHS) {
            if (PATH_MATCHER.match(pattern, path)) {
                return true;
            }
        }
        for (String pattern : AUTH_ENTRY_PATHS) {
            if (PATH_MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    @Bean
    FilterRegistrationBean<JwtHeaderInjectionFilter> jwtHeaderInjectionFilterRegistration(
            JwtHeaderInjectionFilter filter) {
        FilterRegistrationBean<JwtHeaderInjectionFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    private static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                FilterChain filterChain) throws ServletException, IOException {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken != null) {
                csrfToken.getToken();
            }
            filterChain.doFilter(request, response);
        }
    }
}
