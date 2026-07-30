package com.example.demo.Error;

public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
