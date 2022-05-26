package com.netazoic.ent;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.netazoic.ent.ent.User;
import com.netazoic.util.HttpUtil;

public class authServlet extends ServENT {

	public enum AUTH_VIEW {
		Login("/Auth/Login.hbs", "Conventional Login"), CreateUser("/Auth/CreateUser.hbs",
				"Create a user record, conventional"),

		sql_GetNationalSummary("/Data/sql/OpenYet/GetNationalSummary.sql",
				"Get open-yet summary data at the national level"), sql_GetStateSummary(
						"/Data/sql/OpenYet/GetStateSummary.sql",
						"Get open-yet summary data at the state level"), sql_GetCountySummary(
								"/Data/sql/OpenYet/GetCountySummary.sql",
								"Get open-yet summary data a the county level"),

		sql_GetRemoteDataStats("/Data/sql/GetRemoteDataStats.sql", "Get stats on all remote data tables"),;
		// Why store template path and description into variables?
		public String path;
		String desc;

		AUTH_VIEW(String t, String d) {
			path = t;
			desc = d;
		}
	}

	public enum AUTH_Param {
		user, page_target, wu_login
	}

	public enum AUTH_Route {
		login("/login", "conventional login"),
		CreateUser("/createUser", "Create a user record");


		public String route;
		public String desc;

		AUTH_Route(String r, String d) {
			route = r;
			desc = d;
		}

		public static AUTH_Route getRoute(String rs) {
			for (AUTH_Route r : AUTH_Route.values()) {
				if (r.route.equals(rs))
					return r;
			}
			return null;
		}

	}

	@Override
	public void init(ServletConfig config) throws javax.servlet.ServletException {
		super.init(config);
		defaultRoute = AUTH_Route.login.route;
		routeMap.put(AUTH_Route.login.route, new LoginHdlr());
		routeMap.put(AUTH_Route.CreateUser.route, new CreateUserHdlr());
	}

	public class CreateUserHdlr extends RouteEO {

		@Override
		public void routeAction(HttpServletRequest request, HttpServletResponse response, Connection con,
				HttpSession session) throws IOException, Exception {
			Map<String, Object> map = new HashMap<String, Object>();
			Map settings = getSettings();
			String login = request.getParameter(AUTH_Param.wu_login.name());

			User user = new User();

			try {
				String destPage = AUTH_VIEW.CreateUser.path;

				if (login != null) {
					user.createRecord(HttpUtil.getRequestMap(request), con);
					session = request.getSession();
					session.setAttribute(AUTH_Param.user.name(), user);
					// Look for a forwarded deep-linking target
					destPage = (String) session.getAttribute(AUTH_Param.page_target.name());
					if (destPage == null) destPage = "/";
					ajaxResponse(user,response);
				} else {

					RequestDispatcher dispatcher = request.getRequestDispatcher(destPage);
					dispatcher.forward(request, response);
				}

			} catch (Exception ex) {
				throw new ServletException(ex);
			}
		}
	}

	public class LoginHdlr extends RouteEO {

		@Override
		public void routeAction(HttpServletRequest request, HttpServletResponse response, Connection con,
				HttpSession session) throws IOException, Exception {
			Map<String, Object> map = new HashMap<String, Object>();
			Map settings = getSettings();
			String email = request.getParameter("email");
			String password = request.getParameter("password");

			User user = new User();

			try {
				Boolean flgLoggedIn = user.checkLogin(email, password, con);
				String destPage = AUTH_VIEW.Login.path;

				if (flgLoggedIn) {
					session = request.getSession();
					session.setAttribute(AUTH_Param.user.name(), user);
					// Look for a forwarded deep-linking target
					destPage = (String) session.getAttribute(AUTH_Param.page_target.name());
					if (destPage == null)
						destPage = "/";
				} else {
					String message = "Invalid login/password";
					request.setAttribute("message", message);
				}

				RequestDispatcher dispatcher = request.getRequestDispatcher(destPage);
				dispatcher.forward(request, response);

			} catch (SQLException ex) {
				throw new ServletException(ex);
			}
		}
	}

}
