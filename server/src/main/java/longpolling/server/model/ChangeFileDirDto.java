package longpolling.server.model;

import lombok.Data;

/**
 * @author luojx
 * @date 2022/10/22 11:25
 */
@Data
public class ChangeFileDirDto {
    private String dataId;
    /**
     * contains file name
     */
    private String path;
    private String filename;
}
