package com.khang.employee.repository;

import java.util.List;
import java.util.Optional;

public interface KhangCrudRepository<K, ID> {
    K save (K entity);
    Optional<K> findById(ID id);
    List<K> findAll();
    void deleteById(ID id);
    long count();
}
