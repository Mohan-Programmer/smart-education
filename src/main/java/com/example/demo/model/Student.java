package com.example.demo.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "students")
public class Student {
    @Id
    private String id;         // studentId
    private String rollNo;     // e.g., 21CSE001
    private String name;       // Student name
    private String classId;    // Which class this student belongs to
    private String deviceId;   // For device binding (optional)
}
