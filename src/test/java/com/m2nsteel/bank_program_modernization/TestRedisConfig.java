package com.m2nsteel.bank_program_modernization;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.net.ServerSocket;

@TestConfiguration
public class TestRedisConfig {
    private static RedisServer redisServer;
    private static int redisPort;

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() throws IOException {
        if (redisServer == null) startRedis();
        return new LettuceConnectionFactory("127.0.0.1", redisPort);
    }

    private synchronized void startRedis() throws IOException {
        if (redisServer != null) return;
        try (ServerSocket socket = new ServerSocket(0)) {
            redisPort = socket.getLocalPort();
        }
        redisServer = new RedisServer(redisPort);
        redisServer.start();
        System.setProperty("spring.data.redis.port", String.valueOf(redisPort));
        System.out.println("🚀 Singleton Embedded Redis started on: " + redisPort);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (redisServer != null) {
                try {
                    redisServer.stop();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }));
    }
}