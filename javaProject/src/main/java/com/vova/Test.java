package com.vova;

import javax.sound.midi.Soundbank;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static java.util.Calendar.MONTH;

/**
 * @author WangYang - vova
 * @version Create in 14:49 2024/10/23
 */


public class Test {
    public static void main(String[] args) throws ParseException {

        String queryStartDate = "20240731"; // 测试用例，可以调整为不同日期测试
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

        Calendar calendar = Calendar.getInstance(); // 获取对日期操作的类对象
        calendar.setTime(sdf.parse(queryStartDate));
        calendar.add(Calendar.MONTH, -1);
        System.out.println(sdf.format(calendar.getTime()));


    }
}
