package com.example.demo.controller;

import com.example.demo.model.AttendanceRecord;
import com.example.demo.model.AttendanceToken;
import com.example.demo.service.AttendanceService;
import com.example.demo.model.AttendanceAlert;
import com.example.demo.dto.TeacherDashboardResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;


@CrossOrigin(origins = "*") // allow all origins
@RestController
@RequestMapping("/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    /**
     * Step 1: Generate QR for students (valid for 30 sec)
     */
    @GetMapping("/class/{classId}/teacher/{teacherId}/qr")
    public ResponseEntity<byte[]> generateAttendanceQr(@PathVariable String classId,
                                                       @PathVariable String teacherId) throws Exception {
        AttendanceToken token = attendanceService.generateAttendanceToken(classId, teacherId);
        byte[] qrCode = attendanceService.generateQrCode(token.getToken(), 250, 250);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"attendance.png\"")
                .contentType(MediaType.IMAGE_PNG)
                .body(qrCode);
    }

    /**
     * Step 2: Validate token (student sends after scanning QR)
     * Accepts optional deviceId, latitude, and longitude
     */
@PostMapping("/validate")
public ResponseEntity<String> validateToken(
        @RequestParam String token,
        @RequestParam String studentId,
        @RequestParam String classId,
        @RequestParam String deviceId,
        @RequestParam double latitude,
        @RequestParam double longitude) {

    String result = attendanceService.validateTokenWithSecurity(
            token, studentId, classId, deviceId, latitude, longitude);
    return ResponseEntity.ok(result);
}


    /**
     * Step 3: Teacher fetches attendance report by class + date
     */
    @GetMapping("/class/{classId}/report")
    public ResponseEntity<List<AttendanceRecord>> getReport(@PathVariable String classId,
                                                            @RequestParam String date) {
        LocalDate localDate = LocalDate.parse(date); // expects format: yyyy-MM-dd
        List<AttendanceRecord> report = attendanceService.getAttendanceReport(classId, localDate);
        return ResponseEntity.ok(report);
    }

    /**
     * Step 4: Teacher gets live attendance count for a class (today)
     */
    @GetMapping("/class/{classId}/live-count")
    public ResponseEntity<Long> getLiveCount(@PathVariable String classId) {
        long count = attendanceService.getLiveCount(classId);
        return ResponseEntity.ok(count);
    }

    /**
     * Step 5: Teacher fetches alerts for a class
     */
    @GetMapping("/class/{classId}/alerts")
    public ResponseEntity<List<AttendanceAlert>> getAlerts(@PathVariable String classId) {
        List<AttendanceAlert> alerts = attendanceService.getAlertsForClass(classId);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Step 6a: Teacher Dashboard API (simple version: all records, no pagination)
     */
    @GetMapping("/class/{classId}/dashboard")
    public ResponseEntity<TeacherDashboardResponse> getDashboard(@PathVariable String classId,
                                                                 @RequestParam String date) {
        LocalDate localDate = LocalDate.parse(date); // expects yyyy-MM-dd
        TeacherDashboardResponse dashboard = attendanceService.getTeacherDashboard(classId, localDate);
        return ResponseEntity.ok(dashboard);
    }

    /**
     * Step 6b: Teacher Dashboard API (paged version with filtering)
     */
    @GetMapping("/class/{classId}/dashboard/paged")
    public ResponseEntity<TeacherDashboardResponse> getDashboardPaged(@PathVariable String classId,
                                                                      @RequestParam String date,
                                                                      @RequestParam(defaultValue = "0") int page,
                                                                      @RequestParam(defaultValue = "20") int size,
                                                                      @RequestParam(required = false) String severity) {

        LocalDate localDate = LocalDate.parse(date);
        Pageable pageable = PageRequest.of(page, size);

        TeacherDashboardResponse dashboard =
                attendanceService.getTeacherDashboardPaged(classId, localDate, pageable, severity);

        return ResponseEntity.ok(dashboard);
    }
}
