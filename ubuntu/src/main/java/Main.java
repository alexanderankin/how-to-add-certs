import lombok.SneakyThrows;
import org.junit.Assert;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public class Main implements Runnable {
    public static void main(String[] args) {
        new Main().run();
    }

    private static String b64(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    @SneakyThrows
    @Override
    public void run() {
        try (ConfiguredServerContainer server = new ConfiguredServerContainer()) {
            server.startAndCopyConfigs();

            try (GenericContainer<?> ubuntu = new GenericContainer<>(
                    new ImageFromDockerfile()
                            .withDockerfileFromBuilder(b -> {
                                b.from("ubuntu:18.04");
                                b.run("apt-get update");
                                b.run("apt-get install -y wget ca-certificates");
                                b.run("echo '" + b64(server.cert()) + "' | base64 -d > /cert.pem");
                            })
            )) {
                ubuntu.withCreateContainerCmdModifier(m -> m.withName("ubuntu-" + UUID.randomUUID()))
                        .withNetworkMode("host")
                        .withCommand("sleep", "infinity")
                        .start();

                String http = server.httpBaseUrl().toString();
                Container.ExecResult httpResult = ubuntu.execInContainer("wget", "-q", http, "-O", "-");
                int httpResultCode = httpResult.getExitCode();
                Assert.assertEquals(0, httpResultCode);

                {
                    String https = server.httpsBaseUrl().toString();
                    Container.ExecResult httpsResult = ubuntu.execInContainer("wget", https, "-O", "-");
                    int httpsResultCode = httpsResult.getExitCode();
                    Assert.assertNotEquals(0, httpsResultCode);
                }

                // needlessly complicated ubuntu-ism
                ubuntu.execInContainer("sh", "-c", "echo my-certs/cert.crt >> /etc/ca-certificates.conf");
                ubuntu.execInContainer("mkdir", "-p", "/usr/share/ca-certificates/my-certs");
                ubuntu.execInContainer("sh", "-c", "mv /cert.pem /usr/share/ca-certificates/my-certs/cert.crt");
                ubuntu.execInContainer("sh", "-c", "update-ca-certificates");

                {
                    String https = server.httpsBaseUrl().toString();
                    Container.ExecResult httpsResult = ubuntu.execInContainer("wget", https, "-O", "-");
                    int httpsResultCode = httpsResult.getExitCode();
                    Assert.assertEquals(0, httpsResultCode);
                }
            }
        }
    }
}
