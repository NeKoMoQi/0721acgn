package com.example.demo.controller;

import com.example.demo.com.Result;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.util.Set;

@RestController
public class MediaReportController {
    private static final Set<String> TYPES = Set.of("animation", "comic", "novel");
    private static final Set<String> REASONS = Set.of("unavailable", "missing", "incorrect");
    private final JdbcTemplate mysql;

    public MediaReportController(@Qualifier("mysqlDataSource") DataSource mysql) {
        this.mysql = new JdbcTemplate(mysql);
    }

    @PostMapping("/api/media/report")
    public Result report(@RequestBody Report report) {
        if (report.mediaType() == null || !TYPES.contains(report.mediaType()) || report.mediaId() < 1
                || report.reason() == null || !REASONS.contains(report.reason())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的资源报错");
        }
        String position = checkedText(report.position(), 80);
        String detail = checkedText(report.detail(), 500);
        mysql.update(
                "INSERT INTO tb_media_report (media_type, media_id, position_label, reason, detail) VALUES (?, ?, ?, ?, ?)",
                report.mediaType(), report.mediaId(), position, report.reason(), detail);
        return Result.success();
    }

    private static String checkedText(String value, int limit) {
        if (value == null) return "";
        value = value.strip();
        if (value.length() > limit) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "报错内容过长");
        return value;
    }

    public record Report(String mediaType, int mediaId, String position, String reason, String detail) {}
}
