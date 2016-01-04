package com.netazoic.ent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netazoic.util.ParseUtil;

/*
 * References:
 * http://www.javaranch.com/journal/200601/JDBCConnectionPooling.html
 * http://www.cyberciti.biz/faq/howto-add-postgresql-user-account/
 * http://tomcat.apache.org/tomcat-6.0-doc/jndi-datasource-examples-howto.html#PostgreSQL
 */

public class ServENT extends HttpServlet {
	public Map<String, NetRoute> routeMap;
	public String defaultRoute;
	public  DataSource dataSource = null;
	public String driverManagerURL, driverManagerUser, driverManagerPwd;

	public ParseUtil parser = new ParseUtil();

	public enum ENT_Param{
		netRoute, routeString, Settings, jndiDB, sqliteDB, dbUser, dbPwd, TemplatePath;}


	public void init(ServletConfig config) throws javax.servlet.ServletException {
		super.init( config);
		ServletContext context;
		Map<Object, String> settings;

		// We need to create the ConnectionPool, ServerSettings, Codes,
		// Authenticator, and anything else we need here.
		context = config.getServletContext();
		routeMap = new HashMap<String,NetRoute>();
		String jndiDB = null;
		String sqliteDB = null;
		synchronized (context) {
			settings = getSettings();
			if (settings == null) {
				context.log("Creating Settings.");
				settings = new HashMap<Object, String>();
				Enumeration<String> params = context.getInitParameterNames();
				Object temp;
				while (params.hasMoreElements()) {
					temp = params.nextElement();
					settings.put(temp, context.getInitParameter(temp.toString()));
				}
				putSettings(settings);
			}
			try {
				//JNDI data connector
				jndiDB = context.getInitParameter(ENT_Param.jndiDB.name());
				//if present, should be a string in the form "jdbc/<dbname>"
				// default
				//if(jndiDB == null) jndiDB = "postgres";
				if(jndiDB != null){
					// Look up the JNDI data source only once at init time
					InitialContext cxt = new InitialContext();
					dataSource = (DataSource) cxt.lookup( "java:/comp/env/" + jndiDB );
					if ( dataSource == null ) {
						throw new ServletException("Data source not found!");
					}
				}
				//SQLite data connector
				//SQLite does not support JNDI or connection pooling.
				//So we need to use the DriverManager method for making a connection
				sqliteDB = context.getInitParameter(ENT_Param.sqliteDB.name());
				if(sqliteDB != null){
					driverManagerURL = "";
					if(sqliteDB.indexOf("jdbc:")<0) driverManagerURL = "jdbc:";
					if(sqliteDB.indexOf("sqlite:")<0) driverManagerURL += "sqlite:";
					driverManagerURL += sqliteDB;
					driverManagerUser = context.getInitParameter(ENT_Param.dbUser.name());
					driverManagerPwd = context.getInitParameter(ENT_Param.dbPwd.name());
				}
			} catch (NamingException e) {
				//specified dbName not found in the tomcat server.xml file
				log("DBName not found in server.xml: " + jndiDB);
			}

		}
		
		parser.templatePath = config.getServletContext().getInitParameter(ENT_Param.TemplatePath.name());
		if(parser.templatePath == null){
			throw new javax.servlet.ServletException("TemplatePath param not set.");
		}
		parser.appRootPath = config.getServletContext().getRealPath("/");

	}

	public void ajaxResponse(String json, HttpServletResponse response)
			throws IOException {
		response.setContentType("application/json");
		response.setHeader("Cache-Control", "no-cache");
		response.getWriter().write(json);
	}

	public void doParamParsing(HttpServletRequest request,
			Enumeration<String> params) {
		String paramName;
		// Load the parameters into the attributes.
		while (params.hasMoreElements()) {
			paramName = params.nextElement();
			if (request.getAttribute(paramName)==null){
				String[] vals = request.getParameterValues(paramName);
				//Handling of multi-valued form params
				if(vals.length == 1) request.setAttribute(paramName, vals[0]);
				else {
					String val = "";
					for(String s : vals){
						if(s.equals(""))continue;
						val += s;
						val += ":";
					}
					if(val.indexOf(":") > -1){
						//Strip any opening ':' junk
						while(val.indexOf(":") == 0){
							val = val.substring(1);
						}
						//Strip a trailing ':'
						val = val.substring(0,val.lastIndexOf(":"));
					}
					request.setAttribute(paramName, val);
				}
			}
		}
	}

	public NetRoute getRoute(HttpServletRequest request) {
		String routeString = getRoutePrimary(request);



		NetRoute route = (NetRoute)routeMap.get(routeString);
		if(route==null) route = (NetRoute)routeMap.get(defaultRoute);
		return route;
	}

	public String getRoutePrimary(HttpServletRequest request) {
		String url = request.getRequestURI();
		//TODO make smarter to handle multi-segmented routes like route/sub-route

		//Action is the last part of the uri, after the last "/" and before any query
		// string or # locator
		Integer idxQ=0,idxP=0,idxE;
		if(url.matches("\\?")) idxQ = url.indexOf('?');
		if(url.matches("#")) idxP = url.indexOf('#');
		idxE = idxQ>0?idxQ:idxP>0?idxP:url.length();
		String routeString = url.substring(url.lastIndexOf('/')+1,idxE);

		if(routeString != null){
			request.setAttribute(ENT_Param.routeString.name(), routeString);
		}
		return routeString;
	}


	public Connection getConnection() throws SQLException {
		Connection con = null;
		if(dataSource != null) 	con = dataSource.getConnection();
		else if(driverManagerURL != null){
			if (driverManagerUser != null && driverManagerPwd != null) {
				con = DriverManager.getConnection(driverManagerURL, driverManagerUser, driverManagerPwd);
			} else {
				con = DriverManager.getConnection(driverManagerURL);
			}
		}
		return con;
	}

	public Map<Object, String> getSettings() {
		return (Map<Object, String>)getServletContext().getAttribute(ENT_Param.Settings.name());
	}
	public String getSetting(String key){
		return getSettings().get(key);
	}

	public void putSettings(Map<Object,String>settings){
		getServletContext().setAttribute(ENT_Param.Settings.name(), settings);
	}

	public static JsonNode putToJSON(HttpServletRequest req) throws JsonProcessingException, IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(req.getInputStream()));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = br.readLine()) != null) {
			sb.append(line);
		}
		String data = br.readLine();
		ObjectMapper mapper = new ObjectMapper();
		JsonNode obj = mapper.readTree(sb.toString());
		return obj;
	}

	public void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException{
		request.setCharacterEncoding("utf-8");
		Enumeration<String> params = request.getParameterNames();
		doParamParsing(request, params);
		HttpSession session = request.getSession();
		NetRoute route = getRoute(request);
		if (route!=null)
			try {
				route.doRoute(request, response, session);
			} catch (Exception e) {
				throw new ServletException(e);
			}
		else {
			// no handler for the requested route
			throw new ServletException("Invalid Request!");
		}
	}

	public abstract class RouteEO implements NetRoute {

		public void doRoute(HttpServletRequest request,
				HttpServletResponse response, HttpSession session)
						throws Exception {
			Connection con = null;
			try{
				con = getConnection();
				//Do stuff here
				routeAction(request,response,con,session);
			} 
			catch (SQLException sqlException) {
				sqlException.printStackTrace();
			}
			finally {
				if (con != null) 
					try {con.close();} catch (SQLException e) {}
			}
		}

		public abstract void routeAction(HttpServletRequest request, HttpServletResponse response, Connection con, HttpSession session) throws IOException, Exception;

	}
}