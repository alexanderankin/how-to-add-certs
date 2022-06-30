import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;

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

            TrustingRestTemplate restTemplate = new TrustingRestTemplate();
            ResponseEntity<String> entity = restTemplate.getForEntity(serverContainer.httpBaseUrl(), String.class);
            assertNotNull(entity);
            assertNotNull(entity.getBody());
            assertTrue(entity.getBody().contains("<title>Welcome to nginx!</title>"));
            assertEquals(entity.getStatusCode(), HttpStatus.OK);

            assertThrows(ResourceAccessException.class, () -> restTemplate.getForEntity(serverContainer.httpsBaseUrl(), String.class));

            ResponseEntity<String> https = restTemplate.fix().getForEntity(serverContainer.httpsBaseUrl(), String.class);
            assertNotNull(https);
            assertNotNull(https.getBody());
            assertTrue(https.getBody().contains("<title>Welcome to nginx!</title>"));
            assertEquals(https.getStatusCode(), HttpStatus.OK);
        }
    }
}
