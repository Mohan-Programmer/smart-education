// com.example.demo.service.AttendanceService.java
package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.dto.TeacherDashboardResponse;
import com.google.zxing.*;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class AttendanceService {

    @Autowired private StudentRepository studentRepo;
    @Autowired private AttendanceAlertRepository alertRepo;
    @Autowired private AttendanceTokenRepository tokenRepo;
    @Autowired private AttendanceRecordRepository recordRepo;

    private final double CLASS_RADIUS_METERS = 30;

    // 1️⃣ Generate unique token with teacher coordinates
    public AttendanceToken generateAttendanceToken(String classId, String teacherId, Double teacherLat, Double teacherLon) {
        String token = UUID.randomUUID().toString();
        AttendanceToken attendanceToken = new AttendanceToken(token, classId, teacherId, LocalDateTime.now(), teacherLat, teacherLon);
        tokenRepo.save(attendanceToken);
        return attendanceToken;
    }

    // 2️⃣ Generate QR Code (PNG bytes)
    public byte[] generateQrCode(String token, int width, int height) throws Exception {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        BitMatrix matrix = new MultiFormatWriter().encode(token, BarcodeFormat.QR_CODE, width, height, hints);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }

    // 3️⃣ Save alert
    private void createAlert(String studentId, String classId, String message, String severity) {
        AttendanceAlert alert = new AttendanceAlert(null, studentId, classId, message, severity, LocalDateTime.now());
        alertRepo.save(alert);
    }

    // 4️⃣ Geo-fencing check
    private boolean isWithinClassroom(double studentLat, double studentLon, double centerLat, double centerLon, double radiusMeters) {
        double earthRadius = 6371000; // meters
        double dLat = Math.toRadians(studentLat - centerLat);
        double dLon = Math.toRadians(studentLon - centerLon);

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.cos(Math.toRadians(centerLat)) * Math.cos(Math.toRadians(studentLat)) *
                   Math.sin(dLon/2) * Math.sin(dLon/2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double distance = earthRadius * c;
        return distance <= radiusMeters;
    }

    // 5️⃣ Validate QR token + device + geo + duplicate
    public String validateTokenWithSecurity(String token, String studentId, String classId,
                                            String submittedDeviceId, Double submittedLat, Double submittedLon) {

        Optional<AttendanceToken> optionalToken = tokenRepo.findById(token);
        if (optionalToken.isEmpty()) {
            createAlert(studentId, classId, "Invalid QR attempt", "HIGH");
            return "❌ Invalid QR code";
        }

        AttendanceToken stored = optionalToken.get();

        // Expiry check (30 sec)
        if (stored.getCreatedAt().plusSeconds(30).isBefore(LocalDateTime.now())) {
            tokenRepo.deleteById(token);
            createAlert(studentId, classId, "Tried with expired QR", "MEDIUM");
            return "⏳ QR code expired";
        }

        if (!stored.getClassId().equals(classId)) {
            createAlert(studentId, classId, "Token mismatch (wrong class)", "HIGH");
            return "❌ Token not valid for this class";
        }

        // Duplicate attendance check
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        List<AttendanceRecord> existing = recordRepo.findByStudentAndClassAndDateRange(studentId, classId, startOfDay, endOfDay);
        if (!existing.isEmpty()) {
            createAlert(studentId, classId, "Duplicate attendance attempt", "LOW");
            return "⚠️ Attendance already marked";
        }

        // Fetch student
        Optional<Student> studentOpt = studentRepo.findById(studentId);
        String studentName = "Unknown";
        if (studentOpt.isPresent()) {
            Student student = studentOpt.get();
            studentName = student.getName();

            // Device binding check
            if (submittedDeviceId != null && !submittedDeviceId.equals(student.getDeviceId())) {
                createAlert(studentId, classId, "Device mismatch", "HIGH");
            }

            // Geo-fencing check (using teacher's coordinates from the stored token)
            if (submittedLat != null && submittedLon != null && stored.getTeacherLat() != null && stored.getTeacherLon() != null &&
                !isWithinClassroom(submittedLat, submittedLon, stored.getTeacherLat(), stored.getTeacherLon(), CLASS_RADIUS_METERS)) {
                createAlert(studentId, classId, "Outside classroom", "MEDIUM");
                return "❌ Outside the classroom location";
            }
        }

        // Save attendance
        AttendanceRecord record = new AttendanceRecord(null, studentId, studentName, classId, LocalDateTime.now());
        recordRepo.save(record);

        return "✅ Attendance marked for student " + studentName;
    }

    // 6️⃣ Get attendance report for a class
    public List<AttendanceRecord> getAttendanceReport(String classId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        return recordRepo.findAllByClassAndDateRange(classId, startOfDay, endOfDay);
    }

    // 7️⃣ Get live attendance count
    public long getLiveCount(String classId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        return recordRepo.findAllByClassAndDateRange(classId, startOfDay, endOfDay).size();
    }

    // 8️⃣ Get alerts for a class
    public List<AttendanceAlert> getAlertsForClass(String classId) {
        return alertRepo.findByClassId(classId);
    }

    // 9️⃣ Teacher dashboard
    public TeacherDashboardResponse getTeacherDashboard(String classId, LocalDate date) {
        List<AttendanceRecord> report = getAttendanceReport(classId, date);
        long liveCount = getLiveCount(classId);
        List<AttendanceAlert> alerts = getAlertsForClass(classId);
        return new TeacherDashboardResponse(liveCount, report, alerts);
    }

    // 10️⃣ Teacher dashboard with paging
    public TeacherDashboardResponse getTeacherDashboardPaged(String classId, LocalDate date, Pageable pageable, String severityFilter) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        Page<AttendanceRecord> reportPage = recordRepo.findPagedByClassAndDateRange(classId, startOfDay, endOfDay, pageable);
        Page<AttendanceAlert> alertPage = (severityFilter != null && !severityFilter.isEmpty()) ?
                alertRepo.findByClassIdAndSeverity(classId, severityFilter, pageable) :
                alertRepo.findByClassId(classId, pageable);

        long liveCount = getLiveCount(classId);

        return new TeacherDashboardResponse(liveCount, reportPage.getContent(), alertPage.getContent());
    }

    // 11️⃣ Get latest token for a class (Optimized)
    public AttendanceToken getLatestTokenForClass(String classId) {
        return tokenRepo.findTopByClassIdOrderByCreatedAtDesc(classId).orElse(null);
    }
}