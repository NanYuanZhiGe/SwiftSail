package com.nyzg.dock.conf;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;


@Configuration
@Data
@ConfigurationProperties("spring.cloud.nacos.config")//固定字段，用于获取nacos的配置
public class RedissonConf {
    String serverAddr;
    String username;
    String password;
    String namespace;
    String group;

    @Value("${proj.redisson.data-id:redisson.yaml}")
    String dataId;//redisson配置的名称

    @Bean
    public RedissonClient obtainRedissonClient() {
        try {
            Properties properties = new Properties();
            properties.put("serverAddr", serverAddr);
            properties.put("username", username);
            properties.put("password", password);
            properties.put("namespace", namespace);
            ConfigService configService = NacosFactory.createConfigService(properties);
            //从nacos对应的组中读取配置
            String nacosRedissonConfig = configService.getConfig(dataId, group, 5000).replace("'", "");
            Config config = Config.fromYAML(nacosRedissonConfig);
            //使用主从配置
            config.useSingleServer();
            return Redisson.create(config);
        } catch (Exception e) {
            throw new RuntimeException("无法创建redis客户端：" + e);
        }
    }
}
