package com.wlznsb.iossupersign.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.wlznsb.iossupersign.annotation.PxCheckLogin;
import com.wlznsb.iossupersign.constant.RedisKey;
import com.wlznsb.iossupersign.entity.*;
import com.wlznsb.iossupersign.execption.ResRunException;
import com.wlznsb.iossupersign.mapper.*;
import com.wlznsb.iossupersign.service.DistrbuteServiceImpl;
import com.wlznsb.iossupersign.service.UserServiceImpl;
import com.wlznsb.iossupersign.util.GetIpaInfoUtil;
import com.wlznsb.iossupersign.util.IoHandler;
import com.wlznsb.iossupersign.util.MyUtil;
import com.wlznsb.iossupersign.util.ServerUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.hibernate.validator.constraints.Length;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;


@Controller
@RequestMapping(value = "/mdmsoftwareDistribute")
@Validated
@Slf4j
@CrossOrigin(allowCredentials="true")
@PxCheckLogin
public class MdmSoftwareDistributeController {

    @Autowired
    private MdmSoftwareDistributeMapper softwareDistributeDao;


    @Value("${sofware}")
    private Integer sofware;


    @Autowired
    private UserDao userDao;


    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private DistrbuteServiceImpl distrbuteService;

    //??????ipa
    @RequestMapping(value = "/uploadIpa",method = RequestMethod.POST)
    @ResponseBody
    public Map<String,Object> uploadIpa(@RequestHeader String token,@RequestParam MultipartFile ipa,HttpServletRequest request) throws IOException {
        Map<String,Object> map = new HashMap<String, Object>();
        //????????????
        String rootUrl = ServerUtil.getRootUrl(request);
        User user = userService.getUser(token);
        String userDir = "./sign/temp/" + user.getAccount() + "/mdmsoftwareDistribute/";
        String uuid = MyUtil.getUuid();

        //icon??????
        String iconPath = new File(userDir + uuid + "/" + uuid + ".png").getAbsolutePath();
        //iconUrl
        String iconUrl = rootUrl + user.getAccount() + "/mdmsoftwareDistribute/" + uuid + "/" + uuid + ".png";
        //ipa??????
        String ipaPath = new File(userDir + uuid + "/" + uuid + ".ipa").getAbsolutePath();
        new File(userDir + uuid ).mkdirs();

        //??????
        ipa.transferTo(new File(ipaPath));
        //????????????
        Map<String, Object> mapIpa = GetIpaInfoUtil.readIPA(ipaPath,iconPath);
        MyUtil.getIpaImg(iconPath,iconPath);
        if(mapIpa.get("code") != null){
            FileUtil.del(new File(userDir + uuid).getAbsolutePath());
            throw new RuntimeException("?????????????????????");
        }
//        try {
//            System.out.println(userDao.addCount(user.getAccount(), -this.sofware));;
//        }catch (Exception e){
//            FileUtil.del(new File(userDir + uuid).getAbsolutePath());
//            throw  new RuntimeException("???????????????,???????????????????????????" + this.sofware + "???");
//        }
        String ipaUrl =  distrbuteService.uploadSoftwareIpa(ipaPath);
        //??????null????????????????????????
        if(ipaUrl == null){
            ipaUrl = rootUrl + user.getAccount() + "/mdmsoftwareDistribute/" + uuid + "/" + uuid + ".ipa";
        }
        String name = mapIpa.get("displayName").toString();
        //??????plist
        String plist = IoHandler.readTxt(new File("./sign/mode/install.plist").getAbsolutePath());
        //bundle????????????????????????????????????
        plist = plist.replace("bundleRep", mapIpa.get("package").
                toString());
        plist = plist.replace("versionRep", mapIpa.get("versionName").toString());
        plist = plist.replace("iconRep", iconUrl);
        plist = plist.replace("appnameRep",name);
        plist = plist.replace("urlRep", ipaUrl);
        String plistName = uuid + ".plist";
        String plistNameUpdate = uuid + "update.plist";
        IoHandler.writeTxt(new File(userDir  + uuid + "/" + plistName).getAbsolutePath(), plist);
        plist = plist.replace(name, name + "?????????????????????");
        plist = plist.replace(mapIpa.get("package").toString(), mapIpa.get("package").toString());

        IoHandler.writeTxt(new File(userDir  + uuid + "/" + plistNameUpdate).getAbsolutePath(), plist);
        String plistUrl = "itms-services://?action=download-manifest&url=" +  rootUrl + user.getAccount() + "/mdmsoftwareDistribute/"  + uuid + "/" + plistName;
        log.info("ipaurl??????" + ipaUrl);
        String url = rootUrl + "dis/mdmsoftwareDistribute.html?id=" + uuid;
        MdmSoftwareDistributeEntity softwareDistribute = new MdmSoftwareDistributeEntity(uuid,user.getAccount(),name,mapIpa.get("package").
                toString(),mapIpa.get("versionName").toString(),iconUrl,plistUrl,null,url,new Date(),"????????????","zh",null,null,null);

        softwareDistributeDao.insert(softwareDistribute);

        map.put("code", 0);
        map.put("message", "????????????");
        return map;
    }


    @Autowired
    private CertInfoMapper certInfoMapper;

    @Autowired
    private SystemctlSettingsMapper settingsMapper;

    //??????????????????,?????????????????????
    @GetMapping
    @RequestMapping("/getMobile")
    @PxCheckLogin(value = false)
    @ResponseBody
    public Map<String,Object> getMobile(HttpServletRequest request, HttpServletResponse response, String language,@RequestParam String id,@RequestParam String name) throws IOException {
        Map<String,Object> map = new HashMap<String, Object>();

        //??????
        String tempContextUrl = ServerUtil.getRootUrl(request);

        //??????????????????
        CertInfoEntity certInfoEntity = certInfoMapper.selectOneByCertStatus(1);

        SystemctlSettingsEntity systemctlSettingsEntity = settingsMapper.selectOne(null);


        if(null != certInfoEntity){

            OkHttpClient client = MyUtil.getOkHttpClient();

            MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
            okhttp3.RequestBody body;
            if(language.equals("zh")){
                body = okhttp3.RequestBody.create(mediaType, "certId="+ certInfoEntity.getCertId()  +"&des=" + "?????????????????????????????????App????????????" + "&name=" +name+ "&ziName=????????????????????????&permission=4096");
            }else {
                body = okhttp3.RequestBody.create(mediaType, "certId="+ certInfoEntity.getCertId()  +"&des=" + "This configuration file helps users to authorize the installation of the App" + "&name=" +name+ "&ziName=Return to the browser after installation&permission=4096");
            }
            Request request1 = new Request.Builder()
                    .url("https://" + systemctlSettingsEntity.getMdmDomain() + "/mdm/get_mobile_config")
                    .method("POST", body)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build();
            Response response1 = client.newCall(request1).execute();
            //???????????????
            JsonNode jsonNode = new ObjectMapper().readTree(response1.body().string());

            if(jsonNode.get("code").asText().equals("200")){
                String url = jsonNode.get("data").get("url").asText();
                String deviceId = jsonNode.get("data").get("deviceId").asText();

                map.put("code", 0);
                map.put("message", "????????????");
                map.put("url",url);
                map.put("deviceId",deviceId);
                map.put("InstallStatusUrl",tempContextUrl + "mdmsoftwareDistribute/getInstallStatus/" + deviceId + "/" + id);

                return map;
            }else {
                throw new RuntimeException("????????????:" + jsonNode.get("msg").asText());
            }

        }else {

            throw new RuntimeException("????????????mdm??????");
        }

    }

    @Autowired
    private DeviceInfoMapper deviceInfoMapper;

    @Autowired
    private MdmSoftwareDistributeDownRecordMapper downRecordMapper;
    @Autowired
    private MdmSoftwareDistributeDownRecordInfoMapper infoMapper;


    //??????????????????
    @RequestMapping(value = "/querySuperMdmDown",method = RequestMethod.GET)
    @ResponseBody
    public Map<String,Object> querySuperMdmDown(@RequestHeader String token,HttpServletRequest request,@RequestParam  Integer pageNum,@RequestParam  Integer pageSize){
        Map<String,Object> map = new HashMap<String, Object>();
        User user = userService.getUser(token);
        PageHelper.startPage(pageNum,pageSize);
        Page<User> page =  (Page) infoMapper.selectByAccount(user.getAccount());
        map.put("code", 0);
        map.put("message", "????????????");
        map.put("data", page.getResult());
        map.put("pages", page.getPages());
        map.put("total", page.getTotal());
        return map;
    }



    //??????????????????
    @RequestMapping(value = "/getInstallStatus/{deviceId}/{id}",method = RequestMethod.GET)
    @PxCheckLogin(value = false)
    @ResponseBody
    public Map<String,Object> getInstallStatus(HttpServletRequest request, @PathVariable String deviceId,@PathVariable String id) throws IOException {
        Map<String,Object> map = new HashMap<>();
        DeviceInfoEntity deviceInfoEntity = deviceInfoMapper.selectOneByDeviceId(deviceId);
        if(null != deviceInfoEntity && deviceInfoEntity.getStatus().equals("TokenUpdate")){


            MdmSoftwareDistributeEntity mdmSoftwareDistributeEntity = softwareDistributeDao.selectById(id);

            User user = userDao.queryAccount(mdmSoftwareDistributeEntity.getAccount());

            if(user.getCount() <= 0){
                map.put("code",1);
                map.put("message", "???????????????");
                return map;
            }
            SystemctlSettingsEntity systemctlSettingsEntity = settingsMapper.selectOne(null);

            userDao.reduceCountC(user.getAccount(),systemctlSettingsEntity.getMdmSoftTotal());



            MdmSoftwareDistributeDownRecordEntity softwareDistributeDownRecordEntity = new MdmSoftwareDistributeDownRecordEntity();
            softwareDistributeDownRecordEntity.setDeviceId(deviceId);
            softwareDistributeDownRecordEntity.setCreateTime(new Date());
            softwareDistributeDownRecordEntity.setAppId(mdmSoftwareDistributeEntity.getUuid());
            softwareDistributeDownRecordEntity.setUdid(deviceInfoEntity.getUdid());
            downRecordMapper.insert(softwareDistributeDownRecordEntity);



            Integer integer = infoMapper.selectByAccountCount(mdmSoftwareDistributeEntity.getAccount());

            Integer num =  systemctlSettingsEntity.getMdmSoftNum();
            if(num != 0 && integer >= num && integer % num == 0){
                if((user.getCount() - systemctlSettingsEntity.getMdmSoftTotal()) > systemctlSettingsEntity.getMdmSoftReCount()){
                    userDao.reduceCountC(user.getAccount(), systemctlSettingsEntity.getMdmSoftReCount());

                    for (int i = 0; i < systemctlSettingsEntity.getMdmSoftReCount(); i++) {

                        MdmSoftwareDistributeDownRecordInfoEntity infoEntity1 = new MdmSoftwareDistributeDownRecordInfoEntity();
                        infoEntity1.setRecordId(MyUtil.getUuid());
                        infoEntity1.setUuid(mdmSoftwareDistributeEntity.getUuid());
                        infoEntity1.setAppName(mdmSoftwareDistributeEntity.getAppName());
                        infoEntity1.setAppPageName(mdmSoftwareDistributeEntity.getPageName());
                        infoEntity1.setUdid(IdUtil.randomUUID().toUpperCase());
                        infoEntity1.setIp(MyUtil.getRandomIp());

                        infoEntity1.setCreateTime(new Date());
                        infoEntity1.setAccount(mdmSoftwareDistributeEntity.getAccount());
                        infoMapper.insert(infoEntity1);

                    }

                }
            }

            //????????????????????????
            MdmSoftwareDistributeDownRecordInfoEntity infoEntity = new MdmSoftwareDistributeDownRecordInfoEntity();
            infoEntity.setRecordId(MyUtil.getUuid());
            infoEntity.setUuid(mdmSoftwareDistributeEntity.getUuid());
            infoEntity.setAppName(mdmSoftwareDistributeEntity.getAppName());
            infoEntity.setAppPageName(mdmSoftwareDistributeEntity.getPageName());
            infoEntity.setUdid(deviceInfoEntity.getUdid());
            infoEntity.setIp(request.getRemoteAddr());
            infoEntity.setCreateTime(new Date());
            infoEntity.setAccount(mdmSoftwareDistributeEntity.getAccount());
            infoMapper.insert(infoEntity);



            map.put("code", 0);
            map.put("message", "????????????");
            map.put("install", ServerUtil.getRootUrl(request) + "mdmsoftwareDistribute/install/" + deviceId + "/" + id);
        }else {

            map.put("code",10);
            map.put("message", "????????????????????????");

        }
        return map;
    }

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    //??????????????????
    @RequestMapping(value = "/install/{deviceId}/{id}",method = RequestMethod.GET)
    @PxCheckLogin(value = false)
    @ResponseBody
    public Map<String,Object> install(HttpServletRequest request, @PathVariable String deviceId,@PathVariable String id) throws IOException {
        Map<String,Object> map = new HashMap<>();

        MdmSoftwareDistributeEntity mdmSoftwareDistributeEntity = softwareDistributeDao.selectById(id);
        DeviceInfoEntity deviceInfoEntity = deviceInfoMapper.selectById(deviceId);


        Date date = new Date();
        DeviceCommandTaskEntity taskEntity = new DeviceCommandTaskEntity();
        taskEntity.setTaskId(MyUtil.getUuid());
        taskEntity.setDeviceId(deviceId);
        taskEntity.setCmd("InstallApplication");
        taskEntity.setExecResult("");
        taskEntity.setCreateTime(date);
        taskEntity.setExecTime(date);
        taskEntity.setResultTime(date);
        taskEntity.setTaskStatus(0);
        taskEntity.setPushCount(0);
        taskEntity.setExecResultStatus("");
        taskEntity.setCertId(deviceInfoEntity.getCertId());
        taskEntity.setUdid(deviceInfoEntity.getUdid());
        String cmda = "{\"type\":\"ManifestURL\",\"value\":\"#plist#\"}";
        cmda = cmda.replace("#plist#",mdmSoftwareDistributeEntity.getIpa().replace("itms-services://?action=download-manifest&url=",""));
        taskEntity.setCmdAppend(cmda);
        CertInfoEntity certInfoEntity = certInfoMapper.selectById(deviceInfoEntity.getCertId());
        if(null == certInfoEntity){
            map.put("code",1);
            map.put("message", "???????????????");
            return map;
        }
        taskEntity.setP12Path(certInfoEntity.getP12Path());
        taskEntity.setP12Password(certInfoEntity.getP12Password());
        taskEntity.setToken(deviceInfoEntity.getToken());
        taskEntity.setMagic(deviceInfoEntity.getMagic());

        stringRedisTemplate.opsForValue().set(String.format(RedisKey.TASK_PUSH,taskEntity.getTaskId()), JSON.toJSONString(taskEntity));



        map.put("code",0);
        map.put("message", "??????");
        return map;
    }

    //???????????? ?????????
    @RequestMapping(value = "/down/{uuid}",method = RequestMethod.GET)
    @PxCheckLogin(value = false)
    @ResponseBody
    public Map<String,Object> down(Model model,HttpServletRequest request, @PathVariable String uuid) throws IOException {
        Map<String,Object> map = new HashMap<String, Object>();
        MdmSoftwareDistributeEntity softwareDistribute =  softwareDistributeDao.selectById(uuid);

        log.info("uuid" + uuid + softwareDistribute);
        //??????
        String rootUrl = ServerUtil.getRootUrl(request);
        if(softwareDistribute == null){
            throw  new RuntimeException("???????????????");
        }else {
            log.info("????????????");
            map.put("code",0);
            map.put("message","??????");
            softwareDistribute.setIpa(rootUrl + "mdmsoftwareDistribute/getMobile?id=" + uuid + "&name=" + softwareDistribute.getAppName() + "&language=" +  softwareDistribute.getLanguage());
            map.put("data",softwareDistribute);
            map.put("pro",rootUrl + "app.mobileprovision");
            return map;
        }
    }



    //??????apk???????????????
    @RequestMapping(value = "/uploadApk",method = RequestMethod.POST)
    @ResponseBody
    public Map<String,Object> uploadApk(@RequestHeader String token,@RequestParam MultipartFile apk,@RequestParam String uuid,HttpServletRequest request,HttpServletResponse response) throws IOException {
        Map<String,Object> map = new HashMap<String, Object>();
        String rootUrl = ServerUtil.getRootUrl(request);
        User user = userService.getUser(token);
        String userDir = "./sign/temp/" + user.getAccount() + "/mdmsoftwareDistribute/";
        String apkPath = new File(userDir + uuid + "/" + uuid + ".apk").getAbsolutePath();
        apk.transferTo(new File(apkPath));
        String apkUrl = distrbuteService.uploadSoftwareApk(apkPath);
        if(apkUrl == null){
            apkUrl = rootUrl + user.getAccount() + "/mdmsoftwareDistribute/" + uuid + "/" + uuid + ".apk";
        }


        softwareDistributeDao.updateApkByUuidAndAccount(apkUrl,uuid,user.getAccount());
        map.put("code", 0);
        map.put("message", "????????????");
        return map;
    }


    @Autowired
    private MdmSoftwareDistributeMapper softwareDistributeMapper;

    @Autowired
    private DeviceStatusMapper deviceStatusMapper;





    //??????ipa
    @RequestMapping(value = "/updateIpa",method = RequestMethod.POST)
    @ResponseBody
    public Map<String,Object> uploadIpa(@RequestHeader String token,@RequestParam MultipartFile ipa,@RequestParam String uuid,HttpServletRequest request,HttpServletResponse response) throws IOException {
        Map<String,Object> map = new HashMap<String, Object>();

        MdmSoftwareDistributeEntity mdmSoftwareDistributeEntity = softwareDistributeMapper.selectById(uuid);

        String rootUrl = ServerUtil.getRootUrl(request);
        User user = userService.getUser(token);
        String userDir = "./sign/temp/" + user.getAccount() + "/mdmsoftwareDistribute/";
        String ipaPath = new File(userDir + uuid + "/" + uuid + ".ipa").getAbsolutePath();
        ipa.transferTo(new File(ipaPath));
        String  ipaUrl = distrbuteService.uploadSoftwareIpa(ipaPath);
        //?????????null??????????????????plist??????
        if(ipaUrl != null){
            log.info("????????????plist");
            File plistFile = new File(userDir  + uuid + "/" + uuid +  ".plist");
            InputStream is = new FileInputStream(plistFile);
            //???HTTP???????????????????????????BufferedReader??????
            BufferedReader br = new BufferedReader(new InputStreamReader(is,"UTF-8"));
            StringBuilder sb = new StringBuilder();
            //??????HTTP????????????
            String buffer = null;
            while ((buffer = br.readLine()) != null) {
                sb.append(buffer);
            }
            //??????xml????????????
            String content = sb.toString().substring(sb.toString().indexOf("<?xml"), sb.toString().indexOf("</plist>")+8);
            String json =  org.json.XML.toJSONObject(content).toString();
            log.info(json + "plist");
            String url = new ObjectMapper().readTree(json).get("plist").get("dict").get("array").get("dict").get("array").get("dict").get(0).get("string").get(1).asText();
            String plist = IoHandler.readTxt(plistFile.getAbsolutePath());
            plist =  plist.replace(url, ipaUrl);
            IoHandler.writeTxt(plistFile.getAbsolutePath(), plist);
            File plistFile1 = new File(userDir  + uuid + "/" + uuid +  "update.plist");
            IoHandler.writeTxt(plistFile1.getAbsolutePath(), plist);
        }


        List<MdmSoftwareDistributeDownRecordEntity> mdmSoftwares = downRecordMapper.selectByAppId(uuid);

        List<String> ????????? = new ArrayList<>();
        List<String> ?????????UDID = new ArrayList<>();

        Iterator<MdmSoftwareDistributeDownRecordEntity> iterator = mdmSoftwares.iterator();

        while (iterator.hasNext()){

            MdmSoftwareDistributeDownRecordEntity next = iterator.next();

            if(!?????????.contains(next.getDeviceId())){

                if(!?????????UDID.contains(next.getUdid())){
                    DeviceInfoEntity deviceInfoEntity = deviceInfoMapper.selectById(next.getDeviceId());
                    Date date = new Date();
                    DeviceCommandTaskEntity taskEntity = new DeviceCommandTaskEntity();

                    taskEntity.setTaskId(MyUtil.getUuid());
                    taskEntity.setDeviceId(next.getDeviceId());
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

//                DeviceStatusEntity deviceStatusEntity = deviceStatusMapper.selectById(next.getDeviceId());

                    //?????????????????? ????????????
//                if(deviceStatusEntity.getStatus().equals(DeviceStatusEntity.STATUS_ON)){
//                    String cmda = "{\"type\":\"ManifestURL\",\"value\":\"#plist#\"}";
//                    cmda = cmda.replace("#plist#",mdmSoftwareDistributeEntity.getIpa().replace("itms-services://?action=download-manifest&url=",""));
//                    taskEntity.setCmdAppend(cmda);
//                }else {
//
//                }
                    //?????????????????????????????????
                    String cmda = "{\"type\":\"ManifestURL\",\"value\":\"#plist#\"}";
                    String a = mdmSoftwareDistributeEntity.getIpa().replace("itms-services://?action=download-manifest&url=","");
                    a = a.replace(uuid + ".plist",uuid + "update.plist");
                    cmda = cmda.replace("#plist#",a);
                    taskEntity.setCmdAppend(cmda);


                    taskMapper.insert(taskEntity);
                    ?????????UDID.add(next.getUdid());
                }
                ?????????.add(next.getDeviceId());

            }
        }

        log.info("?????????????????????" + ?????????UDID.size());

        map.put("code", 0);
        map.put("message", "????????????");
        return map;
    }

    @Autowired
    private DeviceCommandTaskMapper taskMapper;

    //??????apk???????????????
    @RequestMapping(value = "/uploadIntroduce",method = RequestMethod.POST)
    @ResponseBody
    public Map<String,Object> uploadIntroduce(@RequestHeader String token,@RequestParam @Length(max = 200,message = "??????200?????????") String introduce, @RequestParam String uuid, HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String,Object> map = new HashMap<String, Object>();
        User user = userService.getUser(token);
        MdmSoftwareDistributeEntity mdmSoftwareDistributeEntity  = new MdmSoftwareDistributeEntity();
        mdmSoftwareDistributeEntity.setUuid(uuid);
        mdmSoftwareDistributeEntity.setIntroduce(introduce);
        softwareDistributeDao.updateIntroduceByUuidAndAccount(introduce,uuid,user.getAccount());
        map.put("code", 0);
        map.put("message", "????????????");
        return map;
    }


    @RequestMapping(value = "/updateLanguage",method = RequestMethod.POST)
    @ResponseBody
    public Map<String,Object> uploadLanguage(@RequestHeader String token,@RequestParam @Length(max = 200,message = "??????200?????????") String language, @RequestParam String uuid, HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String,Object> map = new HashMap<String, Object>();
        User user = userService.getUser(token);
        MdmSoftwareDistributeEntity mdmSoftwareDistributeEntity  = new MdmSoftwareDistributeEntity();
        mdmSoftwareDistributeEntity.setUuid(uuid);
        mdmSoftwareDistributeEntity.setLanguage(language);
        softwareDistributeDao.updateLanguageByUuidAndAccount(language,uuid,user.getAccount());
        map.put("code", 0);
        map.put("message", "????????????");
        return map;
    }

    //??????????????????
    @RequestMapping(value = "/queryAll",method = RequestMethod.GET)
    @ResponseBody
    public Map<String,Object> queryAll(@RequestHeader String token,HttpServletRequest request,@RequestParam  Integer pageNum,@RequestParam  Integer pageSize){
        Map<String,Object> map = new HashMap<String, Object>();
        User user = userService.getUser(token);

        PageHelper.startPage(pageNum,pageSize);
        Page<User> page;
        List<MdmSoftwareDistributeEntity> softwareDistributes;
        //??????????????????
        if(user.getType() == 1){
            softwareDistributes = softwareDistributeDao.querAll();
        }else {
            softwareDistributes = softwareDistributeDao.queryAccountAll(user.getAccount());
        }

        Iterator<MdmSoftwareDistributeEntity> iterator = softwareDistributes.iterator();

        while (iterator.hasNext()){
            MdmSoftwareDistributeEntity next = iterator.next();
            next.setIcon(ServerUtil.getRootUrl(request) + next.getAccount() + "/mdmsoftwareDistribute/" + next.getUuid() + "/" + next.getUuid() + ".png");
            Integer dayCount = infoMapper.selectByUuidCount(next.getUuid(),"day");
            Integer lastDayCount = infoMapper.selectByUuidCount(next.getUuid(),"lastDay");
            Integer sumCount = infoMapper.selectByUuidCount(next.getUuid(),null);
            next.setDayCount(dayCount);
            next.setSumCount(sumCount);
            next.setLastDayCount(lastDayCount);
        }

        page =  (Page)softwareDistributes;
        map.put("code", 0);
        map.put("message", "????????????");
        map.put("data", page.getResult());
        map.put("pages", page.getPages());
        map.put("total", page.getTotal());
        return map;
    }

    /**
     * ????????????
     * @param uuid
     * @param request
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/delete",method = RequestMethod.POST)
    @ResponseBody
    public Map<String,Object> delete(@RequestHeader String token,@RequestParam  String uuid,HttpServletRequest request) throws Exception {
        Map<String,Object> map = new HashMap<String, Object>();
        User user = userService.getUser(token);
        Integer res;
        if(user.getType() == 1){
            res  = softwareDistributeDao.adminDelete(uuid);
        }else {
            res = softwareDistributeDao.delete(user.getAccount(),uuid);
        }
        if(res == 1){
            String userDir = "./sign/temp/" + user.getAccount() + "/mdmsoftwareDistribute/";
            //????????????
            FileUtil.del(new File(userDir + uuid));
            map.put("code", 0);
            map.put("message", "????????????");
        }
        return map;
    }
}


