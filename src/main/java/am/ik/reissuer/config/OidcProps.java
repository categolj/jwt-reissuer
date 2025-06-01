package am.ik.reissuer.config;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@ConfigurationProperties(prefix = "reissuer.oidc")
public record OidcProps(URI issuerUrl, Resource bearerToken, String clientBundleName) {
}