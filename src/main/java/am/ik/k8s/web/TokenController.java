package am.ik.k8s.web;

import am.ik.k8s.jwt.JwtProps;
import am.ik.k8s.jwt.JwtSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
public class TokenController {

	private final JwtSigner jwtSigner;

	private final JwtProps jwtProps;

	private final Clock clock;

	public TokenController(JwtSigner jwtSigner, JwtProps jwtProps, Clock clock) {
		this.jwtSigner = jwtSigner;
		this.jwtProps = jwtProps;
		this.clock = clock;
	}

	@PostMapping(path = "/token")
	public String token(@AuthenticationPrincipal Jwt jwt, UriComponentsBuilder uriComponentsBuilder) {
		String issuer = uriComponentsBuilder.path("").build().toString();
		Instant now = clock.instant();
		JWTClaimsSet.Builder jwtBuilder = new JWTClaimsSet.Builder().issuer(issuer)
			.audience(this.jwtProps.audience())
			.issueTime(Date.from(now))
			.expirationTime(Date.from(now.plusSeconds(this.jwtProps.tokenTtl().toSeconds())));
		if (jwt.getSubject() != null) {
			jwtBuilder.subject(jwt.getSubject());
		}
		if (jwt.getNotBefore() != null) {
			jwtBuilder.notBeforeTime(new Date(jwt.getNotBefore().toEpochMilli()));
		}
		if (jwt.getId() != null) {
			jwtBuilder.jwtID(jwt.getId());
		}
		Map<String, Object> kubernetesClaim = jwt.getClaimAsMap("kubernetes.io");
		if (kubernetesClaim != null) {
			jwtBuilder.claim("kubernetes.io", kubernetesClaim);
		}
		return this.jwtSigner.sign(jwtBuilder.build()).serialize();
	}

}
