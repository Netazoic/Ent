package com.netazoic.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.netazoic.ent.ENTException;
import com.netazoic.ent.ifServENT;
import com.netazoic.ent.rdENT;

public  class RemoteDataObj implements ifRemoteDataObj{

	public  rdENT rdEnt;


	public rdENT getEnt(HashMap<String, Object> recMap) throws ENTException{
		if(rdEnt!=null) return rdEnt;
		try{
			rdENT ent = rdEnt.getClass().newInstance();
			ent.con = con;
			if(recMap!=null) ent.init(recMap);
			return ent;
		}catch(Exception ex){
			throw new ENTException(ex);
		}
	}

	public enum RDQ_API_Param{
		moduleID, RemoteDataQuery, RemoteHost, RemoteSystemCode, RemoteDataQueryCode, fqdn,
		psSelectRemoteDataID,
		psInsertRemoteData,
		psActivateRemoteData,
		psSelectRemoteData,
		psUpdateRemoteData,
		psDeleteRemoteData,
		deleteRemoteData,
		updateRemoteData, rdqCode;
	}

	public RemoteDataObj( HashMap<String, PreparedStatement> psMap,
			ifServENT mgr, Connection con ) throws Exception {
		super();
		this.mgr = mgr;
		this.con = con;
		this.initPSMap(psMap);
		init();
	}

	public RemoteDataObj(ifServENT mgr){
		super();
		this.mgr = mgr;
		//Must run init manually
	}

	public RemoteDataObj(){
		super();
	}




	public String fqdn;
	public String pk;


	public ifServENT mgr;
	public Connection con = null;

	PreparedStatement psSelectRemoteDataID=null,
			psInsertRemoteData = null,
			psActivateRemoteData=null,
			psSelectRemoteData=null,
			psUpdateRemoteData=null,
			psDeleteRemoteData=null;

	NamedParameterStatement npsCreateRecord;


	public void init() throws Exception{
		Map settings = new HashMap<String,Object>();
		String q1,q2,q3,q4,q5,q6,q7,q8 = null;
		try {
			//String ctp = RMD_CTP.sql_Create_RemoteData_Record.ctpPath;
			String ctp = rdEnt.nit.sql_CreateENT;
			if(ctp!=null) {
				q1 = mgr.parseQuery(ctp, settings);
				psInsertRemoteData = con.prepareStatement(q1);
			}else {
				try {		
					npsCreateRecord = rdEnt.setupInsertStatement(con);
				}catch(Exception ex) {

				}
			}

			//ctp = RMD_CTP.sql_Update_RemoteData_Record.ctpPath;
			ctp = rdEnt.nit.sql_UpdateENT;
			if(ctp!=null) {
				q2 = mgr.parseQuery(ctp, settings);
				psUpdateRemoteData = con.prepareStatement(q2);
			}

			//String q4 = "SELECT * from module WHERE moduleID = ?";
			ctp = rdEnt.nit.sql_RetrieveENT;
			if(ctp!=null) {
				q4 = mgr.parseQuery(ctp, settings);
				psSelectRemoteData = con.prepareStatement(q4);
			}

			// ctp = rdEnt.nit.sql_DeleteENT;
			ctp = rdEnt.sql_DeleteRemoteData;
			if(ctp!=null) {
				q6 = mgr.parseQuery(ctp, settings);
				//String q6 = "UPDATE module SET mdactiveto = now()";
				psDeleteRemoteData = con.prepareStatement(q6);
			}else {
				psDeleteRemoteData = rdEnt.setupExpireAllStatement(con);
			}


		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void initPSMap(HashMap<String,PreparedStatement> psMap){
		this.psSelectRemoteData = psMap.get(RDQ_API_Param.psSelectRemoteData.name());
		this.psActivateRemoteData = psMap.get(RDQ_API_Param.psActivateRemoteData.name());
		this.psInsertRemoteData = psMap.get(RDQ_API_Param.psInsertRemoteData.name());
		this.psUpdateRemoteData = psMap.get(RDQ_API_Param.psUpdateRemoteData.name());
		this.psDeleteRemoteData = psMap.get(RDQ_API_Param.psDeleteRemoteData.name());
	}

	public Integer createCombinedData() {
		int ctCreated = 0;
		try {
			con.setAutoCommit(false);
			rdEnt.con = con;
			ctCreated = rdEnt.createCombinedRecs();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			try {
				con.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}finally {
			try {
				con.setAutoCommit(true);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return ctCreated;

	}

	public boolean createRemoteDataRecord(HashMap<String, Object> recMap, Connection con2) throws ENTException, SQLException {
		boolean flgCreate = false;

		//		rdEnt = getEnt(recMap);
		rdEnt.init(recMap);

		try{
			if(npsCreateRecord!=null) {
				rdEnt.setInsertStatement(npsCreateRecord);
				npsCreateRecord.execute();
				flgCreate = true;

			}
			else {
				rdEnt.setInsertStatement(psInsertRemoteData);
				flgCreate = psInsertRemoteData.execute();
			}

		}catch(Exception ex){
			throw ex;
		}
		return flgCreate;
	}

	@Override
	public int expireAllRemoteDataRecords(HashMap<String, Object> recMap) throws SQLException, ENTException {
		// Clear all existing RemoteData records for queryCode.
		// deleteRemoteData.setString(1, queryCode);
		if(rdEnt==null) rdEnt = getEnt(recMap);
		if(psDeleteRemoteData != null) {
			rdEnt.setExpireAllStatement(psDeleteRemoteData);
			psDeleteRemoteData.execute();
			return psDeleteRemoteData.getUpdateCount();
		}
		return 0;
	}

	public boolean checkRemoteDataRecord(HashMap<String, Object> recMap) throws ENTException {

		rdEnt = getEnt(recMap);
		Boolean flgFound = false;
		try{
			rdEnt.setCheckRecordStatement(psSelectRemoteData);
			ResultSet rs = psSelectRemoteData.executeQuery();
			if(rs.next()){
				flgFound = true;
			}
		}catch(Exception ex){
			throw new ENTException(ex);
		}finally{
		}
		return flgFound;
	}

	public boolean updateRemoteDataRecord(HashMap<String, Object> recMap, Connection con2) throws ENTException, SQLException {
		boolean flgUpdate = false;
		Integer flgReturn = null;

		rdEnt = getEnt(recMap);
		psUpdateRemoteData.clearParameters();
		rdEnt.setUpdateStatement(psUpdateRemoteData);

		try{
			flgReturn = psUpdateRemoteData.executeUpdate();
			flgUpdate = flgReturn > 0;

		}catch(Exception ex){

			throw ex;
		}
		return flgUpdate;
	}

	@Override
	public void setMgr(ifServENT manager) {
		this.mgr = manager;

	}

	@Override
	public void setCon(Connection con) {
		this.con = con;

	}

	@Override
	public void close(){
		if(psSelectRemoteDataID!=null) try{psSelectRemoteDataID.close();psSelectRemoteDataID=null;}catch(Exception ex){}
		if(psInsertRemoteData!=null) try{psInsertRemoteData.close();psInsertRemoteData=null;}catch(Exception ex){}
		if(psSelectRemoteData!=null) try{psSelectRemoteData.close();psSelectRemoteData=null;}catch(Exception ex){}
		if(psDeleteRemoteData!=null) try{psDeleteRemoteData.close();psDeleteRemoteData=null;}catch(Exception ex){}
		if(psUpdateRemoteData!=null) try{psUpdateRemoteData.close();psUpdateRemoteData=null;}catch(Exception ex){}
	}



}
