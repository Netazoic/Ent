package com.netazoic.util;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.netazoic.ent.ENTException;


public class RSObj {
	public String identifier;
	public int numRows;
	public Map<String,Object> attributes;
	public Map<String,Object>[] items;

	public RSObj(){}

	public static String createRSOTable(RSObj rso, String tblName,Connection con) throws SQLException, ENTException{
		ResultSet rs = null;
		int ct = 0;
		boolean flg = false;
		/*
		 * Create a table based on rso data.
		 * Builes a CREATE statement like the following:
		 * 
		 * CREATE TABLE films (
    code        char(5) CONSTRAINT firstkey PRIMARY KEY,
    title       varchar(40) NOT NULL,
    did         integer NOT NULL,
    date_prod   date,
    kind        varchar(10),
    len         interval hour to minute
);


		 */
		Statement myStat = con.createStatement();
		if (rso.items.length == 0) return null;
		// get the list of fields
		String strFields = null;
		String strSQL, strVals;
		String myQuery = null;
		try{
			if(tblName == null)
				tblName = "rso_" + rso.identifier + "_" + rso.numRows;
			Map flds = rso.items[0];
			Set<String> fldSet = flds.keySet();
			/*
    // Determine if table exists. If it does, delete it
    myQuery = "SELECT count(1) as ct FROM "  + tblName;
    flg = myStat.execute(myQuery);
    if(flg){
      // drop the table
      myQuery = "DROP TABLE " + tblName;
      myStat.execute(myQuery);
    }
			 */
			//Create the table
			myQuery = "CREATE TABLE " + tblName + "(\n";
			for (String fld : fldSet){
				myQuery += fld + "   text,";
			}
			myQuery = myQuery.substring(0,myQuery.length()-1);
			myQuery += "\n);";
			myStat.execute(myQuery);
		}catch(Exception ex){
			throw new ENTException(ex);
		}
		finally{
			if(myStat!=null)try{myStat.close();myStat = null;}catch(Exception ex){}
		}
		return tblName;
	}

	public static ResultSet execSQL(String myQuery, Statement myStat) throws ENTException{

		String strVal = null;
		ResultSet rs = null;
		try{
			rs = myStat.executeQuery(myQuery);
		}
		catch(Exception ex){
			throw new ENTException(ex);
		}
		finally{
			// dont! if(myStat != null)try{myStat.close();myStat=null;}catch(SQLException sqlex){}
		}
		return rs;
	}


	public static RSObj getRSObj(String myQuery, String key, Connection myCon) throws ENTException, SQLException{
		//getEFastParams(request,userSession);
		Statement myStat = null;
		RSObj rso = null;
		try{
			myStat = myCon.createStatement(
					ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_UPDATABLE);
		}catch(Exception ex){
			myStat = myCon.createStatement(
					ResultSet.TYPE_FORWARD_ONLY,
					ResultSet.CONCUR_READ_ONLY);
		}
		try{
			// see http://java.sun.com/j2se/1.4.2/docs/api/java/sql/ResultSet.html
			ResultSet rs = execSQL(myQuery,myStat);
			rso = RSObj.loadResultSetMap(rs,key);
			rso.numRows = rso.items.length;
			rso.attributes = new HashMap();
		}
		catch (SQLException sqlEx){
			throw new ENTException(sqlEx);
		}
		catch(Exception ex){
			throw new ENTException(ex);
		}
		finally{
			if(myStat != null)try{myStat.close();myStat=null;}catch(SQLException sqlex){}
		}
		return rso;  
	}

	public static RSObj getRSObj(ResultSet rs, String key) throws ENTException{
		RSObj rso = null;
		try{
			rso = RSObj.loadResultSetMap(rs,key);
			rso.numRows = rso.items.length;
			rso.attributes = new HashMap();
		}
		catch(Exception ex){
			throw new ENTException(ex);
		}
		finally{
		}
		return rso;  

	}

	public Map<String,Object>[] getItems(){
		return items;
	}

	/*
  public static RSObj getRSObjFromJSON(String json){
    // Get an rsobj from a json string

    // this would be cool, but doesn't seem to work
    //JSONDeserializer<RSObj> ds = new JSONDeserializer<RSObj>();
    //RSObj rso =  ds.use(null,RSObj.class).deserialize(json);

    RSObj rso = new RSObj();
    JSONDeserializer<Map> ds = new JSONDeserializer<Map>();
    Map rsoMap = ds.deserialize(json);
    rso.attributes = (Map<String, Object>) rsoMap.get("attributes");
    rso.identifier = (String)rsoMap.get("identifier");
    ArrayList aList = (ArrayList)rsoMap.get("items");
    Map<String,Object>[] itmMap = (Map<String,Object>[])toMapSet(aList,rso.identifier);
    rso.items = itmMap;
    return rso;
  }
	 */

	/*
  public static String itemsToJson(RSObj rso){
      String jsonData = null;
      JSONSerializer json = new JSONSerializer();
      jsonData = json.exclude("*.class").serialize(rso.items);
      return jsonData;
  }
	 */
	/*
  public  String itemsToJson(){
    String jsonData = null;
    JSONSerializer json = new JSONSerializer();
    jsonData = json.exclude("*.class").serialize(items);
    return jsonData;
}
	 */

	public static String itemsToCSV(RSObj rso, String key){
		String csv = "";
		int i =0;
		for(Map f : rso.items){
			csv += f.get(key);
			csv += ",\r\n";
			i++;
		}
		if(i>0)csv = csv.substring(0,csv.length()-3);
		return csv;
	}

	public static int loadRSOTable(RSObj rso, String tblName,Connection con) throws SQLException, ENTException{
		ResultSet rs = null;
		/*
		 * Fill an rs table with rso items
		 */
		int ct = 0;
		PreparedStatement psInsert = null;
		if (rso.items.length == 0) return 0;

		// get the list of fields
		String strFields = null;
		String strSQL, strVals;
		String myQuery = null;
		try{
			Map flds = rso.items[0];
			Set<String> fldSet = flds.keySet();
			//create insert PS
			myQuery = "INSERT INTO " + tblName + " VALUES(\n";
			for (String fld : fldSet){
				myQuery += "?,";
			}
			myQuery = myQuery.substring(0,myQuery.length()-1);
			myQuery += "\n);";
			psInsert = con.prepareStatement(myQuery);
			// insert rows
			for (Map row : rso.items){
				int x=0;
				for (String fld : fldSet){
					x++;   // setString indexing starts at 1
					psInsert.setString(x, (String)row.get(fld));    
				}
				psInsert.execute();
				ct++;
			}
		}catch(Exception ex){
			throw new ENTException(ex);
		}
		finally{
			if(psInsert != null)try{psInsert.close();psInsert = null;}catch(Exception ex){};
		}
		return ct;
	}

	public static RSObj loadResultSetMap(
			ResultSet rset, String key) throws SQLException	{
		ResultSetMetaData rsetMD = rset.getMetaData();
		LinkedList<Map<String,Object>> llist = new LinkedList();
		RSObj rso = new RSObj();
		rso.identifier = key;
		Map<String,Object> mapRow = null;
		String dateVal = null;
		int i = 0;
		int colNum, colCount=rsetMD.getColumnCount(), colType;
		String colName;

		while (rset.next()){
			mapRow = new HashMap();
			for (colNum=1;colNum<=colCount;colNum++){
				colName=rsetMD.getColumnLabel(colNum);
				if (colName==null) continue;
				colType = rsetMD.getColumnType(colNum);
				if (colType == Types.DATE) {
					Date myDate = rset.getDate(colNum);
					if(myDate == null) 
						dateVal = null;
					else
						dateVal = DateFormat.getDateInstance().format(myDate);
					//mapRow.put(colName,rset.getDate(colNum));
					mapRow.put(colName,dateVal);
				} else if (colType == Types.TIME) {
					mapRow.put(colName,rset.getTime(colNum));
				} else if (colType == Types.TIMESTAMP) {
					mapRow.put(colName,rset.getTimestamp(colNum));
				} else {	
					mapRow.put(colName, rset.getString(colNum));
				}
			}
			llist.add(mapRow);
			i++;
		}
		mapRow = null;
		rso.items = new HashMap[llist.size()];
		for(int idx = 0; idx < llist.size(); idx++){
			rso.items[idx]=llist.get(idx);
		}
		return rso;
	}

	public static boolean loadRSObj(HttpServletRequest request,
			RSObj rso, String prefix, boolean roll, boolean overwrite) 
					throws ENTException{
		//Load first item in rsObj into request
		Map[] items = rso.items;
		if(items.length == 0) return false;
		if(prefix == null) prefix = "";
		// What is "roll"?
		//if (roll && items.length == 0) return false;
		Map item0 = items[0];
		Set<String> keys = item0.keySet();
		int colNum, colCount=item0.size(), colType;
		String colName;
		// Iterate through the columns, adding the value of each one to the
		// request.
		for (String k : keys){
			colName = k;
			if (colName==null) continue;
			if (!overwrite && request.getAttribute(prefix + colName)!=null) continue;
			request.setAttribute(prefix + colName, item0.get(colName));
		}
		return true;
	}

	public static <K,V> Map<K,V>[] toMapSet(List<V> list, String keyField){
		//Convert an array list to a set of Hash Maps
		Map<K,V>[] mapSet = new HashMap[list.size()];
		int i = 0;
		for(V obj : list){
			Map m = (HashMap<K,V>) obj;
			mapSet[i] = m;
			i++;
		}
		return mapSet;
	}

	public static Map<String,String> stringIt(Map<String,Object> m) throws ENTException{
		Map<String,String> m2 = new HashMap();
		String st;
		Object e;
		Class cl;
		for(String k : m.keySet()){
			e = m.get(k);
			if(e == null) continue;
			cl = e.getClass();

			if (e instanceof Date) {
				m2.put(k, unix2pgds(e.toString()));    
			} else {
				m2.put(k, e.toString());
			}

		}
		return(m2);
	}

	public static String unix2pgds(String ds) throws ENTException {
		/*
		 * convert a unix datestring to postgres format
		 * convert 1963-12-03 to 12/03/1963
		 */
		if (ds==null) {
			return null;
		} else {
			try {
				//Strip anything past ymd
				ds = ds.replaceAll(" .*$", "");
				//incoming
				SimpleDateFormat myDF1 = new SimpleDateFormat("yyyy-MM-dd");
				//target
				SimpleDateFormat myDF2 = new SimpleDateFormat("MM/dd/yyyy");
				java.util.Date theDate = myDF1.parse(ds);
				return myDF2.format(theDate);
			} catch (ParseException pex) {
				throw new ENTException("pg2unixds: Error parsing date: " + ds);
			}

		}
	}

}
