package com.example.demo.service.impl;

import com.example.demo.com.Xunli;
import com.example.demo.com.XunliPoint;
import com.example.demo.mapper.postgresql.XunliMapper;
import com.example.demo.service.XunliService;
import com.example.demo.util.ImageCdnRewriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class XunliServiceImpl implements XunliService {
    private static final Logger log = LoggerFactory.getLogger(XunliServiceImpl.class);
    private XunliMapper xunliMapper;
    @Autowired
    public XunliServiceImpl(XunliMapper xunliMapper) {
        this.xunliMapper = xunliMapper;
    }

    /** 库里存的是 OSS 原始地址，出接口前统一换成图片 CDN（数据库不改）。 */
    private static List<Xunli> cdnWorks(List<Xunli> works) {
        if (works != null) {
            works.forEach(ImageCdnRewriter::applyCdn);
        }
        return works;
    }

    private static List<XunliPoint> cdnPoints(List<XunliPoint> points) {
        if (points != null) {
            points.forEach(ImageCdnRewriter::applyCdn);
        }
        return points;
    }

    public List<Xunli> getXunli(int zoom){
        return cdnWorks(xunliMapper.getXunli(zoom));
    }
    public List<XunliPoint> getXunliPoint(int zoom){
        return cdnPoints(xunliMapper.getXunliPoint(zoom));
    }
    public List<XunliPoint> UpdateXunliPoint(Double NeLng, Double NeLat,Double WsLng,Double WsLat,int Zoom){
        return cdnPoints(xunliMapper.UpdateXunliPoint(NeLng,NeLat,WsLng,WsLat,Zoom));
    }
    public List<Xunli> UpdateXunli(Double NeLng, Double NeLat,Double WsLng,Double WsLat,int Zoom){
        return cdnWorks(xunliMapper.UpdateXunli(NeLng,NeLat,WsLng,WsLat,Zoom));
    }
    public Xunli getXunliByid(int xunliid){
        return ImageCdnRewriter.applyCdn(xunliMapper.getXunliByid(xunliid));
    }
    public List<XunliPoint> getXunliPointByid (int xunliid){
        return cdnPoints(xunliMapper.getXunliPointByid(xunliid));
    }
    public List<Xunli> SearchXunli (String information){
        return cdnWorks(xunliMapper.SearchXunli(information));
    }
    public List<Xunli> SearchXunlinew (){
        return cdnWorks(xunliMapper.SearchXunlinew());
    }
    public List<Xunli> SearchXunlihot (){
        return cdnWorks(xunliMapper.SearchXunlihot());
    }
    public List<Xunli> SearchXunlirange(){
        return cdnWorks(xunliMapper.SearchXunlirange());
    }
    public List<XunliPoint> Clickgetpoint(Double lng, Double lat){
        return cdnPoints(xunliMapper.Clickgetpoint(lng,lat));
    }
    public int getmaxid(){return xunliMapper.getmaxid();}
    public void saveBatch(XunliPoint point){xunliMapper.addnew(point);};
    public List<Xunli> getPickerXunliList() {
        return cdnWorks(xunliMapper.getPickerXunliList());
    }
    public List<XunliPoint> getPickerXunliPointList() {
        return cdnPoints(xunliMapper.getPickerXunliPointList());
    }
}
