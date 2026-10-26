package com.example.demo.dto;

import lombok.Data;

@Data
public class AcgnVotePostDto {
    private String mediaType;
    private int mediaId;
    /** up | down */
    private String side;
}
