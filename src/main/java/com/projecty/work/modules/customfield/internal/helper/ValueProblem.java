package com.projecty.work.modules.customfield.internal.helper;

/**
 * Something is wrong with one answer or one rule. The message is meant for the person who typed it.
 */
public class ValueProblem extends RuntimeException {

	public ValueProblem(String message) {
		super(message);
	}

}
