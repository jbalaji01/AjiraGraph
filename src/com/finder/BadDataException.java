package com.finder;

public class BadDataException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	private int errorCode;
	
	public BadDataException(int errorCode, String msg) {
		super(msg);
		this.setErrorCode(errorCode);
	}

	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}
}
