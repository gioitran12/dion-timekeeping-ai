package Dion.timekeeping.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimekeepingResponse {
    private boolean success;
    private String message;
    private String employeeName;
    private String employeeCode;
    private String checkType;
    private String time;
    private Double confidenceScore;
    private String status; // ON_TIME, LATE, EARLY_LEAVE
    private Boolean locationValid;
}
