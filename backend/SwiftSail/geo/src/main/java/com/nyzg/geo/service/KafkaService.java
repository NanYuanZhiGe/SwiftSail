package com.nyzg.geo.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.RemoveObjectsArgs;
import io.minio.messages.DeleteObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@Slf4j
public class KafkaService {

    private final String mPublicBucket;
    final private MinioClient mMinioClient;

    final private JdbcTemplate mJdbcTemplate;

    public KafkaService(
            JdbcTemplate jdbcTemplate,
            MinioClient minioClient,
            @Value("${minio.public-bucket}")
            String publicBucket) {
        mJdbcTemplate = jdbcTemplate;
        mMinioClient = minioClient;
        mPublicBucket = publicBucket;
    }

    @KafkaListener(topics = "ImageDelete", groupId = "imageGroup", concurrency = "1")
    public void deleteImageList(List<DeleteObject> message, Acknowledgment ack) {
        try {
            mMinioClient.removeObjects(RemoveObjectsArgs.builder()
                    .bucket(mPublicBucket)
                    .objects(message)
                    .build());
        } catch (Exception e) {
            log.error("异步删除展览图片失败", e);
        }
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
