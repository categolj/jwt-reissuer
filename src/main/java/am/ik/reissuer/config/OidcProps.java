package am.ik.reissuer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@ConfigurationProperties(prefix = "reissuer.oidc")
public record OidcProps(String apiServerUrl, Resource bearerToken, String clientBundleName) {
}