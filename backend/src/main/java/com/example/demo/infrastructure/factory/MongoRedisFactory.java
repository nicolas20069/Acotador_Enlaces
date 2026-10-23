package com.example.demo.infrastructure.factory;

import com.example.demo.application.ports.ShortUrlCache;
import com.example.demo.application.ports.ShortUrlInfrastructureFactory;
import com.example.demo.application.ports.ShortUrlRepository;
import com.example.demo.infrastructure.cache.redis.RedisLinkCache;
import com.example.demo.infrastructure.persistence.mongo.MongoShortUrlRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

// Familia principal: MongoDB para datos persistentes + Redis para caché.
public class MongoRedisFactory implements ShortUrlInfrastructureFactory {
    private final MongoTemplate mongo;
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public MongoRedisFactory(MongoTemplate mongo, StringRedisTemplate redis, ObjectMapper mapper) {
        this.mongo = mongo;
        this.redis = redis;
        this.mapper = mapper;
    }

    @Override public ShortUrlRepository createRepository() { return new MongoShortUrlRepository(mongo); }
    @Override public ShortUrlCache createCache() { return new RedisLinkCache(redis, mapper); }
}
