package com.framework;

import com.framework.closure.Callback;
import com.framework.tool.*;
import com.framework.plugins.upload.Qiniu;
import javax.servlet.http.*;
import java.io.*;
import java.util.*;

public class Base {
	public String module;
	public String app;
	public String act;
	public Request request;
	public HttpServletResponse response;
	public String session_id;
	public long now;
	public String ip;
	public Map<String, String> headers;
	public static String[] uriMap;
	public static Map<String, Object> uploadThird;
	public boolean appKeepRunning = true;

	public void __construct(HttpServletRequest request, HttpServletResponse response) {
		Map<String, String> moduleMap = Common.getModule(request);
		this.module = moduleMap.get("module");
		this.app = moduleMap.get("app");
		this.act = moduleMap.get("act");
		this.request = Common.request();
		this.response = response;
		this.session_id = request.getSession().getId().toLowerCase();
		this.now = Common.time();
		this.ip = Common.ip();
		this.headers = Common.getHeaders();

		if (uriMap == null) {
			String uri_map = Common.getYml("sdk.uri.map", "");
			if (uri_map != null && uri_map.length() > 0) uriMap = uri_map.split(",");
		}
		if (uriMap != null) {
			for (String map : uriMap) {
				String[] items = map.split("=");
				if (request.getRequestURI().matches(items[0])) {
					String[] key = items[1].split("&");
					this.app = key[0];
					this.act = key[1];
					break;
				}
			}
		}

		if (uploadThird == null) {
			boolean UPLOAD_LOCAL = Common.getYml("spring.servlet.upload.local", true);
			if (UPLOAD_LOCAL) {
				String uploadType = Common.getYml("spring.servlet.upload.type", "");
				if (uploadType != null && uploadType.length() > 0) {
					uploadThird = new HashMap<>();
					String[] uploadFields = Common.getYml("spring.servlet.upload.fields", "").split("\\|");
					if (uploadType.equalsIgnoreCase("qiniu")) {
						uploadThird.put("package", Qiniu.class);
						for (String field : uploadFields) {
							String[] fields = field.split(":");
							switch (fields[0]) {
								case "qiniu_accessKey":uploadThird.put("accessKey", fields[1]);break;
								case "qiniu_secretKey":uploadThird.put("secretKey", fields[1]);break;
								case "qiniu_bucketname":uploadThird.put("bucket", fields[1]);break;
								case "qiniu_domain":uploadThird.put("domain", fields.length > 1 ? fields[1] : Common.domain());break;
							}
						}
					}
				}
			}
		}
	}
	
	public boolean getAppKeepRunning() {
		return this.appKeepRunning;
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
	public Object redirect(String url) {
		this.appKeepRunning = false;
		return Common.redirect(url);
	}
	public Object success() {
		return Common.success();
	}
	public Object success(Object data) {
		return Common.success(data);
	}
	public Object success(Object data, String msg) {
		return Common.success(data, msg);
	}
	public Object success(Object data, String msg, Object extend) {
		return Common.success(data, msg, extend);
	}
	public Object error() {
		return Common.error();
	}
	public Object error(String msg) {
		return Common.error(msg);
	}
	public Object error(String msg, String url) {
		return Common.error(msg, url);
	}
	public Object error(String msg, int code) {
		return Common.error(msg, code);
	}
}
