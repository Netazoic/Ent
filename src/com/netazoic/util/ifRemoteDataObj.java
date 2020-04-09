package com.netazoic.util;


import java.io.Closeable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;

import com.netazoic.ent.ifServENT;


public interface ifRemoteDataObj extends Closeable {

    void expireAllRemoteDataRecords(HashMap<String, Object> recMap) throws SQLException, Exception;


    boolean checkRemoteDataRecord(HashMap<String, Object> recMap)throws Exception;

    boolean updateRemoteDataRecord(HashMap<String, Object> recMap, Connection con) throws Exception, SQLException;

    boolean createRemoteDataRecord(HashMap<String, Object> recMap, Connection con) throws Exception, SQLException;

    void setMgr(ifServENT manager);

    void setCon(Connection con);

    void initPSMap(HashMap<String, PreparedStatement> psMap);




}
