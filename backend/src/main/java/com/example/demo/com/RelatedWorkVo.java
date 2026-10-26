package com.example.demo.com;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelatedWorkVo {
    /** animation | comic | novel */
    private String type;
    private int id;
    private String name;
    private String picurl;
}
