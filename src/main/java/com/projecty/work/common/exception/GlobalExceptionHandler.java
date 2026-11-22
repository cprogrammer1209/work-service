package com.projecty.work.common.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import com.projecty.work.common.constants.AppConstants;
import com.projecty.work.common.dto.AppError;
import com.projecty.work.common.dto.AppResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns every error into an {@link AppResponse} error body: business errors, validation errors, standard
 * Spring MVC errors (bad JSON, wrong method, …) and unexpected exceptions.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(AppException.class)
	public ResponseEntity<AppResponse<Void>> handleAppException(AppException ex) {
		return ResponseEntity.status(ex.getStatus())
			.body(AppResponse.error(new AppError(ex.getCode(), ex.getMessage(), ex.getDetails())));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<AppResponse<Void>> handleUnexpected(Exception ex) {
		logger.error("Unhandled exception", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(AppResponse.error(AppError.of(AppConstants.ERROR_INTERNAL, AppConstants.MESSAGE_INTERNAL)));
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, Object> errors = new LinkedHashMap<>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			errors.putIfAbsent(error.getField(), error.getDefaultMessage());
		}
		return ResponseEntity.badRequest()
			.body(AppResponse.error(new AppError(AppConstants.ERROR_VALIDATION_FAILED,
					AppConstants.MESSAGE_VALIDATION_FAILED, errors)));
	}

	/**
	 * Standard Spring MVC errors arrive here with a ProblemDetail body; re-shape them into the envelope, using
	 * the status name as the code (for example {@code METHOD_NOT_ALLOWED}).
	 */
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		HttpStatus status = HttpStatus.resolve(statusCode.value());
		String code = (status != null) ? status.name() : AppConstants.ERROR_INTERNAL;
		String message = (body instanceof ProblemDetail problem && problem.getDetail() != null) ? problem.getDetail()
				: ex.getMessage();
		return ResponseEntity.status(statusCode).headers(headers).body(AppResponse.error(AppError.of(code, message)));
	}

}
