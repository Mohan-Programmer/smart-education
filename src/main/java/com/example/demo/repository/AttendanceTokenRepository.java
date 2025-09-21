// com.example.demo.repository.AttendanceTokenRepository.java
package com.example.demo.repository;

import com.example.demo.model.AttendanceToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface AttendanceTokenRepository extends MongoRepository<AttendanceToken, String> {
    // 🔍 New method to efficiently find the latest token
    Optional<AttendanceToken> findTopByClassIdOrderByCreatedAtDesc(String classId);
}