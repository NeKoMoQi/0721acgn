package com.example.demo.com;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString

public class XunliPoint {
    int id;
    int xunliid;
    String xunlicolor;
    /** 作品名（JOIN tb_xunli 时） */
    String xunliname;
    /** 巡礼点自身名称（地标/场景名）；与 xunliname 分离，避免只显示番组名 */
    String pointXunliname;
    String xunlipic;
    String gaoqingpic;
    String ep;
    String time;
    /** 作品侧城市（JOIN 全站 picker 时使用） */
    String city;
    List<Double> geopoint;
    int distancemeters;
    Double lat;
    Double lng;
}
