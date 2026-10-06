package Dion.timekeeping.dto;

import Dion.timekeeping.entity.TimekeepingRecord;
import lombok.Data;
import java.util.List;

@Data
public class StreamTimekeepingRequest {
    private List<Double> faceDescriptor; // 128 số float trích xuất real-time
    private TimekeepingRecord.CheckType checkType; // CHECKIN hoặc CHECKOUT
    private Double latitude; // GPS hiện tại (nếu có)
    private Double longitude;
}
