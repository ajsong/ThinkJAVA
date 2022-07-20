package com.framework;

import com.framework.closure.Callback;
import com.framework.tool.*;
import javax.servlet.http.*;
import java.util.*;

public class Model {
	static String connection;
	static String name;
	static String table;
	
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
	public String sessionString(String key) {
		return Common.sessionString(key);
	}
	public int sessionInt(String key) {
		return Common.sessionInt(key);
	}
	public float sessionFloat(String key) {
		return Common.sessionFloat(key);
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
	public boolean strlen(String str) {
		return str.length() > 0;
	}
	public String trim(String str) {
		return Common.trim(str);
	}
	public String trim(String str, String symbol) {
		return Common.trim(str, symbol);
	}
	public String ltrim(String str, String symbol) {
		return Common.ltrim(str, symbol);
	}
	public String rtrim(String str, String symbol) {
		return Common.rtrim(str, symbol);
	}
	public boolean isAjax() {
		return Common.isAjax();
	}
	public boolean isNullOrEmpty(Object value) {
		return Common.isNullOrEmpty(value);
	}
	public Object Number(String str) {
		return Common.Number(str);
	}
	public String json_encode(Object obj) {
		return Common.json_encode(obj);
	}
	public <T> T json_decode(String value) {
		return Common.json_decode(value);
	}
	public long time() {
		return Common.time();
	}
	public String date(String format) {
		return Common.date(format);
	}
	public String date(String format, String timestamp) {
		return Common.date(format, timestamp);
	}
	public String date(String format, long timestamp) {
		return Common.date(format, timestamp);
	}
	public long strtotime(String mark) {
		return Common.strtotime(mark);
	}
	public long strtotime(String mark, String timestamp) {
		return Common.strtotime(mark, timestamp);
	}
	public long strtotime(String mark, long timestamp) {
		return Common.strtotime(mark, timestamp);
	}
	public String preg_replace(String pattern, Callback callback, String str) {
		return Common.preg_replace(pattern, callback, str);
	}
	public boolean preg_match(String pattern, String str) {
		return Common.preg_match(pattern, str);
	}
	public String add_domain(String url) {
		return Common.add_domain(url);
	}
	public <T> T add_domain_deep(T obj, String field) {
		return Common.add_domain_deep(obj, field);
	}
}
