package com.example.springbootnyano.config;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> duplicate(DataIntegrityViolationException e) {
        return ResponseEntity.status(409).body(Map.of("message",
                "Dữ liệu bị trùng: tên sản phẩm hoặc thông số biến thể đã tồn tại"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> badBody(HttpMessageNotReadableException e) {
        return ResponseEntity.status(400).body(Map.of("message",
                "Dữ liệu gửi lên không hợp lệ — kiểm tra các ô số (tồn kho, giá, % giảm)"));
    }
}