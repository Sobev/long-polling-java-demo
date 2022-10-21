package longpolling.server;


/**
* @author luojx
*/
public enum StatusCodeEnum {

    /**
     *
     */
    SUCCESS(0, "success"),

    REQUEST_ERROR(1, "server error"),

    ERROR(2, "request param error");


    private int code;

    private String msg;

    StatusCodeEnum(int code, String msg){
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

}
