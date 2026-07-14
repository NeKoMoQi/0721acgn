package com.example.demo.dto;

import lombok.Data;

@Data
public class AcgnCommentPostDto {
    private String mediaType;
    private int mediaId;
    private String nickname;
    private String avatar;
    private String body;
}
