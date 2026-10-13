package com.personalfinance.repository;

import com.personalfinance.entity.Category;
import com.personalfinance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUserIsNull();
    List<Category> findByUserIsNullOrUser(User user);
    Optional<Category> findByNameAndUser(String name, User user);
    Optional<Category> findByNameAndUserIsNull(String name);
    boolean existsByNameIgnoreCaseAndUser(String name, User user);
    boolean existsByNameIgnoreCaseAndUserIsNull(String name);
    Optional<Category> findByNameIgnoreCaseAndUser(String name, User user);
    Optional<Category> findByNameIgnoreCaseAndUserIsNull(String name);
}
