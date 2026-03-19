import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileUtil {

    public static String getFile(String filePath) {
        try {
            return new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException e) {
            System.err.println("读取文件失败: " + e.getMessage());
            return "";
        }
    }
}
