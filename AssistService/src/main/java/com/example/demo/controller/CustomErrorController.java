package com.example.demo.controller;

import com.example.demo.common.ErrorCode;
import com.example.demo.dto.CommonResponseDTO;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CustomErrorController implements ErrorController {

    @RequestMapping("${server.error.path:${error.path:/error}}")
    public ResponseEntity<CommonResponseDTO<?>> handleError(HttpServletRequest request) {
        Object statusObj = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = (statusObj instanceof Integer i) ? i : HttpStatus.INTERNAL_SERVER_ERROR.value();

        ErrorCode errorCode = ErrorCode.fromStatus(status);

        return ResponseEntity.status(errorCode.getHttpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(CommonResponseDTO.error(errorCode));
    }
}
