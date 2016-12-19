package com.netazoic.ent;

import java.io.IOException;
import java.sql.Connection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;




public interface RouteAction {
	public abstract void doRoute(HttpServletRequest request, HttpServletResponse
	  response, HttpSession session) throws ServletException, IOException, Exception;

	public abstract void routeAction(HttpServletRequest request, HttpServletResponse response, Connection con, HttpSession session) 
			throws IOException, Exception;
	}