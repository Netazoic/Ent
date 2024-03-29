package com.netazoic.ent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netazoic.util.JSONUtil;
import com.netazoic.util.ParseUtil;

/*
 * References:
 * http://www.javaranch.com/journal/200601/JDBCConnectionPooling.html
 * http://www.cyberciti.biz/faq/howto-add-postgresql-user-account/
 * http://tomcat.apache.org/tomcat-6.0-doc/jndi-datasource-examples-howto.html#PostgreSQL
 */

public class ServENT extends HttpServlet implements ifServENT {
	public Map<String, RouteAction> routeMap;
	public String defaultRoute;
	public DataSource dataSource = null;
	public String driverManagerURL, driverManagerUser, driverManagerPwd;

	public boolean flgDebug = false;

	public ParseUtil parser = new ParseUtil();
	private static final Logger logger = LogManager.getLogger(ServENT.class);

	public enum ENT_Param {
		netRoute, routeString, Settings, jndiDB, sqliteDB, dbUser, dbPwd, TemplatePath, request, response, context;
	}

	public void init(ServletConfig config) throws javax.servlet.ServletException {
		super.init(config);
		ServletContext context;
		Map<String, Object> settings;

		// We need to create the ConnectionPool, ServerSettings, Codes,
		// Authenticator, and anything else we need here.
		context = config.getServletContext();
		routeMap = new HashMap<String, RouteAction>();
		String jndiDB = null;
		String sqliteDB = null;
		synchronized (context) {
			settings = getSettings();
			if (settings == null) {
				context.log("Creating Settings.");
				settings = new HashMap<String, Object>();
				Enumeration<String> params = context.getInitParameterNames();
				Object temp;
				while (params.hasMoreElements()) {
					temp = params.nextElement();
					settings.put((String) temp, context.getInitParameter(temp.toString()));
				}
				putSettings(settings);
			}
			try {
				// JNDI data connector
				jndiDB = context.getInitParameter(ENT_Param.jndiDB.name());
				// if present, should be a string in the form "jdbc/<dbname>"
				// default
				// if(jndiDB == null) jndiDB = "postgres";
				if (jndiDB != null) {
					// Look up the JNDI data source only once at init time
					InitialContext cxt = new InitialContext();
					dataSource = (DataSource) cxt.lookup("java:/comp/env/" + jndiDB);
					if (dataSource == null) {
						throw new ServletException("Data source not found!");
					}
				}
				// SQLite data connector
				// SQLite does not support JNDI or connection pooling.
				// So we need to use the DriverManager method for making a connection
				sqliteDB = context.getInitParameter(ENT_Param.sqliteDB.name());
				if (sqliteDB != null) {
					driverManagerURL = "";
					if (sqliteDB.indexOf("jdbc:") < 0)
						driverManagerURL = "jdbc:";
					if (sqliteDB.indexOf("sqlite:") < 0)
						driverManagerURL += "sqlite:";
					driverManagerURL += sqliteDB;
					driverManagerUser = context.getInitParameter(ENT_Param.dbUser.name());
					driverManagerPwd = context.getInitParameter(ENT_Param.dbPwd.name());
				}
			} catch (NamingException e) {
				// specified dbName not found in the tomcat server.xml file
				log("DBName not found in server.xml: " + jndiDB);
			}

		}

		parser.templatePath = config.getServletContext().getInitParameter(ENT_Param.TemplatePath.name());
		if (parser.templatePath == null) {
			throw new javax.servlet.ServletException("TemplatePath param not set.");
		}
		parser.appRootPath = config.getServletContext().getRealPath("/");

	}

	public void ajaxError(Exception ex, HttpServletResponse res) throws IOException {
		ajaxError(ex.getMessage(), res);
	}

	public void ajaxError(Exception ex, HttpServletResponse res, boolean flgDebug) throws IOException {
		ajaxError(ex, res);
		String jsonError = getJSON(ex);

		if (flgDebug) {
			String stMsg = "\n\n";
			StackTraceElement[] ste = ex.getStackTrace();
			for (StackTraceElement st : ste) {
				stMsg += st.toString() + "\n";
			}
			res.getWriter().print(stMsg);
		}
	}

	public void ajaxError(String errMsg, HttpServletResponse res) throws IOException {
		// res.sendError(ex.getMessage());
		PrintWriter pw = null;
		try {
			pw = res.getWriter();
		} catch (IllegalStateException isex) {
			res.reset();
			pw = res.getWriter();
		}
		res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errMsg); // 500
		// res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errMsg);
		res.setContentType("text");
		res.setHeader("Cache-Control", "no-cache");

		pw.print(errMsg);
	}

	public void ajaxError(String errMsg, Exception ex, HttpServletResponse res) throws IOException {
		// res.sendError(ex.getMessage());
		PrintWriter pw = null;
		try {
			pw = res.getWriter();
		} catch (IllegalStateException isex) {
			res.reset();
			pw = res.getWriter();
		}
		res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errMsg); // 500
		// res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errMsg);
		res.setContentType("text");
		res.setHeader("Cache-Control", "no-cache");
		if (flgDebug) {
			String stMsg = "\n\n";
			StackTraceElement[] ste = ex.getStackTrace();
			for (StackTraceElement st : ste) {
				stMsg += st.toString() + "\n";
			}
			res.getWriter().print(stMsg);
		}

		pw.print(errMsg);
	}

	public void ajaxResponse(Object obj, HttpServletResponse response) throws IOException, ENTException {
		String respJson = JSONUtil.toJSON(obj);
		ajaxResponse(respJson, response);
	}

	public void ajaxResponse(String strJson, HttpServletResponse response) throws IOException {

		ajaxResponse(strJson, "application/json", response);
	}

	public void ajaxResponse(String strResponse, String contentType, HttpServletResponse response) throws IOException {
		response.setContentType(contentType);
		response.setHeader("Cache-Control", "no-cache");
		response.getWriter().write(strResponse);
	}

	private boolean authenticate(HttpServletRequest request, RouteAction route) {
		// Check to see if the user has a sessionID
		// if not, redirect to 401
		HttpSession session = request.getSession(false);
		if (session == null) {
			return false;
		}
		return true;

	}

	public Boolean checkAJAX(HttpServletRequest request) {
		/*
		 * Determines if this request passed in by an ajax XHR method
		 * 
		 */
		Boolean flgXhr = false;
		String strXhr = (String) request.getHeader("X-Requested-With");
		if (strXhr == null) {
			strXhr = (String) request.getAttribute("xhr");
			if (strXhr == null || !strXhr.equalsIgnoreCase("true"))
				strXhr = null;
		}
		flgXhr = (strXhr != null && !strXhr.equals("com.android.browser"));
		return (flgXhr);
	}

	private boolean checkPermissions(HttpServletRequest request, RouteAction route) {
		// TODO Auto-generated method stub
		return true;
	}

	public void doParamParsing(HttpServletRequest request, Enumeration<String> params) {
		String paramName;
		// Load the parameters into the attributes.
		while (params.hasMoreElements()) {
			paramName = params.nextElement();
			if (request.getAttribute(paramName) == null) {
				String[] vals = request.getParameterValues(paramName);
				// Handling of multi-valued form params
				if (vals.length == 1)
					request.setAttribute(paramName, vals[0]);
				else {
					String val = "";
					for (String s : vals) {
						if (s.equals(""))
							continue;
						val += s;
						val += ":";
					}
					if (val.indexOf(":") > -1) {
						// Strip any opening ':' junk
						while (val.indexOf(":") == 0) {
							val = val.substring(1);
						}
						// Strip a trailing ':'
						val = val.substring(0, val.lastIndexOf(":"));
					}
					request.setAttribute(paramName, val);
				}
			}
		}
	}

	public RouteAction getRouteHandler(HttpServletRequest request) {
		String routeString = getRoutePrimary(request);
		RouteAction route = (RouteAction) routeMap.get(routeString);
		if (route == null)
			route = (RouteAction) routeMap.get(defaultRoute);
		return route;
	}

	public String getRoutePrimary(HttpServletRequest request) {
		String url = request.getRequestURI();
		// TODO make smarter to handle multi-segmented routes like route/sub-route

		// Action is the last part of the uri, after the last "/" and before any query
		// string or # locator
		Integer idxQ = 0, idxP = 0, idxE;
		if (url.matches("\\?"))
			idxQ = url.indexOf('?');
		if (url.matches("#"))
			idxP = url.indexOf('#');
		idxE = idxQ > 0 ? idxQ : idxP > 0 ? idxP : url.length();
		String routeString = url.substring(url.indexOf('/'), idxE);

		if (routeString != null) {
			request.setAttribute(ENT_Param.routeString.name(), routeString);
		}
		return routeString;
	}

	public String getRouteString(HttpServletRequest request) {
		return getRoutePrimary(request);
	}
	
	public Connection getConnection(String connKey) throws SQLException {
		//TODO hook up connection pooling
		// For now, just return a connection
		return getConnection();
	}

	public Connection getConnection() throws SQLException {
		Connection con = null;
		if (dataSource != null)
			con = dataSource.getConnection();
		else if (driverManagerURL != null) {
			if (driverManagerUser != null && driverManagerPwd != null) {
				con = DriverManager.getConnection(driverManagerURL, driverManagerUser, driverManagerPwd);
			} else {
				con = DriverManager.getConnection(driverManagerURL);
			}
		}
		return con;
	}

	public HashMap<String, Object> getRequestMap(HttpServletRequest request) {
		HashMap<String, Object> m = new HashMap<String, Object>();
		Enumeration<String> rkeys = request.getAttributeNames();
		String k;
		while (rkeys.hasMoreElements()) {
			k = rkeys.nextElement();
			m.put(k, request.getAttribute(k));
		}
		return m;
	}

	public Map<String, Object> getSettings() {
		return (Map<String, Object>) getServletContext().getAttribute(ENT_Param.Settings.name());
	}

	public Object getSetting(String key) {
		return getSettings().get(key);
	}

	public String parseQuery(String tPath) throws Exception {
		Map<String, Object> map = new HashMap();
		return parser.parseQuery(tPath, map);
	}

	@Override
	public String parseQuery(String tPath, Map<String, Object> map) throws Exception {
		return parser.parseQuery(tPath, map);
	}

	public void parseOutput(Map<String, Object> map, String tPath, HttpServletResponse resp) throws Exception {
		parser.parseOutput(map, tPath, resp.getWriter());

	}

	public void putSettings(Map<String, Object> settings) {
		getServletContext().setAttribute(ENT_Param.Settings.name(), settings);
	}

	public String getJSON(Object obj) throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(obj);
	}
	
	public void releaseConnection(Connection con) {
		//TODO hook up to connection pool
		if (con != null)
			try {
				con.close();
			} catch (SQLException e) {
			}
		
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

	public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		logger.debug("Starting up service for " + this.getClass());

		request.setCharacterEncoding("utf-8");
		Enumeration<String> params = request.getParameterNames();
		doParamParsing(request, params);
		HttpSession session = request.getSession();
		RouteAction route = getRouteHandler(request);
		// Boolean flgXHR = checkAJAX(request);
		Boolean flgAjax = true;
		if (!authenticate(request, route)) {
			// Throw a 401
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "please log in");
		}
		// TODO if authenticated, check permissions for route
		if (!checkPermissions(request, route)) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
					"We are sorry, but you are not authorized for this action.");
		}
		if (route != null)
			try {
				route.doRoute(request, response, session);
			} catch (Exception ex) {
				if (flgAjax) {
					ajaxError(ex, response, flgDebug);
					// ajaxResponse(ex.getMessage(),response);
				} else {
					throw new ServletException(ex);
				}
			}
		else {
			// no handler for the requested route
			throw new ServletException("Invalid Request!");
		}
	}

	public abstract class RouteEO implements RouteAction {
		protected HashMap<String, Object> requestMap;

		public void init() {
		}

		public void doRoute(HttpServletRequest request, HttpServletResponse response, HttpSession session)
				throws Exception {
			Connection con = null;
			requestMap = getRequestMap(request);
			try {
				con = getConnection();
				// Do stuff here
				routeAction(request, response, con, session);
			} catch (SQLException sqlException) {
				sqlException.printStackTrace();
			} catch (Exception ex) {
				logger.debug(ex.getMessage());
				ex.printStackTrace();
			} finally {
				releaseConnection(con);
			}
		}

		public abstract void routeAction(HttpServletRequest request, HttpServletResponse response, Connection con,
				HttpSession session) throws IOException, Exception;

	}
}