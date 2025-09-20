package com.example.demo.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceToken {
    @Id
    private String token;
    private String classId;
    private String teacherId;
    private LocalDateTime createdAt;
     private Double teacherLat;   // Add this
    private Double teacherLon;  
}
