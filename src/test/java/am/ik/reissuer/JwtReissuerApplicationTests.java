package am.ik.reissuer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "reissuer.oidc.api-server-url=https://accounts.google.com")
class JwtReissuerApplicationTests {

	RestClient restClient;

	@LocalServerPort
	int port;

	@BeforeEach
	void setup(@Autowired RestClient.Builder restClientBuilder) {
		this.restClient = restClientBuilder.baseUrl("http://localhost:" + port)
			.defaultStatusHandler(s -> true, (request, response) -> {
				/* no-op */})
			.build();
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
			.uri("/token")
			.headers(httpHeaders -> httpHeaders.setBearerAuth(token))
			.retrieve()
			.toEntity(JsonNode.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

}
