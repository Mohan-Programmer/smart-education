package com.example.demo.dto;

import com.example.demo.model.AttendanceAlert;
import com.example.demo.model.AttendanceRecord;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TeacherDashboardResponse {
    private long liveCount;                         // total present today
    private List<AttendanceRecord> attendanceReport; // detailed attendance list
    private List<AttendanceAlert> alerts;            // suspicious attempts
}
