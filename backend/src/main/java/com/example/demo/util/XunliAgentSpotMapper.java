package com.example.demo.util;

import com.example.demo.com.Xunli;
import com.example.demo.com.XunliPoint;
import com.example.demo.dto.XunliAgentSpot;

import java.util.List;

public final class XunliAgentSpotMapper {
    private XunliAgentSpotMapper() {
    }

    public static XunliAgentSpot fromWork(Xunli work) {
        XunliAgentSpot spot = new XunliAgentSpot();
        spot.setId(work.getXunliid() > 0 ? String.valueOf(work.getXunliid()) : String.valueOf(work.getId()));
        spot.setType("work");
        spot.setName(firstText(work.getJapanname(), work.getCity(), work.getXunliname(), "主视点"));
        spot.setWorkTitle(trimToNull(work.getXunliname()));
        spot.setCity(trimToNull(work.getCity()));
        spot.setXunliid(work.getXunliid());
        spot.setGeopoint(normalizedCoords(work.getMaingeo()));
        applyLngLat(spot, spot.getGeopoint());
        spot.setImageUrl(firstText(work.getBcimg(), work.getHengpic(), null));
        spot.setAddress(buildWorkAddress(work));
        return spot;
    }

    public static XunliAgentSpot fromPoint(XunliPoint point) {
        XunliAgentSpot spot = new XunliAgentSpot();
        spot.setId(point.getId() > 0 ? String.valueOf(point.getId()) : null);
        spot.setType("point");
        spot.setName(firstText(point.getPointXunliname(), point.getXunliname(), "站内巡礼点"));
        spot.setWorkTitle(trimToNull(point.getXunliname()));
        spot.setCity(trimToNull(point.getCity()));
        spot.setXunliid(point.getXunliid());
        spot.setGeopoint(normalizedCoords(point.getGeopoint()));
        applyLngLat(spot, spot.getGeopoint());
        spot.setImageUrl(firstText(point.getXunlipic(), point.getGaoqingpic(), null));
        spot.setAddress(buildPointAddress(point));
        spot.setEp(trimToNull(point.getEp()));
        spot.setTime(trimToNull(point.getTime()));
        return spot;
    }

    private static void applyLngLat(XunliAgentSpot spot, List<Double> coords) {
        if (coords == null || coords.size() < 2) {
            return;
        }
        spot.setLng(coords.get(0));
        spot.setLat(coords.get(1));
    }

    private static List<Double> normalizedCoords(List<Double> coords) {
        if (coords == null || coords.size() < 2) {
            return null;
        }
        Double lng = coords.get(0);
        Double lat = coords.get(1);
        if (lng == null || lat == null) {
            return null;
        }
        if (!Double.isFinite(lng) || !Double.isFinite(lat)) {
            return null;
        }
        if (lng < 122 || lng > 154 || lat < 20 || lat > 46) {
            return null;
        }
        return List.of(lng, lat);
    }

    private static String buildWorkAddress(Xunli work) {
        String city = trimToNull(work.getCity());
        Integer count = work.getCount();
        if (city != null && count != null && count > 0) {
            return city + " · " + count + " 地标";
        }
        return firstText(city, "站内巡礼主视点");
    }

    private static String buildPointAddress(XunliPoint point) {
        StringBuilder text = new StringBuilder();
        String workTitle = trimToNull(point.getXunliname());
        if (workTitle != null) {
            text.append("《").append(workTitle).append("》");
        }
        String ep = trimToNull(point.getEp());
        if (ep != null) {
            if (!text.isEmpty()) {
                text.append(" · ");
            }
            text.append("EP").append(ep);
        }
        String city = trimToNull(point.getCity());
        if (city != null) {
            if (!text.isEmpty()) {
                text.append(" · ");
            }
            text.append(city);
        }
        return text.isEmpty() ? "站内巡礼点" : text.toString();
    }

    private static String firstText(String... values) {
        for (String value : values) {
            String trimmed = trimToNull(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
