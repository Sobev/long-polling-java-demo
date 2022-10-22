package longpolling.server.util;


import longpolling.server.util.Res;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author luojx
 */
@ControllerAdvice(basePackages = {"com.jf.rbac.controller"})
public class ControllerExceptionAdvice {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * 异常捕获 统一处理
     *
     * @param e e
     * @return json
     */
    @ResponseBody
    @org.springframework.web.bind.annotation.ExceptionHandler({Exception.class})
    public Res<Object> handleException(Exception e, HttpServletRequest request) {
        logger.error("{}统一捕获异常=>:", request.getRequestURI(), e);
        // json参数 格式异常
        if (e instanceof org.springframework.http.converter.HttpMessageNotReadableException) {
            return Res.requestError("json参数格式不正确");
        }
        //参数与约束不匹配
        if (e instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException validException = (MethodArgumentNotValidException) e;
            StringBuilder builder = new StringBuilder();
            List<FieldError> fieldErrors = validException.getBindingResult().getFieldErrors();
            for (FieldError fieldError : fieldErrors) {
                String message = String.format("%s :%s", fieldError.getField(), fieldError.getDefaultMessage());
                builder.append(message).append("  \n");
            }
            return Res.requestError(builder.toString());
        }

        return Res.error(e.getMessage());
    }
}
