package com.sobev.longpolling.client;

import ch.qos.logback.classic.Level;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;


import java.io.BufferedReader;
import java.io.InputStreamReader;

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
  public void longPolling(String url, String dataId){
    String endpoint = url + "?dataId=" + dataId;
    HttpGet request = new HttpGet(endpoint);
    CloseableHttpResponse response = httpClient.execute(request);
    switch (response.getStatusLine().getStatusCode()){
      case 200:
        BufferedReader bf = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = bf.readLine()) != null){
          builder.append(line);
        }
        response.close();
        String configInfo = builder.toString();
        log.info("dataId: [{}] changed, receive configInfo: {}", dataId, configInfo);
        longPolling(url,dataId);
        break;
      // ② 304 Response code tag configuration unchanged
      case 304:
        log.info("longPolling dataId: [{}] once finished, configInfo is unchanged, longPolling again", dataId);
        longPolling(url, dataId);
        break;
      default:throw new RuntimeException("unExpected HTTP status code");
    }
  }

  public static void main(String[] args) {
    // httpClient It prints a lot debug Log, close it
    ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("org.apache.http");
    logger.setLevel(Level.WARN);
    ConfigClient configClient = new ConfigClient();
    System.out.println("client started");
    // ③ yes dataId: user Configuration monitoring
    configClient.longPolling("http://127.0.0.1:8989/listener", "user");
  }
}
