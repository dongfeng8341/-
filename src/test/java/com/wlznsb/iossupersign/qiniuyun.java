package com.wlznsb.iossupersign;


import cn.hutool.core.util.IdUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;
import com.wlznsb.iossupersign.entity.MdmPackStatusEntity;
import com.wlznsb.iossupersign.mapper.MdmPackStatusMapper;
import com.wlznsb.iossupersign.util.AppleApiUtil;
import com.wlznsb.iossupersign.util.GetIpaInfoUtil;
import com.wlznsb.iossupersign.util.MyUtil;
import com.wlznsb.iossupersign.util.SettingUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class qiniuyun {

    @Value("${aliyun.accessKey}")
    private String aliyunAccessKey;
    @Value("${aliyun.secretKey}")
    private String aliyunSecretKey;
    @Value("${aliyun.bucket}")
    private String aliyunBucket;
    @Value("${aliyun.url}")
    private String aliyunUrl;

    public static void main(String[] args) {

        System.out.println( new File("/sign/mode/cert/server.crt").exists());;
    }

    static {

        System.out.println(11111111);
    }

    @Value("${qiniuyun.accessKey}")
    private String port;

    /**
     * ????????????????????????
     *
     * @throws IOException
     */

    @Test
    public void test() throws IOException {
        Long time = System.currentTimeMillis();

        //?????????????????????Region??????????????????
        Configuration cfg = new Configuration(Region.region2());
//...???????????????????????????
        cfg.useHttpsDomains = false;
        UploadManager uploadManager = new UploadManager(cfg);
//...???????????????????????????????????????
        String accessKey = "EwSEYiKZtViB3YiqVQR8Y-Go4vijLhhY3WxIJxCz";
        String secretKey = "ToI-iR-Dhq-udEwIrZEhauqJCpbX6vrl-yk4ZXuh";
        String bucket = "abcqweqwedq";
//?????????Windows????????????????????? D:\\qiniu\\test.png
        String localFilePath = "C:\\Users\\xujimu\\Desktop\\120011002.ipa";
//???????????????key?????????????????????????????????hash??????????????????
        String key = "1231.ipa";
        Auth auth = Auth.create(accessKey, secretKey);
        String upToken = auth.uploadToken(bucket);

        try {
            Response response = uploadManager.put(localFilePath, key, upToken);
            //???????????????????????????
            DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
        } catch (Exception ex) {
                ex.printStackTrace();
        }

    }


    @Autowired
    private MdmPackStatusMapper packStatusMapper;

    @Test
    public  void  test1() throws InterruptedException {
        MdmPackStatusEntity mdmPackStatusEntity = new MdmPackStatusEntity();
        mdmPackStatusEntity.setId(MyUtil.getUuid());
        mdmPackStatusEntity.setAccount("super");
        mdmPackStatusEntity.setPageName("com.ppgjx.com");
        mdmPackStatusEntity.setIis("tesrt");
        mdmPackStatusEntity.setStatus("????????????");
        mdmPackStatusEntity.setUuid(MyUtil.getUuid());
        mdmPackStatusEntity.setUdid(IdUtil.randomUUID().toUpperCase());
        mdmPackStatusEntity.setIp(MyUtil.getRandomIp());
        packStatusMapper.insert(mdmPackStatusEntity);
    }

    @Test
    public  void  test3() throws InterruptedException {

    }


    @Test
    public  void  test2() throws URISyntaxException {

        String json = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\"><plist version=\"1.0\"><dict>\t<key>IMEI</key>\t<string>35 917307 461827 5</string>\t<key>PRODUCT</key>\t<string>iPhone9,2</string>   <key>SERIAL</key>\t<string>C39SLG8ZHG00</string>\t<key>UDID</key>\t<string>02BC34E0AD3D9769DE4292154344B450917FC9A2</string>\t<key>VERSION</key>\t<string>17F80</string></dict></plist>";

        final RestTemplate rest = new RestTemplate();
        final HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        final CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setRedirectStrategy(new LaxRedirectStrategy())
                .build();
        factory.setHttpClient(httpClient);
        rest.setRequestFactory(factory);
        String url = "https://www.86scw.com/source/sign/install/receive.php?id=6460563J6F6U465B313W733";
        //?????????????????????
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("Content-Type", "application/xml");
        HttpEntity<String> request = new HttpEntity<>(json,requestHeaders);
        ResponseEntity<String> exchange  = rest.exchange(url, HttpMethod.GET, request, String.class);
        System.out.println(exchange.getBody());


    }



}
