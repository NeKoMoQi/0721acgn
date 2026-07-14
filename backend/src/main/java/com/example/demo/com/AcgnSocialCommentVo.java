package com.example.demo.com;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcgnSocialCommentVo {
    private String nickname;
    private String avatar;
    private String body;
    private long at;
}
