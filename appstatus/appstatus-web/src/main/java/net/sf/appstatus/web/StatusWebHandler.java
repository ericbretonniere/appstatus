/*
 * Copyright 2010-2012 Capgemini
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 * 
 */
package net.sf.appstatus.web;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.appstatus.core.AppStatus;
import net.sf.appstatus.core.AppStatusStatic;
import net.sf.appstatus.web.pages.AbstractPage;
import net.sf.appstatus.web.pages.BatchPage;
import net.sf.appstatus.web.pages.Icons;
import net.sf.appstatus.web.pages.ServicesPage;
import net.sf.appstatus.web.pages.StatusPage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle the Web UI of AppStatus.
 * 
 * @author Olivier Lafon
 * @author Nicolas Richeton
 */
public class StatusWebHandler {
	private static Logger logger = LoggerFactory.getLogger(AppStatus.class);
	private String allowIp = null;
	private AppStatus appStatus = null;
	private Map<String, AbstractPage> pages = null;

	/**
	 * Handle a GET request.
	 * 
	 * @param req
	 * @param resp
	 * @throws IOException
	 */
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		if (allowIp != null) {
			if (!req.getRemoteAddr().equals(allowIp)) {
				resp.sendError(401, "IP not authorized");
				return;
			}
		}

		if (req.getParameter("icon") != null) {
			Icons.render(resp.getOutputStream(), req.getParameter("icon"));
			return;
		}

		if (req.getParameter("p") != null
				&& pages.containsKey(req.getParameter("p"))) {
			pages.get(req.getParameter("p")).doGet(appStatus, req, resp);

		} else {
			pages.get("status").doGet(appStatus, req, resp);
		}
	}

	/**
	 * Handle a POST request.
	 * 
	 * @param req
	 * @param resp
	 * @throws IOException
	 */
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		if (allowIp != null) {
			if (!req.getRemoteAddr().equals(allowIp)) {
				resp.sendError(401, "IP not authorized");
				return;
			}
		}

		if (req.getParameter("p") != null
				&& pages.containsKey(req.getParameter("p"))) {
			pages.get(req.getParameter("p")).doPost(appStatus, req, resp);

		} else {
			pages.get("status").doPost(appStatus, req, resp);
		}

		doGet(req, resp);
	}

	/**
	 * Does the initialization work.
	 * <p>
	 * Read configuration from /status-web-conf.properties
	 * <p>
	 * If you need to inject custom objects using these methods, please do it
	 * before calling init.
	 * <ul>
	 * <li>{@link #setPages(Map)}</li>
	 * <li>{@link #setAppStatus(AppStatus)}</li>
	 * </ul>
	 */
	public void init() {
		// Use default instance if not set
		if (appStatus == null) {
			appStatus = AppStatusStatic.getInstance();
		}
		appStatus.init();

		if (pages == null) {
			pages = new HashMap<String, AbstractPage>();

			if (appStatus.getBatchManager() != null) {
				pages.put("batch", new BatchPage());
			}

			if (appStatus.getServiceManager() != null) {
				pages.put("services", new ServicesPage());
			}

			pages.put("status", new StatusPage());
		}

		try {

			InputStream is = StatusWebHandler.class
					.getResourceAsStream("/status-web-conf.properties");

			if (is == null) {
				logger.warn("/status-web-conf.properties not found in classpath. Using default configuration");
			} else {
				Properties p = new Properties();
				p.load(is);
				is.close();

				if (allowIp == null) {
					allowIp = (String) p.get("ip.allow");
				}
			}
		} catch (Exception e) {
			logger.error(
					"Error loading configuration from /status-web-conf.properties.",
					e);
		}

	}

	/**
	 * Restrict access to a single IP.
	 * 
	 * @param allowIp
	 */
	public void setAllowIp(String allowIp) {
		this.allowIp = allowIp;
	}

	/**
	 * Set the AppStatus object to use in the web interface.
	 * 
	 * @param appStatus
	 */
	public void setAppStatus(AppStatus appStatus) {
		this.appStatus = appStatus;
	}

	/**
	 * Set the available pages in the web interface.
	 * 
	 * @param pages
	 */
	public void setPages(Map<String, AbstractPage> pages) {
		this.pages = pages;
	}

}