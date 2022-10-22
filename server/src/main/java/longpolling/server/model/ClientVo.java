package longpolling.server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author luojx
 * @date 2022/10/21 13:49
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class ClientVo {
    private String ip;
    private String uuid;
    private boolean status;
    private String path;
    private String filename;

    public ClientVo(String ip, String uuid) {
        this.ip = ip;
        this.uuid = uuid;
    }
}
