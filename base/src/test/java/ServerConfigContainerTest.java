import lombok.SneakyThrows;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import static org.junit.Assert.*;

public class ServerConfigContainerTest {
    @Test
    public void showUsage() {
        try (ConfiguredServerContainer serverContainer = new ConfiguredServerContainer()) {
            serverContainer.start();
            serverContainer.copyConfigs();
            System.out.println(serverContainer.httpBaseUrl());
            System.out.println(serverContainer.httpsBaseUrl());
        }
    }

    @Test
    public void test() {
        try (ConfiguredServerContainer serverContainer = new ConfiguredServerContainer()) {
            serverContainer.start();
            serverContainer.copyConfigs();

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> entity = restTemplate.getForEntity(serverContainer.httpBaseUrl(), String.class);
            assertNotNull(entity);
            assertNotNull(entity.getBody());
            assertTrue(entity.getBody().contains("<title>Welcome to nginx!</title>"));
            assertEquals(entity.getStatusCode(), HttpStatus.OK);

            assertThrows(ResourceAccessException.class, () -> restTemplate.getForEntity(serverContainer.httpsBaseUrl(), String.class));

            restTemplate.setRequestFactory(trustingCerts());
            ResponseEntity<String> https = restTemplate.getForEntity(serverContainer.httpsBaseUrl(), String.class);
            assertNotNull(https);
            assertNotNull(https.getBody());
            assertTrue(https.getBody().contains("<title>Welcome to nginx!</title>"));
            assertEquals(https.getStatusCode(), HttpStatus.OK);
        }
    }

    @SneakyThrows
    SSLContext sslContext() {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate caCert = (X509Certificate) cf.generateCertificate(getClass().getResourceAsStream("cert.pem"));

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null);
        ks.setCertificateEntry("caCert", caCert);

        tmf.init(ks);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);
        return sslContext;
    }

    private ClientHttpRequestFactory trustingCerts() {
        final HttpClient httpClient = HttpClientBuilder.create().setSSLContext(sslContext()).build();

        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }
}
