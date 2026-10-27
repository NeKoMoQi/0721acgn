package com.example.demo;

import com.example.demo.Interceptor.LoginCheakInterceptor;
import com.example.demo.filter.DemoFilter;
import com.example.demo.utils.JwtUtils;
import com.example.demo.utils.PublicAccessPolicy;
import java.util.Map;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;

class GuestAccessTests {
    @ParameterizedTest
    @CsvSource({
        "GET,/api/animation", "GET,/api/animation/byid", "GET,/api/animation/episodes",
        "GET,/api/cartoon", "GET,/api/cartoon/chapter", "GET,/api/cartoonpage",
        "GET,/api/novel", "GET,/api/novel/chapter/byid", "GET,/api/novel/content", "GET,/api/searchresult",
        "GET,/api/related", "GET,/api/acgn/user/state", "GET,/api/acgn/social/comments",
        "POST,/api/acgn/user/library/visit", "POST,/api/acgn/user/progress",
        "POST,/api/acgn/user/library/favorite-toggle", "POST,/api/admin/acl/index/login",
        "POST,/acgn/live2d/agent/chat", "POST,/acgn/live2d/agent/stream",
        "GET,/api/media/video.mp4", "HEAD,/api/media/video.mp4"
    })
    void guestBrowsingPassesBothLayers(String method, String path) throws Exception {
        var request = new MockHttpServletRequest(method, path);
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();
        new DemoFilter().doFilter(request, response, chain);
        assertNotNull(chain.getRequest());
        assertTrue(new LoginCheakInterceptor().preHandle(request, response, new Object()));
    }

    @ParameterizedTest
    @CsvSource({
        "POST,/api/animation/preview-video", "POST,/api/animation/preview-video/local",
        "POST,/api/animation/episode-video", "POST,/api/animation/episode-video/local",
        "POST,/api/updatepicbyoss", "POST,/api/xunli/addnew", "POST,/api/battle/reset",
        "GET,/api/xunli/agent/routes", "POST,/api/xunli/agent/routes",
        "GET,/api/xunli/agent/routes/fixture", "DELETE,/api/xunli/agent/routes/fixture",
        "GET,/api/admin/acl/index/info", "POST,/api/admin/acl/index/logout",
        "GET,/depts", "GET,/api/animation/private", "GET,/api/private/login-bypass",
        "POST,/api/media/video.mp4", "DELETE,/api/cartoon"
    })
    void protectedOperationsReturn401InBothLayers(String method, String path) throws Exception {
        assertFalse(PublicAccessPolicy.isPublic(method, path));
        var request = new MockHttpServletRequest(method, path);
        var filterResponse = new MockHttpServletResponse();
        var chain = new MockFilterChain();
        new DemoFilter().doFilter(request, filterResponse, chain);
        assertNull(chain.getRequest());
        assertEquals(401, filterResponse.getStatus());
        assertTrue(filterResponse.getContentAsString().contains("NOT_LOGIN"));
        var mvcResponse = new MockHttpServletResponse();
        assertFalse(new LoginCheakInterceptor().preHandle(request, mvcResponse, new Object()));
        assertEquals(401, mvcResponse.getStatus());
    }

    @Test
    void localGalgameAndErrorRulesSurviveTheAgentMerge() {
        // 本地新增的 Galgame 板块与 /error 透传规则，不能被圣地巡礼更新包覆盖掉。
        assertTrue(PublicAccessPolicy.isPublic("GET", "/api/galgame"));
        assertTrue(PublicAccessPolicy.isPublic("GET", "/api/galgame/hot"));
        assertTrue(PublicAccessPolicy.isPublic("GET", "/api/galgame/byid"));
        assertTrue(PublicAccessPolicy.isPublic("GET", "/api/activity/byid"));
        assertTrue(PublicAccessPolicy.isPublic("GET", "/api/acgn/user/byid"));
        assertTrue(PublicAccessPolicy.isPublic("POST", "/error"));
    }

    @Test
    void invalidTokenIsRejectedWhileSignedTokenCanAccessAccountInfo() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/admin/acl/index/info");
        request.addHeader("token", "invalid-token");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();
        new DemoFilter().doFilter(request, response, chain);
        assertEquals(401, response.getStatus());
        assertNull(chain.getRequest());
        var interceptorResponse = new MockHttpServletResponse();
        assertFalse(new LoginCheakInterceptor().preHandle(request, interceptorResponse, new Object()));
        assertEquals(401, interceptorResponse.getStatus());

        request.removeHeader("token");
        request.addHeader("token", JwtUtils.generateJwt(new HashMap<>(Map.of("username", "reader"))));
        var signedResponse = new MockHttpServletResponse();
        var signedChain = new MockFilterChain();
        new DemoFilter().doFilter(request, signedResponse, signedChain);
        assertNotNull(signedChain.getRequest());
        assertTrue(new LoginCheakInterceptor().preHandle(request, signedResponse, new Object()));
    }
}
