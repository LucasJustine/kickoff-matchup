package fr.kickoffmatchup.api_gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Client HTTP unique vers l'auth server, avec timeouts : un auth server lent ne doit pas
 * bloquer indéfiniment les threads de la gateway.
 */
@Configuration
public class AuthServerClientConfig {

    @Bean
    RestClient authServerClient(
            RestClient.Builder builder,
            @Value("${app.auth-server-url}") String authServerUrl,
            @Value("${app.auth-server-connect-timeout:2s}") Duration connectTimeout,
            @Value("${app.auth-server-read-timeout:5s}") Duration readTimeout) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(connectTimeout).build());
        factory.setReadTimeout(readTimeout);
        return builder.baseUrl(authServerUrl).requestFactory(factory).build();
    }
}
