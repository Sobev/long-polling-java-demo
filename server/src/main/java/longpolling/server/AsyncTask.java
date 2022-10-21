package longpolling.server;

import lombok.Data;

import javax.servlet.AsyncContext;

/**
 * @author luojx
 * @date 2022/10/21 9:28
 */
@Data
public class AsyncTask {
    // The context of the long polling request, including the request and response body
    private AsyncContext asyncContext;
    // Timeout flag
    private boolean timeout;

    public AsyncTask(AsyncContext asyncContext, boolean timeout) {
        this.asyncContext = asyncContext;
        this.timeout = timeout;
    }
}
