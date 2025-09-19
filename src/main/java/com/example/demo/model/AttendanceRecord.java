package com.example.demo.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "attendance")
public class AttendanceRecord {
    @Id
    private String id;
    private String studentId;
    private String studentName;  // ✅ added
    private String classId;
    private LocalDateTime markedAt;
}
