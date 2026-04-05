package org.example;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

public class Test1 {
    public static void main(String[] args) throws IOException {
        String path = "C:\\java_learning\\java_projects\\git-demo\\Basic-code3\\JsonFiles\\Result_1.xlsx";
        // 创建文件输入流，用于读取 Excel 文件的二进制数据；
        InputStream inputStream = new FileInputStream(path);
        // Apache POI 库的核心接口，用于统一处理 Excel 文件（兼容 xls/xlsx）；
        Workbook workbook = null;
        // POI 中专门处理xlsx 格式（Excel 2007+）的 Workbook 实现类
        workbook = new XSSFWorkbook(inputStream);
        // 获取 Excel 中第三个工作表（POI 中 Sheet 索引从 0 开始，0 = 第一个、1 = 第二个、2 = 第三个）；
        Sheet sheet = workbook.getSheetAt(0);
        //获取工作表中实际存在的非空行数量
        for (int i = 0; i < sheet.getPhysicalNumberOfRows(); i++) {
            //获取第 i 行的行对象（Row 是 POI 中封装 Excel 行的类）。
            Row row = sheet.getRow(i);
            //获取当前行的第二列单元格（Cell 索引从 0 开始）
            Cell cell = row.getCell(1);
            //强制将单元格类型转为字符串（避免数字 / 日期 / 公式等类型解析失败，保证能通过getStringCellValue()读取）；
            cell.setCellType(CellType.STRING);
            String business = cell.getStringCellValue();

            Cell jsonText = row.getCell(9);
            jsonText.setCellType((CellType.STRING));
            String json = jsonText.getStringCellValue();

            //使用 Hutool 工具类的 JSONUtil 将 JSON 字符串解析为JSONObject对象（方便遍历 key）；
            JSONObject jsonObject = JSONUtil.parseObj(json);
            System.out.println("create table iceberg.dataware.dwd_" + business.replace("itsm_", "") + " as ");
            System.out.println("select sample_time");
            //遍历 JSON 根节点的所有 key，生成get_json_object(records,"$.字段名") as 字段名（get_json_object是 Hive/Spark SQL 的 JSON 解析函数，
            // 从records字段中提取指定 JSON 路径的值）；
            //第一层 JSON 字段：遍历 JSON 根节点的所有 key，生成get_json_object(records,"$.字段名") as 字段名（get_json_object是 Hive/Spark SQL 的 JSON 解析函数，
            // 从records字段中提取指定 JSON 路径的值）；
            for (String s : jsonObject.keySet()) {
                System.out.println(",get_json_object(records,\"$." + s + "\") as " + s);
            }

            JSONObject content = jsonObject.getJSONObject("content");
            Set<String> strings = content.keySet();
            //第二层 content 子节点：读取 JSON 中content字段的子对象，遍历其 key，
            // 生成get_json_object(records,"$.content.字段名") as 字段名（解析嵌套 JSON）。
            for (String string : strings) {
                System.out.println(",get_json_object(records,\"$.content." + string + "\") as " + string);
            }
            System.out.println("from (SELECT sample_time, explode(records) AS records\n" +
                    "FROM (SELECT date(sample_time)                               sample_time\n" +
                    "           , from_json(records , 'array<string>') AS records\n" +
                    "      FROM (SELECT GET_JSON_OBJECT(GET_JSON_OBJECT(request_response_content , '$.data') , '$.records')              AS records\n" +
                    "                 , _dispatcher_processed_timestamp                                                                  AS sample_time\n" +
                    "                 , ROW_NUMBER() OVER (PARTITION BY business,task_id ORDER BY _dispatcher_processed_timestamp DESC ) AS rn\n" +
                    "            FROM iceberg.dataware.t_ods_metro_http_puller_log\n" +
                    "            WHERE business = '" + business + "'\n" +
                    "              AND _event_timestamp >= '${curr_date} 00:00:00') AS f\n" +
                    "      WHERE rn = 1) AS z) as s");
            System.out.println();
            System.out.println();
            System.out.println();
            System.out.println();
        }
    }
}
