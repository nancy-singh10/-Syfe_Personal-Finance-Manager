package com.personalfinance.config;

import com.personalfinance.entity.Category;
import com.personalfinance.repository.CategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    public DataInitializer(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) {
        // Seed default categories if they don't already exist
        seedDefaultCategory("Salary", "INCOME");
        seedDefaultCategory("Food", "EXPENSE");
        seedDefaultCategory("Rent", "EXPENSE");
        seedDefaultCategory("Transportation", "EXPENSE");
        seedDefaultCategory("Entertainment", "EXPENSE");
        seedDefaultCategory("Healthcare", "EXPENSE");
        seedDefaultCategory("Utilities", "EXPENSE");
    }

    private void seedDefaultCategory(String name, String type) {
        if (!categoryRepository.existsByNameIgnoreCaseAndUserIsNull(name)) {
            Category category = new Category(name, type, false, null);
            categoryRepository.save(category);
        }
    }
}
