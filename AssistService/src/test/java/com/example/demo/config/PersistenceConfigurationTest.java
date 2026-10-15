package com.example.demo.config;

import com.example.demo.dao.ChatMessageDAO;
import com.example.demo.model.ChatMessage;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PersistenceConfigurationTest {
    @Test
    void bootLoadsTheConfiguredMapperAndTransactionManagerWithoutCustomFactories() throws Exception {
        var properties = new ResourcePropertySource("classpath:application.properties");
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MybatisAutoConfiguration.class,
                        DataSourceTransactionManagerAutoConfiguration.class, TransactionAutoConfiguration.class))
                .withBean(DataSource.class, () -> mock(DataSource.class))
                .withInitializer(context -> {
                    AutoConfigurationPackages.register((BeanDefinitionRegistry) context.getBeanFactory(), "com.example.demo");
                    context.getEnvironment().getPropertySources().addLast(properties);
                })
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(ChatMessageDAO.class)
                            .hasSingleBean(SqlSessionTemplate.class).hasSingleBean(PlatformTransactionManager.class);
                    assertThat(context).hasBean("org.springframework.transaction.config.internalTransactionAdvisor");
                    var config = context.getBean(SqlSessionFactory.class).getConfiguration();
                    assertThat(config.isMapUnderscoreToCamelCase()).isTrue();
                    assertThat(config.getTypeAliasRegistry().resolveAlias("ChatMessage")).isEqualTo(ChatMessage.class);
                    String namespace = ChatMessageDAO.class.getName() + ".";
                    var select = config.getMappedStatement(namespace + "getMessagesBySessionId");
                    assertThat(select.getBoundSql(Map.of("sessionId", "user@example.test")).getSql())
                            .contains("WHERE session_id = ?", "ORDER BY created_at ASC");
                    assertThat(select.getResultMaps().get(0).getType()).isEqualTo(ChatMessage.class);
                    assertThat(config.getMappedStatement(namespace + "saveMessage").getKeyProperties()).containsExactly("id");
                });
    }
}
