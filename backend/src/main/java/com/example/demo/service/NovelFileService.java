package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 把 OSS 上的 epub 正文取回来，交给前端同源接口使用。
 *
 * <p>存储桶没有配跨域规则，浏览器直接 fetch EPUB 会被 CORS 拦掉，epub.js 也就读不到文件。
 * 这里由后端取字节再以同源响应返回，前端拿到 ArrayBuffer 就能正常解析。</p>
 *
 * <p>只允许访问自有 OSS 域名，避免这个接口被当成任意 URL 代理（SSRF）。</p>
 */
@Service
public class NovelFileService {

    private static final String ALLOWED_HOST_SUFFIX = ".aliyuncs.com";
    private static final int MAX_BYTES = 80 * 1024 * 1024;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    /** 下载整卷正文；地址非法、上游非 200、内容为空或过大都会抛异常。 */
    public byte[] fetch(String url) throws IOException, InterruptedException {
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid url", e);
        }
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || host == null
                || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                || !host.toLowerCase().endsWith(ALLOWED_HOST_SUFFIX)) {
            throw new IllegalArgumentException("url not allowed: " + host);
        }
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new IOException("upstream status " + response.statusCode());
        }
        byte[] body = response.body();
        if (body == null || body.length == 0) {
            throw new IOException("empty body");
        }
        if (body.length > MAX_BYTES) {
            throw new IOException("file too large");
        }
        return body;
    }
}
