package com.example.demo.util;

import com.example.demo.com.Xunli;
import com.example.demo.com.XunliPoint;

/**
 * 巡礼接口对外输出时，把自建 OSS 的公开图片地址换成图片 CDN 地址。
 *
 * <p>只匹配完整 OSS 主机名（{@code acgn0721.oss-cn-hangzhou.aliyuncs.com}），路径与查询参数原样保留；
 * 数据库里仍是原始 OSS 链接，不做批量更新。image.anitabi.cn 等外链、已经是 CDN 的链接、
 * 带签名参数的 OSS 链接（{@code ?Signature=...}）都原样返回。</p>
 */
public final class ImageCdnRewriter {

    public static final String OSS_HOST = "acgn0721.oss-cn-hangzhou.aliyuncs.com";
    public static final String CDN_BASE = "https://img.0721acgn.com";

    private static final String[] OSS_PREFIXES = {
            "https://" + OSS_HOST,
            "http://" + OSS_HOST
    };

    private ImageCdnRewriter() {
    }

    /** 单个 URL 的转换；不匹配或为空时原样返回。 */
    public static String rewrite(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        for (String prefix : OSS_PREFIXES) {
            if (url.regionMatches(true, 0, prefix, 0, prefix.length())) {
                String rest = url.substring(prefix.length());
                // 必须到主机边界为止，避免 acgn0721.oss-....com.evil 之类的伪装主机
                if (rest.isEmpty() || rest.startsWith("/") || rest.startsWith("?") || rest.startsWith("#")) {
                    // 带签名的 OSS 链接改主机就会失效，原样返回
                    if (isSigned(rest)) {
                        return url;
                    }
                    return CDN_BASE + rest;
                }
            }
        }
        return url;
    }

    private static boolean isSigned(String rest) {
        String lower = rest.toLowerCase();
        return lower.contains("ossaccesskeyid=") || lower.contains("signature=")
                || lower.contains("security-token=") || lower.contains("x-oss-signature=");
    }

    public static Xunli applyCdn(Xunli work) {
        if (work == null) {
            return null;
        }
        work.setBcimg(rewrite(work.getBcimg()));
        work.setHengpic(rewrite(work.getHengpic()));
        return work;
    }

    public static XunliPoint applyCdn(XunliPoint point) {
        if (point == null) {
            return null;
        }
        point.setXunlipic(rewrite(point.getXunlipic()));
        point.setGaoqingpic(rewrite(point.getGaoqingpic()));
        return point;
    }
}
