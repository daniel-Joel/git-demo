package org.example;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.Set;

public class ResourceEx {

    public static void main(String[] args) throws IOException {
        String path = "JsonFiles/Result_45.xlsx";
        InputStream inputStream = new FileInputStream(path);
        // 定义一个org.apache.poi.ss.usermodel.Workbook的变量
        Workbook workbook = null;
        // 截取路径名 . 后面的后缀名，判断是xls还是xlsx
        // 如果这个判断不对，就把equals换成 equalsIgnoreCase()
        workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);
        // getLastRowNum,getPhysicalNumberOfRows
        for (int i = 0; i < sheet.getPhysicalNumberOfRows(); i++) {
            Row row = sheet.getRow(i);
            Cell cell = row.getCell(1);
            cell.setCellType(CellType.STRING);
            // 获取得到字符串
            String business = cell.getStringCellValue();
            Cell jsonText = row.getCell(9);
            jsonText.setCellType(CellType.STRING);
            String json = jsonText.getStringCellValue();
//            System.out.println(json);
            JSONObject jsonObject = JSONUtil.parseObj(json);
            System.out.println("create table iceberg.dataware.dwd_" + business.replace("itsm_", "") + " as ");
            System.out.println("select sample_time");
            for (String s : jsonObject.keySet()) {
                System.out.println(",get_json_object(records,\"$." + s + "\") as " + s);
            }
            JSONObject content = jsonObject.getJSONObject("content");
            Set<String> strings = content.keySet();
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


//    public static void writeToFile(String content, String fileName) {
//        // 创建目标目录
//        File outputDir = new File("D:\\Project\\Test\\JsonEndFiles");
//        if (!outputDir.exists()) {
//            outputDir.mkdirs(); // 创建目录
//        }
//        // 创建输出文件
//        File outputFile = new File(outputDir, fileName);
//
//        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {
//            bw.write(content);
//            System.out.println("文件已写入: " + outputFile.getAbsolutePath());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
    }

    public static StringBuffer getFileText(File jsonFile) {

        StringBuffer buffer = new StringBuffer();
        try (BufferedReader br = new BufferedReader(new FileReader(jsonFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                buffer.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer;
    }
}
