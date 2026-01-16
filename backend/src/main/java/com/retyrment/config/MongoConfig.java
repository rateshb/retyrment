package com.retyrment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoAuditing
@EnableMongoRepositories(basePackages = "com.retyrment.repository")
public class MongoConfig {
    // MongoDB configuration is handled by Spring Boot auto-configuration
    // This class enables auditing for @CreatedDate and @LastModifiedDate
}
