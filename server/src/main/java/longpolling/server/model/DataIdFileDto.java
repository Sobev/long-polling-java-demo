package longpolling.server.model;

import lombok.Data;

/**
 * @author luojx
 * @date 2022/10/21 9:20
 */
@Data
public class DataIdFileDto {
    /** 数据标识 */
    private String dataId;
    /** 文件路径 /etc/nginx/nginx.conf */
    private String filePath;
}
