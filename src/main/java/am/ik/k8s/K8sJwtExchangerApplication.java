package am.ik.k8s;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class K8sJwtExchangerApplication {

	public static void main(String[] args) {
		SpringApplication.run(K8sJwtExchangerApplication.class, args);
	}

}
