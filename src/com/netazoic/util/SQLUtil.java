package com.netazoic.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;



public class SQLUtil {
	
	public enum SQL_Param{
		UserName, DatabaseServer, Password, jdbcLogLevel
	}
	
	public static void closeStatement(Statement myStat) {
		if(myStat!=null)try{myStat.close();myStat = null;}catch(Exception ex){}
	}

	public static Long execSQLLong(PreparedStatement ps, Connection con) throws SQLException{
		//For prepared statements RETURNING Long, such as a CREATE query that returns record ID
		Long id = (long) 0;
		try{		
			ResultSet rs = ps.executeQuery();
			id = rs.getLong(1);
		}
		catch(SQLException sqlex){
			throw new SQLException(sqlex);
		}
		finally{
		}
		return id;
	}
	public static int execSQL(String q, Connection con) throws SQLException{
		Statement myStat = null;
		int result = 0;
		try{
			myStat = con.createStatement();
			myStat.execute(q);
			result = myStat.getUpdateCount();
		}
		catch(SQLException sqlex){
			throw new SQLException(sqlex);
		}
		finally{
			if(myStat != null)try{myStat.close();myStat=null;}catch(SQLException sqlex){}
		}
		return result;
	}

	public synchronized static String execSQL(String myQuery,String key, Connection con) throws SQLException{

		Statement myStat = null;
		String result = null;
		ResultSet rs = null;
		try{
			myStat = con.createStatement();
			rs = myStat.executeQuery(myQuery);
			if(rs.next()){
				result = rs.getString(key);
			}
		}
		catch(SQLException sqlex){
			throw new SQLException(sqlex);
		}
		finally{
			if(myStat != null)try{myStat.close();myStat=null;}catch(SQLException sqlex){}
		}
		return result;
	}
	
	  public synchronized static ResultSet execSQL(String myQuery, Statement myStat) throws SQLException{

		    String strVal = null;
		    ResultSet rs = null;
		    try{
		      rs = myStat.executeQuery(myQuery);
		    }
		    catch(SQLException ex){
		      throw ex;
		    }
		    finally{
		      // don't close the statement		    
		    }
		    return rs;
		  }
		  
	
	  // Call this function to make a string SQL compatible.
	  public static String fixString(String result){
	    // Iterate through the string, searching for cases of a single quote
	    // character, and replacing them with two single quote characters.
	    if (result == null) return null;  
	    result = result.replaceAll("\'\'", "DOUBLE_SINGLE_QUOTE");
	    result = result.replaceAll("\'", "''");
	    result = result.replaceAll("DOUBLE_SINGLE_QUOTE", "''");
	     //result = result.replace('\\', '/');
	    return result;
	  }
	
	public static Connection getConnection(String userName, String password, String connURL, String jdbcLogLevel) throws SQLException {

	    Connection conn = null;
	    try {
	    	Properties props = new Properties();
	    	props.setProperty("user", userName);
	    	props.setProperty("password", password);
	    	props.setProperty("logUnclosedConnections", "true");
	    	props.setProperty("logLevel", jdbcLogLevel);	
	    	//props.setProperty("standard_conforming_strings", "on");
	    	conn = DriverManager.getConnection(connURL,props);
	    } catch (SQLException sqlex) {
	      // TODO Auto-generated catch block
	      throw sqlex;
	    }
	    return conn;
	}
	
	 public static Connection getConnection(Map<String,String>map) throws  SQLException{
		    Connection conn;
		    String userName,password,jdbcLogLevel,connURL;
		    connURL = (String)map.get(SQL_Param.DatabaseServer.name());
		    userName = (String)map.get(SQL_Param.UserName.name());
		    password = (String)map.get(SQL_Param.Password.name());
		    jdbcLogLevel = (String)map.get(SQL_Param.jdbcLogLevel.name());
		    return getConnection(userName,password,connURL,jdbcLogLevel);
		  }


	  public static Long getNextID(String idField, String table, Connection con) throws SQLException{
		  String q = "SELECT max(" + idField + ")+1 as id FROM " + table;
		  String id = SQLUtil.execSQL(q, "id", con);
		  return Long.parseLong(id);	  
	  }
	  /*
	  public static Long getNextID(String seqName,Connection con){
		  String q = "SELECT NEXTVAL as id FROM " + seqName;
		  String id = SQLUtil.execSQL(q, "id", con);
	  }
	  */
	  
	public static void releaseConnection(Connection con) throws SQLException{
		con.close();
		con = null;
	}
}
