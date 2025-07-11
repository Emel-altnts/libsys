package com.d_tech.libsys.dto;

import lombok.Data;

@Data
public class AuthRequest {
    private String username;
    private String password;
}