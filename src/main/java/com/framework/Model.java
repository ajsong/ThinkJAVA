package com.framework;

import com.framework.tool.*;
import javax.servlet.http.*;
import java.util.*;

public class Model {
	public String app;
	public String act;
	public Request request;
	public HttpServletResponse response;
	public String session_id;
	public long now;
	public String ip;
	public Map<String, String> headers;
	public Map<String, String> configs;
	public boolean is_wx;
	public boolean is_mini;
	public boolean is_web;
	public boolean is_wap;
	
	public void __construct(HttpServletRequest request, HttpServletResponse response) {
		Map<String, String> moduleMap = Common.getModule(request);
		this.app = moduleMap.get("app");
		this.act = moduleMap.get("act");
		this.request = Common.request();
		this.response = response;
		this.session_id = request.getSession().getId().toLowerCase();
		this.now = Common.time();
		this.ip = Common.ip();
		this.headers = Common.getHeaders();
		this.is_wx = Common.isWX();
		this.is_mini = Common.isMini();
		this.is_wap = Common.isWap();
		this.is_web = Common.isWeb();
	}
	
	public Object session(String key) {
		return Common.session(key);
	}
	public void session(String key, Object value) {
		Common.session(key, value);
	}
	public <T> T session(String key, Class<T> clazz) {
		return Common.session(key, clazz);
	}
	public DataList sessionDataList(String key) {
		return Common.sessionDataList(key);
	}
	public DataMap sessionDataMap(String key) {
		return Common.sessionDataMap(key);
	}
	public String cookie(String key) {
		return Common.cookie(key);
	}
	public void cookie(String key, String value) {
		cookie(key, value, -1);
	}
	public void cookie(String key, String value, int expire) {
		Common.cookie(key, value, expire);
	}
}
