package com.nyzg.geo.service;

import com.nyzg.common.ss_utils.Tuple;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

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
import java.util.HashMap;
import java.util.Iterator;

@Service
@Slf4j
public class ImageService {
    private final static int HASH_LEN=16;
    @Nonnull
    public String getImageHash(@Nonnull byte[] image, @Nonnull String prefix) {
        MessageDigest sha256;
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            log.error("处理文件哈希出错", e);
            return String.format("%s-0.jpeg", prefix);
        }
        byte[] digest = sha256.digest(image);
        String fileHash = Base64.getUrlEncoder().encodeToString(digest);
        if (fileHash.length() > HASH_LEN) {
            fileHash = fileHash.substring(0, HASH_LEN);
        }
        return String.format("%s-%s.jpeg", prefix, fileHash);
    }

    @Nullable
    public Tuple<ByteArrayInputStream, Integer, String> getProgressiveImageNullAtFail(@NotNull byte[] imageInput, @NotNull String hashPrefix) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageInput));
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            MessageDigest sha256;
            try {
                sha256 = MessageDigest.getInstance("SHA-256");
            } catch (Exception e) {
                log.error("处理文件哈希出错", e);
                return null;
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
                return null;
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
                return null;
            }

            byte[] digest = sha256.digest();
            String minioFileName;
            String fileHash = Base64.getUrlEncoder().encodeToString(digest);
            if (fileHash.length() > HASH_LEN) {
                fileHash = fileHash.substring(0, HASH_LEN);
            }
            minioFileName = String.format("%s-%s.jpeg", hashPrefix, fileHash);
            return new Tuple<>(new ByteArrayInputStream(bos.toByteArray()), bos.size(), minioFileName);
        } catch (Exception e) {
            log.error("ImageService", e);
            return null;
        }
    }
}
