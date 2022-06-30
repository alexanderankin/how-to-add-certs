import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class Main {
    /**
     * openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -sha256 -days 365
     *
     * @param args program arguments
     * @see <a href="https://stackoverflow.com/a/10176685">Stackoverflow answer</a>
     */
    @SneakyThrows
    public static void main(String[] args) {
        Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);

        // validate args
        if (args.length != 3) throw new IllegalStateException("Usage: <certOut> <keyOut> path to output folder...");

        // parse args
        List<String> a = Arrays.asList(args);
        String certFileName = !StringUtils.isEmpty(a.get(0)) ? a.get(0) : "cert.pem";
        String keyFileName = !StringUtils.isEmpty(a.get(1)) ? a.get(1) : "cert-key.pem";
        Path outputDirectory = !StringUtils.isEmpty(a.get(2)) ? Paths.get(a.get(2)) : Paths.get("certs");

        FileUtils.forceMkdir(outputDirectory.toFile());

        try (GenericContainer<?> c = new GenericContainer<>("alpine")) {
            c.setCommand("sleep", "infinity");
            c.start();

            Container.ExecResult apkResult = c.execInContainer("apk", "add", "openssl");
            Assert.assertEquals("openssl added", 0, apkResult.getExitCode());

            Container.ExecResult opensslResult = c.execInContainer("openssl",
                    "req",
                    "-x509",
                    "-nodes",
                    "-newkey",
                    "rsa:4096",
                    "-keyout",
                    keyFileName,
                    "-out",
                    certFileName,
                    "-sha256",
                    "-days",
                    "365",
                    "-subj",
                    "/C=US/ST=Oregon/L=Portland/O=Company Name/OU=Org/CN=localhost");
            Assert.assertEquals("openssl added", 0, opensslResult.getExitCode());

            copyFile(outputDirectory, certFileName, c.execInContainer("cat", certFileName));
            copyFile(outputDirectory, keyFileName, c.execInContainer("cat", keyFileName));
        }
    }

    private static void copyFile(Path where, String what, Container.ExecResult c) throws IOException {
        Files.writeString(where.resolve(what), c.getStdout(), StandardCharsets.UTF_8);
    }
}
