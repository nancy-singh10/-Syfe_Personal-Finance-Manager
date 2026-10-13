package com.personalfinance.controller;

import com.personalfinance.dto.category.CategoryListResponse;
import com.personalfinance.dto.category.CategoryRequest;
import com.personalfinance.dto.category.CategoryResponse;
import com.personalfinance.dto.common.MessageResponse;
import com.personalfinance.entity.User;
import com.personalfinance.service.AuthService;
import com.personalfinance.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final AuthService authService;

    public CategoryController(CategoryService categoryService, AuthService authService) {
        this.categoryService = categoryService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<CategoryListResponse> getAllCategories() {
        User user = authService.getCurrentAuthenticatedUser();
        CategoryListResponse response = categoryService.getAllCategories(user);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        User user = authService.getCurrentAuthenticatedUser();
        CategoryResponse response = categoryService.createCustomCategory(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<MessageResponse> deleteCategory(@PathVariable("name") String name) {
        User user = authService.getCurrentAuthenticatedUser();
        MessageResponse response = categoryService.deleteCategory(name, user);
        return ResponseEntity.ok(response);
    }
}
