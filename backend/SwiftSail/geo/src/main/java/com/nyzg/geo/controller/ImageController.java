package com.nyzg.geo.controller;

import com.nyzg.common.netobj.BaseResp;
import com.nyzg.common.netobj.HttpResp;
import com.nyzg.geo.conf.KafkaTopicConf;
import com.nyzg.geo.netobj.PublicImageReq;
import com.nyzg.geo.netobj.PublicImageResp;
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
    private final static HttpResp LARGE_IMAGE_IS_NOT_ALLOW = new HttpResp(false, "图片太大，不允许上传");
    public final static HttpResp PARSE_IMAGE_FAIL = new HttpResp(false, "无法解析上传的图片");
    public final static HttpResp PROGRESSIVE_PARSE_FAIL = new HttpResp(false, "解析图片为progressive jpeg失败");
    public final static HttpResp SERVER_LACK_STORAGE_SERVICE = new HttpResp(false, "服务端缺失存储服务");

    private final String mPublicBucket;
    private final MinioClient minioClient;
    private final KafkaTemplate<String, String> mKafkaTemplate;

    public ImageController(
            KafkaTemplate<String, String> kafkaTemplate,
            MinioClient minioClient,
            @Value("${minio.public-bucket}")
            String publicBucket) {
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
            return LARGE_IMAGE_IS_NOT_ALLOW;
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

        //获取progressive jpeg解析器
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        ImageWriter writer = null;
        while (writers.hasNext()) {
            ImageWriter w = writers.next();
            //TwelveMonkeys 的写入器，支持progressive jpeg
            if (w.getClass().getName().contains("twelvemonkeys")) {
                writer = w;
                break;
            }
            if (writer == null) {
                writer = w;
            }
        }
        if (writer == null) {
            return PROGRESSIVE_PARSE_FAIL;
        }

        //解析为progressive jpeg
        //写入临时文件
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        MessageDigest sha256;
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            log.error("处理文件哈希出错", e);
            return BaseResp.INTERNAL_ERROR;
        }
        try (OutputStream hashStream = new OutputStream() {
            @Override
            public void write(int b) {
                bos.write(b);
                sha256.update((byte) b);
            }

            @Override
            public void write(@NotNull byte[] b, int off, int len) {
                bos.write(b, off, len);
                sha256.update(b, off, len);
            }

            @Override
            public void write(@NotNull byte[] b) throws IOException {
                bos.write(b);
                sha256.update(b);
            }
        }) {
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(hashStream)) {
                writer.setOutput(ios);
                JPEGImageWriteParam param = new JPEGImageWriteParam(null);
                param.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(0.9f); // 质量可调
                writer.write(null, new IIOImage(image, null, null), param);
            }
            writer.dispose();
        } catch (Exception e) {
            writer.dispose();
            return PROGRESSIVE_PARSE_FAIL;
        }

        byte[] digest = sha256.digest();
        String minioFileName;
        String fileHash = Base64.getUrlEncoder().encodeToString(digest);
        if (fileHash.length() > 16) {
            fileHash = fileHash.substring(0, 16);
        }
        minioFileName = String.format("userIcon-%s.jpeg", fileHash);
        //写入对象存储
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(mPublicBucket)
                            .object(minioFileName)
                            .stream(new ByteArrayInputStream(bos.toByteArray()), bos.size(), -1)
                            .build());
            mKafkaTemplate.send(KafkaTopicConf.TOPIC_IMAGE_PROCESS, userId,String.format("%s|%s", userId, minioFileName));
        } catch (Exception e) {
            log.error("ImageController-sendSmallImageHead: ", e);
            return SERVER_LACK_STORAGE_SERVICE;
        }
        return new HttpResp(true, new PublicImageResp(minioFileName));
    }
}
