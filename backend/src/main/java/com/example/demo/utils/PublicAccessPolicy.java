package com.example.demo.utils;

import com.alibaba.fastjson.JSONObject;
import com.example.demo.com.Result;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

/** Filter 和 MVC 拦截器共用同一份游客访问规则，避免页面放行后接口仍拦截。 */
public final class PublicAccessPolicy {
    private PublicAccessPolicy() {}

    private static final Set<String> READ_PATHS = Set.of(
            "/api/news", "/api/news/byid", "/api/news/image",
            "/api/activity", "/api/activity/byid", "/api/activity/all", "/api/activity/hot", "/api/activity/leibie",
            "/api/peoplecount", "/api/searchresult", "/api/related",
            "/api/animation", "/api/animation/hot", "/api/animation/byid",
            "/api/animation/episodes", "/api/animation/episode", "/api/animation/public-clips",
            "/api/galgame", "/api/galgame/hot", "/api/galgame/byid",
            "/api/cartoon", "/api/cartoon/byid", "/api/cartoonpage", "/api/cartoon/hot", "/api/cartoon/chapter",
            "/api/novel", "/api/novel/chapter", "/api/novel/byid", "/api/novel/count",
            "/api/novel/chapter/byid", "/api/novel/content", "/api/novel/hot", "/api/shengdi/hot",
            "/api/xunli", "/api/xunli/point", "/api/xunli/point/update", "/api/xunli/update",
            "/api/xunli/byid", "/api/xunli/point/byid", "/api/xunli/search", "/api/xunli/searchnew",
            "/api/xunli/searchhot", "/api/xunli/searchran", "/api/xunli/picker/works", "/api/xunli/picker/points",
            "/api/xunli/agent/picker/works", "/api/xunli/agent/picker/points", "/api/xunli/agent/picker/spots",
            "/api/xunli/point/clicksearch",
            "/api/acgn/user", "/api/acgn/user/byid", "/api/acgn/getbirth", "/api/acgn/user/state",
            "/api/acgn/social/engagement", "/api/acgn/social/comments");

    // 已有的浏览器匿名数据功能保持可用，不将设备标识伪装为账号同步。
    private static final Set<String> ANONYMOUS_POST_PATHS = Set.of(
            "/api/admin/acl/index/login", "/acgn/live2d/agent/chat", "/acgn/live2d/agent/stream",
            "/api/acgn/user/library/visit", "/api/acgn/user/library/favorite-toggle",
            "/api/acgn/user/subscribe/toggle", "/api/acgn/user/progress", "/api/acgn/user/import-legacy",
            "/api/acgn/social/vote", "/api/acgn/social/comments");

    public static boolean isPublic(String method, String path) {
        if ("OPTIONS".equals(method)) return true;
        // 错误转发路径必须放行，否则任何异常都会被过滤链改写成 401 NOT_LOGIN，把真实错误藏起来
        if ("/error".equals(path)) return true;
        if ("GET".equals(method) || "HEAD".equals(method)) {
            return READ_PATHS.contains(path) || path.startsWith("/api/media/");
        }
        return "POST".equals(method) && ANONYMOUS_POST_PATHS.contains(path);
    }

    public static void rejectUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JSONObject.toJSONString(Result.error("NOT_LOGIN")));
    }
}
