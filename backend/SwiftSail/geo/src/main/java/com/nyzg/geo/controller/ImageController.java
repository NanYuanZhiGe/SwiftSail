package com.nyzg.geo.controller;

import com.nyzg.common.netobj.BaseResp;
import com.nyzg.common.netobj.HttpResp;
import com.nyzg.geo.netobj.PublicImageReq;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Iterator;

@RestController
@Slf4j
public class ImageController {
    private final static HttpResp LARGE_IMAGE_IS_NOT_ALLOW = new HttpResp(false, "图片太大，不允许上传");
    public final static HttpResp PARSE_IMAGE_FAIL = new HttpResp(false, "无法解析上传的图片");
    public final static HttpResp PROGRESSIVE_PARSE_FAIL = new HttpResp(false, "解析图片为progressive jpeg失败");
    public final static HttpResp SERVER_LACK_STORAGE_SERVICE = new HttpResp(false, "服务端缺失存储服务");

    @Value("${minio.url}")
    String minioUrl;
    @Value("${minio.accessKey}")
    String accessKey;
    @Value("${minio.secretKey}")
    String secretKey;

    @Value("${minio.public-bucket}")
    String publicBucket;

    private final String mTmpFolder;

    public ImageController(
            @Value("${minio.tmp-folder}") String tmpFolder,
            @Value("${minio.tmp-folder-win}") String tmpFolderWin) {
        String osName = System.getProperty("os.name").toLowerCase();
        ;
        if (osName.contains("windows")) {
            mTmpFolder = tmpFolderWin;
        } else {
            mTmpFolder = tmpFolder;
        }
    }


    @PostMapping("/send/small/image/head")
    public HttpResp sendSmallImageHead(
            @RequestHeader("userId") String userId,
            @RequestBody PublicImageReq req) {
        //参数校验
        if (userId == null || userId.isBlank()) {
            return BaseResp.LOGIN_REQUIRED;
        }
        long numUserId = Long.parseLong(userId);
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

        //解析为progressive jpeg
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

        //写入临时文件
        String outputPath = String.format("%s/%d-headIcon-tmp.jpeg", mTmpFolder, numUserId);
        File output = new File(outputPath);
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(ios);
            JPEGImageWriteParam param = new JPEGImageWriteParam(null);
            //使用progressive jpeg
            param.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.9f);
            writer.write(null, new IIOImage(image, null, null), param);
        } catch (IOException e) {
            return PROGRESSIVE_PARSE_FAIL;
        } finally {
            writer.dispose();
        }
        //写入对象存储
        boolean success = true;
        try (MinioClient minioClient = MinioClient.builder()
                .endpoint(minioUrl)
                .credentials(accessKey, secretKey)
                .build()) {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(publicBucket).build());
            if (!found) {
                return SERVER_LACK_STORAGE_SERVICE;
            }
            minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket(publicBucket)
                            .object(String.format("%d-headIcon.jpeg", numUserId))
                            .filename(outputPath)
                            .build());
        } catch (Exception e) {
            log.error("ImageController-sendSmallImageHead: ", e);
            success = false;
        } finally {
            output.delete();
        }
        return success ? HttpResp.COMMON_SUCCESS : SERVER_LACK_STORAGE_SERVICE;
    }
}
