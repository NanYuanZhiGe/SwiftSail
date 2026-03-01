package com.nyzg.geo.controller;

import com.nyzg.common.Pair;
import com.nyzg.common.netobj.BaseResp;
import com.nyzg.common.netobj.HttpResp;
import com.nyzg.common.ss_utils.Tuple;
import com.nyzg.geo.conf.KafkaTopicConf;
import com.nyzg.geo.netobj.PublicImageReq;
import com.nyzg.geo.netobj.PublicImageResp;
import com.nyzg.geo.service.ImageService;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Iterator;

@RestController
@Slf4j
public class ImageController {
    public final static HttpResp PARSE_IMAGE_FAIL = new HttpResp(false, "无法解析上传的图片");
    public final static HttpResp PROGRESSIVE_PARSE_FAIL = new HttpResp(false, "解析图片为progressive jpeg失败");
    public final static HttpResp SERVER_LACK_STORAGE_SERVICE = new HttpResp(false, "服务端缺失存储服务");

    private final String mPublicBucket;
    private final MinioClient minioClient;
    private final KafkaTemplate<String, String> mKafkaTemplate;
    private final ImageService mImageService;

    public ImageController(
            ImageService imageService,
            KafkaTemplate<String, String> kafkaTemplate,
            MinioClient minioClient,
            @Value("${minio.public-bucket}")
            String publicBucket) {
        mImageService = imageService;
        mKafkaTemplate = kafkaTemplate;
        this.minioClient = minioClient;
        mPublicBucket = publicBucket;
    }

    @PostConstruct
    public void postConstruct() {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(mPublicBucket).build());
            if (!found) {
                throw new RuntimeException("No Public Bucket Available");
            }
        } catch (Exception e) {
            log.error("Check Minio Bucket Error", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 返回的content里面是PublicImageResp，里面只有一个string
     */
    @PostMapping("/send/small/image/head")
    public HttpResp sendSmallImageHead(
            @RequestHeader("userId") String userId,
            @RequestBody PublicImageReq req) {
        //参数校验
        if (userId == null || userId.isBlank()) {
            return BaseResp.LOGIN_REQUIRED;
        }
        if (req == null) {
            return HttpResp.COMMON_SUCCESS;
        }
        //头像不得大于128KB
        if (req.getData().length > 128 * 1024) {
            return BaseResp.LARGE_IMAGE_IS_NOT_ALLOW;
        }
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(req.getData()));
            if (image == null) {
                return PARSE_IMAGE_FAIL;
            }
        } catch (Exception e) {
            return PARSE_IMAGE_FAIL;
        }

        Tuple<ByteArrayInputStream, Integer, String> userIconTuple = mImageService.getProgressiveImageNullAtFail(req.getData(), "userIcon");
        if (userIconTuple == null) {
            return PROGRESSIVE_PARSE_FAIL;
        }
        //写入对象存储
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(mPublicBucket)
                            .object(userIconTuple.getC())
                            .stream(userIconTuple.getA(), userIconTuple.getB(), -1)
                            .build());
            mKafkaTemplate.send(KafkaTopicConf.TOPIC_IMAGE_PROCESS, userId, String.format("%s|%s", userId, userIconTuple.getC()));
        } catch (Exception e) {
            log.error("ImageController-sendSmallImageHead: ", e);
            return SERVER_LACK_STORAGE_SERVICE;
        }
        return new HttpResp(true, new PublicImageResp(userIconTuple.getC()));
    }
}
