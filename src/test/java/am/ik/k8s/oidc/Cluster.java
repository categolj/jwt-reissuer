package am.ik.k8s.oidc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;

record Cluster(String server, String certificateAuthorityData, String clientCertificateData, String clientKeyData) {

	static Cluster fromConfigYaml(String yaml) {
		String server = null;
		String certificateAuthorityData = null;
		String clientCertificateData = null;
		String clientKeyData = null;
		try (BufferedReader reader = new BufferedReader(new StringReader(yaml))) {
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();

				if (line.startsWith("server:")) {
					server = line.substring(line.indexOf('"') + 1, line.lastIndexOf('"'));
				}
				else if (line.startsWith("certificate-authority-data:")) {
					certificateAuthorityData = line.substring(line.indexOf('"') + 1, line.lastIndexOf('"'));
				}
				else if (line.startsWith("client-certificate-data:")) {
					clientCertificateData = line.substring(line.indexOf('"') + 1, line.lastIndexOf('"'));
				}
				else if (line.startsWith("client-key-data:")) {
					clientKeyData = line.substring(line.indexOf('"') + 1, line.lastIndexOf('"'));
				}
			}
			return new Cluster(server, certificateAuthorityData, clientCertificateData, clientKeyData);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
