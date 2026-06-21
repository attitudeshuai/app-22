package com.umbrellapoint.dto.qrcode;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ScanReturnRequest {
    @NotBlank(message = "二维码内容不能为空")
    private String qrCode;
}
