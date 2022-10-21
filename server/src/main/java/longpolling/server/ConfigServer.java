package longpolling.server;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * @author luojx
 * @date 2021/9/6 14:08
 * https://www.fatalerrors.org/a/understanding-long-polling-how-configuration-center-implements-push.html
 */
@SpringBootApplication
public class ConfigServer {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServer.class, args);
    }
}
