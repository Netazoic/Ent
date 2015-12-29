package com.netazoic.ent;

public class ENTException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ENTException(Exception ex){
		super(ex);
	}
	
	public ENTException(String msg){
		super(msg);
	}

}
