package com.umbrellapoint.dto.borrow;

import com.umbrellapoint.entity.BorrowRecord;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BorrowStatusRequest {
    @NotNull(message = "借还状态不能为空")
    private BorrowRecord.BorrowStatus status;

    private Long returnStationId;
}
