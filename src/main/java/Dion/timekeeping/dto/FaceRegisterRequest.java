package Dion.timekeeping.dto;

import lombok.Data;
import java.util.List;

@Data
public class FaceRegisterRequest {
    private Long employeeId;
    private List<Double> faceDescriptor; // 128 số float trích xuất từ camera của trình duyệt
}
