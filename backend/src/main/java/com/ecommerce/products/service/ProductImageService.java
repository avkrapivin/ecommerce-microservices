package com.ecommerce.products.service;

import com.ecommerce.products.dto.ProductImageDto;
import com.ecommerce.products.entity.Product;
import com.ecommerce.products.entity.ProductImage;
import com.ecommerce.products.repository.ProductImageRepository;
import com.ecommerce.products.repository.ProductRepository;
import com.ecommerce.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductImageService {
    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;
    private final S3Service s3Service;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @Transactional(readOnly = true)
    @Cacheable(value = "productImages", key = "'product:' + #productId")
    public List<ProductImageDto> getProductImages(Long productId) {
        return productImageRepository.findByProductId(productId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "productImages", key = "'product:' + #productId")
    public ProductImageDto uploadImage(Long productId, MultipartFile file, boolean isMain) throws IOException {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        // Загружаем файл в S3
        String fileName = s3Service.uploadFile(file);

        // Если это главное изображение, снимаем флаг с других изображений
        if (isMain) {
            productImageRepository.findByProductIdAndIsMain(productId, true)
                    .forEach(image -> image.setMain(false));
        }

        // Создаем запись в базе данных
        ProductImage image = new ProductImage();
        image.setProduct(product);
        image.setImageUrl(s3Service.getFileUrl(fileName));
        image.setFileName(fileName);
        image.setFileType(file.getContentType());
        image.setFileSize(file.getSize());
        image.setMain(isMain);

        return convertToDto(productImageRepository.save(image));
    }

    @Transactional
    @CacheEvict(value = "productImages", key = "'product:' + #productId")
    public void deleteImage(Long productId, Long imageId) throws IOException {
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found with id: " + imageId));

        if (!image.getProduct().getId().equals(productId)) {
            throw new ResourceNotFoundException("Image not found for product with id: " + productId);
        }

        // Удаляем файл из S3
        s3Service.deleteFile(image.getFileName());

        // Удаляем запись из базы данных
        productImageRepository.delete(image);
    }

    @Transactional
    @CacheEvict(value = "productImages", key = "'product:' + #productId")
    public ProductImageDto setMainImage(Long productId, Long imageId) {
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found with id: " + imageId));

        if (!image.getProduct().getId().equals(productId)) {
            throw new ResourceNotFoundException("Image not found for product with id: " + productId);
        }

        // Снимаем флаг с других изображений
        productImageRepository.findByProductIdAndIsMain(productId, true)
                .forEach(img -> img.setMain(false));

        // Устанавливаем флаг для выбранного изображения
        image.setMain(true);

        return convertToDto(productImageRepository.save(image));
    }

    private ProductImageDto convertToDto(ProductImage image) {
        ProductImageDto dto = new ProductImageDto();
        dto.setId(image.getId());
        dto.setProductId(image.getProduct().getId());
        dto.setImageUrl(image.getImageUrl());
        dto.setFileName(image.getFileName());
        dto.setFileType(image.getFileType());
        dto.setFileSize(image.getFileSize());
        dto.setMain(image.isMain());
        dto.setCreatedAt(image.getCreatedAt().format(FORMATTER));
        dto.setUpdatedAt(image.getUpdatedAt().format(FORMATTER));
        return dto;
    }
} 