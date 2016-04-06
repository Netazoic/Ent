package com.netazoic.ent;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public interface IF_Ent<T> {

	public abstract void copyRecord(IF_Ent<T> entS, String webuserID)
			throws ENTException;

	public abstract Long createRecord(HashMap<String,Object> paramMap,
			Connection con) throws ENTException;

	public abstract void deleteRecord(String webuserID, String comments)
			throws ENTException;

	public abstract String getJSON() throws ENTException;

	public abstract void retrieveRecord() throws ENTException;

	public abstract void retrieveRecord(HashMap<String,Object> paramMap)
			throws ENTException;

	public abstract void setFieldVals(ResultSet rs) throws SQLException, ENTException;

	public abstract void updateRecord(HashMap<String,Object> paramMap)
			throws ENTException;

	void initENT() throws ENTException;

}