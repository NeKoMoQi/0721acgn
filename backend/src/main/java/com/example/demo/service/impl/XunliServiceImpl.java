package com.example.demo.service.impl;

import com.example.demo.com.Xunli;
import com.example.demo.com.XunliPoint;
import com.example.demo.mapper.postgresql.XunliMapper;
import com.example.demo.service.XunliService;
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
    public List<Xunli> getXunli(int zoom){
        return xunliMapper.getXunli(zoom);
    }
    public List<XunliPoint> getXunliPoint(int zoom){
        return xunliMapper.getXunliPoint(zoom);
    }
    public List<XunliPoint> UpdateXunliPoint(Double NeLng, Double NeLat,Double WsLng,Double WsLat,int Zoom){
        return xunliMapper.UpdateXunliPoint(NeLng,NeLat,WsLng,WsLat,Zoom);
    }
    public List<Xunli> UpdateXunli(Double NeLng, Double NeLat,Double WsLng,Double WsLat,int Zoom){
        return xunliMapper.UpdateXunli(NeLng,NeLat,WsLng,WsLat,Zoom);
    }
    public Xunli getXunliByid(int xunliid){
        return xunliMapper.getXunliByid(xunliid);
    }
    public List<XunliPoint> getXunliPointByid (int xunliid){
        return xunliMapper.getXunliPointByid(xunliid);
    }
    public List<Xunli> SearchXunli (String information){
        return xunliMapper.SearchXunli(information);
    }
    public List<Xunli> SearchXunlinew (){
        return xunliMapper.SearchXunlinew();
    }
    public List<Xunli> SearchXunlihot (){
        return xunliMapper.SearchXunlihot();
    }
    public List<Xunli> SearchXunlirange(){
        return xunliMapper.SearchXunlirange();
    }
    public List<XunliPoint> Clickgetpoint(Double lng, Double lat){
        return xunliMapper.Clickgetpoint(lng,lat);
    }
    public int getmaxid(){return xunliMapper.getmaxid();}
    public void saveBatch(XunliPoint point){xunliMapper.addnew(point);};
    public List<Xunli> getPickerXunliList() {
        return xunliMapper.getPickerXunliList();
    }
    public List<XunliPoint> getPickerXunliPointList() {
        return xunliMapper.getPickerXunliPointList();
    }
}
