package com.ecommerce.user.repository;

import com.ecommerce.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByCognitoId(String cognitoId);
    boolean existsByEmail(String email);
    
    // Admin analytics queries
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :todayStart AND u.createdAt < :todayEnd")
    Integer countTodayUsers(@Param("todayStart") LocalDateTime todayStart, @Param("todayEnd") LocalDateTime todayEnd);
} 