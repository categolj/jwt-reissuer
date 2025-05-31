package am.ik.reissuer.web;

import am.ik.reissuer.jwt.JwtProps;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
public class OidcController {

	private final JwtProps jwtProps;

	public OidcController(JwtProps jwtProps) {
		this.jwtProps = jwtProps;
	}

	// https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_providers_create_oidc.html#manage-oidc-provider-prerequisites
	@GetMapping(path = "/.well-known/openid-configuration")
	public Map<String, Object> openIdConfiguration(UriComponentsBuilder builder) {
		return Map.of("issuer", builder.replacePath("").build().toString(), "jwks_uri",
				builder.replacePath("openid/v1/jwks").build().toString(), "response_types_supported",
				List.of("id_token"), "id_token_signing_alg_values_supported", List.of("RS256"),
				"subject_types_supported", List.of("public"));
	}

	@GetMapping(path = "/openid/v1/jwks")
	public Map<String, Object> tokenKeys() {
		RSAKey key = new RSAKey.Builder(this.jwtProps.publicKey()).keyID(this.jwtProps.keyId()).build();
		return new JWKSet(key).toJSONObject();
	}

}
