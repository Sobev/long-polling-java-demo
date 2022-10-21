package longpolling.server;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.GsonBuilder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import longpolling.comm.ConfigDto;
import longpolling.comm.VerifyDto;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @author luojx
 * @date 2022/10/21 9:26
 */
@Controller
@RequestMapping("/config")
@Slf4j
public class ConfigController {
    /**
     * guava Multiple values provided Map，One key Can correspond to multiple value
     */
    private Multimap<String, AsyncTask> dataIdContext = Multimaps.synchronizedSetMultimap(HashMultimap.create());

    private ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("longPolling-timeout-checker-%d")
            .build();
    private ScheduledExecutorService timeoutChecker = new ScheduledThreadPoolExecutor(1, threadFactory);

    /**
     * storage all listener clients
     */
    private Cache<String, String> CLIENT_HEART_BEAT = CacheBuilder.newBuilder()
            .expireAfterWrite(40L, TimeUnit.SECONDS)
            .build();

    /**
     * dataId - listen file map
     */
    private ConcurrentHashMap<String, String> dataIdFile = new ConcurrentHashMap<>();

    @PostConstruct
    public void readDataIdPathFromFile() {
        try {
            File conf = ResourceUtils.getFile("classpath:dataIdFile.conf");
            BufferedReader reader = new BufferedReader(new FileReader(conf));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] split = line.split(" ");
                dataIdFile.put(split[0], split[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
    @ResponseBody
    @SneakyThrows
    public Res<String> publishConfig(@RequestBody ConfigDto configDto) {
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
        return Res.success("success");
    }

    @GetMapping("/clients")
    public String connectedClients(Model model) {
        System.out.println(CLIENT_HEART_BEAT.size());
        List<ClientVo> clients = CLIENT_HEART_BEAT.asMap().keySet()
                .stream()
                .map(client -> {
                    String[] split = client.split("_");
                    return new ClientVo(split[0], split[1]);
                })
                .collect(Collectors.toList());
        model.addAttribute("clients", clients);
        return "index";
    }

    @GetMapping("/getFileContent")
    @ResponseBody
    public Res<String> getFileContentByDataId(@RequestParam("dataId") String dataId) throws IOException {
        if (!StringUtils.hasText(dataId)) {
            return Res.error("dataId null");
        }
        if (!dataIdFile.containsKey(dataId)) {
            return Res.error("no such dataId");
        }
        String path = dataIdFile.get(dataId);
        StringBuilder builder = new StringBuilder();
        try(BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return Res.success(builder.toString());
    }

    @PostMapping("/addDataId")
    @ResponseBody
    public Res<String> addDataId(@RequestBody DataIdFileDto dto) {
        if (dataIdFile.get(dto.getDataId()) != null) {
            return Res.error("dataId already exists");
        }
        File file = new File(dto.getFilePath());
        if (!file.exists()) {
            return Res.error("file not exists, please create one");
        }
        dataIdFile.put(dto.getDataId(), dto.getFilePath());
        return Res.success("success");
    }

    @PostMapping("verifyRes")
    @ResponseBody
    public void verifyRes(@RequestBody VerifyDto dto) {
        //change build id store into db
        log.info(dto.getVerify());
    }

    @PostMapping("queryBuild")
    @ResponseBody
    public Object queryBuild() {
        return 1;
    }
}
