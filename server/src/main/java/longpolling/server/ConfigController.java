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
import longpolling.server.model.ChangeFileDirDto;
import longpolling.server.model.ClientVo;
import longpolling.server.model.DataIdFileDto;
import longpolling.server.util.FileAccessorUtil;
import longpolling.server.util.Res;
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
import java.util.*;
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

        CLIENT_HEART_BEAT.put(dataId, request.getRemoteHost());
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

    @GetMapping({"/**", "/clients"})
    public String connectedClients(Model model) {
        System.out.println(CLIENT_HEART_BEAT.size());
        Map<String, String> clientMap = CLIENT_HEART_BEAT.asMap();
        List<ClientVo> clientVos = dataIdFile.entrySet().stream()
                .map(entry -> {
                    ClientVo clientVo;
                    if (clientMap.containsKey(entry.getKey())) {
                        clientVo = new ClientVo(clientMap.get(entry.getKey()), entry.getKey());
                        clientVo.setStatus(true);
                    } else {
                        clientVo = new ClientVo("_", entry.getKey());
                        clientVo.setStatus(false);
                    }
                    String filepath = entry.getValue();
                    int index = filepath.lastIndexOf(File.separator);
                    String path = filepath.substring(0, index);
                    String filename = filepath.substring(index + 1);
                    clientVo.setPath(path);
                    clientVo.setFilename(filename);
                    return clientVo;
                }).collect(Collectors.toList());

        model.addAttribute("clients", clientVos);
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
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
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

    @PostMapping("/changeFileDir")
    @ResponseBody
    public Res<String> changeFileDir(@RequestBody ChangeFileDirDto dto) {
        if (!StringUtils.hasText(dto.getPath())) {
            return Res.error("not valid path");
        }
        //change dataIdFile map then change conf file
//        boolean b = FileAccessorUtil.writeFile(dto.getDataId(), dto.getPath() + File.separator + dto.getFilename());
//        if(!b) {
//            return Res.error("file write failed");
//        }
        dataIdFile.put(dto.getDataId(), dto.getPath() + File.separator + dto.getFilename());
        return Res.success("success");
    }

    @PostMapping("verifyRes")
    @ResponseBody
    public void verifyRes(@RequestBody VerifyDto dto) {
        //change build id store into db
        log.info("file change callback {}", dto.getVerify());
    }

    @PostMapping("queryBuild")
    @ResponseBody
    public Object queryBuild() {
        return 1;
    }
}
