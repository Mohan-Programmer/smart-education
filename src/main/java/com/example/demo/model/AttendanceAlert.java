package com.example.demo.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "attendance_alerts")
public class AttendanceAlert {
    @Id
    private String id;
    private String studentId;
    private String classId;
    private String message;        // e.g., "Expired QR", "Device mismatch"
    private String severity;       // HIGH / MEDIUM / LOW
    private LocalDateTime createdAt;
}
