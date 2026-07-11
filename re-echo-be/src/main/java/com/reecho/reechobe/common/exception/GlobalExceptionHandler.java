package com.reecho.reechobe.common.exception;

import com.reecho.reechobe.common.response.ApiResponse;
import com.reecho.reechobe.common.response.FieldErrorResponse;
import com.reecho.reechobe.common.response.ValidationErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

// 예외를 API 문서의 공통 응답 envelope으로 변환한다.
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        ApiResponse<Void> response = ApiResponse.error(
                errorCode.getHttpStatus(),
                errorCode.getCode(),
                exception.getMessage(),
                null
        );
        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ValidationErrorResponse>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception
    ) {
        return validationErrorResponse(toFieldErrors(exception));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<ValidationErrorResponse>> handleBindException(BindException exception) {
        return validationErrorResponse(toFieldErrors(exception));
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            HttpRequestMethodNotSupportedException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleInvalidRequestException(Exception exception) {
        ErrorCode errorCode = CommonErrorCode.INVALID_REQUEST;
        ApiResponse<Void> response = ApiResponse.error(
                errorCode.getHttpStatus(),
                errorCode.getCode(),
                errorCode.getDefaultMessage(),
                null
        );
        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error("Unhandled exception. method={}, uri={}", request.getMethod(), request.getRequestURI(), exception);

        ErrorCode errorCode = CommonErrorCode.INTERNAL_SERVER_ERROR;
        ApiResponse<Void> response = ApiResponse.error(
                errorCode.getHttpStatus(),
                errorCode.getCode(),
                errorCode.getDefaultMessage(),
                null
        );
        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    private ResponseEntity<ApiResponse<ValidationErrorResponse>> validationErrorResponse(
            List<FieldErrorResponse> fieldErrors
    ) {
        ErrorCode errorCode = CommonErrorCode.VALIDATION_ERROR;
        ValidationErrorResponse validationError = new ValidationErrorResponse(fieldErrors);
        ApiResponse<ValidationErrorResponse> response = ApiResponse.error(
                errorCode.getHttpStatus(),
                errorCode.getCode(),
                errorCode.getDefaultMessage(),
                validationError
        );
        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    private List<FieldErrorResponse> toFieldErrors(BindException exception) {
        return exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> new FieldErrorResponse(
                        fieldError.getField(),
                        fieldError.getDefaultMessage()
                ))
                .toList();
    }
}
