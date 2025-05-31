package am.ik.k8s.config;

import java.time.Clock;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.spring.LogbookClientHttpRequestInterceptor;

@Configuration(proxyBeanMethods = false)
public class AppConfig {

	@Bean
	RestClientCustomizer restClientCustomizer(Logbook logbook) {
		return builder -> builder.requestInterceptor(new LogbookClientHttpRequestInterceptor(logbook));
	}

	@Bean
	Clock clock() {
		return Clock.systemDefaultZone();
	}

}
