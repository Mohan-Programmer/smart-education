package com.example.demo.repository;

import com.example.demo.model.AttendanceToken;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AttendanceTokenRepository extends MongoRepository<AttendanceToken, String> {
}
