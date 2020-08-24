package com.netazoic.ent;



import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLType;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.netazoic.util.NamedParameterStatement;
import com.netazoic.util.SQLUtil;

public abstract class rdENT<T> extends ENT<T> implements ifDataSrcWrapper {


	public String sql_ActivateENT;
	public String sql_DeleteRemoteData;
	
	NamedParameterStatement npsCreate;
	protected ifDataSrc dataSrc;
	protected String dataURL;
	public SRC_ORG srcOrg;
	
	public enum DataFmt{
		JSON, CSV
	}
	
	public enum SRC_ORG implements if_SRC_ORG{
		JH_G("Johns Hopkins Global Time Series"),
		JH_US("Johns Hopkins US Time Series"),
		CTP("Covid Tracking Project");

		public String code;
		String srcName;

		SRC_ORG(String n){
			this.code=this.name();
			this.srcName = n;
		}

		@Override
		public String getCode() {
			return this.code;

		}
	}

	@Override
	public void initENT() throws ENTException{
		super.initENT();
	}


	public rdENT() throws ENTException {
		super();
	}



	public rdENT(HashMap<String,Object> recMap) throws ENTException{
		// Set up rdEnt class vars using values from recMap
	}

	public void init(HashMap<String,Object> recMap) throws ENTException {
		// Do some stuff to set class vars based on recMap
		//List<Field> flds = this.getFields();
		try {
			this.setFieldVals(recMap);
		}catch(Exception ex) {
			throw new ENTException(ex);
		}
	}

	
	
	public Integer createCombinedRecs(LocalDate lastUpdate) throws Exception {
		Integer retInt = 1;
		return retInt;
	}
	
	public Integer expireCombinedRecs() throws SQLException {
		//Expire all records of this type in the combined records table
		String q = "DELETE FROM covid.combined WHERE sourcecode = '" + this.srcOrg.code + "'";
		return SQLUtil.execSQL(q, con);
	}
	
	public void setInsertStatement (NamedParameterStatement nps) throws SQLException, ENTException {
		List<Field> flds = this.getLocalFields(new LinkedList<Field>(), this.getClass(), true);
		Object fldVal = null;
		for(Field fld : flds) {
			Class<?> type = fld.getType();
			SQLType sqlType = null;
			try {
				fldVal = fld.get(this);
//				fldVal = type.getClass().cast(fldVal);
				if(type.equals(LocalDate.class)) sqlType = JDBCType.DATE;
				if(sqlType == null)	nps.setObject(fld.getName(), fldVal);
				else nps.setObject(fld.getName(), fldVal, sqlType);
			} catch(SQLException sqlEx) {
				throw sqlEx;
			} catch (IllegalArgumentException e) {
				throw new ENTException(e);
			} catch (IllegalAccessException e) {
				throw new ENTException(e);
			}
			
		}
		String ret = "done";  //Debugging
	}

	// Check for a local copy of a remote record

	
	public abstract void setInsertStatement(PreparedStatement psInsertRemoteData) throws SQLException;
	// Set values of prepared statment based on class var values


	private String setupNPSInsertQuery() throws SQLException {
		String q;
		List<Field> flds = this.getLocalFields(new LinkedList<Field>(), this.getClass(), true);
		q = "INSERT INTO " + this.nit.NIT_TABLE;
		q += "(";
		String fldName;
		for(Field fld : flds) {
			q += fld.getName() + ",";
		}
		q = q.substring(0,q.lastIndexOf(","));
		q += ") \n";
		q += " VALUES (";
		for(Field fld : flds) {
			q += ":" + fld.getName() + ",";
		}
		q = q.substring(0,q.lastIndexOf(","));
		q += ");";
		return q;
	}


	public NamedParameterStatement setupInsertStatement( Connection con) throws SQLException {
		String q = this.setupNPSInsertQuery();
		NamedParameterStatement nps = new NamedParameterStatement(con,q);
		return nps;
	}


	public void setUpdateStatement(PreparedStatement psUpdateRemoteData) throws SQLException{
	// Set values of prepared statment based on class var values
	}


	public abstract void setExpireAllStatement(PreparedStatement psDeleteRemoteData) throws SQLException;
	// Expire all local copies of remote data

	public void setCheckRecordStatement(PreparedStatement psSelectRemoteData) throws SQLException{
		// Check for a local copy of a remote record
	}


}


