package com.example.demo.integration.support;

import com.haru.migration.R03MigrationBootstrap;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** An isolated schema created only by this test process and dropped only by {@link #close()}. */
public final class MySqlTestDatabase implements AutoCloseable {
    private static final Set<String> OWNED_DATABASES = ConcurrentHashMap.newKeySet();
    private static final String URL = required("HARU_TEST_MYSQL_URL");
    private static final String USER = required("HARU_TEST_MYSQL_USER");
    private static final String PASSWORD = requiredPresent("HARU_TEST_MYSQL_PASSWORD");

    private final String name;
    private final DataSource dataSource;
    private final JdbcTemplate jdbc;
    private final SqlSessionTemplate sqlSession;
    private final TransactionTemplate transactions;
    private boolean closed;

    private MySqlTestDatabase(String name) throws Exception {
        this.name = name;
        R03MigrationBootstrap.initialize(URL, name, USER, PASSWORD);
        DriverManagerDataSource source = new DriverManagerDataSource(databaseUrl(name), USER, PASSWORD);
        source.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource = source;
        jdbc = new JdbcTemplate(source);
        sqlSession = new SqlSessionTemplate(sqlSessionFactory(source));
        transactions = new TransactionTemplate(new DataSourceTransactionManager(source));
    }

    public static MySqlTestDatabase create() throws Exception {
        String name = "haru_r03_" + UUID.randomUUID().toString().replace("-", "");
        R03MigrationBootstrap.validateServerUrl(URL);
        validateOwnedName(name);
        assertDatabaseAbsent(name);
        OWNED_DATABASES.add(name);
        try {
            return new MySqlTestDatabase(name);
        } catch (Exception exception) {
            try {
                cleanupOwnedDatabase(name);
            } catch (Exception cleanupFailure) {
                exception.addSuppressed(cleanupFailure);
            }
            throw exception;
        }
    }

    public DataSource dataSource() { return dataSource; }
    public JdbcTemplate jdbc() { return jdbc; }
    public SqlSessionTemplate sqlSession() { return sqlSession; }
    public TransactionTemplate transactions() { return transactions; }
    public String name() { return name; }

    public void seedFixture() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new PathMatchingResourcePatternResolver().getResource("classpath:integration/fixture.sql"));
        populator.setSqlScriptEncoding("UTF-8");
        populator.execute(dataSource);
    }

    @Override
    public void close() throws Exception {
        if (closed) return;
        cleanupOwnedDatabase(name);
        closed = true;
    }

    private static SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        Configuration configuration = new Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMappers("com.example.demo.mapper");
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setConfiguration(configuration);
        factory.setTypeAliasesPackage("com.example.demo.model");
        factory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/**/*.xml"));
        return factory.getObject();
    }

    private static void assertDatabaseAbsent(String name) throws Exception {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT 1 FROM information_schema.SCHEMATA WHERE SCHEMA_NAME=?")) {
            statement.setString(1, name);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) throw new IllegalStateException("Refusing to reuse existing test database " + name);
            }
        }
    }

    private static void cleanupOwnedDatabase(String name) throws Exception {
        validateOwnedName(name);
        if (!OWNED_DATABASES.contains(name)) {
            throw new IllegalStateException("Refusing to drop a database not created by this test process");
        }
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS `" + name + "`");
        }
        OWNED_DATABASES.remove(name);
    }

    private static void validateOwnedName(String name) {
        if (!name.matches("haru_r03_[0-9a-f]{32}")) {
            throw new IllegalArgumentException("Refusing a non-R03 test database name");
        }
    }

    private static String databaseUrl(String database) {
        int queryStart = URL.indexOf('?');
        return queryStart < 0 ? URL + database : URL.substring(0, queryStart) + database + URL.substring(queryStart);
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException(name + " must be explicitly configured");
        return value;
    }

    private static String requiredPresent(String name) {
        String value = System.getenv(name);
        if (value == null) throw new IllegalStateException(name + " must be explicitly configured (an empty value is allowed)");
        return value;
    }
}
