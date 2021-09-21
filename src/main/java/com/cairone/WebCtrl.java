package com.cairone;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchProviderException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.HashMap;
import java.util.Map;

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WebCtrl {

	private final RestTemplate restTemplate;
	
	@Value("${app.googleapis.url} ")
	private String googleApiUrl;
	
	@GetMapping("/{serviceAccount}")
	@ResponseStatus(code = HttpStatus.OK)
	public PubKeyDto getPublicKey(
			@PathVariable String serviceAccount, @RequestParam String kid) throws CertificateException, IOException, NoSuchProviderException {
		
		ParameterizedTypeReference<HashMap<String, String>> responseType = 
	               new ParameterizedTypeReference<HashMap<String, String>>() {};
	               
        RequestEntity<Void> request = RequestEntity
    		   .get(googleApiUrl + "/" + serviceAccount)
               .accept(MediaType.APPLICATION_JSON)
               .build();
       
        Map<String, String> keys = restTemplate.exchange(request, responseType).getBody();
		String cert = keys.get(kid);
        
		String pubKeyPem;
		
		try(InputStream is = new ByteArrayInputStream(cert.getBytes(StandardCharsets.UTF_8))) { 
	
			CertificateFactory certFactory = CertificateFactory.getInstance("X.509", "BC");
			Certificate cer = certFactory.generateCertificate(is);
			pubKeyPem = toPEM("PUBLIC KEY", cer.getPublicKey().getEncoded());
		}
		
		return new PubKeyDto(pubKeyPem);
	}
	
	public String toPEM(String header, byte[] encoded) throws IOException {

		PemObject pemObject = new PemObject(header, encoded);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try (PemWriter pemWriter = new PemWriter(new OutputStreamWriter(baos))) {
			pemWriter.writeObject(pemObject);
		}

		return baos.toString();
	}
}
