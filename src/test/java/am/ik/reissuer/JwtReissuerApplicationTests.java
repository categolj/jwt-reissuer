package am.ik.reissuer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.nimbusds.jwt.JWTClaimsSet;
import java.net.URI;
import java.util.Base64;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = { "spring.http.client.redirects=dont_follow", "reissuer.jwt.audience=test-app" })
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class JwtReissuerApplicationTests {

	RestClient restClient;

	@LocalServerPort
	int port;

	Function<Set<String>, String> accessTokenSupplier;

	@BeforeEach
	void setup(@Autowired RestClient.Builder restClientBuilder, @Value("${reissuer.oidc.issuer-uri}") URI issuerUrl) {
		this.restClient = restClientBuilder.baseUrl("http://localhost:" + port)
			.defaultStatusHandler(s -> true, (request, response) -> {
				/* no-op */})
			.build();
		this.accessTokenSupplier = scopes -> OAuth2.authorizationCodeFlow(issuerUrl,
				restClientBuilder.defaultStatusHandler(s -> true, (request, response) -> {
					/* no-op */}).build(),
				new OAuth2.User("test@example.com", "test"), new OAuth2.Client("test-app", "secret"),
				URI.create("http://localhost:8080/login/oauth2/code/todo-frontend"), scopes);
	}

	@Test
	void openidConfiguration() {
		ResponseEntity<JsonNode> response = this.restClient.get()
			.uri("/.well-known/openid-configuration")
			.retrieve()
			.toEntity(JsonNode.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		JsonNode body = response.getBody();
		assertThat(body).isNotNull();
		assertThat(body.get("issuer")).isEqualTo(new TextNode("http://localhost:" + port));
		assertThat(body.get("jwks_uri")).isEqualTo(new TextNode("http://localhost:" + port + "/openid/v1/jwks"));
	}

	@Test
	void jwksUri() {
		ResponseEntity<JsonNode> response = this.restClient.get()
			.uri("/openid/v1/jwks")
			.retrieve()
			.toEntity(JsonNode.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		JsonNode body = response.getBody();
		assertThat(body).isNotNull();
		assertThat(body.has("keys")).isTrue();
		assertThat(body.get("keys").isArray()).isTrue();
		assertThat(body.get("keys").size()).isEqualTo(1);
	}

	@Test
	void invalidToken() {
		String token = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImExIn0.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMiwiZXhwIjoxNTMxNzYyMDY1fQ.z4qfO0leZK2mYp_w-jFNidTx-Ri0PRMHLsOAG1Den7ZR4QntIJhU17U0afgoe5VzISXS6jW61ga3XEk39ey1G7a_-ARIVZLYN11fHDhsPuzN7PPkbT5uWpHEUhVWRR8dxHqXmNiDaWjNhTnzHCBpfrRHj5pR_dzubbuE_uPuvDk";
		ResponseEntity<JsonNode> response = this.restClient.post()
			.uri("/reissue")
			.headers(httpHeaders -> httpHeaders.setBearerAuth(token))
			.retrieve()
			.toEntity(JsonNode.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void validToken() throws Exception {
		String token = this.accessTokenSupplier.apply(Set.of("openid"));
		ResponseEntity<String> response = this.restClient.post()
			.uri("/reissue")
			.headers(httpHeaders -> httpHeaders.setBearerAuth(token))
			.retrieve()
			.toEntity(String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		JWTClaimsSet claimsSet = JWTClaimsSet
			.parse(new String(Base64.getDecoder().decode(response.getBody().split("\\.")[1])));
		assertThat(claimsSet.getIssuer()).isEqualTo("http://localhost:" + port);
		assertThat(claimsSet.getAudience()).containsExactly("test-app");
		assertThat(claimsSet.getSubject()).isEqualTo("test@example.com");
	}

}
