package com.example.demo.repository;

import com.example.demo.model.AttendanceAlert;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface AttendanceAlertRepository extends MongoRepository<AttendanceAlert, String> {

    // List version
    List<AttendanceAlert> findByClassId(String classId);

    // Paged version
    Page<AttendanceAlert> findByClassId(String classId, Pageable pageable);

    // Paged version with severity filter
    Page<AttendanceAlert> findByClassIdAndSeverity(String classId, String severity, Pageable pageable);
}
