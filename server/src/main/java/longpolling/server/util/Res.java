package longpolling.server.util;

import lombok.Data;

import java.io.Serializable;

/**
 * @author luojx
 */
@Data
public class Res<T> implements Serializable {
    private static final long serialVersionUID = 6115763584010270155L;

    private Integer code;
    private String message;
    private T data;
    public Res(){

    }
    private Res(StatusCodeEnum codeEnum, T data){
        this.code = codeEnum.getCode();
        this.message = codeEnum.getMsg();
        this.data = data;
    }

    private Res(StatusCodeEnum codeEnum, String message){
        this.code = codeEnum.getCode();
        this.message = codeEnum.getMsg();
    }

    private Res(int status, String message, T data){
        this.code = status;
        this.message = message;
        this.data = data;
    }
    /**
     * 成功
     * @param data
     * @param <W>
     * @return
     */
    public static <W>Res<W> success(W data){
        return new Res<>(StatusCodeEnum.SUCCESS, data );
    }

    /**
     * 成功(自定义，前端不会拦截，由具体业务决定如何处理)
     * @param message : 业务提示消息
     * @param data : 可以放自定义业务状态标志位
     * @return
     */
    public static <W>Res<W> success(String message, W data){
        return new Res<>(StatusCodeEnum.SUCCESS.getCode(), message, data );
    }

    /**
     * 业务失败
     * @param code : 业务失败状态码 StatusCodeEnum
     * @param message : 提示
     * @param data : 失败操作后返回的业务数据
     * @return
     */
    public static<T> Res<T> fail(int code, String message, T data){
        return new Res<>(code, message, data);
    }
    /**
     * 服务端异常
     * @param message
     * @return
     */
    public static<T> Res<T> error(String message){
        return new Res<>(StatusCodeEnum.ERROR.getCode(), message, null );
    }

    /**
     * 请求错误
     * @param
     * @return
     */
    public static Res<Object> requestError(){
        return new Res<>(StatusCodeEnum.REQUEST_ERROR, null );
    }

    /**
     * 请求错误
     * @param message 提示信息
     * @return
     */
    public static <T>Res<T> requestError(String message){
        return new Res<>(StatusCodeEnum.REQUEST_ERROR.getCode(), message,null );
    }

}
