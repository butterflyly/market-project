package com.example.demo.Controller.Error;

import com.example.demo.DTO.CommonResponse;
import com.example.demo.Error.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

// 프로젝트 전체에서 발생하는 예외를 한곳에서 잡아 깔끔한 JSON 응답으로 변환해주는 방식
@RestControllerAdvice(annotations = RestController.class)
@Slf4j
public class GlobalExceptionHandler {

    // 비즈니스 로직 에러
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<CommonResponse<String>> handleBusinessException(BusinessException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new CommonResponse<>(false, ex.getMessage(), null));
    }

    // 400 에러: 유효성 검사 실패나 잘못된 요청
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CommonResponse<String>> handleIllegal(IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(new CommonResponse<>(false, e.getMessage(), null));
    }

    // 500 에러: 예상치 못한 서버 내부 오류
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<String>> handleException(Exception e) {
        log.error("Unhandled Error: ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new CommonResponse<>(false, "서버 오류가 발생했습니다.", null));
    }

    // Valid 검증
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResponse<List<String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {

        // 1. 발생한 모든 에러 메시지를 리스트로 수집
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage) // DTO에 적은 메시지 추출
                .toList();

        // 2. 우리가 만든 단단한 규격(CommonResponse)에 담아서 리턴
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new CommonResponse<>(false, "입력값이 올바르지 않습니다.", errors));
    }
}
