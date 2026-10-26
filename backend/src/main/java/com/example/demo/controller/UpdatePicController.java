package com.example.demo.controller;

import com.example.demo.service.UpdatePicService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
public class UpdatePicController {
    private final UpdatePicService updatePicService;
    @Autowired
    public UpdatePicController(UpdatePicService updatePicService) {
        this.updatePicService = updatePicService;
    }
    @PostMapping("/api/updatepicbyoss")
    public ResponseEntity<?> updatePicByOss(@RequestParam("file") MultipartFile file) {

        try {
            log.info("收到文件上传请求: {} ({} bytes)",
                    file.getOriginalFilename(), file.getSize());
            String url = updatePicService.uploadImage(file);
            Map<String, Object> map = new HashMap<>();
            map.put("code", 200);
            map.put("message", "success");
            map.put("url", url);
            return ResponseEntity.ok(map);
        } catch (IllegalArgumentException e) {
            // 参数错误处理
            return ResponseEntity.badRequest().body(errorResponse(400, e.getMessage()));
        } catch (Exception e) {
            // 服务器错误处理
            log.error("图片上传失败", e);
            return ResponseEntity.internalServerError().body(errorResponse(500, "服务器内部错误"));
        }
    }
    private Map<String, Object> errorResponse(int code, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", code);
        response.put("message", message);
        return response;
    }
}
