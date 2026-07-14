package com.example.demo.com;

import lombok.Data;

@Data
public class AttackRequest {
    private String attackerId;
    private String targetId;
    private Long battleId;
}