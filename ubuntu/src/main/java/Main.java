import lombok.SneakyThrows;

import java.nio.charset.StandardCharsets;

public class Main implements Runnable {
    public static void main(String[] args) {
        new Main().run();
    }

    @SneakyThrows
    @Override
    public void run() {
        System.out.println("hi");
        String s = new String(getClass().getClassLoader().getResourceAsStream("cert.pem").readAllBytes(), StandardCharsets.UTF_8);
        System.out.println(s);
    }
}
