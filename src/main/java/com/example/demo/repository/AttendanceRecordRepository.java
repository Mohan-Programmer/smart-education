package com.example.demo.repository;

import com.example.demo.model.AttendanceRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface AttendanceRecordRepository extends MongoRepository<AttendanceRecord, String> {

    // ✅ Find attendance for a student in a class between two times
    @Query("{ 'studentId': ?0, 'classId': ?1, 'markedAt': { $gte: ?2, $lte: ?3 } }")
    List<AttendanceRecord> findByStudentAndClassAndDateRange(
            String studentId, String classId, LocalDateTime start, LocalDateTime end);

    // ✅ Find all attendance records for a class in a date range (full list)
    @Query("{ 'classId': ?0, 'markedAt': { $gte: ?1, $lte: ?2 } }")
    List<AttendanceRecord> findAllByClassAndDateRange(
            String classId, LocalDateTime start, LocalDateTime end);

    // ✅ Find attendance records for a class in a date range (paged)
    @Query("{ 'classId': ?0, 'markedAt': { $gte: ?1, $lte: ?2 } }")
    Page<AttendanceRecord> findPagedByClassAndDateRange(
            String classId, LocalDateTime start, LocalDateTime end, Pageable pageable);
}
