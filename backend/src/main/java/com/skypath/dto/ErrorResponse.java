package com.skypath.dto;

public record ErrorResponse(
        String error,
        String message,
        int statusCode
) {
}
