package com.sobev.longpolling.server;


import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.*;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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

    private static final Cache<String, String> CLIENT_HEART_BEAT = CacheBuilder.newBuilder()
            .expireAfterWrite(40L, TimeUnit.SECONDS)
            .build();

    @RequestMapping("/listener")
    public void addListener(HttpServletRequest request, HttpServletResponse response) {
        String dataId = request.getParameter("dataId");
        CLIENT_HEART_BEAT.put("CLIENT_HEART_BEAT", request.getRemoteHost() + "_" + dataId);
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
            response.getWriter().println(configDto.getConfigInfo());
            task.getAsyncContext().complete();
        }
        return "success";
    }

    @PostMapping("/clients")
    public Object connectedClients() {
        ImmutableMap<String, String> client_heart_beat = CLIENT_HEART_BEAT.getAllPresent(ImmutableList.of("CLIENT_HEART_BEAT"));
        return client_heart_beat;
    }

    public static void main(String[] args) {
        SpringApplication.run(ConfigServer.class, args);
    }
}
