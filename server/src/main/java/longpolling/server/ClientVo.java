package longpolling.server;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author luojx
 * @date 2022/10/21 13:49
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientVo {
    private String ip;
    private String uuid;
}
