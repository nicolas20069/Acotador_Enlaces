package com.example.demo.infrastructure.factory;

import com.example.demo.application.ports.ShortUrlCache;
import com.example.demo.application.ports.ShortUrlInfrastructureFactory;
import com.example.demo.application.ports.ShortUrlRepository;
import com.example.demo.infrastructure.cache.noop.NoCacheLinkCache;
import com.example.demo.infrastructure.persistence.mongo.MongoShortUrlRepository;
import org.springframework.data.mongodb.core.MongoTemplate;

// Familia alternativa demostrativa; NO usar para evaluar el requisito Redis/WebSocket.
public class MongoOnlyFactory implements ShortUrlInfrastructureFactory {
    private final MongoTemplate mongo;
    public MongoOnlyFactory(MongoTemplate mongo) { this.mongo = mongo; }
    @Override public ShortUrlRepository createRepository() { return new MongoShortUrlRepository(mongo); }
    @Override public ShortUrlCache createCache() { return new NoCacheLinkCache(); }
}
