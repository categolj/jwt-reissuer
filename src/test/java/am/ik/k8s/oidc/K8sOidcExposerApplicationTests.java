package am.ik.k8s.oidc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.k3s.K3sContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class K8sOidcExposerApplicationTests {

	RestClient restClient;

	@Container
	static K3sContainer k3sContainer = new K3sContainer(DockerImageName.parse("rancher/k3s:v1.31.5-k3s1"))
		.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("k3s")));

	@LocalServerPort
	int port;

	@DynamicPropertySource
	static void k3sProperties(DynamicPropertyRegistry registry) {
		Cluster cluster = Cluster.fromConfigYaml(k3sContainer.getKubeConfigYaml());
		registry.add("kubernetes.api-server-url", cluster::server);
		registry.add("kubernetes.client-bundle-name", () -> "k3s");
		registry.add("kubernetes.bearer-token", () -> "");
		registry.add("spring.ssl.bundle.pem.k3s.truststore.certificate",
				() -> "base64:" + cluster.certificateAuthorityData());
		registry.add("spring.ssl.bundle.pem.k3s.keystore.certificate",
				() -> "base64:" + cluster.clientCertificateData());
		registry.add("spring.ssl.bundle.pem.k3s.keystore.private-key", () -> "base64:" + cluster.clientKeyData());
	}

	@BeforeEach
	void setup(@Autowired RestClient.Builder restClientBuilder) {
		this.restClient = restClientBuilder.baseUrl("http://localhost:" + port).build();
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
		assertThat(body.get("issuer")).isEqualTo(new TextNode("https://kubernetes.default.svc.cluster.local"));
		assertThat(body.get("jwks_uri")).isEqualTo(new TextNode("http://localhost:%d/openid/v1/jwks".formatted(port)));
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

}
