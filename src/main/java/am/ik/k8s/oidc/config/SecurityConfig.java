package am.ik.k8s.oidc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
			.authorizeHttpRequests(
					authz -> authz.requestMatchers(HttpMethod.POST, "/token").authenticated().anyRequest().permitAll())
			.oauth2ResourceServer(oauth -> oauth.jwt(jwt -> {
			}))
			.csrf(csrf -> csrf.disable())
			.build();
	}

}