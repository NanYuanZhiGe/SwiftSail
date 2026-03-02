package com.nyzg.geo.controller;

import com.nyzg.common.Pair;
import com.nyzg.common.netobj.BaseResp;
import com.nyzg.common.netobj.HttpResp;
import com.nyzg.geo.conf.KafkaTopicConf;
import com.nyzg.geo.conf.KryoSerializer;
import com.nyzg.geo.dbobj.PersonalData;
import com.nyzg.geo.netobj.PersonalInfo;
import com.nyzg.geo.service.ImageService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;

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

    @Value("${minio.public-bucket}")
    String publicBucket;
    @Resource
    @Qualifier("ListDeleteObject")
    KafkaTemplate<String, byte[]> kafkaTemplateDelete;

    @Resource
    @Qualifier("MapAddObject")
    KafkaTemplate<String, byte[]> kafkaTemplateAdd;

    @Resource
    KryoSerializer kryoSerializer;

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
        HttpResp resp = paramValidateNullAtSucceed(userId, personalInfo);
        if (resp != null) {
            return resp;
        }
        //-------------------实际业务-----------------------
        try {
            //获取信号量，防止请求太多驻留在内存中导致oom
            if (!imageProcessControl.tryAcquire(1)) {
                //拿不到直接返回服务繁忙
                return BaseResp.SERVER_TOO_BUSY;
            }
            //---------获取最新的数据库中的数据，同步更新数据库---------
            String bkHash = null;
            final String prefix = "userLy";
            if (personalInfo.getBackground() != null) {
                bkHash = imageService.getImageHash(personalInfo.getBackground(), prefix);
            }
            HashMap<String, byte[]> layoutImageHash = new HashMap<>();
            if (personalInfo.getLayouts() != null) {
                for (var v : personalInfo.getLayouts()) {
                    layoutImageHash.put(imageService.getImageHash(v, prefix), v);
                }
            }
            Pair<PersonalData, List<String>> resultPair = getNewestPersonalDataNullAtFail(Long.parseLong(userId), personalInfo, bkHash, layoutImageHash);
            if (resultPair == null) {//失败
                imageProcessControl.release();
                return BaseResp.INTERNAL_ERROR;
            }
            //----------异步删除元素-----------
            if (!resultPair.getB().isEmpty()) {
                kafkaTemplateDelete.send(KafkaTopicConf.TOPIC_IMAGE_DELETE, userId, kryoSerializer.serialize(resultPair.getB()))
                        .whenComplete((rs, ex) -> {
                            if (ex != null) {
                                log.error("kafka删除元素有问题", ex);
                            }
                        });
            }
            //--------------异步添加元素-----------
            if (!layoutImageHash.isEmpty()) {
                kafkaTemplateAdd.send(KafkaTopicConf.TOPIC_IMAGE_ADD, userId, kryoSerializer.serialize(layoutImageHash))
                        .whenComplete((rs, ex) -> {
                            if (ex != null) {
                                log.error("kafka添加元素有问题", ex);
                            }
                        });
            }
            imageProcessControl.release();
            return new HttpResp(true, resultPair.getA());
        } catch (Exception e) {
            imageProcessControl.release();
            log.error("updatePersonalInfo-Probably database transaction exception", e);
            return BaseResp.INTERNAL_ERROR;
        }
    }

    @Nullable
    private Pair<PersonalData, List<String>> getNewestPersonalDataNullAtFail(
            long userId,
            @Nonnull PersonalInfo personalInfo,
            @Nullable String bkHash,
            @Nonnull HashMap<String, byte[]> layoutImageHash) {
        try {
            return new TransactionTemplate(transactionManager)
                    .execute(transactionStatus -> {
                        //获取旧的数据，没有就返回一个默认值
                        PersonalData personalData = jdbcTemplate.query("""
                                        SELECT `promiseScore`,`inTimeScore`,`levelScore`,`cooperationScore`,`communicateScore`,`bkImage`,`layoutImage` FROM `personalDataTable` WHERE `userId`=?
                                        """, (rs, num) -> {
                                    PersonalData data = new PersonalData();
                                    return getPersonalData(rs, data);
                                }, userId)
                                .stream().findFirst().orElseGet(() -> {
                                    PersonalData data = new PersonalData();
                                    data.setUserId(userId);
                                    data.setAppellation(personalInfo.getAppellation());
                                    data.setGender(personalInfo.getGender());
                                    data.setDescription(personalInfo.getDescription());
                                    data.setPromiseScore((short) 10);
                                    data.setInTimeScore((short) 10);
                                    data.setLevelScore((short) 10);
                                    data.setCooperationScore((short) 10);
                                    data.setCommunicateScore((short) 10);
                                    return data;
                                });
                        //去重，去除多余的数据
                        List<String> q = new LinkedList<>();
                        if (personalData.getLayoutImage() != null) {
                            q.addAll(Arrays.stream(personalData.getLayoutImage().split("\\|"))
                                    .filter(name -> {
                                        boolean res = layoutImageHash.containsKey(name);
                                        if (res) {
                                            layoutImageHash.remove(name);
                                        }
                                        return true;
                                    }).toList());
                        }
                        //限制图片最多为9个，多余就删除
                        final List<String> deleteObjects = new ArrayList<>(10);
                        for (var v : layoutImageHash.keySet()) {
                            if (q.size() >= 9) {
                                deleteObjects.add(q.removeFirst());
                            }
                            q.add(v);
                        }
                        final StringBuilder layoutImageBuilder = new StringBuilder();
                        for (var v : q) {
                            layoutImageBuilder.append(v).append("|");
                        }
                        if (layoutImageBuilder.length() > 1) {
                            layoutImageBuilder.deleteCharAt(layoutImageBuilder.length() - 1);
                        }
                        //如果旧图片不为空，然后新图片和旧图片不一样，就就也要删除
                        if (personalInfo.getBackground() != null && personalData.getBkImage() == null) {
                            //需要添加到背景中
                            layoutImageHash.put(bkHash, personalInfo.getBackground());
                        } else if (personalInfo.getBackground() != null && !personalData.getBkImage().equals(bkHash)) {
                            //需要删除旧的
                            deleteObjects.add(personalData.getBkImage());
                            //需要添加到背景中
                            layoutImageHash.put(bkHash, personalInfo.getBackground());
                        }
                        //更新数据
                        personalData.setBkImage(bkHash);
                        personalData.setLayoutImage(layoutImageBuilder.toString());
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
                                userId,
                                personalInfo.getAppellation(),
                                personalInfo.getGender(),
                                personalInfo.getDescription(),
                                bkHash,
                                personalData.getLayoutImage()
                        );
                        return new Pair<>(personalData, deleteObjects);
                    });
        } catch (Exception e) {
            log.error("updatePersonalInfo-Probably database transaction exception", e);
            return null;
        }
    }

    @Nullable
    private HttpResp paramValidateNullAtSucceed(
            String userId,
            PersonalInfo personalInfo) {
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
        return null;
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
