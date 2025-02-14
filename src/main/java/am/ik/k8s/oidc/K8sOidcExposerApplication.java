package am.ik.k8s.oidc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class K8sOidcExposerApplication {

	public static void main(String[] args) {
		SpringApplication.run(K8sOidcExposerApplication.class, args);
	}

}
