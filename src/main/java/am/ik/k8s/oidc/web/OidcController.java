package am.ik.k8s.oidc.web;

import am.ik.k8s.oidc.KubernetesProps;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.boot.autoconfigure.web.client.RestClientSsl;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
public class OidcController {

	private final RestClient restClient;

	public OidcController(KubernetesProps props, RestClient.Builder restClientBuilder, RestClientSsl clientSsl) {
		if (StringUtils.hasLength(props.clientBundleName())) {
			System.out.println("Configure " + props.clientBundleName());
			restClientBuilder.apply(clientSsl.fromBundle(props.clientBundleName()));
		}
		this.restClient = restClientBuilder.defaultHeaders(headers -> {
			if (props.bearerToken() != null) {
				try {
					headers.setBearerAuth(props.bearerToken().getContentAsString(StandardCharsets.UTF_8));
				}
				catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
		}).baseUrl(props.apiServerUrl()).defaultStatusHandler(status -> true, (request, response) -> {

		}).build();
	}

	@GetMapping(path = "/.well-known/openid-configuration")
	public Map<String, Object> openidConfiguration(UriComponentsBuilder uriComponentsBuilder) {
		Map<String, Object> configuration = getConfiguration();
		String proxyJwksUri = uriComponentsBuilder.path("/openid/v1/jwks").build().toString();
		configuration.put("jwks_uri", proxyJwksUri);
		return configuration;
	}

	@GetMapping(path = "/openid/v1/jwks")
	public Map<String, Object> jwks() {
		Map<String, Object> configuration = getConfiguration();
		String jwksUri = (String) configuration.get("jwks_uri");
		return this.restClient.get()
			.uri(URI.create(jwksUri).getPath())
			.retrieve()
			.body(new ParameterizedTypeReference<>() {
			});
	}

	private Map<String, Object> getConfiguration() {
		ResponseEntity<Map<String, Object>> response = this.restClient.get()
			.uri("/.well-known/openid-configuration")
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});
		Map<String, Object> body = response.getBody();
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new ResponseStatusException(response.getStatusCode(), body != null ? body.toString() : "");
		}
		if (body == null) {
			throw new IllegalStateException("Response body is null");
		}
		return body;
	}

}
