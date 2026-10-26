package com.example.demo.controller;

import com.example.demo.com.AcgnLibraryItemVo;
import com.example.demo.com.AcgnUser;
import com.example.demo.com.AcgnUserBundleVo;
import com.example.demo.com.Result;
import com.example.demo.dto.AcgnMediaTouchDto;
import com.example.demo.dto.AcgnProgressPostDto;
import com.example.demo.service.AcgnUserDataService;
import com.example.demo.service.AcgnUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
public class AcgnUserController {

    private final AcgnUserDataService acgnUserDataService;
    private final AcgnUserService acgnUserService;

    public AcgnUserController(AcgnUserDataService acgnUserDataService, AcgnUserService acgnUserService) {
        this.acgnUserDataService = acgnUserDataService;
        this.acgnUserService = acgnUserService;
    }

    /** 首页：随机角色（原前端 GET /api/acgn/user） */
    @GetMapping("/api/acgn/user")
    public Result<AcgnUser> randomAcgnUser() {
        AcgnUser u = acgnUserService.selectOneRandom();
        if (u == null) {
            u = acgnUserEmpty("暂无角色数据");
        }
        return Result.success(u);
    }

    /** 角色详情：按 id 取单个角色（首页“了解更多”用） */
    @GetMapping("/api/acgn/user/byid")
    public Result<AcgnUser> acgnUserById(@RequestParam("id") int id) {
        if (id < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        AcgnUser u = acgnUserService.selectByRandom(id);
        if (u == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "角色不存在");
        return Result.success(u);
    }

    /** 首页：今日生日角色（原前端 GET /api/acgn/getbirth） */
    @GetMapping("/api/acgn/getbirth")
    public Result<AcgnUser> todayBirthdayUser() {
        LocalDate now = LocalDate.now();
        AcgnUser u = acgnUserService.selectBybirth(now.getMonthValue(), now.getDayOfMonth());
        if (u == null) {
            u = new AcgnUser();
            u.setName("今日暂无过生日角色");
            u.setAvatarurl("/enter.jpg");
        }
        return Result.success(u);
    }

    private static AcgnUser acgnUserEmpty(String name) {
        AcgnUser u = new AcgnUser();
        u.setName(name);
        u.setSex("");
        u.setBirthday("");
        u.setTalent("");
        u.setAvatarurl("/enter.jpg");
        return u;
    }

    @GetMapping("/api/acgn/user/state")
    public Result<AcgnUserBundleVo> state(@RequestHeader(value = "X-Acgn-Client", required = false) String clientId) {
        return Result.success(acgnUserDataService.getBundle(clientId));
    }

    @PostMapping("/api/acgn/user/library/visit")
    public Result<Void> visit(
            @RequestHeader(value = "X-Acgn-Client", required = false) String clientId,
            @RequestBody AcgnMediaTouchDto body) {
        try {
            acgnUserDataService.recordVisit(clientId, body);
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/api/acgn/user/library/favorite-toggle")
    public Result<java.util.Map<String, Boolean>> favoriteToggle(
            @RequestHeader(value = "X-Acgn-Client", required = false) String clientId,
            @RequestBody AcgnMediaTouchDto body) {
        try {
            boolean on = acgnUserDataService.toggleFavorite(clientId, body);
            return Result.success(java.util.Map.of("starred", on));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/api/acgn/user/subscribe/toggle")
    public Result<java.util.Map<String, Boolean>> subscribeToggle(
            @RequestHeader(value = "X-Acgn-Client", required = false) String clientId,
            @RequestBody AcgnMediaTouchDto body) {
        try {
            boolean on = acgnUserDataService.toggleSubscribe(clientId, body);
            return Result.success(java.util.Map.of("subscribed", on));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/api/acgn/user/progress")
    public Result<Void> progress(
            @RequestHeader(value = "X-Acgn-Client", required = false) String clientId,
            @RequestBody AcgnProgressPostDto body) {
        try {
            int mid = body.getMediaId() == null ? -1 : body.getMediaId();
            acgnUserDataService.saveProgress(clientId, body.getMediaType(), mid, body.getPayload());
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/api/acgn/user/import-legacy")
    public Result<Void> importLegacy(
            @RequestHeader(value = "X-Acgn-Client", required = false) String clientId,
            @RequestBody List<AcgnLibraryItemVo> rows) {
        try {
            acgnUserDataService.importLegacyRows(clientId, rows == null ? List.of() : rows);
            return Result.success();
        } catch (Exception e) {
            log.warn("importLegacy: {}", e.getMessage());
            return Result.error("导入失败");
        }
    }
}
