package com.example.demo.application.ports;

import com.example.demo.domain.model.ShortLink;
import java.util.List;
import java.util.Optional;

public interface ShortUrlRepository {
    ShortLink save(ShortLink link);
    List<ShortLink> findAll();
    Optional<ShortLink> findByCode(String code);
    boolean existsByCode(String code);
    boolean deleteById(Long id);
}
