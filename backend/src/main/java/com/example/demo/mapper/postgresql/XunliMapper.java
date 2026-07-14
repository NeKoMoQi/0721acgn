package com.example.demo.mapper.postgresql;

import com.example.demo.com.Xunli;
import com.example.demo.com.XunliPoint;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface XunliMapper {
    List<Xunli> getXunli(int zoom);
    List<XunliPoint> getXunliPoint(int zoom);
    List<XunliPoint> UpdateXunliPoint(Double NeLng, Double NeLat,Double WsLng,Double WsLat,int Zoom);
    List<Xunli> UpdateXunli(Double NeLng, Double NeLat,Double WsLng,Double WsLat,int Zoom);
    Xunli getXunliByid(int xunliid);
    List<XunliPoint> getXunliPointByid (int xunliid);
    List<Xunli> SearchXunli (String information);
    List<Xunli> SearchXunlinew ();
    List<Xunli> SearchXunlihot ();
    List<Xunli> SearchXunlirange();
    List<XunliPoint> Clickgetpoint(Double lng, Double lat);
    int getmaxid();

    void addnew(XunliPoint point);

    /** 全站作品列表（途经点导入，不限视口） */
    List<Xunli> getPickerXunliList();

    /** 全站地标点 + 作品信息（必经途点导入，单请求条数上限） */
    List<XunliPoint> getPickerXunliPointList();
}
