package longpolling.client;

import ch.qos.logback.classic.Level;
import com.google.gson.Gson;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
public class ConfigClient {

    private final CloseableHttpClient httpClient;
    private final RequestConfig requestConfig;

    public ConfigClient() {
        this.httpClient = HttpClientBuilder.create().build();
        // ① httpClient The client timeout should be greater than the timeout of the long polling agreement
        this.requestConfig = RequestConfig.custom().setSocketTimeout(40000).build();
    }

    @SneakyThrows
    public void longPolling(String url, String dataId) {
        String endpoint = url + "?dataId=" + dataId;
        HttpGet request = new HttpGet(endpoint);
        try {
            CloseableHttpResponse response = httpClient.execute(request);
            switch (response.getStatusLine().getStatusCode()) {
                case 200:
                    BufferedReader bf = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8));
                    StringBuilder builder = new StringBuilder();
                    String line;
                    while ((line = bf.readLine()) != null) {
                        builder.append(line);
                    }
                    response.close();
                    String res = builder.toString();
                    log.info("dataId: [{}] changed, receive configInfo: {}", dataId, res);
                    ConfigDto configDto = new Gson().fromJson(res, ConfigDto.class);
                    //write file
                    writeFile("D:\\JF\\nginx.conf", configDto.getConfigInfo());
                    List<String> checkStatusCmd = configDto.getCheckStatusCmd();
                    ShellCallback shellCallback = new ShellCallback();
                    if (checkStatusCmd != null && checkStatusCmd.size() > 0) {
                        //TODO: check file changed Follow-up
                        for (String cmd : checkStatusCmd) {
                            Object[] callback = shellCallback.callback(cmd, s -> ShellUtil.shellCommand(cmd, "/etc/nginx"));
                            if((Integer) callback[0] == 1) {
                                //execute err
                                //TODO: call run err api
                                break;
                            }

                        }
                    }
                    longPolling(url, dataId);
                    break;
                // ② 304 Response code tag configuration unchanged
                case 304:
                    log.info("longPolling dataId: [{}] once finished, configInfo is unchanged, longPolling again", dataId);
                    longPolling(url, dataId);
                    break;
                default:
//                throw new RuntimeException("unExpected HTTP status code");
            }
        } catch (Exception e) {
            log.error("lost connection!!!");
            Thread.sleep(5000L);
            longPolling(url, dataId);
        }
    }

    private static void writeFile(String filePath, String content) {
        File file = new File(filePath);
        if (!file.exists()) {
            try {
                boolean create = file.createNewFile();
                if (!create) {
                    log.error("file create failed");
                }
            } catch (IOException e) {
                log.error("file create failed");
            }
        }
        try (FileWriter fileWriter = new FileWriter(file)) {
            //replace the content
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(content);
            bufferedWriter.flush();
            bufferedWriter.close();
            log.info("file write done");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String shellCommand(String commandLine, String dir) {
        try {
            Process process =
                    new ProcessBuilder(new String[]{"bash", "-c", commandLine})
                            .redirectErrorStream(true)
                            .directory(new File(dir))
                            .start();
            StringBuilder builder = new StringBuilder();
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                builder.append(line);
            }
//            if (process.waitFor() == 0) {
//                System.out.println("Success!");
//            }
            return builder.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        // httpClient It prints a lot debug Log, close it
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("org.apache.http");
        logger.setLevel(Level.WARN);
        ConfigClient configClient = new ConfigClient();
        logger.info("client started");
        // ③ yes dataId: user Configuration monitoring
        configClient.longPolling("http://127.0.0.1:8989/listener", "gcc7436c-6ca8-430c-8dbc-1fec7b67bd3a");
    }
}
