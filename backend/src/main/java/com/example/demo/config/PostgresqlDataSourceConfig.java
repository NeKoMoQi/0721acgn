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
@MapperScan(basePackages = PostgresqlDataSourceConfig.PACKAGE,
        sqlSessionFactoryRef = PostgresqlDataSourceConfig.SQL_SESSION_FACTORY
)
public class PostgresqlDataSourceConfig {

    /** 指定 Sql Session Factory 的 Bean 名称 */
    static final String SQL_SESSION_FACTORY = "postgresqlSqlSessionFactory";
    /** 指定 Mapper 类的包路径 */
    static final String PACKAGE = "com.example.demo.mapper.postgresql";
    /** 指定数据库 Mapper 对应的 xml 文件路径 */
    static final String MAPPER = "classpath:com/example/demo/mapper/postgresql/*.xml";

    /**
     * 配置数据源，这里设置为 hikari 数据库连接池
     * @return DataSource
     */
    @Bean(name = "postgresqlDataSource")
    @ConfigurationProperties("spring.datasource.postgresql")
    public DataSource dataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    /**
     * 数据源事务管理器
     * @return 数据源事务管理器
     */
    @Bean(name = "PostgresqlTransactionManager") // 统一命名为 postgresqlTransactionManager
    public DataSourceTransactionManager dataSourceTransactionManager(
            @Qualifier("postgresqlDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean(name = SQL_SESSION_FACTORY)
    public SqlSessionFactory postgresqlSqlSessionFactory(@Qualifier("postgresqlDataSource") DataSource masterDataSource) throws Exception {
        final SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(masterDataSource);
        sessionFactory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources(PostgresqlDataSourceConfig.MAPPER));
        return sessionFactory.getObject();
    }

}
