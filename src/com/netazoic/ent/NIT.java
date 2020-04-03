package com.netazoic.ent;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;




public class NIT<T> {
	
	public String ENTITY_NAME;
	public String NIT_TABLE;
	public String FLD_NIT_ID;


	public  String entityName, nitTable, nitName,evtTable,statusTable,evtClass,fld_nitID;
	public  String sql_RetrieveENT, sql_CreateENT, sql_UpdateENT, sql_DeleteENT, sql_seqName, sql_ExpireENT;
	public Field nitIDField;
	
	public Long nitID;
	
	public String sql_CreateEVT;
	public String nitCode = null;
	public String nitTitle = null;
	public String nitDesc = null;
	public String nitURL = null;
	public String nitTypeCode = null;
    public Class<? extends Enum> ctpClass;
    @JsonIgnore
    public HashMap<String,String> ctpMap;
    
    public  enum NIT_TP implements if_TP{
        CREATE("/NEWS/ent/Foo/CreateRecord.ctp"),
        RETRIEVE("/NEWS/ent/Foo/EditReord.ctp"),
        EDIT("/NEWS/ent/Foo/EditRecord.ctp"),

        sql_CREATE_RECORD("/NEWS/ent/Foo/sql/CreateRecord.sql"),
        sql_GET_LIST("/NEWS/ent/Foo/sql/GetRecordList.sql"),
        sql_GET_RECORD("/NEWS/ent/Foo/sql/GetRecord.sql"),
        sql_RETRIEVE_RECORD("/NEWS/ent/Foo/sql/GetRecord.sql"),  //Alias for GET_RECORD
        sql_UPDATE_RECORD("/NEWS/ent/Foo/sql/UpdateRecord.sql"),
        sql_DELETE_RECORD("/NEWS/ent/Foo/sql/DeleteRecord.sql"),

        sql_CREATE_EVT("/NEWS/ent/Foo/sql/CreateEvt.sql"),

        //search
        CUSTOM_SEARCH("/NEWS/ent/Foo/RecordSearch.ctp"),
        QUEUE("/NEWS/ent/Foo/RecordQueue.ctp"),
        sql_GET_QUEUE("/NEWS/ent/Foo/sql/GetRecordQueue.sql");
        public String tPath;
        private static final Map<String,NIT_TP> lookup
        = new HashMap<String,NIT_TP>();

        public static final Map<String,String> pathMap
        = new HashMap<String,String>();

        static {
            for(NIT_TP f : EnumSet.allOf(NIT_TP.class))
                lookup.put(f.getTP(), f);
        }

        static{
            for(NIT_TP f : EnumSet.allOf(NIT_TP.class)){
                pathMap.put(f.name(),f.getTP());
            }
        }

        private NIT_TP(String p) {
            this.tPath = p;
        }

        public static NIT_TP get(String path) {
            return lookup.get(path);
        }
        public static String getPathLookup(String key){
            return pathMap.get(key);
        }

        @Override
        public String getTP() {
            return this.tPath;
        }

        @Override
        public void setTP(String p) {
            this.tPath = p;

        }
    }
	
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
            if(ctpClass != null){
                loadCTPVals(ctpClass);
            }

		}catch(Exception ex){
			throw new ENTException(ex);
		}
	}
	
    public void loadCTPVals(Class<? extends Enum> enumClass) throws ENTException{
        if_TP f2;
        boolean flgDebug = false;
        if(ctpMap == null) ctpMap = new HashMap<String,String>();
        for(NIT_TP f : EnumSet.allOf(NIT_TP.class)){
            try{
                f2 = (if_TP) Enum.valueOf(enumClass, f.name());
                ctpMap.put(f.name(), f2.getTP());
                //f.ctpPath = f2.getCTP();
            }catch(Exception ex){
                // No definition for f.name in ATY_CTP
                // nada
                // throw new ClarescoException(ex);
                // DEBUG
                if(flgDebug) System.out.println(ex.getMessage());
            }
        }
        //Set some nit ctp vals
        sql_UpdateENT = ctpMap.get(NIT_TP.sql_UPDATE_RECORD.name());
        sql_CreateENT = ctpMap.get(NIT_TP.sql_CREATE_RECORD.name());
        sql_CreateEVT = ctpMap.get(NIT_TP.sql_CREATE_EVT.name());
        String getCTP = ctpMap.get(NIT_TP.sql_GET_RECORD.name());
        if(getCTP == null){
            getCTP = ctpMap.get(NIT_TP.sql_RETRIEVE_RECORD.name());
            ctpMap.put(NIT_TP.sql_GET_RECORD.name(),getCTP);
        }
        sql_RetrieveENT = getCTP;
        sql_DeleteENT = ctpMap.get(NIT_TP.sql_DELETE_RECORD.name());
    }

}
