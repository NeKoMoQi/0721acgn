package com.example.demo.util;

import com.example.demo.com.Xunli;
import com.example.demo.com.XunliPoint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ImageCdnRewriterTests {

    private static final String OSS = "https://acgn0721.oss-cn-hangzhou.aliyuncs.com";
    private static final String CDN = "https://img.0721acgn.com";

    @Test
    void rewritesOssHostKeepingPathAndQuery() {
        assertEquals(CDN + "/xunli-v2/images/a.jpg",
                ImageCdnRewriter.rewrite(OSS + "/xunli-v2/images/a.jpg"));
        assertEquals(CDN + "/xunli-v2/images/a.jpg?x-oss-process=image/resize,w_700",
                ImageCdnRewriter.rewrite(OSS + "/xunli-v2/images/a.jpg?x-oss-process=image/resize,w_700"));
    }

    @Test
    void rewritesHttpSchemeAndUppercaseHost() {
        assertEquals(CDN + "/xunli/51.jpg",
                ImageCdnRewriter.rewrite("http://acgn0721.oss-cn-hangzhou.aliyuncs.com/xunli/51.jpg"));
        assertEquals(CDN + "/xunli/51.jpg",
                ImageCdnRewriter.rewrite("https://ACGN0721.OSS-CN-HANGZHOU.ALIYUNCS.COM/xunli/51.jpg"));
    }

    @Test
    void leavesExternalAndCdnUrlsAlone() {
        String anitabi = "https://image.anitabi.cn/bangumi/362575.jpg?plan=h360";
        assertEquals(anitabi, ImageCdnRewriter.rewrite(anitabi));
        String cdn = CDN + "/xunli-v2/images/a.jpg";
        assertEquals(cdn, ImageCdnRewriter.rewrite(cdn));
        String otherOss = "https://other-bucket.oss-cn-hangzhou.aliyuncs.com/x.jpg";
        assertEquals(otherOss, ImageCdnRewriter.rewrite(otherOss));
    }

    @Test
    void leavesSignedOssUrlsAlone() {
        String signed = OSS + "/private/x.jpg?OSSAccessKeyId=abc&Expires=1700000000&Signature=xyz";
        assertEquals(signed, ImageCdnRewriter.rewrite(signed));
    }

    @Test
    void doesNotMatchLookalikeHost() {
        String lookalike = "https://acgn0721.oss-cn-hangzhou.aliyuncs.com.evil.example/x.jpg";
        assertEquals(lookalike, ImageCdnRewriter.rewrite(lookalike));
    }

    @Test
    void handlesNullAndEmpty() {
        assertNull(ImageCdnRewriter.rewrite(null));
        assertEquals("", ImageCdnRewriter.rewrite(""));
    }

    @Test
    void appliesToWorkAndPointFields() {
        Xunli work = new Xunli();
        work.setBcimg(OSS + "/xunli/51.jpg");
        work.setHengpic(null);
        ImageCdnRewriter.applyCdn(work);
        assertEquals(CDN + "/xunli/51.jpg", work.getBcimg());
        assertNull(work.getHengpic());

        XunliPoint point = new XunliPoint();
        point.setXunlipic(OSS + "/xunli/point/15/16/16-1.jpg");
        point.setGaoqingpic("https://image.anitabi.cn/points/x.jpg?plan=h360");
        ImageCdnRewriter.applyCdn(point);
        assertEquals(CDN + "/xunli/point/15/16/16-1.jpg", point.getXunlipic());
        assertEquals("https://image.anitabi.cn/points/x.jpg?plan=h360", point.getGaoqingpic());
    }
}
