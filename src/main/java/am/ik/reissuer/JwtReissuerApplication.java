package am.ik.reissuer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class JwtReissuerApplication {

	public static void main(String[] args) {
		SpringApplication.run(JwtReissuerApplication.class, args);
	}

}
