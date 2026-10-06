package Dion.timekeeping.controller;

import Dion.timekeeping.dto.FaceRegisterRequest;
import Dion.timekeeping.dto.StreamTimekeepingRequest;
import Dion.timekeeping.dto.TimekeepingResponse;
import Dion.timekeeping.service.TimekeepingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/timekeeping")
@RequiredArgsConstructor
public class TimekeepingApiController {

    private final TimekeepingService timekeepingService;

    /**
     * API quét và điểm danh real-time từ luồng video webcam
     */
    @PostMapping("/stream-recognize")
    public ResponseEntity<TimekeepingResponse> recognizeFaceFromStream(@RequestBody StreamTimekeepingRequest request) {
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
