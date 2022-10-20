package longpolling.server;


import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.GsonBuilder;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import longpolling.comm.ConfigDto;
import longpolling.comm.VerifyDto;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;


/**
 * @author luojx
 * @date 2021/9/6 14:08
 * https://www.fatalerrors.org/a/understanding-long-polling-how-configuration-center-implements-push.html
 */
@Slf4j
@SpringBootApplication
@RestController
public class ConfigServer {
    @Data
    private static class AsyncTask {
        // The context of the long polling request, including the request and response body
        private AsyncContext asyncContext;
        // Timeout flag
        private boolean timeout;

        public AsyncTask(AsyncContext asyncContext, boolean timeout) {
            this.asyncContext = asyncContext;
            this.timeout = timeout;
        }
    }

    // guava Multiple values provided Map，One key Can correspond to multiple value
    private Multimap<String, AsyncTask> dataIdContext = Multimaps.synchronizedSetMultimap(HashMultimap.create());

    private ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("longPolling-timeout-checker-%d")
            .build();
    private ScheduledExecutorService timeoutChecker = new ScheduledThreadPoolExecutor(1, threadFactory);

    private Cache<String, String> CLIENT_HEART_BEAT = CacheBuilder.newBuilder()
            .expireAfterWrite(40L, TimeUnit.SECONDS)
            .build();

    @RequestMapping("/listener")
    public void addListener(HttpServletRequest request, HttpServletResponse response) {
        String dataId = request.getParameter("dataId");

        CLIENT_HEART_BEAT.put(request.getRemoteHost() + "_" + dataId, "alias");
        // Turn on asynchronous
        AsyncContext asyncContext = request.startAsync(request, response);
        AsyncTask asyncTask = new AsyncTask(asyncContext, true);

        // maintain dataId And asynchronous request context
        dataIdContext.put(dataId, asyncTask);
        timeoutChecker.schedule(() -> {
            if (asyncTask.isTimeout()) {
                dataIdContext.remove(dataId, asyncTask);
                response.setCharacterEncoding("UTF-8");
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                asyncContext.complete();
            }
        }, 30000, TimeUnit.MILLISECONDS);
    }

    // Configure publishing access point
    @PostMapping("/publishConfig")
    @SneakyThrows
    public String publishConfig(@RequestBody ConfigDto configDto) {
        log.info("publish configInfo dataId: [{}], configInfo: {}", configDto.getDataId(), configDto.getConfigInfo());
        Collection<AsyncTask> asyncTasks = dataIdContext.removeAll(configDto.getDataId());
        for (AsyncTask task : asyncTasks) {
            task.setTimeout(false);
            HttpServletResponse response = (HttpServletResponse) task.getAsyncContext().getResponse();
            response.setStatus(HttpServletResponse.SC_OK);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().println(new GsonBuilder().disableHtmlEscaping().create().toJson(configDto));
            task.getAsyncContext().complete();
        }
        return "success";
    }

    @PostMapping("/clients")
    public Object connectedClients() {
        System.out.println(CLIENT_HEART_BEAT.size());
        Set<String> set = CLIENT_HEART_BEAT.asMap().keySet();
        return set;
    }

    @PostMapping("/verifyRes")
    public void verifyRes(@RequestBody VerifyDto dto) {
        log.info(dto.getVerify());
    }

    public static void main(String[] args) {
        SpringApplication.run(ConfigServer.class, args);
    }
}
