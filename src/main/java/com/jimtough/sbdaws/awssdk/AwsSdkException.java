package com.jimtough.sbdaws.awssdk;

/**
 * Thrown when an AWS SDK call fails or throws an exception
 * 
 * @author JTOUGH
 */
public class AwsSdkException extends Exception {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor
	 * @param message String
	 * @param cause Exception
	 */
	public AwsSdkException(String message, Exception cause) {
		super(message, cause);
	}

	/**
	 * Constructor
	 * @param message String
	 */
	public AwsSdkException(String message) {
		super(message);
	}
	
}
