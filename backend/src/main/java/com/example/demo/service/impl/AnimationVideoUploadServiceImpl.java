package com.example.demo.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.example.demo.config.OssConfig;
import com.example.demo.service.AnimationVideoUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Slf4j
@Service
public class AnimationVideoUploadServiceImpl implements AnimationVideoUploadService {

    private static final long MAX_BYTES = 1024L * 1024 * 1024;

    private final OSS ossClient;
    private final OssConfig ossConfig;

    public AnimationVideoUploadServiceImpl(OSS ossClient, OssConfig ossConfig) {
        this.ossClient = ossClient;
        this.ossConfig = ossConfig;
    }

    @Override
    public String uploadPreviewVideo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("文件名无效");
        }
        String ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        validateVideo(ext, file.getSize());

        String objectKey = buildObjectKey(ext);
        ObjectMetadata metadata = buildMetadata(ext, file.getSize());
        try (InputStream in = file.getInputStream()) {
            return putObject(objectKey, in, metadata);
        } catch (IOException e) {
            log.error("preview video upload to OSS failed", e);
            throw new RuntimeException("上传失败", e);
        }
    }

    @Override
    public String importPreviewVideo(Path sourcePath) {
        if (sourcePath == null) {
            throw new IllegalArgumentException("sourcePath 不能为空");
        }
        Path source = sourcePath.toAbsolutePath().normalize();
        if (!Files.isRegularFile(source)) {
            throw new IllegalArgumentException("视频文件不存在: " + source);
        }
        String filename = source.getFileName().toString();
        if (!filename.contains(".")) {
            throw new IllegalArgumentException("文件名无效");
        }
        String ext = filename.substring(filename.lastIndexOf(".")).toLowerCase();
        try {
            long size = Files.size(source);
            validateVideo(ext, size);
            String objectKey = buildObjectKey(ext);
            ObjectMetadata metadata = buildMetadata(ext, size);
            try (InputStream in = Files.newInputStream(source)) {
                return putObject(objectKey, in, metadata);
            }
        } catch (IOException e) {
            log.error("preview video import to OSS failed", e);
            throw new RuntimeException("导入失败", e);
        }
    }

    private String putObject(String objectKey, InputStream in, ObjectMetadata metadata) {
        PutObjectRequest request = new PutObjectRequest(
                ossConfig.getBucketName(),
                objectKey,
                in,
                metadata
        );
        ossClient.putObject(request);
        ossClient.setObjectAcl(ossConfig.getBucketName(), objectKey, CannedAccessControlList.PublicRead);
        String url = "https://" + ossConfig.getBucketName() + "." + ossConfig.getEndpoint() + "/" + objectKey;
        log.info("preview video uploaded to OSS: {}", url);
        return url;
    }

    private String buildObjectKey(String ext) {
        String prefix = ossConfig.getFolderPrefix();
        if (prefix == null) {
            prefix = "";
        }
        if (!prefix.isBlank() && !prefix.endsWith("/")) {
            prefix += "/";
        }
        return prefix + "animation-video/" + UUID.randomUUID() + ext;
    }

    private static ObjectMetadata buildMetadata(String ext, long size) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(size);
        metadata.setContentType(contentType(ext));
        return metadata;
    }

    private static void validateVideo(String extWithDot, long size) {
        if (!isAllowedVideo(extWithDot)) {
            throw new IllegalArgumentException("仅支持 mp4 / webm / mov / mkv");
        }
        if (size > MAX_BYTES) {
            throw new IllegalArgumentException("视频不能超过 1024MB");
        }
    }

    private static String contentType(String extWithDot) {
        return switch (extWithDot) {
            case ".webm" -> "video/webm";
            case ".mov" -> "video/quicktime";
            case ".mkv" -> "video/x-matroska";
            default -> "video/mp4";
        };
    }

    private static boolean isAllowedVideo(String extWithDot) {
        return ".mp4".equals(extWithDot)
                || ".webm".equals(extWithDot)
                || ".mov".equals(extWithDot)
                || ".mkv".equals(extWithDot);
    }
}
