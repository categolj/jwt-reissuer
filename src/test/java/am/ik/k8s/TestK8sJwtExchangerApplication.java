package am.ik.k8s;

import org.springframework.boot.SpringApplication;

public class TestK8sJwtExchangerApplication {

	public static void main(String[] args) {
		SpringApplication.from(K8sJwtExchangerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
