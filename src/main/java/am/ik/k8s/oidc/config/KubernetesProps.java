package am.ik.k8s.oidc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.core.io.Resource;

@ConfigurationProperties(prefix = "kubernetes")
public record KubernetesProps(@DefaultValue("https://kubernetes.default.svc") String apiServerUrl, Resource bearerToken,
		String clientBundleName) {
}