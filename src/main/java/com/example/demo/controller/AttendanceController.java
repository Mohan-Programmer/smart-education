// com.example.demo.controller.AttendanceController.java
package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.service.AttendanceService;
import com.example.demo.dto.TeacherDashboardResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    // 1️⃣ Generate QR for students (valid 30 sec)
    @GetMapping("/class/{classId}/teacher/{teacherId}/qr")
    public ResponseEntity<byte[]> generateAttendanceQr(@PathVariable String classId,
                                                       @PathVariable String teacherId,
                                                       @RequestParam Double teacherLat,
                                                       @RequestParam Double teacherLon) throws Exception {
        AttendanceToken token = attendanceService.generateAttendanceToken(classId, teacherId, teacherLat, teacherLon);
        byte[] qrCode = attendanceService.generateQrCode(token.getToken(), 250, 250);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"attendance.png\"")
                .contentType(MediaType.IMAGE_PNG)
                .body(qrCode);
    }

    // 2️⃣ Validate token with teacher's live coordinates
    @PostMapping("/validate")
    public ResponseEntity<String> validateToken(
            @RequestParam String token,
            @RequestParam String studentId,
            @RequestParam String classId,
            @RequestParam String deviceId,
            @RequestParam double latitude,
            @RequestParam double longitude) { // ❌ Removed teacherLat and teacherLon

        String result = attendanceService.validateTokenWithSecurity(
            token, studentId, classId, deviceId, latitude, longitude
        );

        return ResponseEntity.ok(result);
    }

    // 3️⃣ Get class attendance report
    @GetMapping("/class/{classId}/report")
    public ResponseEntity<List<AttendanceRecord>> getReport(@PathVariable String classId,
                                                            @RequestParam String date) {
        LocalDate localDate = LocalDate.parse(date);
        List<AttendanceRecord> report = attendanceService.getAttendanceReport(classId, localDate);
        return ResponseEntity.ok(report);
    }

    // 4️⃣ Get live count
    @GetMapping("/class/{classId}/live-count")
    public ResponseEntity<Long> getLiveCount(@PathVariable String classId) {
        long count = attendanceService.getLiveCount(classId);
        return ResponseEntity.ok(count);
    }

    // 5️⃣ Get alerts
    @GetMapping("/class/{classId}/alerts")
    public ResponseEntity<List<AttendanceAlert>> getAlerts(@PathVariable String classId) {
        List<AttendanceAlert> alerts = attendanceService.getAlertsForClass(classId);
        return ResponseEntity.ok(alerts);
    }

    // 6️⃣ Teacher Dashboard
    @GetMapping("/class/{classId}/dashboard")
    public ResponseEntity<TeacherDashboardResponse> getDashboard(@PathVariable String classId,
                                                                 @RequestParam String date) {
        LocalDate localDate = LocalDate.parse(date);
        TeacherDashboardResponse dashboard = attendanceService.getTeacherDashboard(classId, localDate);
        return ResponseEntity.ok(dashboard);
    }

    // 7️⃣ Teacher Dashboard (paged)
    @GetMapping("/class/{classId}/dashboard/paged")
    public ResponseEntity<TeacherDashboardResponse> getDashboardPaged(@PathVariable String classId,
                                                                      @RequestParam String date,
                                                                      @RequestParam(defaultValue = "0") int page,
                                                                      @RequestParam(defaultValue = "20") int size,
                                                                      @RequestParam(required = false) String severity) {
        LocalDate localDate = LocalDate.parse(date);
        Pageable pageable = PageRequest.of(page, size);
        TeacherDashboardResponse dashboard = attendanceService.getTeacherDashboardPaged(classId, localDate, pageable, severity);
        return ResponseEntity.ok(dashboard);
    }

    // 8️⃣ Get latest QR token - ❌ THIS ENDPOINT IS NO LONGER NEEDED, THE FRONTEND WILL DIRECTLY REQUEST THE QR IMAGE.
    // @GetMapping("/class/{classId}/latest-qr")
    // public ResponseEntity<AttendanceToken> getLatestQr(@PathVariable String classId) {
    //     AttendanceToken latestToken = attendanceService.getLatestTokenForClass(classId);
    //     return ResponseEntity.ok(latestToken);
    // }
}