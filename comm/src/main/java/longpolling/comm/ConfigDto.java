package longpolling.comm;

import lombok.Data;

import java.util.List;

/**
 * @author luojx
 * @date 2022/10/17 9:18
 */
@Data
public class ConfigDto  {
    /** 数据标识 */
    private String dataId;
    /** 配置信息 */
    private String configInfo;
    /** 检查状态cmd exp: [nginx -t, nginx -s reload] */
    private List<String> checkStatusCmd;
    private String path;
    private String filename;
}
