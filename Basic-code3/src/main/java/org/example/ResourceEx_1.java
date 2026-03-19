package org.example;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.util.Set;

public class ResourceEx_1 {

    public static void main(String[] args) {
        File file = new File("Files/AliBaba.json");
        StringBuffer buffer = new StringBuffer();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                buffer.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        String json = buffer.toString();
        JSONObject jsonObject = JSONUtil.parseObj(json);
        Set<String> strings = jsonObject.keySet();
        for (String string : strings) {
            System.out.println(string);
        }
    }
}

