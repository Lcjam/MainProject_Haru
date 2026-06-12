package com.example.demo.config;

import com.example.demo.listener.NotificationSubscriber;
import com.example.demo.listener.RedisMessageListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import java.time.Duration;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;
    
    @Value("${spring.data.redis.password:}")
    private String redisPassword;
    
    @Value("${spring.data.redis.timeout:2000}")
    private long timeout;
    
    @Value("${chat.redis.topic.name:chat}")
    private String chatTopicName;

    @Value("${notification.redis.topic.name:notification}")
    private String notificationTopicName;

    /**
     * Redis ConnectionFactory 설정
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);
        
        // 비밀번호가 설정된 경우 사용
        if (redisPassword != null && !redisPassword.isEmpty()) {
            redisConfig.setPassword(redisPassword);
        }
        
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(timeout))
                .build();
        
        return new LettuceConnectionFactory(redisConfig, clientConfig);
    }

    /**
     * RedisTemplate 설정
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    /**
     * Redis Message Listener 설정
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter chatListenerAdapter,
            NotificationSubscriber notificationSubscriber) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // 채팅 메시지 리스너
        container.addMessageListener(chatListenerAdapter, chatChannelTopic());

        // 알림 메시지 리스너
        container.addMessageListener(notificationSubscriber, notificationChannelTopic());

        return container;
    }

    /**
     * Redis 메시지 리스너 어댑터 (채팅)
     */
    @Bean
    public MessageListenerAdapter chatListenerAdapter(RedisMessageListener redisMessageListener) {
        return new MessageListenerAdapter(redisMessageListener, "onMessage");
    }

    /**
     * Redis 채널 설정 (채팅)
     */
    @Bean
    public ChannelTopic chatChannelTopic() {
        return new ChannelTopic(chatTopicName);
    }

    /**
     * Redis 채널 설정 (알림)
     */
    @Bean
    public ChannelTopic notificationChannelTopic() {
        return new ChannelTopic(notificationTopicName);
    }
}
