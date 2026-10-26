package com.example.demo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@Component
public class AcgnSocialMysqlSchemaInitializer implements ApplicationRunner {

    private static final String DDL_VOTE =
            """
                    CREATE TABLE IF NOT EXISTS `tb_acgn_vote` (
                      `client_id` VARCHAR(64) NOT NULL,
                      `media_type` VARCHAR(16) NOT NULL COMMENT 'animation|comic|novel',
                      `media_id` INT NOT NULL,
                      `vote` TINYINT NOT NULL COMMENT '1 up, -1 down',
                      `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                      PRIMARY KEY (`client_id`, `media_type`, `media_id`),
                      KEY `idx_media` (`media_type`, `media_id`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """;

    private static final String DDL_COMMENT =
            """
                    CREATE TABLE IF NOT EXISTS `tb_acgn_comment` (
                      `id` BIGINT NOT NULL AUTO_INCREMENT,
                      `media_type` VARCHAR(16) NOT NULL,
                      `media_id` INT NOT NULL,
                      `nickname` VARCHAR(64) NOT NULL DEFAULT 'visitor',
                      `avatar` VARCHAR(1024) NOT NULL DEFAULT '',
                      `body` VARCHAR(900) NOT NULL,
                      `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      PRIMARY KEY (`id`),
                      KEY `idx_media_time` (`media_type`, `media_id`, `id` DESC)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """;

    private final DataSource mysqlDataSource;

    public AcgnSocialMysqlSchemaInitializer(@Qualifier("mysqlDataSource") DataSource mysqlDataSource) {
        this.mysqlDataSource = mysqlDataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (Connection c = mysqlDataSource.getConnection();
                Statement st = c.createStatement()) {
            st.execute(DDL_VOTE);
            st.execute(DDL_COMMENT);
            ensureAvatarColumn(c, st);
            log.info("ACGN social tables ready: tb_acgn_vote, tb_acgn_comment");
        } catch (SQLException e) {
            log.warn("ACGN social schema init failed: {}", e.getMessage());
        }
    }

    private void ensureAvatarColumn(Connection c, Statement st) throws SQLException {
        DatabaseMetaData meta = c.getMetaData();
        try (ResultSet rs = meta.getColumns(c.getCatalog(), null, "tb_acgn_comment", "avatar")) {
            if (rs.next()) {
                return;
            }
        }
        st.execute("ALTER TABLE `tb_acgn_comment` ADD COLUMN `avatar` VARCHAR(1024) NOT NULL DEFAULT '' AFTER `nickname`");
    }
}
