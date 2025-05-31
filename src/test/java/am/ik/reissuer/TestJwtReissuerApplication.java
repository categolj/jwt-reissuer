package am.ik.reissuer;

import org.springframework.boot.SpringApplication;

public class TestJwtReissuerApplication {

	public static void main(String[] args) {
		SpringApplication.from(JwtReissuerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
