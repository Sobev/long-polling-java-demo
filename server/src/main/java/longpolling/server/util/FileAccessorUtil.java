package longpolling.server.util;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ResourceUtils;

import java.io.*;

/**
 * @author luojx
 * @date 2022/10/22 13:52
 */
public class FileAccessorUtil {
    public static boolean writeFile(String dataId, String path) {
        try {
            StringBuilder builder = new StringBuilder();
            File conf = ResourceUtils.getFile("classpath:dataIdFile.conf");
            BufferedReader reader = new BufferedReader(new FileReader(conf));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] split = line.split(" ");
                if(split[0].equals(dataId)) {
                    builder.append(dataId + " " + path);
                }else {
                    builder.append(line);
                }
            }
            reader.close();
            BufferedWriter writer = new BufferedWriter(new FileWriter(conf));
            writer.write(builder.toString());
            writer.flush();
            writer.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void main(String[] args) {
        try {
            ClassPathResource resource = new ClassPathResource("dataIdFile.conf");
            BufferedWriter writer = new BufferedWriter(new FileWriter(resource.getFile()));
            writer.write("gcc7436c-6ca8-430c-8dbc-1fec7b67bd3a C:\\Users\\DELL\\Downloads\\longpolling.conf.pepsi");
            writer.flush();
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
