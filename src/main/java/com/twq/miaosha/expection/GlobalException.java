package com.twq.miaosha.expection;


import com.twq.miaosha.result.CodeMsg;


/**
 * 自定义全局异常，
 * 对CodeMsg一个封装。
 * @Author: tangwq
 * @Date: 2021/04/27/14:44
 * @Description:
 */
public class GlobalException extends RuntimeException{

	private static final long serialVersionUID = 1L;
	
	private CodeMsg cm;
	
	public GlobalException(CodeMsg cm) {
		super(cm.toString());
		this.cm = cm;
	}

	public CodeMsg getCm() {
		return cm;
	}

}
