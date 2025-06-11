package com.ecommerce.products.specification;

import com.ecommerce.products.dto.ProductFilterDto;
import com.ecommerce.products.entity.Product;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ProductSpecification implements Specification<Product> {
    private final ProductFilterDto filter;

    public ProductSpecification(ProductFilterDto filter) {
        this.filter = filter;
    }

    @Override
    public Predicate toPredicate(Root<Product> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

        // Поиск по названию и описанию
        if (StringUtils.hasText(filter.getSearch())) {
            String searchPattern = "%" + filter.getSearch().toLowerCase() + "%";
            predicates.add(cb.or(
                cb.like(cb.lower(root.get("name")), searchPattern),
                cb.like(cb.lower(root.get("description")), searchPattern)
            ));
        }

        // Фильтр по категории
        if (filter.getCategoryId() != null) {
            predicates.add(cb.equal(root.get("category").get("id"), filter.getCategoryId()));
        }

        // Фильтр по цене
        if (filter.getMinPrice() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("price"), filter.getMinPrice()));
        }
        if (filter.getMaxPrice() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("price"), filter.getMaxPrice()));
        }

        // Фильтр по спецификациям
        if (filter.getSpecifications() != null && !filter.getSpecifications().isEmpty()) {
            Join<Object, Object> specifications = root.join("specifications");
            predicates.add(specifications.get("value").in(filter.getSpecifications()));
        }

        return cb.and(predicates.toArray(new Predicate[0]));
    }
} 