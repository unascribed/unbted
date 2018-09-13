package com.unascribed.nbted;

public class ConsistencyError extends AssertionError {

	public ConsistencyError() {
		super();
	}

	public ConsistencyError(String message) {
		super(message);
	}

	public ConsistencyError(String message, Throwable cause) {
		super(message, cause);
	}

}
