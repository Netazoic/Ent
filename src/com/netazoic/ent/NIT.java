package com.netazoic.ent;

import java.lang.reflect.Field;


public class NIT<T> {
	
	public String ENTITY_NAME;
	public String NIT_TABLE;
	public String FLD_NIT_ID;


	public  String entityName, nitTable, nitName,evtTable,statusTable,evtClass,nitID,fld_nitID;
	public  String sql_RetrieveENT, sql_CreateENT, sql_UpdateENT, sql_DeleteENT, sql_seqName, sql_ExpireENT;
	public Field nitIDField;
	
	public String sql_CreateEVT;
	public String nitCode = null;
	public String nitTitle = null;
	public String nitDesc = null;
	public String nitURL = null;
	public String nitTypeCode = null;
	
	public void initNIT(Class clazz) throws ENTException {
		try{
			assert(ENTITY_NAME != null);
			assert(NIT_TABLE != null);
			assert(FLD_NIT_ID != null);
			entityName = ENTITY_NAME;
			nitTable = NIT_TABLE;
			fld_nitID = FLD_NIT_ID;

			nitIDField = clazz.getField(fld_nitID);
			nitName = entityName;		

		}catch(Exception ex){
			throw new ENTException(ex);
		}
	}

}
