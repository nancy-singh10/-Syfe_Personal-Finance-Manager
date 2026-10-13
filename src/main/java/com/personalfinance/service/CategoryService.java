package com.personalfinance.service;

import com.personalfinance.dto.category.CategoryListResponse;
import com.personalfinance.dto.category.CategoryRequest;
import com.personalfinance.dto.category.CategoryResponse;
import com.personalfinance.dto.common.MessageResponse;
import com.personalfinance.entity.Category;
import com.personalfinance.entity.User;
import com.personalfinance.exception.BadRequestException;
import com.personalfinance.exception.ConflictException;
import com.personalfinance.exception.ForbiddenException;
import com.personalfinance.exception.ResourceNotFoundException;
import com.personalfinance.repository.CategoryRepository;
import com.personalfinance.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    public CategoryService(CategoryRepository categoryRepository, TransactionRepository transactionRepository) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public CategoryListResponse getAllCategories(User user) {
        List<Category> allCategories = categoryRepository.findByUserIsNullOrUser(user);
        List<CategoryResponse> responses = new ArrayList<>();
        for (Category cat : allCategories) {
            responses.add(new CategoryResponse(cat.getName(), cat.getType(), cat.isCustom()));
        }
        return new CategoryListResponse(responses);
    }

    @Transactional
    public CategoryResponse createCustomCategory(CategoryRequest request, User user) {
        String trimmedName = request.getName().trim();
        String type = request.getType().toUpperCase().trim();

        // Check if user already has custom category with this name (case-insensitive)
        if (categoryRepository.existsByNameIgnoreCaseAndUser(trimmedName, user)) {
            throw new ConflictException("Category with name '" + trimmedName + "' already exists for this user");
        }

        // Check if there is already a default category with this name
        if (categoryRepository.existsByNameIgnoreCaseAndUserIsNull(trimmedName)) {
            throw new ConflictException("A default category with name '" + trimmedName + "' already exists");
        }

        Category category = new Category(trimmedName, type, true, user);
        Category saved = categoryRepository.save(category);

        return new CategoryResponse(saved.getName(), saved.getType(), true);
    }

    @Transactional
    public MessageResponse deleteCategory(String name, User user) {
        String trimmedName = name.trim();

        // Check if it matches a default category
        Optional<Category> defaultCat = categoryRepository.findByNameIgnoreCaseAndUserIsNull(trimmedName);
        if (defaultCat.isPresent()) {
            throw new ForbiddenException("Default categories cannot be deleted");
        }

        // Find user custom category
        Category customCat = categoryRepository.findByNameIgnoreCaseAndUser(trimmedName, user)
                .orElseThrow(() -> new ResourceNotFoundException("Category '" + trimmedName + "' not found"));

        // Check if referenced by any transactions of this user
        if (transactionRepository.existsByCategoryIgnoreCaseAndUser(customCat.getName(), user)) {
            throw new BadRequestException("Category '" + customCat.getName() + "' is currently referenced by transactions and cannot be deleted");
        }

        categoryRepository.delete(customCat);
        return new MessageResponse("Category deleted successfully");
    }

    @Transactional(readOnly = true)
    public Category findAccessibleCategoryByName(String name, User user) {
        String trimmed = name.trim();
        // Check default category first
        Optional<Category> defaultCat = categoryRepository.findByNameIgnoreCaseAndUserIsNull(trimmed);
        if (defaultCat.isPresent()) {
            return defaultCat.get();
        }

        // Check user custom category
        return categoryRepository.findByNameIgnoreCaseAndUser(trimmed, user)
                .orElseThrow(() -> new BadRequestException("Category '" + name + "' is invalid or not accessible"));
    }
}
