package com.example.demo.service;

import com.example.demo.model.AttendanceAlert;
import com.example.demo.model.AttendanceRecord;
import com.example.demo.model.AttendanceToken;
import com.example.demo.model.Student;
import com.example.demo.repository.AttendanceAlertRepository;
import com.example.demo.repository.AttendanceRecordRepository;
import com.example.demo.repository.AttendanceTokenRepository;
import com.example.demo.repository.StudentRepository;
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

    @Autowired
    private StudentRepository studentRepo;

    @Autowired
    private AttendanceAlertRepository alertRepo;

    @Autowired
    private AttendanceTokenRepository tokenRepo;

    @Autowired
    private AttendanceRecordRepository recordRepo;

    // Classroom coordinates (example: can be fetched from DB)
    private final double CLASS_LAT = 28.7041; // Delhi latitude
    private final double CLASS_LON = 77.1025; // Delhi longitude
    private final double CLASS_RADIUS_METERS = 30; // 30m radius

    /**
     * Generate unique token and save to DB
     */
    public AttendanceToken generateAttendanceToken(String classId, String teacherId) {
        String token = UUID.randomUUID().toString();
        AttendanceToken attendanceToken = new AttendanceToken(token, classId, teacherId, LocalDateTime.now());
        tokenRepo.save(attendanceToken);
        return attendanceToken;
    }

    /**
     * Generate QR Code image (PNG bytes)
     */
    public byte[] generateQrCode(String token, int width, int height) throws Exception {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        BitMatrix matrix = new MultiFormatWriter().encode(
                token, BarcodeFormat.QR_CODE, width, height, hints);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }

    /**
     * Save an alert
     */
    private void createAlert(String studentId, String classId, String message, String severity) {
        AttendanceAlert alert = new AttendanceAlert(
                null, studentId, classId, message, severity, LocalDateTime.now()
        );
        alertRepo.save(alert);
    }

    /**
     * Geo-fencing check
     */
    private boolean isWithinClassroom(double studentLat, double studentLon,
                                      double classLat, double classLon, double radiusMeters) {
        double earthRadius = 6371000; // meters
        double dLat = Math.toRadians(studentLat - classLat);
        double dLon = Math.toRadians(studentLon - classLon);

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.cos(Math.toRadians(classLat)) * Math.cos(Math.toRadians(studentLat)) *
                   Math.sin(dLon/2) * Math.sin(dLon/2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double distance = earthRadius * c;

        return distance <= radiusMeters;
    }

    /**
     * ✅ Unified validateTokenWithSecurity method
     * Handles: token validation, expiry, duplicate, device binding, geo-fencing
     */
    public String validateTokenWithSecurity(String token, String studentId, String classId,
                                            String submittedDeviceId,
                                            Double submittedLat, Double submittedLon) {

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

        // Duplicate check
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        List<AttendanceRecord> existing = recordRepo.findByStudentAndClassAndDateRange(
                studentId, classId, startOfDay, endOfDay
        );

        if (!existing.isEmpty()) {
            createAlert(studentId, classId, "Duplicate attendance attempt", "LOW");
            return "⚠️ Attendance already marked for student " + studentId;
        }

        // Fetch student details
        String studentName = "Unknown";
        Optional<Student> studentOpt = studentRepo.findById(studentId);
        if (studentOpt.isPresent()) {
            studentName = studentOpt.get().getName();

            // Device binding check
            if (submittedDeviceId != null && !submittedDeviceId.equals(studentOpt.get().getDeviceId())) {
                createAlert(studentId, classId, "Device mismatch", "HIGH");
            }

            // Geo-fencing check
            if (submittedLat != null && submittedLon != null &&
                !isWithinClassroom(submittedLat, submittedLon, CLASS_LAT, CLASS_LON, CLASS_RADIUS_METERS)) {
                createAlert(studentId, classId, "Outside classroom", "MEDIUM");
            }
        }

        // Save new attendance record
        AttendanceRecord record = new AttendanceRecord(
                null, studentId, studentName, classId, LocalDateTime.now()
        );
        recordRepo.save(record);

        return "✅ Attendance marked for student " + studentName;
    }

    // ---------- Remaining methods ----------

    public List<AttendanceRecord> getAttendanceReport(String classId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        return recordRepo.findAllByClassAndDateRange(classId, startOfDay, endOfDay);
    }

    public long getLiveCount(String classId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        return recordRepo.findAllByClassAndDateRange(classId, startOfDay, endOfDay).size();
    }

    public List<AttendanceAlert> getAlertsForClass(String classId) {
        return alertRepo.findByClassId(classId);
    }

    public TeacherDashboardResponse getTeacherDashboard(String classId, LocalDate date) {
        List<AttendanceRecord> report = getAttendanceReport(classId, date);
        long liveCount = getLiveCount(classId);
        List<AttendanceAlert> alerts = getAlertsForClass(classId);
        return new TeacherDashboardResponse(liveCount, report, alerts);
    }

    public TeacherDashboardResponse getTeacherDashboardPaged(
            String classId, LocalDate date, Pageable pageable, String severityFilter) {

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        Page<AttendanceRecord> reportPage =
                recordRepo.findPagedByClassAndDateRange(classId, startOfDay, endOfDay, pageable);

        Page<AttendanceAlert> alertPage;
        if (severityFilter != null && !severityFilter.isEmpty()) {
            alertPage = alertRepo.findByClassIdAndSeverity(classId, severityFilter, pageable);
        } else {
            alertPage = alertRepo.findByClassId(classId, pageable);
        }

        long liveCount = getLiveCount(classId);

        return new TeacherDashboardResponse(
                liveCount,
                reportPage.getContent(),
                alertPage.getContent()
        );
    }
}
