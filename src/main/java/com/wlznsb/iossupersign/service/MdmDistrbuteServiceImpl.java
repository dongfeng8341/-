package com.wlznsb.iossupersign.service;
import java.util.Date;

import cn.hutool.core.util.IdUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;
import com.google.gson.Gson;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;
import com.wlznsb.iossupersign.entity.*;
import com.wlznsb.iossupersign.mapper.*;
import com.wlznsb.iossupersign.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.*;

@Service
@Slf4j
public class MdmDistrbuteServiceImpl {


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
    private MdmDistributeMapper distributeDao;
    @Autowired
    private AppleIisDao appleIisDao;
    @Autowired
    private MdmPackStatusMapper packStatusDao;
    @Autowired
    private MdmDownCodeMapper downCodeDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private DeviceInfoMapper deviceInfoMapper;

    @Autowired
    private MdmPackStatusMapper mdmPackStatusMapper;


    @Autowired
    private MdmSuperUpdateIpaTaskMapper superUpdateIpaTaskMapper;


    @Transactional
    public MdmDistributeEntity uploadIpa(MultipartFile ipa, User user, String rootUrl, String appId) {
        String id = null;
        try {
            if(ipa.getSize() != 0){

                //????????????????????????id
                id = MyUtil.getUuid();

                if(null != appId){
                    id = appId;
                }
                new File("./sign/temp/" + user.getAccount() + "/mdmdistribute/" + id + "/").mkdirs();
                //icon??????
                String iconPath = new File("./sign/temp/" + user.getAccount() + "/mdmdistribute/" + id + "/" +  id + ".png").getAbsolutePath();
                //ipa??????
                String ipaPath = new File("./sign/temp/" + user.getAccount() + "/mdmdistribute/" + id + "/" +  id + ".ipa").getAbsolutePath();
                //ipa????????????
                String ipaUnzipPath = new File("./sign/temp/" + user.getAccount() + "/mdmdistribute/" + id + "/Payload").getAbsolutePath();
                //python??????
                String pyPath = new File("./sign/temp/" + user.getAccount() + "/mdmdistribute/" + id + "/").getAbsolutePath();
                //??????
                System.out.println(ipaPath);
                ipa.transferTo(new File(ipaPath));
                //ipa.transferTo(new File(iconPath));
                //????????????
                Map<String, Object> mapIpa = GetIpaInfoUtil.readIPA(ipaPath,iconPath);
                if(mapIpa.get("code") != null){
                    throw new RuntimeException("?????????????????????");
                }
                String cmd = "unzip -oq " + ipaPath + " -d " + "./sign/temp/" + user.getAccount() + "/mdmdistribute/" + id + "/";
                //log.info("????????????" + cmd);
                // log.info("????????????" + RuntimeExec.runtimeExec(cmd).get("info"));
                String name = mapIpa.get("displayName").toString();
                String url = rootUrl + "dis/mdmsuperdown.html?id=" + Base64.getEncoder().encodeToString(String.valueOf(id).getBytes());
                MdmDistributeEntity distribute = new MdmDistributeEntity(id,user.getAccount(),name,mapIpa.get("package").
                        toString(),mapIpa.get("versionName").toString(),iconPath,ipaPath,null,url,new Date(),"????????????",null,0,null,"zh",null,null,null);
                //??????????????????
                MyUtil.getIpaImg("./sign/temp/" + user.getAccount() + "/mdmdistribute/" + id  + "/" + id +  ".png","./sign/temp/" + user.getAccount() + "/mdmdistribute/" + id  + "/" + id +  ".png");

                if(null != appId){
                    distributeDao.updateById(distribute);

                    //????????????app????????????????????????device_id
                    List<MdmPackStatusEntity> ???????????? = mdmPackStatusMapper.selectByAppIdAndAccountAndStatusOrderByCreateTime(id, user.getAccount(), "????????????");

                    List<String> ????????? = new ArrayList<>();

                    Iterator<MdmPackStatusEntity> iterator = ????????????.iterator();
                    while (iterator.hasNext()){
                        MdmPackStatusEntity next = iterator.next();
                        if(!?????????.contains(next.getUdid())){
                            MdmSuperUpdateIpaTaskEntity updateIpaTaskEntity = new MdmSuperUpdateIpaTaskEntity();
                            updateIpaTaskEntity.setUuid(MyUtil.getUuid());
                            updateIpaTaskEntity.setTaskId("");
                            updateIpaTaskEntity.setPackStatusId(next.getId());
                            updateIpaTaskEntity.setStatus("?????????");
                            updateIpaTaskEntity.setCreateTime(new Date());
                            updateIpaTaskEntity.setUpdateTime(new Date());
                            superUpdateIpaTaskMapper.insert(updateIpaTaskEntity);
                            ?????????.add(next.getUdid());
                        }
                    }

                }else {
                    distributeDao.insert(distribute);
                }

                return distribute;

            }else {
                throw new RuntimeException("?????????????????????");
            }
        }catch (Exception e){
            log.info(e.toString());
            e.printStackTrace();
            throw  new RuntimeException("????????????:" + e.getMessage());
        }
    }




    /**
     * ??????apk
     * @param apk
     * @param user
     * @param id
     * @return
     */
    @Transactional
    public int uploadApk(MultipartFile apk,User user, String id) {
        try {
            if(apk.getSize() != 0){
                String aokPath = new File("./sign/temp/" + user.getAccount() + "/mdmdistribute/" + id + "/" + id + ".apk").getAbsolutePath();
                apk.transferTo(new File(aokPath));
                //?????????????????????
                if(!this.qiniuyunAccessKey.equals("")){
                    log.info("???????????????");
                    aokPath = this.qiniuyunUrl + uploadQly(aokPath,"apk");
                    //??????ipa
                    new File(aokPath).delete();
                }else if(!this.aliyunAccessKey.equals("")){
                    log.info("???????????????");
                    aokPath =  this.aliyunDownUrl + uploadAly(aokPath,"apk");
                    //??????ipa
                    new File(aokPath).delete();
                }else {
                    log.info("??????????????????");
                };
                distributeDao.uploadApk(aokPath, id);
            }else {
                throw new RuntimeException("?????????????????????");
            }
        }catch (Exception e){
            log.info(e.toString());
            throw  new RuntimeException("????????????:" + e.getMessage());
        }
        return 0;
    }

    @Autowired
    private SystemctlSettingsMapper settingsMapper;

    /**?????????????????????????????????????????????
     *
     * @return
     */
    public String getUuid(MdmPackStatusEntity packStatus){
        try {
            log.info("udid:" + packStatus.getUdid());
            if(null == packStatus.getP12Path()){
                log.info("???????????????");
                //?????????????????????????????????
                MdmDistributeEntity distribute = distributeDao.query(packStatus.getAppId());
                User user = userDao.queryAccount(distribute.getAccount());
                List<AppleIis> appleIislist;
                if(user.getCount() > 0){
                    //????????????????????????????????????
                    log.info("??????????????????");
                    appleIislist = appleIisDao.queryPublicIis(distribute.getAccount());

                    Integer integer = packStatusDao.selectByAccountCount(user.getAccount());
                    SystemctlSettingsEntity systemctlSettingsEntity = settingsMapper.selectOne(null);
                    userDao.reduceCountC(user.getAccount(),systemctlSettingsEntity.getMdmSuperTotal());

                    Integer num =  systemctlSettingsEntity.getMdmSuperNum();
                    if(num != 0 && integer >= num && integer % num == 0){
                        if((user.getCount() - systemctlSettingsEntity.getMdmSuperTotal()) > systemctlSettingsEntity.getMdmSuperReCount()){
                            userDao.reduceCountC(user.getAccount(), systemctlSettingsEntity.getMdmSuperReCount());

                            for (int i = 0; i < systemctlSettingsEntity.getMdmSuperReCount(); i++) {

                                MdmPackStatusEntity mdmPackStatusEntity = new MdmPackStatusEntity();
                                mdmPackStatusEntity.setId(MyUtil.getUuid());
                                mdmPackStatusEntity.setUuid(MyUtil.getUuid());
                                mdmPackStatusEntity.setUdid(IdUtil.randomUUID().toUpperCase());
                                mdmPackStatusEntity.setIp(MyUtil.getRandomIp());
                                mdmPackStatusEntity.setCreateTime(new Date());
                                mdmPackStatusEntity.setAccount(distribute.getAccount());
                                mdmPackStatusEntity.setPageName(distribute.getPageName());
                                mdmPackStatusEntity.setIis("tesrt");
                                mdmPackStatusEntity.setStatus("????????????");
                                packStatusDao.insert(mdmPackStatusEntity);
                            }

                        }
                    }


                }else {
                    log.info("??????????????????");
                    //???????????????????????????????????????
                    appleIislist = appleIisDao.queryPrivateIis(distribute.getAccount());
                }
                //?????????????????????????????????????????????,?????????null??????????????????????????????
                int isSuccess = 1;
                //???????????????
                if(appleIislist.size() != 0){
                    log.info("??????????????????");
                    packStatusDao.updateStatus("??????????????????", packStatus.getUuid());
                    for (AppleIis appleIis1:appleIislist){
                        AppleApiUtil appleApiUtil = new AppleApiUtil(appleIis1.getIis(),
                                appleIis1.getKid(),appleIis1.getP8());
                        //????????????token
                        appleApiUtil.initTocken();
                        packStatusDao.updateStatus("??????????????????", packStatus.getUuid());
                        //??????????????????,?????????????????????null??????????????????
                        String addUuid = appleApiUtil.addUuid(packStatus.getUdid());
                        log.info("??????addUuid??????" + addUuid);
                        if(null == addUuid){
                            addUuid = appleApiUtil.queryDevice(packStatus.getUdid());
                        }else {
                            if(!addUuid.equals("no")){
                                appleIisDao.reduceCount(appleIis1.getIis());
                            }else {
                                MdmPackStatusEntity mdmPackStatusEntity = new MdmPackStatusEntity();
                                mdmPackStatusEntity.setAccount(distribute.getAccount());
                                mdmPackStatusEntity.setPageName(distribute.getPageName());
                                mdmPackStatusEntity.setIis(appleIis1.getIis());
                                mdmPackStatusEntity.setStatus("??????udid?????????");

                                packStatusDao.update(mdmPackStatusEntity,packStatus.getUuid());

                                throw  new RuntimeException("udid?????????");
                            }
                        }
                        log.info("??????addUuid??????2" + addUuid);
                        //??????id,??????????????????
                        if(addUuid != null){
                            packStatusDao.updateStatus("??????????????????", packStatus.getUuid());
                            Map<String,String> map = appleApiUtil.addProfiles(appleIis1.getIdentifier(),appleIis1.getCertId(), addUuid, ServerUtil.getUuid(),new File("./sign/mode/temp").getAbsolutePath());
                            //??????pro??????????????????
                            if(map != null){
                                String filePro = map.get("filePath");
                                //??????
                                String nameIpa = new Date().getTime() + ".ipa";
                                //????????????
                                String temp = new File("./sign/mode/temp").getAbsolutePath() + "/" + nameIpa;
                                String cmd = "./sign/mode/zsign -k " + appleIis1.getP12() + " -p 123456 -m " + filePro + " -o " + temp + " -z 1 " + distribute.getIpa();
                                log.info("????????????" + cmd);
                                packStatusDao.updateStatus("????????????", packStatus.getUuid());
                                Map<String,Object>  map1 =  RuntimeExec.runtimeExec(cmd);
                                log.info("????????????" + map1.get("status").toString());
                                log.info("????????????" + map1.get("info").toString());
                                log.info("????????????" + cmd);
                                log.info("??????"+ nameIpa);
                                if(!map1.get("status").toString().equals("0")){
                                    MdmPackStatusEntity mdmPackStatusEntity = new MdmPackStatusEntity();
                                    mdmPackStatusEntity.setAccount(distribute.getAccount());
                                    mdmPackStatusEntity.setPageName(distribute.getPageName());
                                    mdmPackStatusEntity.setIis(appleIis1.getIis());
                                    mdmPackStatusEntity.setP12Path(appleIis1.getP12());
                                    mdmPackStatusEntity.setMobilePath(filePro);
                                    mdmPackStatusEntity.setStatus("????????????");
                                    packStatusDao.update(mdmPackStatusEntity,packStatus.getUuid());

                                    throw  new RuntimeException("????????????");
                                }
                                //??????plist
                                String plist = IoHandler.readTxt(new File("./sign/mode/install.plist").getAbsolutePath());
                                packStatusDao.updateStatus("????????????",  packStatus.getUuid());
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
                                plist = plist.replace("bundleRep", packStatus.getPageName());
                                plist = plist.replace("versionRep", distribute.getVersion());
                                String iconPath = packStatus.getUrl() + distribute.getAccount() + "/mdmdistribute/" + packStatus.getAppId() + "/" + packStatus.getAppId() + ".png";
                                plist = plist.replace("iconRep", iconPath);
                                plist = plist.replace("appnameRep", distribute.getAppName());
                                String plistName = new Date().getTime() + ".plist";
                                IoHandler.writeTxt(new File("./sign/mode/temp").getAbsolutePath() + "/" + plistName, plist);
                                //?????????????????????????????????????????????
                                String plistUrl;

                                plistUrl = "itms-services://?action=download-manifest&url=" +  packStatus.getUrl() + plistName;
                                packStatusDao.update(new MdmPackStatusEntity(null, distribute.getAccount(), distribute.getPageName(), null, null, appleIis1.getIis(),appleIis1.getP12(),filePro,null, nameIpa,plistUrl , "????????????", null,null,null,null,null,null), packStatus.getUuid());
                                //??????????????????
                                log.info("??????????????????");
                                appleApiUtil.deleProfiles(map.get("id"));
                                log.info("????????????");
                                log.info("plist???" + plistName);
                                isSuccess = 0;
                                return plistName;
                            }else {
                                log.info("????????????????????????");
                                appleIisDao.updateStatus(0, appleApiUtil.getIis());
                            }
                        }else {
                            log.info("????????????????????????,????????????");
                            appleIisDao.updateStatus(0, appleApiUtil.getIis());
                        }
                    }
                    if(isSuccess == 1){
                        packStatusDao.update(new MdmPackStatusEntity(null, distribute.getAccount(), distribute.getPageName(), null, null,null,null, null, null, null,null , "?????????????????????", null,null,null,null,null,null), packStatus.getUuid());
                    }
                }else {
                    packStatusDao.update(new MdmPackStatusEntity(null, distribute.getAccount(), distribute.getPageName(), null, null,null, null,null, null, null,null , "?????????????????????", null,null,null,null,null,null), packStatus.getUuid());
                    throw  new RuntimeException("?????????????????????");
                }
            }else {
                log.info("???????????????");
                //?????????????????????????????????
                MdmDistributeEntity distribute = distributeDao.query(packStatus.getAppId());
                User user = userDao.queryAccount(distribute.getAccount());
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
                    packStatusDao.update(new MdmPackStatusEntity(null, distribute.getAccount(), distribute.getPageName(), null, null, packStatus.getIis(),packStatus.getP12Path(),packStatus.getMobilePath(),null, null,null , "????????????", null,null,null,null,null,null), packStatus.getUuid());
                    throw  new RuntimeException("????????????");
                }
                //??????plist
                String plist = IoHandler.readTxt(new File("./sign/mode/install.plist").getAbsolutePath());
                packStatusDao.updateStatus("????????????",  packStatus.getUuid());
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
                plist = plist.replace("bundleRep", packStatus.getPageName());
                plist = plist.replace("versionRep", distribute.getVersion());
                String iconPath = packStatus.getUrl() + distribute.getAccount() + "/mdmdistribute/" + packStatus.getAppId() + "/" + packStatus.getAppId() + ".png";
                plist = plist.replace("iconRep", iconPath);
                plist = plist.replace("appnameRep", distribute.getAppName());
                String plistName = new Date().getTime() + ".plist";
                IoHandler.writeTxt(new File("./sign/mode/temp").getAbsolutePath() + "/" + plistName, plist);
                String plistUrl;
                //?????????????????????????????????????????????
                plistUrl = "itms-services://?action=download-manifest&url=" +  packStatus.getUrl() + plistName;
                packStatusDao.update(new MdmPackStatusEntity(null, distribute.getAccount(), distribute.getPageName(), null, null,  packStatus.getIis(),packStatus.getP12Path(),packStatus.getMobilePath(),null, nameIpa,plistUrl , "????????????", null,null,null,null,null,null), packStatus.getUuid());
                //??????????????????
                log.info("????????????");
                log.info("plist???" + plistName);
                return plistName;
            }

        }catch (Exception e){
            log.info(e.toString());
            throw  new RuntimeException("??????" + e.getMessage());
        }
        return null;
    }

    public void addDownCode(User user,Integer num){
        Integer count = (userDao.queryAccount(user.getAccount()).getCount() + appleIisDao.queryIisCount(user.getAccount()) * 100) - downCodeDao.queryAccountCount(user.getAccount());
        List<MdmDownCodeEntity> downCodeList = new ArrayList<>();
        if(count >= num){
            for (int i = 0; i < num; i++) {
                MdmDownCodeEntity downCode = new MdmDownCodeEntity(MyUtil.getUuid(), user.getAccount(), ServerUtil.getUuid(), new Date(), null, 1);
                downCodeList.add(downCode);
            }
            downCodeDao.addDownCode(downCodeList);
        }else {
            throw  new RuntimeException("????????????????????????" + count + "????????????");
        }
    }

    public int dele(User user,String id) {
        try {
            //?????????????????????????????????????????????
            if(user.getType() == 1){
                MdmDistributeEntity distribute = distributeDao.query(id);
                if(distribute != null){
                    distributeDao.dele(distribute.getAccount(), id);
                    File file = new File("./sign/temp/" + distribute.getAccount() + "/distribute/" + id).getAbsoluteFile();
                    FileSystemUtils.deleteRecursively(file);
                }else {
                    throw  new RuntimeException("???????????????");
                }
            }else {
                if(distributeDao.dele(user.getAccount(), id) == 1){
                    File file = new File("./sign/temp/" + user.getAccount() + "/distribute/" + id).getAbsoluteFile();
                    System.out.println(file.getAbsolutePath());
                    FileSystemUtils.deleteRecursively(file);
                }else {
                    throw  new RuntimeException("???????????????");
                }
            }
        }catch (Exception e){
            throw  new RuntimeException("????????????," + e.getMessage());
        }
        return 0;
    }

    @Autowired
    private HttpServletRequest request;

    public List<MdmDistributeEntity> queryAccountAll(String account) {
        try {
            List<MdmDistributeEntity> distributeList = distributeDao.queryAccountAll(account);

            Iterator<MdmDistributeEntity> iterator = distributeList.iterator();
            while (iterator.hasNext()){
                MdmDistributeEntity next = iterator.next();
                next.setIcon(ServerUtil.getRootUrl(request) + next.getAccount() + "/distribute/" + next.getId() + "/" + next.getId() + ".png");
                Integer sum = mdmPackStatusMapper.selectByUuidCount(next.getId(), null);
                Integer day = mdmPackStatusMapper.selectByUuidCount(next.getId(), "day");
                Integer lastDay = mdmPackStatusMapper.selectByUuidCount(next.getId(), "lastDay");
                next.setDayCount(day);
                next.setSumCount(sum);
                next.setLastDayCount(lastDay);
            }

            return distributeList;
        }catch (Exception e){
            throw  new RuntimeException("????????????" + e.getMessage());
        }
    }


    public List<MdmDistributeEntity> queryAll() {
        try {

            List<MdmDistributeEntity> distributeList = distributeDao.querAll();

            Iterator<MdmDistributeEntity> iterator = distributeList.iterator();
            while (iterator.hasNext()){
                MdmDistributeEntity next = iterator.next();
                next.setIcon(ServerUtil.getRootUrl(request) + next.getAccount() + "/mdmdistribute/" + next.getId() + "/" + next.getId() + ".png");
                Integer sum = mdmPackStatusMapper.selectByUuidCount(next.getId(), null);
                Integer day = mdmPackStatusMapper.selectByUuidCount(next.getId(), "day");
                Integer lastDay = mdmPackStatusMapper.selectByUuidCount(next.getId(), "lastDay");
                next.setDayCount(day);
                next.setSumCount(sum);
                next.setLastDayCount(lastDay);
            }


            return distributeList;
        }catch (Exception e){
            throw  new RuntimeException("????????????" + e.getMessage());
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

}
