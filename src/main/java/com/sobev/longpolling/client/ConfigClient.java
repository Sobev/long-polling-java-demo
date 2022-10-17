package com.sobev.longpolling.client;

import ch.qos.logback.classic.Level;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;


import java.io.*;

@Slf4j
public class ConfigClient {

    private CloseableHttpClient httpClient;
    private RequestConfig requestConfig;

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
                    BufferedReader bf = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
                    StringBuilder builder = new StringBuilder();
                    String line;
                    while ((line = bf.readLine()) != null) {
                        builder.append(line);
                    }
                    response.close();
                    String configInfo = builder.toString();
                    log.info("dataId: [{}] changed, receive configInfo: {}", dataId, configInfo);
                    //write file
                    writeFile("D:\\JF\\nginx.conf", configInfo);
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
            longPolling(url, dataId);
        }
    }

    private static void writeFile(String filePath, String content) {
        File file = new File(filePath);
        if (!file.exists()) {
            try {
                file.createNewFile();
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

    public static void main(String[] args) throws InterruptedException {
        // httpClient It prints a lot debug Log, close it
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("org.apache.http");
        logger.setLevel(Level.WARN);
        ConfigClient configClient = new ConfigClient();
        System.out.println("client started");
        // ③ yes dataId: user Configuration monitoring
//        Thread pollThread = new Thread(() -> {
//            configClient.longPolling("http://127.0.0.1:8989/listener", "user");
//        });
        configClient.longPolling("http://127.0.0.1:8989/listener", "user");

//        pollThread.join();
    }
}
