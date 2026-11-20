package com.example.demo.util;

import com.example.demo.com.CartoonPage;
import com.example.demo.com.Cartoons;
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

    /** 漫画阅读页：fileurl 换成 CDN，缩图参数原样保留。 */
    @Test
    void appliesToCartoonCoverAndReadingPage() {
        Cartoons cover = new Cartoons();
        cover.setPicurl(OSS + "/comic/cover/1.jpg");
        cover.setHengpic(OSS + "/comic/heng/1.jpg?x-oss-process=image/resize,w_700");
        ImageCdnRewriter.applyCdn(cover);
        assertEquals(CDN + "/comic/cover/1.jpg", cover.getPicurl());
        assertEquals(CDN + "/comic/heng/1.jpg?x-oss-process=image/resize,w_700", cover.getHengpic());

        CartoonPage page = new CartoonPage();
        page.setFileurl(OSS + "/comic/chapter/0000001/001.jpg?x-oss-process=image/resize,m_lfit,w_1600");
        assertEquals(CDN + "/comic/chapter/0000001/001.jpg?x-oss-process=image/resize,m_lfit,w_1600",
                ImageCdnRewriter.applyCdn(page).getFileurl());
    }

    @Test
    void usesConfiguredHosts() {
        try {
            ImageCdnRewriter.configure("bucket.example.com", "https://cdn.example.com/");
            assertEquals("https://cdn.example.com/a/b.jpg?x-oss-process=image/resize,w_700",
                    ImageCdnRewriter.rewrite("https://bucket.example.com/a/b.jpg?x-oss-process=image/resize,w_700"));
            // 老主机不再是本地 OSS，原样返回
            assertEquals(OSS + "/x.jpg", ImageCdnRewriter.rewrite(OSS + "/x.jpg"));
        } finally {
            ImageCdnRewriter.configure(null, null);
        }
        assertEquals(CDN + "/x.jpg", ImageCdnRewriter.rewrite(OSS + "/x.jpg"));
    }

    @Test
    void disableKeepsOssUrls() {
        try {
            ImageCdnRewriter.disable();
            assertEquals(OSS + "/x.jpg", ImageCdnRewriter.rewrite(OSS + "/x.jpg"));
        } finally {
            ImageCdnRewriter.configure(null, null);
        }
    }
}
