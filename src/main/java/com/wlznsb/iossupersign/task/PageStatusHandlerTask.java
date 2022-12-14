package com.wlznsb.iossupersign.task;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.io.file.FileWriter;
import cn.hutool.core.util.ZipUtil;
import com.alibaba.fastjson.JSON;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;
import com.wlznsb.iossupersign.common.TimeLockInfo;
import com.wlznsb.iossupersign.mapper.*;
import com.wlznsb.iossupersign.entity.*;
import com.wlznsb.iossupersign.service.DistrbuteServiceImpl;
import com.wlznsb.iossupersign.service.MdmDistrbuteServiceImpl;
import com.wlznsb.iossupersign.util.*;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
@EnableScheduling
@Slf4j
public class PageStatusHandlerTask {

    @Autowired
    private DistributeDao distributeDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private DistrbuteServiceImpl distrbuteService;

    @Autowired
    private MdmDistrbuteServiceImpl mdmDistrbuteService;

    @Autowired
    private EnterpriseSignCertDao enterpriseSignCertDao;

    @Value("${signCount}")
    private Integer signCount;

    @Autowired
    private PackStatusDao packStatusDao;

    @Autowired
    private MdmPackStatusMapper mdmPackStatusMapper;



    @Value("${server.servlet.context-path}")
    private String index;

    @Autowired
    private PackStatusIosApkDao packStatusIosApkDao;

    private ThreadPoolExecutor poolExecutor;

    private ThreadPoolExecutor mdmSuperPoolExecutor;

    @Autowired
    private PackStatusEnterpriseSignDao packStatusEnterpriseSignDao;

    //??????????????????????????????
    //private Queue<PackStatus> queue = new LinkedList<>();
    // ????????????????????????
   // private LinkedBlockingDeque<PackStatus> queue= new LinkedBlockingDeque<PackStatus>(100);
    //??????????????????
    public PageStatusHandlerTask(@Value("${thread}") Integer thread) {
                                                            //?????????????????????????????????????????????
        poolExecutor =  new ThreadPoolExecutor(thread, thread, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

        mdmSuperPoolExecutor =  new ThreadPoolExecutor(thread, thread, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }

    @Scheduled(cron = "0/5 * * * * *")
    public void run(){
        List<MdmPackStatusEntity> distributeList = mdmPackStatusMapper.queryPage("?????????");
       for (MdmPackStatusEntity packStatus:distributeList){
           packStatusDao.updateStatus("?????????", packStatus.getUuid());
           poolExecutor.execute(new Runnable() {
               @Override
               public void run() {
                   log.info(Thread.currentThread().getName());
                   mdmDistrbuteService.getUuid(packStatus);
               }
           });
       }
    }

    @Scheduled(cron = "0/5 * * * * *")
    public void runmdmsuper(){
        List<PackStatus> distributeList = packStatusDao.queryPage("?????????");
        for (PackStatus packStatus:distributeList){
            packStatusDao.updateStatus("?????????", packStatus.getUuid());
            poolExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    log.info(Thread.currentThread().getName());
                    distrbuteService.getUuid(packStatus);
                }
            });
        }
    }


    @Autowired
    private MdmSuperUpdateIpaTaskMapper updateIpaTaskMapper;

    @Value("${qiniuyun.accessKey}")
    private String qiniuyunAccessKey;
    @Value("${qiniuyun.secretKey}")
    private String qiniuyunSecretKey;
    @Value("${qiniuyun.bucket}")
    private String qiniuyunBucket;
    @Value("${qiniuyun.url}")
    private String qiniuyunUrl;
    @Value("${qiniuyun.reg}")
    private String qiniuyunReg;

    @Value("${aliyun.accessKey}")
    private String aliyunAccessKey;
    @Value("${aliyun.secretKey}")
    private String aliyunSecretKey;
    @Value("${aliyun.bucket}")
    private String aliyunBucket;
    @Value("${aliyun.url}")
    private String aliyunUrl;
    @Value("${aliyun.downUrl}")
    private String aliyunDownUrl;


    @Autowired
    private MdmDistributeMapper mdmDistributeMapper;


    @Autowired
    private DeviceCommandTaskMapper deviceCommandTaskMapper;


    @Autowired
    private DeviceInfoMapper deviceInfoMapper;

    @Autowired
    private DeviceStatusMapper deviceStatusMapper;

    /**
     * mdm ios?????????????????????
     */
    @Scheduled(cron = "0/5 * * * * *")
    public void ios?????????????????????(){
        log.info("ios?????????????????????");
        List<MdmSuperUpdateIpaTaskEntity> ????????? = updateIpaTaskMapper.selectByStatus("?????????");

        for (MdmSuperUpdateIpaTaskEntity s: ?????????) {

            try {
                MdmPackStatusEntity packStatus = mdmPackStatusMapper.selectById(s.getPackStatusId());
                //?????????????????????????????????
                MdmDistributeEntity distribute = mdmDistributeMapper.query(packStatus.getAppId());

                //??????
                String nameIpa = new Date().getTime() + ".ipa";
                //????????????
                String temp = new File("./sign/mode/temp").getAbsolutePath() + "/" + nameIpa;
                String cmd = "./sign/mode/zsign -k " + packStatus.getP12Path() + " -p 123456 -m " + packStatus.getMobilePath() + " -o " + temp + " -z 1 " + distribute.getIpa();
                log.info("????????????" + cmd);
                packStatusDao.updateStatus("????????????", packStatus.getUuid());
                Map<String,Object>  map1 =  RuntimeExec.runtimeExec(cmd);
                log.info("????????????" + map1.get("status").toString());
                log.info("????????????" + map1.get("info").toString());
                log.info("????????????" + cmd);
                log.info("??????"+ nameIpa);
                if(!map1.get("status").toString().equals("0")){
                    throw  new RuntimeException("????????????");
                }
                //??????plist
                String plist = IoHandler.readTxt(new File("./sign/mode/install.plist").getAbsolutePath());

                //?????????????????????
                if(!this.qiniuyunAccessKey.equals("")){
                    log.info("???????????????");
                    plist = plist.replace("urlRep", this.qiniuyunUrl + uploadQly(temp,"ipa"));
                    //??????ipa
                    new File("./sign/mode/temp/" + nameIpa).delete();
                }else if(!this.aliyunAccessKey.equals("")){
                    log.info("???????????????");
                    plist = plist.replace("urlRep", this.aliyunDownUrl + uploadAly(temp,"ipa"));
                    //??????ipa
                    new File("./sign/mode/temp/" + nameIpa).delete();
                }else {
                    log.info("??????????????????");
                    if(SettingUtil.ipaDownUrl != null && !SettingUtil.ipaDownUrl.equals("")){
                        plist = plist.replace("urlRep", SettingUtil.ipaDownUrl  + nameIpa);
                    }else {
                        plist = plist.replace("urlRep", packStatus.getUrl()  + nameIpa);
                    }
                    log.info("ipa??????:" + packStatus.getUrl()  + nameIpa);
                }
                //bundle????????????????????????????????????
                DeviceStatusEntity deviceStatusEntity = deviceStatusMapper.selectById(packStatus.getDeviceId());
                log.info("????????????" + deviceStatusEntity.getStatus());
//                if(deviceStatusEntity.getStatus().equals(DeviceStatusEntity.STATUS_ON)){
//                    log.info("???????????? ????????????");
//                    plist = plist.replace("bundleRep", packStatus.getPageName());
//                }else {
//                    log.info("??????????????? ????????????");
//                    plist = plist.replace("bundleRep", packStatus.getPageName() + "update");
//                }
                plist = plist.replace("bundleRep", packStatus.getPageName());
                log.info(plist);
                plist = plist.replace("versionRep", distribute.getVersion());
                String iconPath = packStatus.getUrl() + distribute.getAccount() + "/mdmdistribute/" + packStatus.getAppId() + "/" + packStatus.getAppId() + ".png";
                plist = plist.replace("iconRep", iconPath);
                plist = plist.replace("appnameRep", distribute.getAppName() + "?????????????????????");
                String plistName = new Date().getTime() + ".plist";
                IoHandler.writeTxt(new File("./sign/mode/temp").getAbsolutePath() + "/" + plistName, plist);
                String plistUrl;
                //?????????????????????????????????????????????
                plistUrl = packStatus.getUrl() + plistName;

                //??????????????????
                log.info("????????????");
                log.info("plist???" + plistName);
                DeviceInfoEntity deviceInfoEntity = deviceInfoMapper.selectById(packStatus.getDeviceId());



                Date date = new Date();
                DeviceCommandTaskEntity taskEntity = new DeviceCommandTaskEntity();
                taskEntity.setTaskId(MyUtil.getUuid());
                taskEntity.setDeviceId(packStatus.getDeviceId());
                taskEntity.setCmd("InstallApplication");
                taskEntity.setExecResult("");
                taskEntity.setCreateTime(date);
                taskEntity.setExecTime(date);
                taskEntity.setResultTime(date);
                taskEntity.setTaskStatus(0);
                taskEntity.setPushCount(0);
                taskEntity.setExecResultStatus("");
                taskEntity.setUdid(deviceInfoEntity.getUdid());
                taskEntity.setCertId(deviceInfoEntity.getCertId());
                String cmda = "{\"type\":\"ManifestURL\",\"value\":\"#plist#\"}";
                cmda = cmda.replace("#plist#",plistUrl);
                taskEntity.setCmdAppend(cmda);


                deviceCommandTaskMapper.insert(taskEntity);
                s.setStatus("?????????");
                s.setUpdateTime(new Date());
                s.setPlistUrl(plistUrl);
                s.setTaskId(taskEntity.getTaskId());
                updateIpaTaskMapper.updateById(s);

            }catch (Exception e){

                log.info("??????ipa??????");
                e.printStackTrace();
                s.setStatus("??????");
                s.setUpdateTime(new Date());
                updateIpaTaskMapper.updateById(s);

            }

        }

    }



    /**
     * ???????????????
     * @return
     */
    public String uploadQly(String localFilePath,String suffix){
        Long time = System.currentTimeMillis();
        Configuration cfg;
        //??????
        if(this.qiniuyunReg.equals("huadong")){
            cfg = new Configuration(Region.qvmRegion0());
        }else if(this.qiniuyunReg.equals("huabei")){
            cfg = new Configuration(Region.qvmRegion1());
        }else {
            cfg = new Configuration(Region.qvmRegion1());
        }

        cfg.useHttpsDomains = false;

        UploadManager uploadManager = new UploadManager(cfg);
        String key = new Date().getTime() + "." + suffix;
        Auth auth = Auth.create(qiniuyunAccessKey, qiniuyunSecretKey);
        String upToken = auth.uploadToken(qiniuyunBucket);
        try {
            Response response = uploadManager.put(localFilePath, key, upToken);
            //???????????????????????????
            DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
            log.info("??????????????????:" + (System.currentTimeMillis() - time)/1000 + "???");
            return putRet.key;
        } catch (Exception ex) {
            log.info("????????????" + ex.toString());
            return null;
        }
    }

    /**
     * ???????????????
     * @param localFilePath
     * @return
     */
    public String uploadAly(String localFilePath,String  suffix){
        Long time = System.currentTimeMillis();
        try {
            String name = System.currentTimeMillis() + "." + suffix;
            String endpoint = aliyunUrl;
            String accessKeyId = aliyunAccessKey;
            String accessKeySecret = aliyunSecretKey;
            OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            PutObjectRequest putObjectRequest = new PutObjectRequest(aliyunBucket,name, new File(localFilePath));
            ossClient.putObject(putObjectRequest);
            ossClient.shutdown();
            log.info("?????????????????????:" + (System.currentTimeMillis() - time)/1000 + "???");
            return name;
        }catch (Exception e){
            log.info("?????????????????????:" + e.toString());
            return null;
        }
    }

    @Resource
    private AppleIisMapper appleIisMapper;

    /**
     * ?????????????????????
     */
    @Scheduled(cron = "0/5 * * * * *")
    public void checkChaojiqianP12() throws IOException {
        log.info("?????????????????????");
        List<AppleIisEntity> appleIisEntities = appleIisMapper.selectByStatus(0);
        for (AppleIisEntity a:
                appleIisEntities) {
            AppleApiUtil appleApiUtil = new AppleApiUtil(a.getIis(),
                    a.getKid(),a.getP8());
            //????????????token
            if(appleApiUtil.init()){
                log.info("???????????????");
                a.setStatus(1);
                appleIisMapper.updateById(a);
            }else {
                log.info("????????????");
            }
            System.out.println(a);

        }
    }


    /**
     * ????????????
     */
    @Scheduled(cron = "0/5 * * * * *")
    public void enterpriseSign(){
        try {
            List<PackStatusEnterpriseSign> list = packStatusEnterpriseSignDao.queryAll();
            for (PackStatusEnterpriseSign packStatusEnterpriseSign:list){
                if("?????????".equals(packStatusEnterpriseSign.getStatus())){
                    EnterpriseSignCert enterpriseSignCert = enterpriseSignCertDao.queryId(packStatusEnterpriseSign.getCertId());
                    if(enterpriseSignCert == null){
                        packStatusEnterpriseSignDao.updateStatus("????????????,???????????????", null, packStatusEnterpriseSign.getId());
                        throw  new RuntimeException("????????????,???????????????");
                    }
                    String uuid = MyUtil.getUuid();
                    String signPath = "./sign/mode/temp/sign" + uuid + ".ipa";

                    //??????????????????
                    if(packStatusEnterpriseSign.getIsTimeLock().equals(1)){
                        log.info("?????????????????????");

                        TimeLockInfo timeLockInfo = new TimeLockInfo();
                        timeLockInfo.setRequest_url(packStatusEnterpriseSign.getLockRequestUrl());

                        File file = new File(packStatusEnterpriseSign.getIpaPath() + "/lock.info");
                        FileWriter fileWriter = new FileWriter(file.getAbsolutePath());
                        fileWriter.write(JSON.toJSONString(timeLockInfo));


//                        String dirPath = "./sign/mode/temp/unsigned_sign" + packStatusEnterpriseSign.getId();
//                        String ipaPath = "./sign/mode/temp/unsigned_sign"  + packStatusEnterpriseSign.getId() + ".ipa";

//                        ZipUtil.zip(ipaPath,dirPath);

                        String dlibPath = new File("./sign/mode/lock.dylib").getAbsolutePath();
                        String cmd = "./sign/mode/zsign -k " + enterpriseSignCert.getCertPath() + " -p " + enterpriseSignCert.getPassword() + " -m " + enterpriseSignCert.getMoblicPath() + " -o " + signPath + " -z 1 " + packStatusEnterpriseSign.getIpaPath() + " -l " + dlibPath;
                        Map<String,Object>  map =  RuntimeExec.runtimeExec(cmd);
                        log.info("????????????" + map.get("status").toString());
                        log.info("????????????" + map.get("info").toString());
                        log.info("????????????" + cmd);
                        if(!map.get("status").toString().equals("0")){
                            packStatusEnterpriseSignDao.updateStatus("????????????", null, packStatusEnterpriseSign.getId());
                            throw  new RuntimeException("????????????");
                        }else {
                            packStatusEnterpriseSignDao.updateStatus("????????????", packStatusEnterpriseSign.getUrl() + "sign" + uuid + ".ipa", packStatusEnterpriseSign.getId());
                        }
                    }else {
                        log.info("????????????????????????");
                        String cmd = "./sign/mode/zsign -k " + enterpriseSignCert.getCertPath() + " -p " + enterpriseSignCert.getPassword() + " -m " + enterpriseSignCert.getMoblicPath() + " -o " + signPath + " -z 1 " + packStatusEnterpriseSign.getIpaPath();
                        log.info("????????????" + cmd);
                        packStatusEnterpriseSignDao.updateStatus("?????????", null, packStatusEnterpriseSign.getId());
                        Map<String,Object>  map =  RuntimeExec.runtimeExec(cmd);
                        log.info("????????????" + map.get("status").toString());
                        log.info("????????????" + map.get("info").toString());
                        log.info("????????????" + cmd);
                        if(!map.get("status").toString().equals("0")){
                            packStatusEnterpriseSignDao.updateStatus("????????????", null, packStatusEnterpriseSign.getId());
                            throw  new RuntimeException("????????????");
                        }else {
                            packStatusEnterpriseSignDao.updateStatus("????????????", packStatusEnterpriseSign.getUrl() + "sign" + uuid + ".ipa", packStatusEnterpriseSign.getId());
                        }
                    }


                }
            }
        } catch (Exception e) {
            log.info(e.toString());
            e.printStackTrace();
        }
    }

    @Autowired
    private IosSignSoftwareDistributeStatusDao distributeStatusDao;

    @Autowired
    private IosSignUdidCertDao iosSignUdidCertDao;

    @Autowired
    private IosSignSoftwareDistributeDao iosSignSoftwareDistributeDao;




    /**
     * ??????????????????
     */
    @Scheduled(cron = "0/5 * * * * *")
    public void iosSignPack(){
        List<IosSignSoftwareDistributeStatus> list = distributeStatusDao.queryStatusAll("?????????");
        for (IosSignSoftwareDistributeStatus ios:list){
            if("?????????".equals(ios.getStatus())){
                try {
                    String uuid = MyUtil.getUuid();
                    IosSignSoftwareDistribute iosSignSoftwareDistribute = iosSignSoftwareDistributeDao.query(ios.getIosId());
                    IosSignUdidCert iosSignUdidCert = iosSignUdidCertDao.query(ios.getCertId());

                    SystemctlSettingsEntity systemctlSettingsEntity = settingsMapper.selectOne(null);

                    if(systemctlSettingsEntity.getMqDomain().equals("www.xxx.com")){
                        distributeStatusDao.updateStatus("????????????????????????-????????????-???????????????", ios.getUuid());
                        throw  new RuntimeException("????????????????????????-????????????-???????????????");
                    }


                    try {
                        userDao.reduceCountC(iosSignSoftwareDistribute.getAccount(),systemctlSettingsEntity.getOneSuperTotal());
                    }catch (Exception e){
                        distributeStatusDao.updateStatus("???????????????", ios.getUuid());
                        continue;
                    }

                    String plist = IoHandler.readTxt(new File("./sign/mode/install.plist").getAbsolutePath());
                    //bundle????????????????????????????????????
                    plist = plist.replace("bundleRep", uuid);
                    plist = plist.replace("versionRep", iosSignSoftwareDistribute.getVersion());
                    plist = plist.replace("iconRep", iosSignSoftwareDistribute.getIcon());
                    plist = plist.replace("appnameRep",iosSignSoftwareDistribute.getAppName());
                    //???ipa??????
                    String uuidTemp = MyUtil.getUuid();
                    String signPath = "./sign/mode/temp/" + uuidTemp +".ipa";
                    String cmd = "./sign/mode/zsign -k " + iosSignUdidCert.getP12Path() + " -p " + iosSignUdidCert.getP12Password() + " -m " + iosSignUdidCert.getMobileprovisionPath() + " -o " + signPath + " -z 1 " + iosSignSoftwareDistribute.getIpa();

                    if(iosSignSoftwareDistribute.getAutoPageName() == 1){
                        log.info("????????????");
                        cmd = "./sign/mode/zsign -k " + iosSignUdidCert.getP12Path() + " -p " + iosSignUdidCert.getP12Password() + " -m " + iosSignUdidCert.getMobileprovisionPath() + " -o " + signPath + " -z 1 " + iosSignSoftwareDistribute.getIpa() + " -b " + new Date().getTime();
                    }
                    log.info("????????????" + cmd);
                    distributeStatusDao.updateStatus("?????????", ios.getUuid());
                    Map<String,Object>  map1 =  RuntimeExec.runtimeExec(cmd);
                    log.info("????????????" + map1.get("status").toString());
                    log.info("????????????" + map1.get("info").toString());
                    log.info("????????????" + cmd);
                    if(!map1.get("status").toString().equals("0")){
                        distributeStatusDao.updateStatus("????????????", ios.getUuid());
                        throw  new RuntimeException("????????????");
                    }
                    distributeStatusDao.updateStatus("????????????", ios.getUuid());
                    //????????????
                    String ipaUrl = distrbuteService.uploadSoftwareIpa(signPath);


                    if(null == ipaUrl){
                        ipaUrl ="https://" +  systemctlSettingsEntity.getMqDomain() + "/" + uuidTemp + ".ipa";
                    }
                    plist = plist.replace("urlRep", ipaUrl);
                    String plistName = uuidTemp + ".plist";
                    IoHandler.writeTxt(new File("./sign/mode/temp/" +  plistName).getAbsolutePath(), plist);
                    String plistUrl = "itms-services://?action=download-manifest&url=" + "https://" +  systemctlSettingsEntity.getMqDomain() + "/" + plistName;
                    distributeStatusDao.updateDownUrl("????????????",plistUrl, ios.getUuid());
                }catch (Exception e){
                    distributeStatusDao.updateStatus("????????????", ios.getUuid());
                }

            }
        }
    }


    /**
     * ????????????????????????????????????
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void checkCert() throws IOException {
        List<EnterpriseSignCert> enterpriseSignCertList = enterpriseSignCertDao.queryAllCert();
        for (EnterpriseSignCert enterpriseSignCert : enterpriseSignCertList){
            if("??????".equals(enterpriseSignCert.getStatus())){

                //????????????
                String data = AppleApiUtil.certVerify(enterpriseSignCert.getCertPath(), enterpriseSignCert.getPassword());
                JsonNode jsonNode = new ObjectMapper().readTree(data);
                if(jsonNode.get("data").get("status").asText().equals("revoked")){
                    log.info(enterpriseSignCert + "??????");
                    enterpriseSignCertDao.updateCertStatus("??????", enterpriseSignCert.getMd5());
                }
            }
        }
    }




    @Autowired
    private SystemctlSettingsMapper settingsMapper;

    @Scheduled(cron = "0/5 * * * * *")
    public void iosApkPack(){
        //??????mode??????
        String modePath = new File("./sign/mode/static/mode").getAbsolutePath();
        //??????temp??????
        String tempPath = new File("./sign/mode/static/down/mq").getAbsolutePath();

        //??????????????????
        String execPath = new File("./").getAbsolutePath();

        //??????????????????
//        String initPath = RuntimeExec.runtimeExec("pwd").get("info").toString();

        try {
            List<PackStatusIosApk> list = packStatusIosApkDao.queryAll();
            for (PackStatusIosApk packStatusIosApk:list){
                //?????????????????????????????????
                //POSIXFactory.getPOSIX().chdir(initPath);
                if("?????????".equals(packStatusIosApk.getStatus())){


                    log.info("??????id" + packStatusIosApk.getId());


                    User user = userDao.queryAccount(packStatusIosApk.getAccount());
                    SystemctlSettingsEntity systemctlSettingsEntity = settingsMapper.selectOne(null);
                    if(systemctlSettingsEntity.getMqDomain().equals("www.xxx.com")){
                        packStatusIosApkDao.updateStatus("????????????????????????-????????????-???????????????","","", new Date(),packStatusIosApk.getId());
                        throw  new RuntimeException("????????????????????????-????????????-???????????????");
                    }

                    //????????????????????????????????????
                    try {
                        userDao.reduceCountC(packStatusIosApk.getAccount(),systemctlSettingsEntity.getWebPackTotal());
                    }catch (Exception e){
                        packStatusIosApkDao.updateStatus("???????????????!","","", new Date(),packStatusIosApk.getId());
                        continue;
                    }
                    //System.out.println( status.getStatusStatus());;
                    // System.out.println("cp -rf " + modePath + " " + tempPath + "/" + status.getStatusId());
                    //?????????????????????
                    String idPath = new File("./sign/mode/static/down/mq/" + packStatusIosApk.getId()).getAbsolutePath();
                    //??????mode?????????????????????
                    log.info("????????????:" + "cp -rf " + modePath + " " + tempPath + "/" + packStatusIosApk.getId());
                    RuntimeExec.runtimeExec("cp -rf " + modePath + " " + tempPath + "/" + packStatusIosApk.getId());

                    log.info("??????shell.sh");
                    String s = FileUtil.readUtf8String(new File(modePath + "/android/temp/shell.sh"));
                    s = s.replace("#????????????#",idPath + "/android/temp/app");
                    FileUtil.writeUtf8String(s,new File(idPath + "/android/temp/shell.sh"));
                    log.info("??????shell??????" + s);

                    // log.info("??????????????????:" + "chmod -R 777 " + tempPath +  "/" + packStatusIosApk.getId());
                   // RuntimeExec.runtimeExec("chmod -R 777 " + tempPath + "/" + packStatusIosApk.getId());

                    //?????????????????????????????????,??????????????????iconpath?????????????????????????????????
                    String mobileConfig = packStatusIosApk.getRemark();
                    //????????????????????????????????????
                    String mobileConfigSign =  idPath +  "/" + "ios" + "/" + "sign.mobileconfig";
                    //?????????????????????,???????????????????????????
                    String serverCrt = new File("./sign/mode/cert/cert.pem").getAbsolutePath();
                    String rootCrt =  new File("./sign/mode/cert/cert.pem").getAbsolutePath();
                    String keyCrt =  new File("./sign/mode/cert/cert.key").getAbsolutePath();
                    if(packStatusIosApk.getKeyCert() != null){
                        serverCrt =  packStatusIosApk.getServerCert();
                        rootCrt =  packStatusIosApk.getRootCert();
                        keyCrt =  packStatusIosApk.getKeyCert();
                    }
                    //??????cmd
                    String cmd =" openssl smime -sign -in " + mobileConfig + " -out " + mobileConfigSign + " -signer " + serverCrt + " -inkey " + keyCrt + " -certfile " + rootCrt + " -outform der -nodetach ";
                    //????????????,?????????????????????????????????
                    log.info("????????????" + cmd);
                    if(!("0".equals(RuntimeExec.runtimeExec(cmd).get("status").toString()))){
                        packStatusIosApkDao.updateStatus("????????????????????????","","", new Date(),packStatusIosApk.getId());
                        RuntimeExec.runtimeExec("rm -rf " + idPath);
                        continue;
                    }else {
                        //????????????????????????
                        String appName = tempPath + "/" + packStatusIosApk.getId()
                                + "/" + "android" + "/" + "temp" + "/"
                                + "app" + "/" + "src" + "/" + "main" + "/"
                                + "AndroidManifest.xml";
                        String uuid = tempPath + "/" + packStatusIosApk.getId()
                                + "/" + "android" + "/" + "temp" + "/" + "app"
                                + "/" + "build.gradle";
                        String url = tempPath + "/" + packStatusIosApk.getId()
                                + "/" + "android" + "/" + "temp" + "/"
                                + "app" + "/" + "src" + "/" + "main" + "/"
                                + "java" + "/" + "com" + "/" + "example" + "/"
                                + "myapplication" + "/" + "Home.java";
                        //??????????????????
                        String appNameText = IoHandler.readTxt(appName);
                        String uuidText = IoHandler.readTxt(uuid);
                        String urlText = IoHandler.readTxt(url);
                        //??????????????????
                        appNameText = appNameText.replace("????????????", packStatusIosApk.getAppName());
                        uuidText = uuidText.replace("com.example.uuid", packStatusIosApk.getPageName()).replace("9.9", packStatusIosApk.getVersion());

                        //???????????????????????????
                        if (packStatusIosApk.getIsVariable() == 0){
                            log.info("???????????????");
                            urlText = urlText.replace("http://www.wlznsb.cn/html", packStatusIosApk.getUrl());
                        }else {
                            log.info("????????????");

                            urlText = urlText.replace("http://www.wlznsb.cn/html", "https://" +  systemctlSettingsEntity.getMqDomain() + "pack/distribute/" + packStatusIosApk.getId());
                        }


                        //??????????????????
                        IoHandler.writeTxt(appName,appNameText);
                        IoHandler.writeTxt(uuid,uuidText);
                        IoHandler.writeTxt(url,urlText);
                        //??????androidlogo
                        log.info("??????logo");
                        RuntimeExec.runtimeExec("mv " + packStatusIosApk.getIcon() + " "
                                + tempPath + "/" + packStatusIosApk.getId() + "/" + "android" + "/" + "temp" + "/"
                                + "app" + "/" + "src" + "/" + "main" + "/" + "res" + "/"
                                + "mipmap-xxxhdpi" + "/" + "icon.png");
                        if(packStatusIosApk.getStartIcon() != null){
                            RuntimeExec.runtimeExec("mv " + packStatusIosApk.getStartIcon() + " "
                                    + tempPath + "/" + packStatusIosApk.getId() + "/" + "android" + "/" + "temp" + "/"
                                    + "app" + "/" + "src" + "/" + "main" + "/" + "res" + "/"
                                    + "mipmap-xxxhdpi" + "/" + "start.png");
                        }
                        //??????????????????
                        //??????????????????
                        String androidPath = tempPath + "/" + packStatusIosApk.getId() + "/" + "android";
                        log.info("????????????" + androidPath);

                        //????????????????????????????????????
                       // POSIXFactory.getPOSIX().chdir(tempPath + "/" + packStatusIosApk.getId() + "/" + "android" + "/" + "temp" + "/");

                        log.info("??????shell??????" + "sh " + idPath + "/android/temp/shell.sh");
                        Map<String,Object> ccc =  RuntimeExec.runtimeExec("sh " + idPath + "/android/temp/shell.sh");
                        log.info(ccc.get("info").toString());
                        boolean isSu =  "0".equals(ccc.get("status").toString());
                        //??????????????????
                        if(isSu){
                            log.info("??????????????????");
                            //??????????????????apk???andorid??????
                            RuntimeExec.runtimeExec("mv " + androidPath + "/" + "temp" + "/"
                                    + "app" + "/" + "build" + "/" + "outputs" + "/" + "apk" + "/" +
                                    "debug" + "/" + "app-debug.apk " + androidPath + "/" +  "app.apk");
                            //???logn?????????html?????????logo??????
                            RuntimeExec.runtimeExec("mv " + androidPath + "/" + "temp" + "/"
                                    + "app" + "/" + "src" + "/" + "main" + "/" + "res" + "/"
                                    + "mipmap-xxxhdpi" + "/" + "icon.png " + idPath + "/" +  "html"
                                    + "/" + "static" + "/" + "picture" + "/" + "logo.png");

                            //??????html??????????????????????????????
                            String indexName = idPath + "/" + "html" + "/" + "index.html";
                            log.info(indexName);

                            String indexText = IoHandler.readTxt(indexName);
                            IoHandler.writeTxt(indexName, indexText.replace("????????????", packStatusIosApk.getAppName()));
                            log.info("??????idnex??????");
                            //??????temp??????
                            RuntimeExec.runtimeExec("rm -rf " + androidPath + "/" + "temp");
                            //??????ios????????????
                            RuntimeExec.runtimeExec("rm -rf " + idPath + "/" + "ios" + "/" + "cert " +  idPath + "/" + "ios" + "/" + "demo.txt");
                            Calendar c = Calendar.getInstance();
                            c.setTime(new Date()); //????????????
                            c.add(Calendar.DATE, 9000); //???????????????1,Calendar.DATE(???),Calendar.HOUR(??????)
                            Date date = c.getTime(); //??????
                            //??????
                            //POSIXFactory.getPOSIX().chdir(idPath);
                            log.info("????????????");
                            RuntimeExec.runtimeExec("tar -zcvf " + idPath  +  "/down.zip " + idPath ).get("info").toString();
                            log.info("??????id" + packStatusIosApk.getId());

                            packStatusIosApkDao.updateStatus("????????????", "https://" +  systemctlSettingsEntity.getMqDomain()  + "/mq/" + packStatusIosApk.getId()
                                        + "/" + "html/index.html", "https://" + systemctlSettingsEntity.getMqDomain() + "/mq/" + packStatusIosApk.getId() + "/" + "down.zip", date,packStatusIosApk.getId());


                        }else {
                            log.info("??????????????????");
                           // RuntimeExec.runtimeExec("rm -rf " + idPath);
                            packStatusIosApkDao.updateStatus("??????????????????","","", new Date(),packStatusIosApk.getId());
                        }
                    }

                }
            }
        } catch (Exception e) {
            log.info("????????????");
            log.info(e.toString());
            e.printStackTrace();
        }
    }




}
