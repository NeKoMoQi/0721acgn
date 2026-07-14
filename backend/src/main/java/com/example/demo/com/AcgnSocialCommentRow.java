package com.example.demo.com;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AcgnSocialCommentRow {
    private Long id;
    private String nickname;
    private String avatar;
    private String body;
    private LocalDateTime createdAt;
}
