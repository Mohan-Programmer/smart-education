package com.example.demo.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tokens")
public class AttendanceToken {
    @Id
    private String token;
    private String classId;
    private String teacherId;   // 🔹 NEW
    private LocalDateTime createdAt;
}

