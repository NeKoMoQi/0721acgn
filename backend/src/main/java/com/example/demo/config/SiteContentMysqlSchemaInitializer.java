package com.example.demo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
@Component
public class SiteContentMysqlSchemaInitializer implements ApplicationRunner {
    private final DataSource mysql;

    public SiteContentMysqlSchemaInitializer(@Qualifier("mysqlDataSource") DataSource mysql) {
        this.mysql = mysql;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (var connection = mysql.getConnection(); var statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE IF NOT EXISTS tb_activity_daily_views (
                    activity_id INT NOT NULL,
                    view_date DATE NOT NULL,
                    views INT NOT NULL DEFAULT 0,
                    PRIMARY KEY (activity_id, view_date),
                    KEY date_activity (view_date, activity_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS tb_media_report (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    media_type VARCHAR(16) NOT NULL,
                    media_id INT NOT NULL,
                    position_label VARCHAR(80) NOT NULL,
                    reason VARCHAR(24) NOT NULL,
                    detail VARCHAR(500) NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
            ensureLookupIndex(connection, statement, "tb_cartoonchapters", "comicid");
            ensureLookupIndex(connection, statement, "tb_cartoonpage", "chapterid");
            ensureLookupIndex(connection, statement, "tb_novelchapter", "novelid");
        } catch (SQLException error) {
            log.warn("Site content schema initialization failed", error);
        }
    }

    private void ensureLookupIndex(Connection connection, Statement statement, String table, String column)
            throws SQLException {
        try (var indexes = connection.getMetaData().getIndexInfo(connection.getCatalog(), null, table, false, false)) {
            while (indexes.next()) {
                if (column.equalsIgnoreCase(indexes.getString("COLUMN_NAME"))
                        && indexes.getInt("ORDINAL_POSITION") == 1) return;
            }
        }
        statement.execute("CREATE INDEX idx_site_" + column + " ON " + table + " (" + column + ")");
    }
}
