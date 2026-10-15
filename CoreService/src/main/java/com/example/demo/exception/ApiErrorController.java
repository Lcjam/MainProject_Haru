package com.example.demo.exception;

import com.example.demo.dto.response.ApiResponse;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiErrorController implements ErrorController {
    @RequestMapping("${server.error.path:/error}")
    public ResponseEntity<ApiResponse<String>> error(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        ApiErrorCode code = ApiErrorCode.fromStatus(status instanceof Integer value ? value : 500);
        return ResponseEntity.status(code.httpStatus()).body(ApiResponse.error(code.message(), code.name()));
    }
}
