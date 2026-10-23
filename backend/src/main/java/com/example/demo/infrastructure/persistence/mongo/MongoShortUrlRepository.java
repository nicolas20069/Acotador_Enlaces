package com.example.demo.infrastructure.persistence.mongo;

import com.example.demo.application.ports.ShortUrlRepository;
import com.example.demo.domain.model.ShortLink;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public class MongoShortUrlRepository implements ShortUrlRepository {
    private final MongoTemplate mongo;
    public MongoShortUrlRepository(MongoTemplate mongo) { this.mongo = mongo; }

    @Override
    public ShortLink save(ShortLink link) {
        ShortUrlSequenceDocument counter = mongo.findAndModify(
                Query.query(Criteria.where("_id").is("links")),
                new Update().inc("value", 1),
                FindAndModifyOptions.options().upsert(true).returnNew(true),
                ShortUrlSequenceDocument.class
        );
        if (counter == null) throw new IllegalStateException("No fue posible generar el identificador");
        MongoShortUrlDocument saved = mongo.save(new MongoShortUrlDocument(
                counter.getValue(), link.originalUrl(), link.shortCode(), link.createdAt()));
        return toDomain(saved);
    }

    @Override
    public List<ShortLink> findAll() {
        Query query = new Query().with(Sort.by(Sort.Direction.ASC, "id"));
        return mongo.find(query, MongoShortUrlDocument.class).stream().map(MongoShortUrlRepository::toDomain).toList();
    }

    @Override
    public Optional<ShortLink> findByCode(String code) {
        MongoShortUrlDocument doc = mongo.findOne(Query.query(Criteria.where("shortCode").is(code)), MongoShortUrlDocument.class);
        return Optional.ofNullable(doc).map(MongoShortUrlRepository::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return mongo.exists(Query.query(Criteria.where("shortCode").is(code)), MongoShortUrlDocument.class);
    }

    @Override
    public boolean deleteById(Long id) {
        Query query = Query.query(Criteria.where("id").is(id));
        return mongo.remove(query, MongoShortUrlDocument.class).getDeletedCount() > 0;
    }

    private static ShortLink toDomain(MongoShortUrlDocument doc) {
        return new ShortLink(doc.getId(), doc.getOriginalUrl(), doc.getShortCode(), doc.getCreatedAt());
    }
}
