// engineType     : 引擎类型,默认SparkSQL
// filePath       : 待处理文件 =》 metro-AepMergeConfig-generator 处理完毕的 txt文件目录
// templatePath   : 模板文件路径
// sourceDbName   : 源头库名称
// sinkDbName     : 目标库名称
// sourceTablePre : 源头表的前缀
// sinkTablePre   : 目标表的前缀
// extendedInfo   : 拓展信息=》除默认参数外，用户可以自定义参数是的模板中的 ${} 变为用户所需要的字符串， 其中 key为 ${} 中的文本（如${columns},则key为columns），value为：要替换的值
// timeZoneConfig : 如果为true,则默认将 DateTime 减八时区 （sparkSql语法
// 保留关键字如下:
//       # columns                  : 字段list
// sinkDbName               : 目标端数据库名称
// sinkTable                : 目标端表名称
// sourceDbName             : 源端数据库名称
// sourceTable              : 源端表名称
// primaryKey               : 主键，仅支持一个字段
// updateColumn             : 更新字段，仅支持一个字段
// RowNumBerPartitionByList : rowNumber分区字段列表
// RowNumBerOrderByList     : rowNumber排序字段列表

public class Config {
    private String engineType;
    private String filePath;
    private String templatePath;
    private String sourceDbName;
    private String sinkDbName;
    private String sourceTablePre;
    private String sinkTablePre;
    private java.util.Map<String, Object> extendedInfo;
    private TimeZoneConfig timeZoneConfig;

    // 构造函数
    public Config() {}

    // Getter 和 Setter 方法
    public String getEngineType() { return engineType; }
    public void setEngineType(String engineType) { this.engineType = engineType; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getTemplatePath() { return templatePath; }
    public void setTemplatePath(String templatePath) { this.templatePath = templatePath; }

    public String getSourceDbName() { return sourceDbName; }
    public void setSourceDbName(String sourceDbName) { this.sourceDbName = sourceDbName; }

    public String getSinkDbName() { return sinkDbName; }
    public void setSinkDbName(String sinkDbName) { this.sinkDbName = sinkDbName; }

    public String getSourceTablePre() { return sourceTablePre; }
    public void setSourceTablePre(String sourceTablePre) { this.sourceTablePre = sourceTablePre; }

    public String getSinkTablePre() { return sinkTablePre; }
    public void setSinkTablePre(String sinkTablePre) { this.sinkTablePre = sinkTablePre; }

    public java.util.Map<String, Object> getExtendedInfo() { return extendedInfo; }
    public void setExtendedInfo(java.util.Map<String, Object> extendedInfo) {
        this.extendedInfo = extendedInfo;
    }

    public TimeZoneConfig getTimeZoneConfig() { return timeZoneConfig; }
    public void setTimeZoneConfig(TimeZoneConfig timeZoneConfig) {
        this.timeZoneConfig = timeZoneConfig;
    }
}
