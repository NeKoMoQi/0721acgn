package com.example.demo.service;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface AnimationVideoUploadService {

    String uploadPreviewVideo(MultipartFile file);

    String importPreviewVideo(Path sourcePath);
}
