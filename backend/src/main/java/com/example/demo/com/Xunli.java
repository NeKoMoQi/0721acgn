package com.example.demo.com;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.postgis.Point;
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString

public class Xunli {
    int id;
    String xunliname;
    String Japanname;
    String city;
    String bcimg;
    String xunlicolor;
    List<Double> maingeo;
    int zoom;
    int count;
    String hengpic;
    int xunliid;
}








