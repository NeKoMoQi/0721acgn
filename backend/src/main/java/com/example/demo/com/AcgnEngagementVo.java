package com.example.demo.com;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcgnEngagementVo {
    private int likes;
    private int dislikes;
    /** null | up | down */
    private String myVote;
}
