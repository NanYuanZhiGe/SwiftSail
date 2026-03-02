package com.nyzg.geo.controller;

import com.nyzg.common.netobj.BaseResp;
import com.nyzg.common.netobj.HttpResp;
import com.nyzg.common.ss_utils.Tuple;
import com.nyzg.geo.conf.KafkaTopicConf;
import com.nyzg.geo.dbobj.PersonalData;
import com.nyzg.geo.netobj.PersonalInfo;
import com.nyzg.geo.service.ImageService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.messages.DeleteObject;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Semaphore;

@RestController
@Slf4j
public class PersonalInfoController {
    @Resource
    JdbcTemplate jdbcTemplate;

    @Resource
    ImageService imageService;

    @Resource
    PlatformTransactionManager transactionManager;
    @Resource
    MinioClient minioClient;

    @Value("${minio.public-bucket}")
    String publicBucket;
    @Resource
    KafkaTemplate<String, List<DeleteObject>> kafkaTemplate;

    final private Semaphore imageProcessControl = new Semaphore(20);


    @GetMapping("/get/personalInfo/{userId}")
    public HttpResp getPersonalInfo(@PathVariable("userId") String userId) {
        try {
            PersonalData result = jdbcTemplate.query("""
                    SELECT `userId`, `appellation`, gender,\s
                    description, promiseScore, inTimeScore,\s
                    levelScore, cooperationScore, communicateScore,\s
                    bkImage, layoutImage FROM `personalDataTable`\s
                    WHERE `userId`=?
                    """, (rs, num) -> {
                PersonalData personalData = new PersonalData();
                personalData.setUserId(rs.getLong("userId"));
                personalData.setAppellation(rs.getString("appellation"));
                personalData.setGender(rs.getShort("gender"));
                personalData.setDescription(rs.getString("description"));
                return getPersonalData(rs, personalData);
            }, Long.parseLong(userId)).stream().findFirst().orElse(null);
            return new HttpResp(true, result);
        } catch (NumberFormatException e) {
            log.error("getPersonalInfo", e);
            return BaseResp.WRONG_PARAM;
        } catch (Exception e) {
            log.error("getPersonalInfo", e);
            return BaseResp.INTERNAL_ERROR;
        }
    }

    @PostMapping("/update/personalInfo")
    public HttpResp updatePersonalInfo(
            @RequestHeader("userId") String userId,
            @RequestBody PersonalInfo personalInfo) {
        //--------------参数校验--------------------
        long lUserId;
        try {
            lUserId = Long.parseLong(userId);
        } catch (Exception ignore) {
            return BaseResp.LOGIN_REQUIRED;
        }
        if (lUserId == 0L) {
            return BaseResp.LOGIN_REQUIRED;
        }
        if (personalInfo == null) {
            return BaseResp.WRONG_PARAM;
        }
        //性别校准
        if (personalInfo.getGender() < 0 || personalInfo.getGender() > 1) {
            personalInfo.setGender((short) 0);
        }
        //称呼校准
        if (personalInfo.getAppellation() == null) {
            personalInfo.setAppellation(personalInfo.getGender() == 0 ? "先生" : "女士");
        } else if (personalInfo.getAppellation().length() > 32) {
            personalInfo.setAppellation(personalInfo.getAppellation().substring(0, 32));
        }
        //自我介绍校准
        if (personalInfo.getDescription() != null && personalInfo.getDescription().length() > 1000) {
            personalInfo.setDescription(personalInfo.getDescription().substring(0, 1000));
        }
        //所有的图片校准
        final int MAX_IMAGE_SIZE = (int) (1.5 * 1024 * 1024);
        if (personalInfo.getBackground() != null) {
            if (personalInfo.getBackground().length > MAX_IMAGE_SIZE) {
                return BaseResp.LARGE_IMAGE_IS_NOT_ALLOW;
            }
        }
        if (personalInfo.getLayouts() != null) {
            //清除里面的null类，同时限制最大数量为9
            personalInfo.setLayouts(personalInfo.getLayouts().stream().filter(Objects::nonNull)
                    .limit(9).toList());
            for (byte[] img : personalInfo.getLayouts()) {
                if (img.length > MAX_IMAGE_SIZE) {
                    return BaseResp.LARGE_IMAGE_IS_NOT_ALLOW;
                }
            }
        }
        //-------------------实际业务-----------------------
        try {
            //获取信号量，防止请求太多驻留在内存中导致oom
            //拿不到直接返回服务繁忙
            if (!imageProcessControl.tryAcquire(1)) {
                return BaseResp.SERVER_TOO_BUSY;
            }
            //背景图片转progressive jpeg
            final Tuple<ByteArrayInputStream, Integer, String> userBkTuple;
            final String userBkUrl;
            if (personalInfo.getBackground() != null) {
                userBkTuple = imageService.getProgressiveImageNullAtFail(personalInfo.getBackground(), "userBk");
                if (userBkTuple != null) {
                    userBkUrl = userBkTuple.getC();
                } else {
                    userBkUrl = null;
                }
            } else {
                userBkUrl = null;
                userBkTuple = null;
            }
            //展示的图片转progressive jpeg
            //key是hash name
            final Map<String, Tuple<ByteArrayInputStream, Integer, String>> lyImgTupleMap = new HashMap<>(16);
            if (personalInfo.getLayouts() != null) {
                for (byte[] img : personalInfo.getLayouts()) {
                    Tuple<ByteArrayInputStream, Integer, String> temp = imageService.getProgressiveImageNullAtFail(img, "userLy");
                    if (temp != null) {
                        lyImgTupleMap.put(temp.getC(), temp);
                    }
                }
            }
            //写用户数据到数据库
            final List<DeleteObject> listToDelete = new ArrayList<>(9);
            @Nonnull PersonalData result = new TransactionTemplate(transactionManager)
                    .execute(transactionStatus -> {
                        PersonalData personalData = jdbcTemplate.query("""
                                        SELECT `promiseScore`,`inTimeScore`,`levelScore`,`cooperationScore`,`communicateScore`,`bkImage`,`layoutImage` FROM `personalDataTable` WHERE `userId`=?
                                        """, (rs, num) -> {
                                    PersonalData data = new PersonalData();
                                    return getPersonalData(rs, data);
                                }, lUserId)
                                .stream().findFirst().orElseGet(() -> {
                                    PersonalData data = new PersonalData();
                                    data.setUserId(lUserId);
                                    data.setAppellation(personalInfo.getAppellation());
                                    data.setGender(personalInfo.getGender());
                                    data.setDescription(personalInfo.getDescription());
                                    data.setPromiseScore((short) 5);
                                    data.setInTimeScore((short) 5);
                                    data.setLevelScore((short) 5);
                                    data.setCooperationScore((short) 5);
                                    data.setCommunicateScore((short) 5);
                                    return data;
                                });
                        /*
                        此时我们知道了旧的图片，我们要做这些事情：
                        根据旧图片和新图片，把结果分为两部分，一个是拿出删除的
                        一个是拿去添加的，
                        其中拿去添加的保留着lyImageTupleMap里面
                         */
                        //保证有数据
                        final StringBuilder userLyUrlStringBuilder = new StringBuilder();
                        final String layoutImageStr;
                        if (personalData.getLayoutImage() != null && !personalData.getLayoutImage().isEmpty()) {
                            List<String> deleteList = Arrays.stream(personalData.getLayoutImage().split("\\|"))
                                    //不要已经有的
                                    .filter(name -> {
                                        boolean res = lyImgTupleMap.containsKey(name);
                                        if (res) {//对象存储中已经有了
                                            //就不要额外添加了
                                            lyImgTupleMap.remove(name);
                                        }
                                        return true;
                                    })
                                    //到这里的时候，已经是去重之后的结果了
                                    // 【   【 】    】-----》 【   m   】【 n 】
                                    .toList();
                            //控制用户的总图片数量为9个
                            //只删除多出来的
                            if (deleteList.size() + lyImgTupleMap.size() > 9) {
                                int endIndex = (deleteList.size() + lyImgTupleMap.size()) - 9;
                                //∵ lyImgTupleMap.size()<=9
                                //∴ deleteList.size() + lyImgTupleMap.size() > 9时
                                //假设endIndex = (deleteList.size() + lyImgTupleMap.size()) - 9>=deleteList.size()
                                //变化得到(deleteList.size() + lyImgTupleMap.size()) >=deleteList.size()+9
                                //而由于lyImgTupleMap.size()<=9
                                //所以endIndex = (deleteList.size() + lyImgTupleMap.size())-9 <=deleteList.size()
                                //所以下面的这个for循环是安全的
                                for (int i = 0; i < endIndex; ++i) {
                                    listToDelete.add(new DeleteObject(deleteList.get(i)));
                                }
                                for (int i = endIndex; i < deleteList.size(); ++i) {
                                    userLyUrlStringBuilder.append(deleteList.get(i)).append("|");
                                }
                            } else {//总元素个数小于等于9
                                for (String s : deleteList) {
                                    userLyUrlStringBuilder.append(s).append("|");
                                }
                            }
                            for (var v : lyImgTupleMap.entrySet()) {
                                userLyUrlStringBuilder.append(v.getKey()).append("|");
                            }
                            if (userLyUrlStringBuilder.length() > 1) {
                                userLyUrlStringBuilder.deleteCharAt(userLyUrlStringBuilder.length() - 1);
                            }
                            layoutImageStr = userLyUrlStringBuilder.toString();
                        } else {
                            layoutImageStr = null;
                        }
                        //更新数据
                        personalData.setLayoutImage(layoutImageStr);
                        jdbcTemplate.update("""
                                        INSERT INTO `personalDataTable`\s
                                        (`userId`, `appellation`, `gender`, `description`, `bkImage`, `layoutImage`)
                                        VALUES (?, ?, ?, ?, ?, ?)
                                        ON DUPLICATE KEY UPDATE
                                            `appellation` = VALUES(`appellation`),
                                            `gender` = VALUES(`gender`),
                                            `description` = VALUES(`description`),
                                            `bkImage` = VALUES(`bkImage`),
                                            `layoutImage` = VALUES(`layoutImage`)
                                        """,
                                lUserId,
                                personalInfo.getAppellation(),
                                personalInfo.getGender(),
                                personalInfo.getDescription(),
                                userBkUrl,
                                layoutImageStr
                        );
                        return personalData;
                    });
            //----------------更新对象存储----------------------------
            //新的背景图和和旧的不一样才触发更新
            if (userBkUrl != null && !userBkUrl.equals(result.getBkImage())) {
                if (result.getBkImage() != null) {//如果旧的图片不为空，旧和三处
                    kafkaTemplate.send(KafkaTopicConf.TOPIC_IMAGE_DELETE, userId, List.of(new DeleteObject(result.getBkImage())))
                            .whenComplete((res, ex) -> {
                                if (ex != null) {
                                    log.error("删除旧的背景图片出错", ex);
                                }
                            });
                }
                //添加新的背景图
                try {
                    minioClient.putObject(PutObjectArgs.builder()
                            .bucket(publicBucket)
                            .stream(userBkTuple.getA(), userBkTuple.getB(), -1)
                            .object(userBkTuple.getC())
                            .build());
                } catch (Exception e) {
                    log.error("添加新的背景图片出错", e);
                }
            }
            //结果集里面是旧的，现在更新为新的
            result.setBkImage(userBkUrl);
            //处理展览图
            //需要删除的数据
            //异步删除
            if (!listToDelete.isEmpty()) {
                kafkaTemplate.send(KafkaTopicConf.TOPIC_IMAGE_DELETE, userId, listToDelete)
                        .whenCompleteAsync((res, ex) -> {
                            if (ex != null) {
                                log.error("kafka异步删除出错", ex);
                            }
                        });
            }
            //需要添加的数据
            for (var v : lyImgTupleMap.entrySet()) {
                try {
                    minioClient.putObject(PutObjectArgs.builder()
                            .bucket(publicBucket)
                            .stream(v.getValue().getA(), v.getValue().getB(), -1)
                            .object(v.getKey())
                            .build());
                } catch (Exception e) {
                    log.error("添加新的展示图片出错", e);
                }
            }

            imageProcessControl.release();
            return new HttpResp(true, result);
        } catch (Exception e) {
            imageProcessControl.release();
            log.error("updatePersonalInfo-Probably database transaction exception", e);
            return BaseResp.INTERNAL_ERROR;
        }
    }

    @NotNull
    private PersonalData getPersonalData(ResultSet rs, PersonalData data) throws SQLException {
        data.setPromiseScore(rs.getShort("promiseScore"));
        data.setInTimeScore(rs.getShort("inTimeScore"));
        data.setLevelScore(rs.getShort("levelScore"));
        data.setCooperationScore(rs.getShort("cooperationScore"));
        data.setCommunicateScore(rs.getShort("communicateScore"));
        data.setBkImage(rs.getString("bkImage"));
        data.setLayoutImage(rs.getString("layoutImage"));
        return data;
    }
}
