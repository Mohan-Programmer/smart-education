package com.example.demo.repository;

import com.example.demo.model.AttendanceAlert;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AttendanceAlertRepository extends MongoRepository<AttendanceAlert, String> {

    // Get all alerts for a class (List version)
    List<AttendanceAlert> findByClassId(String classId);

    // Get all alerts for a class (Paged version)
    Page<AttendanceAlert> findByClassId(String classId, Pageable pageable);

    // Get alerts for a class filtered by severity (Paged version)
    Page<AttendanceAlert> findByClassIdAndSeverity(String classId, String severity, Pageable pageable);
}
