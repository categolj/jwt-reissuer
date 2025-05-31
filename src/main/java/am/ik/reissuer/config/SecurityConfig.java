package am.ik.reissuer.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.JwkSetUriJwtDecoderBuilderCustomizer;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;
import org.zalando.logbook.spring.LogbookClientHttpRequestInterceptor;

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

	@Bean
	public JwkSetUriJwtDecoderBuilderCustomizer jwkSetUriJwtDecoderBuilderCustomizer(
			RestTemplateBuilder restTemplateBuilder, OidcProps props, SslBundles sslBundles,
			LogbookClientHttpRequestInterceptor logbookClientHttpRequestInterceptor) {
		return builder -> {
			try {
				RestTemplateBuilder auth = props.bearerToken() != null
						? restTemplateBuilder.defaultHeader(HttpHeaders.AUTHORIZATION,
								"Bearer " + props.bearerToken().getContentAsString(StandardCharsets.UTF_8))
						: restTemplateBuilder;
				builder.restOperations((StringUtils.hasLength(props.clientBundleName())
						? auth.sslBundle(sslBundles.getBundle(props.clientBundleName())) : auth)
					.interceptors(logbookClientHttpRequestInterceptor)
					.connectTimeout(Duration.ofSeconds(3))
					.readTimeout(Duration.ofSeconds(5))
					.build());
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		};
	}

}