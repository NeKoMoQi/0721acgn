package com.example.demo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 启动时确保 ACGN 用户态表存在（避免手工漏跑 sql/acgn_user_library_mysql.sql）。
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class AcgnUserMysqlSchemaInitializer implements ApplicationRunner {

    private static final String DDL_LIBRARY =
            """
                    CREATE TABLE IF NOT EXISTS `tb_acgn_library` (
                      `client_id` VARCHAR(64) NOT NULL,
                      `media_type` VARCHAR(16) NOT NULL COMMENT 'animation|comic|novel',
                      `media_id` INT NOT NULL,
                      `title` VARCHAR(512) DEFAULT NULL,
                      `pic` VARCHAR(1024) DEFAULT NULL,
                      `starred` TINYINT NOT NULL DEFAULT 0,
                      `starred_at` BIGINT DEFAULT NULL COMMENT 'epoch ms',
                      `last_visit_at` BIGINT NOT NULL COMMENT 'epoch ms',
                      PRIMARY KEY (`client_id`, `media_type`, `media_id`),
                      KEY `idx_cli_visit` (`client_id`, `last_visit_at`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """;

    private static final String DDL_SUBSCRIBE =
            """
                    CREATE TABLE IF NOT EXISTS `tb_acgn_subscribe` (
                      `client_id` VARCHAR(64) NOT NULL,
                      `media_type` VARCHAR(16) NOT NULL,
                      `media_id` INT NOT NULL,
                      `title` VARCHAR(512) DEFAULT NULL,
                      `pic` VARCHAR(1024) DEFAULT NULL,
                      `subscribed_at` BIGINT NOT NULL,
                      PRIMARY KEY (`client_id`, `media_type`, `media_id`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """;

    private static final String DDL_PROGRESS =
            """
                    CREATE TABLE IF NOT EXISTS `tb_acgn_progress` (
                      `client_id` VARCHAR(64) NOT NULL,
                      `media_type` VARCHAR(16) NOT NULL,
                      `media_id` INT NOT NULL,
                      `payload_json` MEDIUMTEXT NOT NULL,
                      `updated_at` BIGINT NOT NULL,
                      PRIMARY KEY (`client_id`, `media_type`, `media_id`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """;

    private final DataSource mysqlDataSource;

    public AcgnUserMysqlSchemaInitializer(@Qualifier("mysqlDataSource") DataSource mysqlDataSource) {
        this.mysqlDataSource = mysqlDataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (Connection c = mysqlDataSource.getConnection();
                Statement st = c.createStatement()) {
            st.execute(DDL_LIBRARY);
            st.execute(DDL_SUBSCRIBE);
            st.execute(DDL_PROGRESS);
            log.info("ACGN user tables ready: tb_acgn_library, tb_acgn_subscribe, tb_acgn_progress");
        } catch (SQLException e) {
            log.warn("ACGN user schema init failed (check DB权限与库名): {}", e.getMessage());
        }
    }
}
