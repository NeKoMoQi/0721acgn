package com.example.demo.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.PutObjectRequest;
import com.example.demo.config.OssConfig;
import com.example.demo.service.UpdatePicService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
public class UpdatePicServiceImpl implements UpdatePicService {

    private final OSS ossClient;
    private final OssConfig ossConfig;

    @Autowired
    public UpdatePicServiceImpl(OSS ossClient, OssConfig ossConfig) {
        this.ossClient = ossClient;
        this.ossConfig = ossConfig;
    }

    @Override
    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        // 验证文件类型
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !isValidImageType(originalFilename)) {
            throw new IllegalArgumentException("不支持的文件类型");
        }

        // 生成唯一文件名
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = ossConfig.getFolderPrefix() + UUID.randomUUID();
        log.info(fileName);
        log.info(fileExtension);
        try {
            // 上传文件到OSS
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    ossConfig.getBucketName(),
                    fileName,
                    file.getInputStream()
            );
            ossClient.putObject(putObjectRequest);

            // 生成文件访问URL
            String fileUrl = "https://" + ossConfig.getBucketName() + "." + ossConfig.getEndpoint() + "/" + fileName;
            log.info("文件上传成功: {}", fileUrl);
            return fileUrl;
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    /**
     * 验证文件类型是否为图片
     */
    private boolean isValidImageType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        return extension.equals("jpg") ||
                extension.equals("jpeg") ||
                extension.equals("png") ||
                extension.equals("gif");
    }
}