package com.wlznsb.iossupersign;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileReader;
import com.dd.plist.*;
import com.wlznsb.iossupersign.util.GetIpaInfoUtil;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: xjm
 * @Date: 2022/03/07/18:33
 * @Description:
 */
public class Test {

    public static void main(String[] args) throws PropertyListFormatException, ParserConfigurationException, SAXException, ParseException, IOException {
        BufferedInputStream inputStream = FileUtil.getInputStream(new File("C:\\Users\\Administrator\\Desktop\\123.plist"));

        String s = FileUtil.readUtf8String(new File("C:\\Users\\Administrator\\Desktop\\123.plist"));
        String b = FileUtil.readUtf8String(new File("C:\\Users\\Administrator\\Desktop\\Info.plist"));
        NSDictionary rootDict = (NSDictionary) PropertyListParser.parse(inputStream.readAllBytes());







    }
}
