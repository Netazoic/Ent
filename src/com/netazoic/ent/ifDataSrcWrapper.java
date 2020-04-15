package com.netazoic.ent;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.HashMap;

import org.apache.logging.log4j.Logger;

import com.netazoic.covid19.ent.ifDataSrc;
import com.netazoic.covid19.ent.ifDataType;
import com.netazoic.ent.rdENT.DataFmt;
import com.netazoic.ent.rdENT.SRC_ORG;
import com.netazoic.util.NamedParameterStatement;
import com.netazoic.util.ifRemoteDataObj;



public interface ifDataSrcWrapper {
	public class MutableInt {
		public int value = 0; // 
		public void increment () { ++value;      }
		public void decrement () { --value; }
		public int  get ()       { return value; }
		public MutableInt(int v) {
			this.value = v;
		}
	}

	public class RemoteDataRecordCtr{
		public MutableInt ctTotalRecords;
		public MutableInt ctBadRecords;
		public MutableInt ctNewRecordsCreated;
		
		public RemoteDataRecordCtr(){
			this.ctTotalRecords = new MutableInt(0);
			this.ctBadRecords = new MutableInt(0);
			this.ctNewRecordsCreated = new MutableInt(0);
		}
	}

	void init(HashMap<String, Object> recMap) throws ENTException;

	void setType(ifDataType type);

	void setInsertStatement(NamedParameterStatement nps) throws SQLException, ENTException;

	void setCon(Connection con);

	PreparedStatement setupExpireAllStatement(Connection con) throws SQLException;

	void setSrc(ifDataSrc src);

	ifDataSrc getSrc();
	
	String getDataURL();
	
	SRC_ORG getSrcOrg();

	DataFmt getFormat();

	void importRecords(ifRemoteDataObj rmdObj, RemoteDataRecordCtr ctrObj, Logger logger, Savepoint savePt, Connection con,
			InputStream is) throws IOException, Exception, SQLException;
}
