package longpolling.client;

import java.util.function.Function;

/**
 * @author luojx
 * @date 2022/10/17 15:25
 */
public class ShellCallback {
    <K, R> R callback(K param, Function<K, R> function) {
        return function.apply(param);
    }
}
