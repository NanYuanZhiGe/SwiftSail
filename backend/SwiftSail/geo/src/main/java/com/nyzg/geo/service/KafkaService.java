package com.nyzg.geo.service;

import com.nyzg.geo.conf.KryoSerializer;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.RemoveObjectsArgs;
import io.minio.messages.DeleteObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


@Service
@Slf4j
public class KafkaService {

    private final String mPublicBucket;
    final private MinioClient mMinioClient;

    final private JdbcTemplate mJdbcTemplate;
    final private KryoSerializer mKryoSerializer;

    public KafkaService(
            KryoSerializer kryoSerializer,
            JdbcTemplate jdbcTemplate,
            MinioClient minioClient,
            @Value("${minio.public-bucket}")
            String publicBucket) {
        mKryoSerializer = kryoSerializer;
        mJdbcTemplate = jdbcTemplate;
        mMinioClient = minioClient;
        mPublicBucket = publicBucket;
    }

    @SuppressWarnings("unchecked")
    @KafkaListener(topics = "ImageAdd", groupId = "imageGroup", containerFactory = "container3")
    public void addImageList(byte[] message, Acknowledgment ack) {
        HashMap<String, byte[]> content = (HashMap<String, byte[]>) mKryoSerializer.deserialize(message, HashMap.class);
        for (var v : content.entrySet()) {
            try {
                mMinioClient.putObject(PutObjectArgs.builder()
                        .bucket(mPublicBucket)
                        .object(v.getKey())
                        .stream(new ByteArrayInputStream(v.getValue()), v.getValue().length, -1)
                        .build());
            } catch (Exception e) {
                log.error("异步添加图片失败", e);
            }
        }
        ack.acknowledge();
    }

    @SuppressWarnings("unchecked")
    @KafkaListener(topics = "ImageDelete", groupId = "imageGroup", containerFactory = "container2")
    public void deleteImageList(byte[] message, Acknowledgment ack) {
        List<String> content = (List<String>) mKryoSerializer.deserialize(message, ArrayList.class);
        try {
            mMinioClient.removeObjects(RemoveObjectsArgs.builder()
                    .bucket(mPublicBucket)
                    .objects(content.stream().map(DeleteObject::new).toList())
                    .build());
        } catch (Exception e) {
            log.error("异步删除展览图片失败", e);
        }
        ack.acknowledge();
    }

    @KafkaListener(topics = "ImageProcess", groupId = "imageGroup", concurrency = "2")
    public void deleteOldHeadIcon(String message, Acknowledgment ack) {
        try {
            //解析参数
            String[] param = message.split("\\|");
            if (param.length != 2) {
                ack.acknowledge();
                return;
            }
            long userId;
            String newFileName;
            try {
                userId = Long.parseLong(param[0]);
                newFileName = param[1];
            } catch (Exception ignore) {
                ack.acknowledge();
                return;
            }
            //更新数据库
            String key = "headIconUrl";
            String oldFileName = mJdbcTemplate.query("""
                            SELECT `v` FROM `shareTable` WHERE `userId`=? AND `k`=?;
                            """, (rs, rowNum) -> rs.getString("v"), userId, key)
                    .stream()
                    .findFirst()
                    .orElse(null);
            if (oldFileName == null) {
                mJdbcTemplate.update("""
                        INSERT IGNORE INTO `shareTable` (userId, k, v) VALUES (?,?,?);
                        """, userId, key, newFileName);
            } else if (!oldFileName.equals(newFileName)) {
                //防止用户上传两张同样的图片导致的意外删除图像资源
                mJdbcTemplate.update("""
                        UPDATE `shareTable` SET `v`=? WHERE `userId`=? AND `k`=?;
                        """, newFileName, userId, key);
                mMinioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(mPublicBucket)
                                .object(oldFileName)
                                .build()
                );
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Kafka消费image icon错误", e);
        }
    }
}
