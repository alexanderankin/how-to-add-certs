import lombok.SneakyThrows;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class TrustingRestTemplate extends RestTemplate {
    public TrustingRestTemplate fix() {
        setRequestFactory(trustingOwnCert());
        return this;
    }

    private ClientHttpRequestFactory trustingOwnCert() {
        final HttpClient httpClient = HttpClientBuilder.create().setSSLContext(sslContext()).build();
        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }

    @SneakyThrows
    private SSLContext sslContext() {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, toTrustFactory(ownCert()).getTrustManagers(), null);
        return sslContext;
    }

    private X509Certificate ownCert() throws CertificateException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(getClass().getResourceAsStream("cert.pem"));
    }

    @NotNull
    private TrustManagerFactory toTrustFactory(X509Certificate caCert) throws NoSuchAlgorithmException, KeyStoreException, IOException, CertificateException {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null);
        ks.setCertificateEntry("caCert", caCert);
        tmf.init(ks);
        return tmf;
    }

}
