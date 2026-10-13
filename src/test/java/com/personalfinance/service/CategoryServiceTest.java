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
import com.personalfinance.repository.CategoryRepository;
import com.personalfinance.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private CategoryService categoryService;

    private User user;
    private Category defaultCat;
    private Category customCat;

    @BeforeEach
    void setUp() {
        user = new User("user@example.com", "pass", "John Doe", "1234567890");
        user.setId(1L);

        defaultCat = new Category("Salary", "INCOME", false, null);
        defaultCat.setId(10L);

        customCat = new Category("Freelance", "INCOME", true, user);
        customCat.setId(20L);
    }

    @Test
    void testGetAllCategories() {
        when(categoryRepository.findByUserIsNullOrUser(user)).thenReturn(List.of(defaultCat, customCat));

        CategoryListResponse response = categoryService.getAllCategories(user);

        assertNotNull(response);
        assertEquals(2, response.getCategories().size());
        assertEquals("Salary", response.getCategories().get(0).getName());
        assertFalse(response.getCategories().get(0).getIsCustom());
        assertEquals("Freelance", response.getCategories().get(1).getName());
        assertTrue(response.getCategories().get(1).getIsCustom());
    }

    @Test
    void testCreateCustomCategorySuccess() {
        CategoryRequest req = new CategoryRequest("Freelance", "INCOME");

        when(categoryRepository.existsByNameIgnoreCaseAndUser("Freelance", user)).thenReturn(false);
        when(categoryRepository.existsByNameIgnoreCaseAndUserIsNull("Freelance")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(customCat);

        CategoryResponse res = categoryService.createCustomCategory(req, user);

        assertNotNull(res);
        assertEquals("Freelance", res.getName());
        assertEquals("INCOME", res.getType());
        assertTrue(res.getIsCustom());
    }

    @Test
    void testCreateCategoryDuplicateThrowsConflict() {
        CategoryRequest req = new CategoryRequest("Freelance", "INCOME");

        when(categoryRepository.existsByNameIgnoreCaseAndUser("Freelance", user)).thenReturn(true);

        assertThrows(ConflictException.class, () -> categoryService.createCustomCategory(req, user));
    }

    @Test
    void testDeleteDefaultCategoryThrowsForbidden() {
        when(categoryRepository.findByNameIgnoreCaseAndUserIsNull("Salary")).thenReturn(Optional.of(defaultCat));

        assertThrows(ForbiddenException.class, () -> categoryService.deleteCategory("Salary", user));
    }

    @Test
    void testDeleteCategoryReferencedByTransactionThrowsBadRequest() {
        when(categoryRepository.findByNameIgnoreCaseAndUserIsNull("Freelance")).thenReturn(Optional.empty());
        when(categoryRepository.findByNameIgnoreCaseAndUser("Freelance", user)).thenReturn(Optional.of(customCat));
        when(transactionRepository.existsByCategoryIgnoreCaseAndUser("Freelance", user)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> categoryService.deleteCategory("Freelance", user));
    }

    @Test
    void testDeleteCategorySuccess() {
        when(categoryRepository.findByNameIgnoreCaseAndUserIsNull("Freelance")).thenReturn(Optional.empty());
        when(categoryRepository.findByNameIgnoreCaseAndUser("Freelance", user)).thenReturn(Optional.of(customCat));
        when(transactionRepository.existsByCategoryIgnoreCaseAndUser("Freelance", user)).thenReturn(false);

        MessageResponse res = categoryService.deleteCategory("Freelance", user);

        assertEquals("Category deleted successfully", res.getMessage());
        verify(categoryRepository).delete(customCat);
    }
}
