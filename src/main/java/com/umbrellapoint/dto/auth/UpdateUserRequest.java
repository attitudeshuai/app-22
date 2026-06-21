package com.umbrellapoint.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {
    @Size(min = 3, max = 50, message = "用户名长度应在3-50个字符之间")
    private String username;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Size(max = 500, message = "头像URL长度不能超过500")
    private String avatar;
}
