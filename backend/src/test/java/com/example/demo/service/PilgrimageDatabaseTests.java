package com.example.demo.service;

import com.example.demo.mapper.postgresql.PilgrimageAgentMapper;
import com.example.demo.service.agent.PilgrimageDataStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in integration test against an isolated PostgreSQL, never a production database. */
@EnabledIfSystemProperty(named = "pilgrimage.test.jdbc", matches = ".+")
class PilgrimageDatabaseTests {
    @Test void migrationMapperOwnershipAndVersionChecksWorkOnPostgres() throws Exception {
        String url = System.getProperty("pilgrimage.test.jdbc");
        String user = System.getProperty("pilgrimage.test.user", "postgres");
        String password = System.getProperty("pilgrimage.test.password", "");
        var datasource = new UnpooledDataSource("org.postgresql.Driver", url, user, password);
        String migration = Files.readString(Path.of("sql/pilgrimage_agent_v2.sql"));
        try (var connection = datasource.getConnection(); var statement = connection.createStatement()) {
            statement.execute(migration);
            statement.execute(migration); // Reapplying must preserve existing rows and seeds.
        }
        var config = new Configuration(new Environment("pilgrimage-test", new JdbcTransactionFactory(), datasource));
        config.addMapper(PilgrimageAgentMapper.class);
        try (var session = new SqlSessionFactoryBuilder().build(config).openSession(true)) {
            var mapper = session.getMapper(PilgrimageAgentMapper.class);
            var store = new PilgrimageDataStore(mapper);
            var json = new ObjectMapper();
            String owner = "user:test-" + UUID.randomUUID();
            var plan = json.readTree("{\"version\":2,\"days\":[{\"pointIds\":[\"1\"]}]}");
            String id = store.save(owner, "宇治一日", plan);
            assertEquals(id, store.save(owner, "重命名行程", plan));
            assertEquals(1, store.routes(owner).size());
            assertEquals("重命名行程", store.route(id, owner).get("title"));
            assertNull(store.route(id, "user:other"));
            assertFalse(store.delete(id, "user:other"));
            assertFalse(store.aliases().isEmpty());
            assertNotNull(store.spotInfo());
            String conversation = UUID.randomUUID().toString();
            var payload = json.readTree("{\"history\":[{\"role\":\"user\",\"content\":\"你好\"}],\"plan\":null}");
            assertEquals(1, store.saveSession(conversation, owner, "chat", payload, 0));
            assertEquals(1L, ((Number) store.session(conversation, owner, "chat").get("version")).longValue());
            assertNull(store.session(conversation, "user:other", "chat"));
            assertNull(store.session(conversation, owner, "route"));
            assertEquals(2, store.saveSession(conversation, owner, "chat", payload, 1));
            assertThrows(IllegalStateException.class, () -> store.saveSession(conversation, owner, "chat", payload, 1));
            assertTrue(store.delete(id, owner));
            assertTrue(store.routes(owner).isEmpty());
        }
    }
}
