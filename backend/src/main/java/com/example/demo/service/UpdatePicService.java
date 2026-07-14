package com.example.demo.service;

import org.springframework.web.multipart.MultipartFile;

public interface UpdatePicService {
    /**
     * 上传图片到阿里云 OSS
     *
     * @param file 上传的文件
     * @return 文件访问 URL
     */
    String uploadImage(MultipartFile file);
}
