package GenerateSql.venv;

import cn.hutool.core.io.FileUtil;
import GenerateSql.Util.MyFileUtil;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SqlGenerator {

    private Config generateConfig;             // 用户传进来的配置对象
    private String engineType;                 // 引擎类型（默认 SparkSQL）
    private String path;                       // 待处理文件目录（如 ./data/）
    private String templatePath;               // 模板文件路径
    private String sourceDbName;               // 源数据库名
    private String sinkDbName;                 // 目标数据库名
    private String sourceTablePre;             // 源表前缀（如 "src_"）
    private String sinkTablePre;               // 目标表前缀（如 "tgt_"）
    private Map<String, Object> extendedInfo;  // 扩展参数（如 primaryKey="id"）
    private TimeZoneConfig timeZoneConfig;     // 时区配置

    public SqlGenerator(Config config) {
        this.generateConfig = config;
        initializeConfig();     //把配置中的值赋给本类的成员变量
    }

    private void initializeConfig() {
        this.engineType = generateConfig.getEngineType() != null ?
                generateConfig.getEngineType() : "SparkSQL";
        this.path = generateConfig.getFilePath();
        this.templatePath = generateConfig.getTemplatePath();
        this.sourceDbName = generateConfig.getSourceDbName();
        this.sinkDbName = generateConfig.getSinkDbName();
        this.sourceTablePre = generateConfig.getSourceTablePre();
        this.sinkTablePre = generateConfig.getSinkTablePre();
        this.extendedInfo = generateConfig.getExtendedInfo();
        this.timeZoneConfig = generateConfig.getTimeZoneConfig();
    }

    /**
     * 根据模板替换SQL
     */
    public String generateSql(Map<String, Object> generateConfigDicts, String template) {
        String sql = "";

        try {
            sql = replacePlaceholders(template, generateConfigDicts);
        } catch (Exception e) {
            System.out.println("当前模板存在部分key未被替换请注意：" + e.getMessage());
            sql = safeReplacePlaceholders(template, generateConfigDicts);
        }

        return sql;
    }

    /**
     * 安全替换占位符
     */
    private String safeReplacePlaceholders(String template, Map<String, Object> values) {
        String result = template;
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(result);

        while (matcher.find()) {
            String placeholder = matcher.group(0); // 包含 ${}
            String key = matcher.group(1); // 不包含 ${}

            if (values.containsKey(key)) {
                Object value = values.get(key);
                result = result.replace(placeholder, value.toString());
            }
        }

        return result;
    }

    /**
     * 正常替换占位符
     */
    private String replacePlaceholders(String template, Map<String, Object> values) {
        String result = template;
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(result);

        while (matcher.find()) {
            String placeholder = matcher.group(0); // 包含 ${}
            String key = matcher.group(1); // 不包含 ${}

            if (!values.containsKey(key)) {
                throw new IllegalArgumentException("缺少键: " + key);
            }

            Object value = values.get(key);
            result = result.replace(placeholder, value.toString());
        }

        return result;
    }

    /**
     * 合并两个Map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> merge(Map<String, Object> dict1, Map<String, Object> dict2) {
        Map<String, Object> res = new HashMap<>(dict1);
        if (dict2 != null) {
            res.putAll(dict2);
        }
        return res;
    }

    /**
     * 处理datetime类型column的时区
     */
    public String columnTimeZoneConfig(String column) {
        int time = timeZoneConfig.getTimeZone();
        System.out.println("当前时间为：" + time);

        if ("SparkSQL".equals(engineType)) {
            String operator = time < 0 ? "-" : "+";
            return String.format("%s %s interval %d hours as %s", column, operator, Math.abs(time), column);
        }
        return column;
    }

    /**
     * 获取列列表
     */
    public String getColumnsList(Path filePath, boolean isHandleTimeZone) throws IOException {
        List<String> columns = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split("[\\s\\t]+", 2); // 最多分2段，避免空指针
                if (parts.length >= 2) {
                    String column = parts[0];
                    String columnType = parts[1].replaceAll("[\\r\\n]", "");
                    if (isHandleTimeZone && "DateTime".equals(columnType)) {
                        column = columnTimeZoneConfig(column);
                    }
                    columns.add(column);
                }
            }
        }
        return String.join(",", columns);
    }

    /**
     * 执行主要处理逻辑
     */
    public void processFiles() {
        try {
            // 获取文件夹下的所有待处理文件
            Path basePath = Paths.get(this.path); // this.path 来自 config.filePath
            if (!Files.exists(basePath) || !Files.isDirectory(basePath)) {
                throw new IllegalArgumentException("目录不存在或非目录: " + basePath);
            }
            List<Path> filePaths = Files.walk(basePath)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".txt"))
                    .collect(Collectors.toList());

            // 获取模板信息
            String template = MyFileUtil.getFile(templatePath);

            // 如果需要进行时区处理则将最外层columns 替换为columnsAddTimeZone
            String processedTemplate = template;
            if (timeZoneConfig != null && timeZoneConfig.getEnable()) {
                processedTemplate = template.replaceFirst("\\$\\{columns\\}", "\\$\\{columnsAddTimeZone\\}");
            }

            for (Path filePath : filePaths) {
                String fileName = filePath.getFileName().toString();

                try (BufferedReader reader = Files.newBufferedReader(filePath)) {
                    // 标记流的当前位置，以便后续重用
                    reader.mark(Integer.MAX_VALUE);

                    // 处理tableName
                    String tableName = fileName.substring(0, fileName.lastIndexOf('.'));
                    String sourceTable = tableName; // 获取source表名称
                    String sinkTable = sinkTablePre + tableName.replaceFirst("^" + Pattern.quote(sourceTablePre), ""); // 获取sink表名称

                    System.out.println(String.format(
                            "当前处理的文件为：%s\n当前使用的模板为：%s\nsource表为：%s\nsink表为：%s",
                            fileName, templatePath, sourceTable, sinkTable));

                    // 获取字段名称，生成字段list
                    reader.reset(); // 重置到标记位置
                    String columnsList = getColumnsList(filePath, false);
                    // 生成模板参数,默认参数
                    Map<String, Object> defaultConfigDicts = new HashMap<>();
                    defaultConfigDicts.put("columns", columnsList);
                    defaultConfigDicts.put("sourceTable", sourceTable);
                    defaultConfigDicts.put("sinkTable", sinkTable);
                    defaultConfigDicts.put("sourceDbName", sourceDbName);
                    defaultConfigDicts.put("sinkDbName", sinkDbName);

                    // 获取字段名称，生成字段list,且针对时区字段处理
                    if (timeZoneConfig != null && timeZoneConfig.getEnable()) {
                        String columnsListAddTimeZone = getColumnsList(filePath, true);
                        defaultConfigDicts.put("columnsAddTimeZone", columnsListAddTimeZone);
                    }

                    Map<String, Object> generateConfigDicts = merge(defaultConfigDicts, extendedInfo); // 默认参数与模板参数合并
                    System.out.println("当前的实际参数替换列表如下：" + generateConfigDicts);

                    String sqlQuery = generateSql(generateConfigDicts, processedTemplate); // 生成SQL
                    System.out.println(String.format("生成的代码为：\n%s\n", sqlQuery));

                } catch (IOException e) {
                    System.err.println("处理文件 " + fileName + " 时出错: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.err.println("读取目录时出错: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        // 示例配置
        Config config = new Config();
        config.setEngineType("SparkSQL");
        config.setFilePath("C:\\java_learning\\java_projects\\git-demo\\Basic-code3\\GenerateSql\\exeFile");           // 相对路径
        config.setTemplatePath("C:\\java_learning\\java_projects\\git-demo\\Basic-code3\\GenerateSql\\template\\overwriteSql.txt");
        config.setSourceDbName("iceberg.dataware");
        config.setSinkDbName("iceberg.dataware");
        config.setSourceTablePre("autotest_");
        config.setSinkTablePre("dwd_asset_");

        // 设置扩展信息
        Map<String, Object> extendedInfo = new HashMap<>();
        extendedInfo.put("RowNumBerPartitionByList", "id");          // 分区字段
        extendedInfo.put("RowNumBerOrderByList", "modify_at");      // 排序字段
        extendedInfo.put("primaryKey", "id");
        extendedInfo.put("updateColumn", "update_at");
        config.setExtendedInfo(extendedInfo);

        // 设置时区配置
        TimeZoneConfig tzConfig = new TimeZoneConfig();
        tzConfig.setEnable(true);
        tzConfig.setTimeZone(-8);
        config.setTimeZoneConfig(tzConfig);

        SqlGenerator generator = new SqlGenerator(config);

        generator.processFiles();

//        Map<String, Object> testParams = new HashMap<>();
//        testParams.put("columns", "id,name,created_at");
//        testParams.put("sourceTable", "test_table");
//        testParams.put("sinkTable", "dwd_test");
//        testParams.put("sourceDbName", "db");
//        testParams.put("sinkDbName", "db");
//        testParams.put("RowNumBerPartitionByList", "id");
//        testParams.put("RowNumBerOrderByList", "created_at");
//
//        String template = FileUtil.getFile("./GenerateSql/template/overwriteSql.txt");
//        String sql = generator.generateSql(testParams, template); // 👈 直接调用
//
//        System.out.println("调试生成SQL：\n" + sql);
    }
}
