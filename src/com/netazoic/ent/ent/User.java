package com.netazoic.ent.ent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.HashMap;

import com.netazoic.ent.ENTException;

public class User extends com.netazoic.ent.ENT {

	Long webuserID;
	String wuLogin;
	String wuPassword; // MD5 encrypted
	String wuFirstName;
	String wuLastName;
	String wuEmail;
	String wuDisplayName;
	LocalTime wuDateAdded;
	String webuserStatusCode;
	Long datapartitionID;
	Long authenticationPolicyID;

	@Override
	public Long createRecord(HashMap paramMap, Connection con) throws ENTException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteRecord(String webuserID, String comments) throws ENTException {
		// TODO Auto-generated method stub

	}

	public Boolean checkLogin(String email, String password, Connection con) throws SQLException {
		Boolean loggedIn = false;
		String sql = "SELECT * FROM webuser WHERE wulogin = ? and wupassword = ?";
		PreparedStatement pstat = null;
		try {
			pstat = con.prepareStatement(sql);
			pstat.setString(1, email);
			pstat.setString(2, password);

			ResultSet result = pstat.executeQuery();

			if (result.next()) {
				this.wuLogin = result.getString("wulogin");
				this.wuEmail = result.getString("wuemail");
				loggedIn = true;
			}
		} catch (SQLException ex) {
			throw ex;
		}
		finally {
			if(pstat!=null)try {pstat.close();pstat=null;}catch(Exception ex) {}
		}
		return loggedIn;
	}

}
