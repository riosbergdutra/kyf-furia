package com.kyf.furia.dto;

public class AuthResponse {
    private String accessToken;
    private String refreshtoken;

    public AuthResponse(String accessToken, String refreshtoken) {
        this.accessToken = accessToken;
        if (refreshtoken != null) this.refreshtoken = refreshtoken;
    }

    // Getters
    public String getAccessToken() { return accessToken; }
    public String getRefreshTOken() { return refreshtoken; }
}
