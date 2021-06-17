package com.twq.miaosha.expection;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.twq.miaosha.result.CodeMsg;
import com.twq.miaosha.result.Result;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;


/**
 * 自定义全局异常处理器，
 * 可以用于拦截各种异常
 * 用于拦截 JSR校验的绑定异常
 *
 * @ControllerAdvice  相当于一个 Controller 但是 这个Controller的返回Resulit，会被前端直接获取。
 * （当输入的参数不满足jsr的校验，就会抛出一个绑定异常。BindException）
 */

@ControllerAdvice
@ResponseBody
public class GlobalExceptionHandler {
	// @ExceptionHandler 异常拦截器注解 value 后面跟我们想拦截的异常。
	@ExceptionHandler(value=Exception.class)
	public Result<String> exceptionHandler(HttpServletRequest request, Exception e){
		e.printStackTrace();
		if(e instanceof GlobalException) { // 处理全局异常
			GlobalException ex = (GlobalException)e;
			return Result.error(ex.getCm());
		}else if(e instanceof BindException) { // 处理绑定异常
			BindException ex = (BindException)e;
			List<ObjectError> errors = ex.getAllErrors();
			ObjectError error = errors.get(0);
			String msg = error.getDefaultMessage();
			return Result.error(CodeMsg.BIND_ERROR.fillArgs(msg));
		}else {
			return Result.error(CodeMsg.SERVER_ERROR);
		}
	}
}
