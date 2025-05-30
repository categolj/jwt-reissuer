package am.ik.k8s.oidc.web;

import am.ik.k8s.oidc.jwt.JwtProps;
import am.ik.k8s.oidc.jwt.JwtSigner;
import com.nimbusds.jwt.JWTClaimsSet;
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

	public TokenController(JwtSigner jwtSigner, JwtProps jwtProps) {
		this.jwtSigner = jwtSigner;
		this.jwtProps = jwtProps;
	}

	@PostMapping(path = "/token")
	public String token(@AuthenticationPrincipal Jwt jwt, UriComponentsBuilder uriComponentsBuilder) {
		String issuer = uriComponentsBuilder.path("").build().toString();
		// Replace the issuer and audience claims in the given JWT
		JWTClaimsSet.Builder jwtBuilder = new JWTClaimsSet.Builder().issuer(issuer).audience(this.jwtProps.audience());
		if (jwt.getSubject() != null) {
			jwtBuilder.subject(jwt.getSubject());
		}
		if (jwt.getNotBefore() != null) {
			jwtBuilder.notBeforeTime(new Date(jwt.getNotBefore().toEpochMilli()));
		}
		if (jwt.getIssuedAt() != null) {
			jwtBuilder.issueTime(new Date(jwt.getIssuedAt().toEpochMilli()));
		}
		if (jwt.getExpiresAt() != null) {
			jwtBuilder.expirationTime(new Date(jwt.getExpiresAt().toEpochMilli()));
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
