import lombok.SneakyThrows;
import org.testcontainers.containers.NginxContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

public class ConfiguredServerContainer extends NginxContainer<ConfiguredServerContainer> {
    public ConfiguredServerContainer() {
        this(DockerImageName.parse("nginx:alpine"));
    }

    @SneakyThrows
    public ConfiguredServerContainer(DockerImageName imageName) {
        super(imageName);
        addExposedPort(80);
        addExposedPort(443);
        rename();
    }

    @SneakyThrows
    public static void main(String[] args) {
        try (ConfiguredServerContainer serverContainer = new ConfiguredServerContainer()) {
            serverContainer.start();
            serverContainer.copyConfigs();
            System.out.println(serverContainer.httpBaseUrl());
            System.out.println(serverContainer.httpsBaseUrl());
        }
    }

    @SuppressWarnings("resource")
    private void rename() {
        withCreateContainerCmdModifier(m -> m.withName("serverContainer-" + UUID.randomUUID()));
    }

    public void startAndCopyConfigs() {
        start();
        copyConfigs();
    }

    public String cert() {
        return new String(readFromJar("cert.pem"), StandardCharsets.UTF_8);
    }

    public String certKey() {
        return new String(readFromJar("cert-key.pem"), StandardCharsets.UTF_8);
    }

    @SneakyThrows
    public void copyConfigs() {
        copyNginxCert("cert.pem");
        copyNginxCert("cert-key.pem");
        execInContainer("rm", "/etc/nginx/conf.d/*");
        copyFileToContainer(Transferable.of(readFromJar("default.conf")), "/etc/nginx/conf.d/default.conf");
        execInContainer("nginx", "-s", "reload");
    }

    private void copyNginxCert(String filename) {
        copyFileToContainer(Transferable.of(readFromJar(filename)), "/nginx-certs/" + filename);
    }

    @SneakyThrows
    private byte[] readFromJar(String filename) {
        try (InputStream stream = getClass().getResourceAsStream(filename)) {
            Objects.requireNonNull(stream, "no " + filename + " found");
            return stream.readAllBytes();
        }
    }

    @SneakyThrows
    public URI httpBaseUrl() {
        return super.getBaseUrl("http", 80).toURI();
    }

    @SneakyThrows
    public URI httpsBaseUrl() {
        return super.getBaseUrl("https", 443).toURI();
    }
}
