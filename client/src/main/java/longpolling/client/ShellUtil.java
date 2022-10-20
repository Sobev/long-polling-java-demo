package longpolling.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author luojx
 * @date 2022/10/17 15:39
 */
public class ShellUtil {
    /**
     *
     * @param commandLine
     * @param dir
     * @return object[2]{success or not, result}
     */
    public static Object[] shellCommand(String commandLine, String dir) {
        Object[] res = new Object[2];
        res[0] = 1;
        try {
            Process process =
                    new ProcessBuilder(new String[]{"bash", "-c", commandLine})
                            .redirectErrorStream(true)
                            .directory(new File(dir))
                            .start();
            StringBuilder builder = new StringBuilder();
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String line = null;
            while ((line = br.readLine()) != null) {
                builder.append(line);
            }
            if (process.waitFor() == 0) {
                res[0] = 0;
                System.out.println("Success!");
            }
            res[1] = builder.toString();
            return res;
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
