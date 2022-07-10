//Developed by @mario 1.2.20220710
package com.framework.tool;

import com.alibaba.fastjson.*;
import com.framework.closure.Callback;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.*;

public class Cache {
	static String cacheType;
	static String cacheDir;
	static String runtimeDir;
	static String rootPath;
	
	static {
		cacheType = Common.getYml("sdk.cache.type", "");
		cacheDir = Common.getYml("sdk.cache.dir", "cache");
		runtimeDir = Common.getYml("sdk.runtime.dir", "runtime");
		rootPath = Common.root_path();
	}
	
	//如果不存在则写入缓存
	public static String remember(String key, String value) {
		return remember(key, value, 0);
	}
	public static String remember(String key, String value, int expire) {
		return remember(key, () -> value, expire);
	}
	public static <T> T remember(String key, Callback callback) {
		return remember(key, callback, 0);
	}
	@SuppressWarnings("unchecked")
	public static <T> T remember(String key, Callback callback, int expire) {
		if (callback == null) return null;
		if (cacheType.equals("redis")) {
			Redis redis = new Redis();
			boolean hasRedis = redis.ping();
			if (hasRedis) {
				if (redis.hasKey(key)) {
					Object ret = Common.json_decode((String) redis.get(key));
					if (ret instanceof JSONArray) return (T) new DataList(ret);
					else if (ret instanceof JSONObject) return (T) new DataMap(ret);
					else return (T) ret;
				}
				Object value = callback.get();
				if (value instanceof DataList) {
					List<Object> list = new ArrayList<>();
					for (DataMap map : ((DataList) value).list) list.add(DataMap.dataToMap(map));
					redis.set(key, JSON.toJSONString(list), expire);
				} else if (value instanceof DataMap) {
					redis.set(key, JSON.toJSONString(DataMap.dataToMap(value)), expire);
				} else if ((value instanceof List) || value.getClass().isArray()) {
					redis.set(key, JSON.toJSONString(value), expire);
				} else {
					redis.set(key, value, expire);
				}
				return (T) value;
			}
		}
		ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		HttpServletRequest request = Objects.requireNonNull(servletRequestAttributes).getRequest();
		Map<String, String> moduleMap = Common.getModule(request);
		String module = moduleMap.get("module");
		String cachePath = rootPath + "/" + runtimeDir + "/" + module + "/" + cacheDir;
		File file = new File(cachePath + "/" + _md5(key));
		if (file.exists()) {
			if (expire == 0 || (new Date().getTime()/1000 - file.lastModified()/1000) <= expire) {
				StringBuilder res = new StringBuilder();
				BufferedReader reader = null;
				try {
					reader = new BufferedReader(new FileReader(file));
					String line;
					while ((line = reader.readLine()) != null) {
						res.append(line);
					}
					Object ret = Common.json_decode(res.toString());
					if (ret instanceof JSONArray) return (T) new DataList(ret);
					else if (ret instanceof JSONObject) return (T) new DataMap(ret);
					else return (T) ret;
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					try{
						if (reader != null) reader.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				return null;
			}
		}
		Object value = callback.get();
		Common.makedir(cachePath);
		try {
			FileWriter fileWritter = new FileWriter(cachePath + "/" + _md5(key));
			if (value instanceof DataList) {
				List<Object> list = new ArrayList<>();
				for (DataMap map : ((DataList) value).list) list.add(DataMap.dataToMap(map));
				fileWritter.write(JSON.toJSONString(list));
			} else if (value instanceof DataMap) {
				fileWritter.write(JSON.toJSONString(DataMap.dataToMap(value)));
			} else if ((value instanceof List) || value.getClass().isArray()) {
				fileWritter.write(JSON.toJSONString(value));
			} else {
				fileWritter.write(String.valueOf(value));
			}
			fileWritter.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return (T) value;
	}
	
	//清除缓存
	public static void delete(String key) {
		Redis redis = new Redis();
		boolean hasRedis = redis.ping();
		if (hasRedis) {
			if (redis.hasKey(key)) redis.del(key);
			return;
		}
		ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		HttpServletRequest request = Objects.requireNonNull(servletRequestAttributes).getRequest();
		Map<String, String> moduleMap = Common.getModule(request);
		String module = moduleMap.get("module");
		String cachePath = rootPath + "/" + runtimeDir + "/" + module + "/" + cacheDir;
		File file = new File(cachePath + "/" + _md5(key));
		if (file.exists()) {
			if (!file.delete()) {
				System.out.println("File " + cachePath + "/" + _md5(key) + " delete failed");
			}
		}
	}
	
	//MD5
	private static String _md5(String str) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(str.getBytes());
			return new BigInteger(1, md.digest()).toString(16);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
}
