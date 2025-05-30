package am.ik.k8s.oidc.web;

import am.ik.k8s.oidc.jwt.JwtProps;
import am.ik.k8s.oidc.jwt.JwtSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import java.text.ParseException;
import java.util.LinkedHashMap;
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
	public String token(@AuthenticationPrincipal Jwt jwt, UriComponentsBuilder builder) throws ParseException {
		System.out.println("Given JWT: " + jwt.getClaims());
		String issuer = builder.path("").build().toString();
		Map<String, Object> claims = new LinkedHashMap<>(jwt.getClaims());
		// Replace the issuer and audience claims in the given JWT
		claims.put("iss", issuer);
		claims.put("aud", this.jwtProps.audience());
		return this.jwtSigner.sign(JWTClaimsSet.parse(claims)).serialize();
	}

}
