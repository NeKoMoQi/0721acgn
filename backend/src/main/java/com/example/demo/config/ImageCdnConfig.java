package com.example.demo.config;

import com.example.demo.util.ImageCdnRewriter;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 图片 CDN 配置：接口输出把 OSS 直链换成 CDN 加速域名。
 *
 * <p>{@code image.cdn.enabled=false} 时完全不改写，接口直接返回 OSS 原始地址。</p>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "image.cdn")
public class ImageCdnConfig {

    /** OSS 直链主机名，例如 acgn0721.oss-cn-hangzhou.aliyuncs.com */
    private String ossHost;

    /** 图片 CDN 域名，例如 https://img.0721acgn.com */
    private String base;

    /** 关闭后接口返回 OSS 原始地址（本地调试想直连 OSS 时用） */
    private boolean enabled = true;

    @PostConstruct
    public void apply() {
        if (!enabled) {
            ImageCdnRewriter.disable();
            return;
        }
        ImageCdnRewriter.configure(ossHost, base);
    }
}
