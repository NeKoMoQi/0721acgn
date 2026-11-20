package com.example.demo.util;

import com.example.demo.com.CartoonPage;
import com.example.demo.com.Cartoons;
import com.example.demo.com.RelatedWorkVo;
import com.example.demo.com.SearchResult;
import com.example.demo.com.Xunli;
import com.example.demo.com.XunliPoint;

import java.util.List;
import java.util.function.Consumer;

/**
 * 接口对外输出时，把自建 OSS 的公开图片地址换成图片 CDN 地址。
 *
 * <p>只匹配完整 OSS 主机名（默认 {@code acgn0721.oss-cn-hangzhou.aliyuncs.com}），路径与查询参数
 * （含 {@code x-oss-process} 缩图参数）原样保留；数据库里仍是原始 OSS 链接，不做批量更新。
 * 外链（如 image.anitabi.cn）、已经是 CDN 的链接、带签名参数的 OSS 链接（{@code ?Signature=...}）
 * 都原样返回。</p>
 *
 * <p>主机名与 CDN 域名可通过 {@code image.cdn.oss-host} / {@code image.cdn.base} 配置，
 * 由 {@link com.example.demo.config.ImageCdnConfig} 在启动时注入；不配置时使用下面的默认值。</p>
 */
public final class ImageCdnRewriter {

    public static final String DEFAULT_OSS_HOST = "acgn0721.oss-cn-hangzhou.aliyuncs.com";
    public static final String DEFAULT_CDN_BASE = "https://img.0721acgn.com";

    private static volatile String ossHost = DEFAULT_OSS_HOST;
    private static volatile String cdnBase = DEFAULT_CDN_BASE;

    private ImageCdnRewriter() {
    }

    /** 启动时注入配置；传空值表示保持默认。 */
    public static void configure(String newOssHost, String newCdnBase) {
        ossHost = isBlank(newOssHost) ? DEFAULT_OSS_HOST : newOssHost.trim();
        cdnBase = isBlank(newCdnBase) ? DEFAULT_CDN_BASE : stripTrailingSlash(newCdnBase.trim());
    }

    /** 关闭改写（本地调试想直连 OSS 时用）。 */
    public static void disable() {
        cdnBase = "";
    }

    /** 单个 URL 的转换；不匹配或为空时原样返回。 */
    public static String rewrite(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        String cdn = cdnBase;
        if (cdn.isEmpty()) {
            return url;
        }
        String host = ossHost;
        int schemeEnd = url.indexOf("://");
        if (schemeEnd < 4) {
            return url;
        }
        String scheme = url.substring(0, schemeEnd).toLowerCase();
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            return url;
        }
        int hostStart = schemeEnd + 3;
        if (!hostEndsAt(url, hostStart, host)) {
            return url;
        }
        String rest = url.substring(hostStart + host.length());
        // 带签名的 OSS 链接改主机就会失效，原样返回
        if (isSigned(rest)) {
            return url;
        }
        return cdn + rest;
    }

    /** 主机名必须完整匹配（大小写不敏感），且以 /、?、# 或结尾收住，避免 .evil 之类伪装主机。 */
    private static boolean hostEndsAt(String url, int hostStart, String host) {
        int hostEnd = hostStart + host.length();
        if (url.length() < hostEnd) {
            return false;
        }
        if (!url.regionMatches(true, hostStart, host, 0, host.length())) {
            return false;
        }
        return hostEnd == url.length() || "/?#".indexOf(url.charAt(hostEnd)) >= 0;
    }

    private static boolean isSigned(String rest) {
        String lower = rest.toLowerCase();
        return lower.contains("ossaccesskeyid=") || lower.contains("signature=")
                || lower.contains("security-token=") || lower.contains("x-oss-signature=");
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String stripTrailingSlash(String s) {
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == '/') {
            end--;
        }
        return s.substring(0, end);
    }

    private static <T> List<T> applyAll(List<T> items, Consumer<T> rewriter) {
        if (items != null) {
            items.forEach(rewriter);
        }
        return items;
    }

    public static Xunli applyCdn(Xunli work) {
        if (work == null) {
            return null;
        }
        work.setBcimg(rewrite(work.getBcimg()));
        work.setHengpic(rewrite(work.getHengpic()));
        return work;
    }

    public static List<Xunli> applyCdnToXunliList(List<Xunli> works) {
        return applyAll(works, ImageCdnRewriter::applyCdn);
    }

    public static XunliPoint applyCdn(XunliPoint point) {
        if (point == null) {
            return null;
        }
        point.setXunlipic(rewrite(point.getXunlipic()));
        point.setGaoqingpic(rewrite(point.getGaoqingpic()));
        return point;
    }

    public static List<XunliPoint> applyCdnToXunliPointList(List<XunliPoint> points) {
        return applyAll(points, ImageCdnRewriter::applyCdn);
    }

    public static Cartoons applyCdn(Cartoons cartoon) {
        if (cartoon == null) {
            return null;
        }
        cartoon.setPicurl(rewrite(cartoon.getPicurl()));
        cartoon.setHengpic(rewrite(cartoon.getHengpic()));
        return cartoon;
    }

    public static List<Cartoons> applyCdnToCartoons(List<Cartoons> cartoons) {
        return applyAll(cartoons, ImageCdnRewriter::applyCdn);
    }

    public static CartoonPage applyCdn(CartoonPage page) {
        if (page == null) {
            return null;
        }
        page.setFileurl(rewrite(page.getFileurl()));
        return page;
    }

    public static List<CartoonPage> applyCdnToCartoonPages(List<CartoonPage> pages) {
        return applyAll(pages, ImageCdnRewriter::applyCdn);
    }

    /** 搜索结果是混合类型，只有封面 picurl 需要换域名。 */
    public static List<SearchResult> applyCdnToSearchResults(List<SearchResult> results) {
        return applyAll(results, r -> r.setPicurl(rewrite(r.getPicurl())));
    }

    /** 相关推荐卡片封面。 */
    public static List<RelatedWorkVo> applyCdnToRelatedWorks(List<RelatedWorkVo> works) {
        return applyAll(works, w -> w.setPicurl(rewrite(w.getPicurl())));
    }
}
