package com.example.demo.service;

import com.example.demo.com.Xunli;
import com.example.demo.com.XunliPoint;

import java.util.List;

public interface XunliService {
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
    void saveBatch(XunliPoint point);

    List<Xunli> getPickerXunliList();

    List<XunliPoint> getPickerXunliPointList();
}
