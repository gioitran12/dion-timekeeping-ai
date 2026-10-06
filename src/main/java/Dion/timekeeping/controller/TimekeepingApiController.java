package Dion.timekeeping.controller;

import Dion.timekeeping.dto.FaceRegisterRequest;
import Dion.timekeeping.dto.StreamTimekeepingRequest;
import Dion.timekeeping.dto.TimekeepingResponse;
import Dion.timekeeping.service.TimekeepingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/timekeeping")
@RequiredArgsConstructor
public class TimekeepingApiController {

    private final TimekeepingService timekeepingService;

    /**
     * API lấy trạng thái chấm công hôm nay của người đang đăng nhập
     */
    @GetMapping("/today-status")
    public ResponseEntity<?> getTodayStatus(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        }
        Map<String, Object> status = timekeepingService.getTodayStatus(principal.getName());
        return ResponseEntity.ok(status);
    }

    /**
     * API quét và điểm danh real-time từ luồng video webcam
     */
    @PostMapping("/stream-recognize")
    public ResponseEntity<TimekeepingResponse> recognizeFaceFromStream(
            @RequestBody StreamTimekeepingRequest request,
            Principal principal) {
        // Gắn username từ session vào request để xác minh khuôn mặt khớp với người login
        if (principal != null) {
            request.setLoggedInUsername(principal.getName());
        }
        TimekeepingResponse response = timekeepingService.processStreamRecognition(request);
        return ResponseEntity.ok(response);
    }

    /**
     * API đăng ký khuôn mặt cho nhân viên
     */
    @PostMapping("/register-face")
    public ResponseEntity<?> registerFace(@RequestBody FaceRegisterRequest request) {
        try {
            timekeepingService.registerFace(request);
            return ResponseEntity.ok(Map.of("success", true, "message", "Đăng ký khuôn mặt thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
