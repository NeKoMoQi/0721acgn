package com.example.demo.controller;

import com.example.demo.com.Result;
import com.example.demo.com.Xunli;
import com.example.demo.com.XunliPoint;
import com.example.demo.dto.XunliAgentSpot;
import com.example.demo.service.XunliService;
import com.example.demo.util.XunliAgentSpotMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class XunliController {
    @Autowired
    private XunliService xunliService;
    @GetMapping("/api/xunli")
    public Result getxunli (@RequestParam String Zoom){
        int zoom = (int) Math.floor(Double.parseDouble(Zoom)) + 1;
        List<Xunli> xunli = xunliService.getXunli(zoom);
        return Result.success(xunli);
    }
    @GetMapping("/api/xunli/point")
    public Result getxunliPoint(@RequestParam String Zoom){
        int zoom = (int) Math.floor(Double.parseDouble(Zoom)) + 1;
        List<XunliPoint> xunlis = xunliService.getXunliPoint(zoom);
        return Result.success(xunlis);
    }
    @GetMapping("/api/xunli/point/update")
    public Result getxunliPointUpdate(@RequestParam Double[] data){
        Double WsLng = data[0];
        Double WsLat = data[1];
        Double NeLng = data[2];
        Double NeLat = data[3];
        Double zoom = data[4];
        int Zoom = (int) Math.floor(zoom) + 1;
        List<XunliPoint> xunli = xunliService.UpdateXunliPoint(WsLng,WsLat,NeLng,NeLat,Zoom);
        System.out.println(xunli);
        return Result.success(xunli);
    }
    @GetMapping("/api/xunli/update")
    public Result getxunliUpdate(@RequestParam Double[] data){
        Double WsLng = data[0];
        Double WsLat = data[1];
        Double NeLng = data[2];
        Double NeLat = data[3];
        Double zoom = data[4];
        int Zoom = (int) Math.floor(zoom) + 1;
        System.out.println(Arrays.toString(data));
        List<Xunli> xunli = xunliService.UpdateXunli(WsLng,WsLat,NeLng,NeLat,Zoom);
        System.out.println(xunli);
        return Result.success(xunli);
    }
    @GetMapping("/api/xunli/byid")
    public Result getxunliById (@RequestParam Integer Xunliid){
        int xunliid = Xunliid;
        Xunli xunliids = xunliService.getXunliByid(xunliid);
        return Result.success(xunliids);
    }
    @GetMapping("/api/xunli/point/byid")
    public Result getxunliPointById (@RequestParam Integer Xunliid){
        int xunliid = Xunliid;
        List<XunliPoint> xunli = xunliService.getXunliPointByid(xunliid);
        return Result.success(xunli);
    }
    @GetMapping("/api/xunli/search")
    public Result getxunliSearch (@RequestParam String Search){
        log.info(Search);
        String search = Search;
        List<Xunli> xunlisearchlist = xunliService.SearchXunli(search);
        log.info(xunlisearchlist.toString());
        return Result.success(xunlisearchlist);
    }
    @GetMapping("/api/xunli/searchnew")
    public Result getxunliSearchNew (){
        List<Xunli> searchnew = xunliService.SearchXunlinew();
        return Result.success(searchnew);
    }
    @GetMapping("/api/xunli/searchhot")
    public Result getxunliSearchHot (){
        List<Xunli> searchhot = xunliService.SearchXunlihot();
        return Result.success(searchhot);
    }
    @GetMapping("/api/xunli/searchran")
    public Result getxunliSearchRan (){
        List<Xunli> searchran = xunliService.SearchXunlirange();
        return Result.success(searchran);
    }
    @GetMapping("/api/xunli/picker/works")
    public Result getPickerWorks() {
        return Result.success(xunliService.getPickerXunliList());
    }

    @GetMapping("/api/xunli/picker/points")
    public Result getPickerPoints() {
        return Result.success(xunliService.getPickerXunliPointList());
    }

    @GetMapping("/api/xunli/agent/picker/works")
    public Result<List<XunliAgentSpot>> getAgentPickerWorks() {
        List<XunliAgentSpot> spots = xunliService.getPickerXunliList()
                .stream()
                .map(XunliAgentSpotMapper::fromWork)
                .collect(Collectors.toList());
        return Result.success(spots);
    }

    @GetMapping("/api/xunli/agent/picker/points")
    public Result<List<XunliAgentSpot>> getAgentPickerPoints() {
        List<XunliAgentSpot> spots = xunliService.getPickerXunliPointList()
                .stream()
                .map(XunliAgentSpotMapper::fromPoint)
                .collect(Collectors.toList());
        return Result.success(spots);
    }

    @GetMapping("/api/xunli/agent/picker/spots")
    public Result<List<XunliAgentSpot>> getAgentPickerSpots() {
        List<XunliAgentSpot> spots = new ArrayList<>();
        xunliService.getPickerXunliList()
                .stream()
                .map(XunliAgentSpotMapper::fromWork)
                .forEach(spots::add);
        xunliService.getPickerXunliPointList()
                .stream()
                .map(XunliAgentSpotMapper::fromPoint)
                .forEach(spots::add);
        return Result.success(spots);
    }

    @GetMapping("/api/xunli/point/clicksearch")
    public Result getxunliPointClickSearch (@RequestParam Double[] data){
        Double Lng = data[0];
        Double Lat = data[1];
        List<XunliPoint> ClickPoint = xunliService.Clickgetpoint(Lng,Lat);
        log.info(ClickPoint.toString());
        return Result.success(ClickPoint);
    }
    @PostMapping("/api/xunli/addnew")
    public Result addxunli(@RequestBody List<Map<String, Object>> xunliData) {
        log.info("Received data: {}", xunliData);
        List<XunliPoint> points = new ArrayList<>();

        // 获取当前最大ID
        int baseId = xunliService.getmaxid() + 1;

        for (Map<String, Object> data : xunliData) {
            XunliPoint point = convertToXunliPoint(data, baseId++);
            points.add(point);
            // 单条插入
            xunliService.saveBatch(point);
        }

        log.info("Converted points: {}", points);
        return Result.success();
    }

    private XunliPoint convertToXunliPoint(Map<String, Object> data, int id) {
        XunliPoint point = new XunliPoint();
        point.setId(id);

        // 正确转换xunliid（整数）
        Object xunliidObj = data.get("xunliid");
        if (xunliidObj instanceof String) {
            point.setXunliid(Integer.parseInt((String) xunliidObj));
        } else if (xunliidObj instanceof Number) {
            point.setXunliid(((Number) xunliidObj).intValue());
        }

        // 设置其他字段
        point.setXunliname((String) data.get("xunliname"));
        point.setGaoqingpic((String) data.get("gaoqingpic"));
        point.setEp((String) data.get("ep"));
        point.setTime((String) data.get("time"));

        // 正确转换经纬度（字符串->double）
        Object latObj = data.get("lat");
        Object lngObj = data.get("lng");
        double lat = parseDouble(latObj);
        double lng = parseDouble(lngObj);

        point.setGeopoint(Arrays.asList(lat, lng));
        point.setDistancemeters(0);
        return point;
    }

    private double parseDouble(Object value) {
        if (value instanceof String) {
            return Double.parseDouble((String) value);
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        throw new IllegalArgumentException("Invalid value type for coordinate: " + value);
    }
}
