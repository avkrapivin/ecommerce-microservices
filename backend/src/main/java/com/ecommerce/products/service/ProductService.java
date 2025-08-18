package com.ecommerce.products.service;

import com.ecommerce.products.dto.*;
import com.ecommerce.products.entity.Product;
import com.ecommerce.products.entity.ProductImage;
import com.ecommerce.products.entity.ProductReview;
import com.ecommerce.products.entity.ProductSpecification;
import com.ecommerce.products.repository.ProductRepository;
import com.ecommerce.products.repository.CategoryRepository;
import com.ecommerce.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#id")
    public ProductDto getProductById(Long id) {
        return productRepository.findById(id)
                .map(this::convertToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "products")
    public List<ProductDto> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#categoryId")
    public List<ProductDto> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#name")
    public List<ProductDto> searchProducts(String name) {
        return productRepository.findByNameContainingIgnoreCase(name).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#filter.toString()")
    public Page<ProductDto> getProducts(ProductFilterDto filter) {
        // Создаем спецификацию для фильтрации
        com.ecommerce.products.specification.ProductSpecification specification = new com.ecommerce.products.specification.ProductSpecification(filter);

        // Создаем сортировку
        Sort sort = createSort(filter.getSortBy(), filter.getSortDirection());

        // Создаем пагинацию
        Pageable pageable = PageRequest.of(
            filter.getPage() != null ? filter.getPage() : 0,
            filter.getSize() != null ? filter.getSize() : 10,
            sort
        );

        // Получаем отфильтрованные и отсортированные продукты
        Page<Product> products = productRepository.findAll(specification, pageable);

        // Преобразуем в DTO
        return products.map(this::convertToDto);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductDto createProduct(BaseProductDto createProductDto) {
        Product product = new Product();
        updateProductFromDto(product, createProductDto);
        return convertToDto(productRepository.save(product));
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductDto updateProduct(Long id, BaseProductDto updateProductDto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        
        updateProductFromDto(product, updateProductDto);
        return convertToDto(productRepository.save(product));
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        productRepository.delete(product);
    }

    @Transactional(readOnly = true)
    public Product getProductEntityById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    public ProductDto convertToDto(Product product) {
        ProductDto dto = new ProductDto();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setStockQuantity(product.getStockQuantity());
        dto.setCategoryId(product.getCategory().getId());
        dto.setCategoryName(product.getCategory().getName());
        dto.setSku(product.getSku());
        dto.setActive(product.isActive());
        dto.setCreatedAt(product.getCreatedAt().toString());
        dto.setUpdatedAt(product.getUpdatedAt().toString());
        
        // Convert images
        if (product.getImages() != null) {
            List<ProductImageDto> imageDtos = product.getImages().stream()
                .map(this::convertImageToDto)
                .collect(Collectors.toList());
            dto.setImages(imageDtos);
        }
        
        // Convert reviews
        if (product.getReviews() != null) {
            List<ProductReviewDto> reviewDtos = product.getReviews().stream()
                .map(this::convertReviewToDto)
                .collect(Collectors.toList());
            dto.setReviews(reviewDtos);
        }
        
        // Convert specifications
        if (product.getSpecifications() != null) {
            List<ProductSpecificationDto> specDtos = product.getSpecifications().stream()
                .map(this::convertSpecificationToDto)
                .collect(Collectors.toList());
            dto.setSpecifications(specDtos);
        }
        
        return dto;
    }

    private void updateProductFromDto(Product product, BaseProductDto dto) {
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStockQuantity(dto.getStockQuantity());
        product.setCategory(categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + dto.getCategoryId())));
        product.setSku(dto.getSku());
        
        // Если active задано явно - используем его, иначе true (для create операций)
        product.setActive(dto.getActive() != null ? dto.getActive() : true);
    }

    private Sort createSort(String sortBy, String sortDirection) {
        if (sortBy == null) {
            return Sort.unsorted();
        }

        Sort.Direction direction = Sort.Direction.ASC;
        if (sortDirection != null && sortDirection.equalsIgnoreCase("desc")) {
            direction = Sort.Direction.DESC;
        }

        return Sort.by(direction, sortBy);
    }
    
    private ProductImageDto convertImageToDto(ProductImage image) {
        ProductImageDto dto = new ProductImageDto();
        dto.setId(image.getId());
        dto.setProductId(image.getProduct().getId());
        dto.setImageUrl(image.getImageUrl());
        // Optional metadata may be null in current schema; skip if absent
        if (image.getFileName() != null) dto.setFileName(image.getFileName());
        if (image.getFileType() != null) dto.setFileType(image.getFileType());
        if (image.getFileSize() != null) dto.setFileSize(image.getFileSize());
        dto.setMain(image.isMain());
        dto.setCreatedAt(image.getCreatedAt().toString());
        dto.setUpdatedAt(image.getUpdatedAt().toString());
        return dto;
    }
    
    private ProductReviewDto convertReviewToDto(ProductReview review) {
        ProductReviewDto dto = new ProductReviewDto();
        dto.setId(review.getId());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        if (review.getUser() != null) {
            dto.setUserId(review.getUser().getId());
            String fullName = (review.getUser().getFirstName() != null ? review.getUser().getFirstName() : "")
                + (review.getUser().getLastName() != null ? " " + review.getUser().getLastName() : "");
            dto.setUserName(fullName.trim().isEmpty() ? review.getUser().getEmail() : fullName.trim());
        }
        dto.setCreatedAt(review.getCreatedAt());
        return dto;
    }
    
    private ProductSpecificationDto convertSpecificationToDto(ProductSpecification spec) {
        ProductSpecificationDto dto = new ProductSpecificationDto();
        dto.setId(spec.getId());
        dto.setProductId(spec.getProduct().getId());
        dto.setName(spec.getName());
        dto.setSpecValue(spec.getSpecValue());
        dto.setCreatedAt(spec.getCreatedAt());
        dto.setUpdatedAt(spec.getUpdatedAt());
        return dto;
    }
} 