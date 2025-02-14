package am.ik.k8s.oidc;

import org.springframework.boot.SpringApplication;

public class TestK8sOidcExposerApplication {

	public static void main(String[] args) {
		SpringApplication.from(K8sOidcExposerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
