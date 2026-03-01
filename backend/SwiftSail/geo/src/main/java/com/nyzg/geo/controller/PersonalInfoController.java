package com.nyzg.geo.controller;

import com.nyzg.common.netobj.BaseResp;
import com.nyzg.common.netobj.HttpResp;
import com.nyzg.common.ss_utils.Tuple;
import com.nyzg.geo.dbobj.PersonalData;
import com.nyzg.geo.netobj.PersonalInfo;
import com.nyzg.geo.service.ImageService;
import io.minio.*;
import io.minio.messages.DeleteObject;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
                personalData.setPromiseScore(rs.getShort("promiseScore"));
                personalData.setInTimeScore(rs.getShort("inTimeScore"));
                personalData.setLevelScore(rs.getShort("levelScore"));
                personalData.setCooperationScore(rs.getShort("cooperationScore"));
                personalData.setCommunicateScore(rs.getShort("communicationScore"));
                personalData.setBkImage(rs.getString("bkImage"));
                personalData.setLayoutImage(rs.getString("layoutImage"));
                return personalData;
            }, Long.parseLong(userId)).stream().findFirst().orElse(null);
            return new HttpResp(true, result);
        } catch (NumberFormatException e) {
            return BaseResp.WRONG_PARAM;
        } catch (Exception e) {
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
            for (byte[] img : personalInfo.getLayouts()) {
                if (img.length > MAX_IMAGE_SIZE) {
                    return BaseResp.LARGE_IMAGE_IS_NOT_ALLOW;
                }
            }
        }
        //-------------------实际业务-----------------------
        try {
            if (!imageProcessControl.tryAcquire(1, 3, TimeUnit.SECONDS)) {
                return BaseResp.SERVER_TOO_BUSY;
            }
            //背景图片校准
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
            //展览图片校准
            final Map<String, Tuple<ByteArrayInputStream, Integer, String>> lyImgTupleMap;
            final Set<String> lyImageUrlSet = new HashSet<>();
            final StringBuilder userLyUrlStringBuilder = new StringBuilder();
            if (personalInfo.getLayouts() != null) {
                lyImgTupleMap = new HashMap<>();
                for (byte[] img : personalInfo.getLayouts()) {
                    Tuple<ByteArrayInputStream, Integer, String> temp = imageService.getProgressiveImageNullAtFail(img, "userLy");
                    if (temp != null) {
                        lyImgTupleMap.put(temp.getC(), temp);
                        lyImageUrlSet.add(temp.getC());
                        userLyUrlStringBuilder.append(temp.getC()).append("|");
                    }
                }
                if (userLyUrlStringBuilder.length() > 1) {
                    userLyUrlStringBuilder.deleteCharAt(userLyUrlStringBuilder.length() - 1);
                }
            } else {
                lyImgTupleMap = null;
            }
            final String userLyUrlStr = userLyUrlStringBuilder.toString();
            //写用户数据到数据库
            @Nonnull PersonalData result = new TransactionTemplate(transactionManager)
                    .execute(transactionStatus -> {
                        PersonalData personalData = jdbcTemplate.query("""
                                        SELECT `promiseScore`,`inTimeScore`,`levelScore`,`cooperationScore`,`communicateScore`,`bkImage`,`layoutImage` FROM `personalDataTable` WHERE `userId`=?
                                        """, (rs, num) -> {
                                    PersonalData data = new PersonalData();
                                    data.setPromiseScore(rs.getShort("promiseScore"));
                                    data.setInTimeScore(rs.getShort("inTimeScore"));
                                    data.setInTimeScore(rs.getShort("levelScore"));
                                    data.setInTimeScore(rs.getShort("cooperationScore"));
                                    data.setInTimeScore(rs.getShort("communicateScore"));
                                    data.setBkImage(rs.getString("bkImage"));
                                    data.setLayoutImage(rs.getString("layoutImage"));
                                    return data;
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
                                userLyUrlStr
                        );
                        return personalData;
                    });
            //更新对象存储
            //删除旧的图片，添加新的
            //新的背景图和和旧的不一样才触发更新
            if (userBkUrl != null && !userBkUrl.equals(result.getBkImage())) {
                if (result.getBkImage() != null) {//如果旧的图片不为空，旧和三处
                    try {
                        minioClient.removeObject(RemoveObjectArgs.builder()
                                .bucket(publicBucket)
                                .object(result.getBkImage())
                                .build());
                    } catch (Exception e) {
                        log.error("删除旧的背景图片出错", e);
                    }
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
            //处理展览图
            if (result.getLayoutImage() != null) {
                if (lyImgTupleMap != null) {//有图片
                    //删除对象旧的、和新的图片不重复的
                    Set<DeleteObject> list = Arrays.stream(result.getLayoutImage().split("\\|"))
                            .filter(str -> {
                                boolean res = lyImageUrlSet.contains(str);
                                if (res) {//删除重复的图片
                                    lyImgTupleMap.remove(str);
                                }
                                return res;
                            })
                            .map(DeleteObject::new)
                            .collect(Collectors.toSet());
                    try {
                        minioClient.removeObjects(RemoveObjectsArgs.builder()
                                .bucket(publicBucket)
                                .objects(list)
                                .build());
                    } catch (Exception e) {
                        log.error("删除旧的展示图片出错", e);
                    }
                }
            }
            //添加新的图片
            if (lyImgTupleMap != null) {
                try {
                    for (var v : lyImgTupleMap.entrySet()) {
                        minioClient.putObject(PutObjectArgs.builder()
                                .bucket(publicBucket)
                                .stream(v.getValue().getA(), v.getValue().getB(), -1)
                                .object(v.getKey())
                                .build());
                    }
                } catch (Exception e) {
                    log.error("添加新的展示图片出错", e);
                }
            }
            //更新为新的url
            result.setBkImage(userBkUrl);
            result.setLayoutImage(userLyUrlStr);
            imageProcessControl.release();
            return new HttpResp(true, result);
        } catch (Exception e) {
            imageProcessControl.release();
            log.error("updatePersonalInfo-", e);
            return BaseResp.INTERNAL_ERROR;
        }
    }
}
