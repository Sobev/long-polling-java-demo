package longpolling.client;

import lombok.Data;

import java.util.List;

/**
 * @author luojx
 * @date 2022/10/20 13:24
 */
@Data
public class FileChangeVerifier {
    private List<String> verifyCommands;

    public FileChangeVerifier(List<String> verifyCommands) {
        this.verifyCommands = verifyCommands;
    }

    public FileChangeVerifier() {
    }

    public FileChangeVerifier addCommands(String command) {
        verifyCommands.add(command);
        return this;
    }

    public List<String> build() {
        return verifyCommands;
    }

    public String verify(String dir) {
        StringBuilder builder = new StringBuilder();
        for (String command : verifyCommands) {
            Object[] res = ShellUtil.shellCommand(command, dir);
            if((int)res[0] != 0) {
                return (String) res[1];
            }
            builder.append((String) res[1]);
            builder.append("\n");
        }
        return builder.toString();
    }
}
