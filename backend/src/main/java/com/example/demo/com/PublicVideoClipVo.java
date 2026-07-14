package com.example.demo.com;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicVideoClipVo {
    private String title;
    /** e.g. YouTube 预告 */
    private String source;
    private String youtubeId;
}
