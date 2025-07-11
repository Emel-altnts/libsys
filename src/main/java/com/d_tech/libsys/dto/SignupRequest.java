package com.d_tech.libsys.dto;

import lombok.Data;

@Data
public class SignupRequest {
    private String username;
    private String password;
    private String confirmPassword;
}