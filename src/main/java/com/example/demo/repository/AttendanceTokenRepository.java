package com.example.demo.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.demo.model.AttendanceToken;

public interface AttendanceTokenRepository extends MongoRepository<AttendanceToken, String> { }
