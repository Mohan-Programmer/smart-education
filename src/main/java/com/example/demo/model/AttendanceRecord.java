package com.example.demo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Document(collection = "attendance")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRecord {
    @Id
    private String id;
    private String studentId;
    private String studentName;
    private String classId;
    private LocalDateTime markedAt;
}

