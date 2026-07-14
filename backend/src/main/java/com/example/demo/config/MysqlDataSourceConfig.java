package com.example.demo.config;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

@Configuration
@MapperScan(
        basePackages = MysqlDataSourceConfig.PACKAGE,  // 指定 Mapper 接口的包路径
        sqlSessionFactoryRef = MysqlDataSourceConfig.SQL_SESSION_FACTORY  // 指定 SqlSessionFactory 的 Bean 名称
)
public class MysqlDataSourceConfig {

    /** 定义常量 */
    static final String SQL_SESSION_FACTORY = "mysqlSqlSessionFactory"; // SqlSessionFactory 的 Bean 名称
    static final String PACKAGE = "com.example.demo.mapper.mysql";         // Mapper 接口的包路径
    static final String MAPPER_LOCATION = "classpath:com/example/demo/mapper/mysql/*.xml"; // Mapper XML 文件路径

    /**
     * 主数据源（MySQL）
     */
    @Primary
    @Bean(name = "mysqlDataSource")
    @ConfigurationProperties("spring.datasource.mysql") // 对应 application.properties 中的前缀
    public DataSource mysqlDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    /**
     * MySQL 事务管理器
     */
    @Primary
    @Bean(name = "mysqlTransactionManager")
    public DataSourceTransactionManager mysqlTransactionManager(
            @Qualifier("mysqlDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    /**
     * MySQL 的 SqlSessionFactory
     */
    @Primary
    @Bean(name = SQL_SESSION_FACTORY)
    public SqlSessionFactory mysqlSqlSessionFactory(
            @Qualifier("mysqlDataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        sessionFactory.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources(MAPPER_LOCATION)
        );
        return sessionFactory.getObject();
    }
}