//Developed by @mario
package com.framework.tool;

import com.alibaba.fastjson.*;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.framework.closure.Callback;
import com.j256.simplemagic.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.core.SpringVersion;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.*;
import org.springframework.web.servlet.ModelAndView;
import org.yaml.snakeyaml.Yaml;
import javax.net.ssl.*;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.*;
import java.io.*;
import java.lang.reflect.*;
import java.math.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.text.*;
import java.util.*;
import java.lang.*;
import java.util.jar.*;
import java.util.regex.*;

public class Common {
	static String sdkVersion = "3.6.20220710";
	static String rootPath;
	static String runtimeDir;
	static Map<String, Object> requests;
	static Map<String, Object> responses;
	static HttpServletRequest request;
	static HttpServletResponse response;
	static Request newRequest;
	static String imgDomain;
	static Redis redis;
	static Map<String, Object> plugins;
	static Map<String, Map<String, Object>> ymls;
	static Map<String, Map<String, Object>> properties;
	static Map<String, Map<String, String>> moduleMap;

	//生命周期结束时执行
	public static void destroy() {
		if (requests != null) {
			requests.clear();
			requests = null;
		}
		if (responses != null) {
			responses.clear();
			responses = null;
		}
		if (plugins != null) {
			plugins.clear();
			plugins = null;
		}
		if (moduleMap != null) {
			moduleMap.clear();
			moduleMap = null;
		}
		newRequest = null;
	}

	//获取版本
	public static Map<String, String> getVersion() {
		Properties properties = System.getProperties();
		Map<String, String> map = new LinkedHashMap<>();
		map.put("JavaVersion", (String) properties.get("java.version"));
		map.put("SpringBootVersion", SpringBootVersion.getVersion());
		map.put("SpringVersion", SpringVersion.getVersion());
		map.put("SdkVersion", sdkVersion);
		return map;
	}
	
	//获取根目录路径
	public static String root_path() {
		if (rootPath == null || rootPath.length() == 0) {
			ApplicationHome ah = new ApplicationHome(Common.class);
			rootPath = rtrim(ah.getSource().getParentFile().getPath(), "/");
		}
		return rootPath;
	}

	//获取全局Request、Response
	public static void getServlet() {
		ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		HttpServletRequest req = Objects.requireNonNull(servletRequestAttributes).getRequest();
		HttpServletResponse res = Objects.requireNonNull(servletRequestAttributes).getResponse();
		getServlet(req, res);
	}
	//设置全局Request、Response
	public static void getServlet(HttpServletRequest req, HttpServletResponse res) {
		if (requests == null) {
			requests = new HashMap<>();
			responses = new HashMap<>();
		}
		String uri = req.getRequestURI();
		res.setContentType("text/html; charset=utf-8");
		requests.put(uri, req);
		responses.put(uri, res);
		request = req;
		response = res;
	}

	//获取/设置自定义Request
	public static Request request() {
		if (newRequest == null) newRequest = new Request();
		return newRequest;
	}
	
	//获取/设置Session
	public static Object session(String key) {
		getServlet();
		HttpServletRequest req = (HttpServletRequest) requests.get(request.getRequestURI());
		//System.out.println(req.getSession().getMaxInactiveInterval());
		try {
			return req.getSession().getAttribute(key);
		} catch (NullPointerException e) {
			return null;
		}
	}
	public static void session(String key, Object value) {
		getServlet();
		HttpServletRequest req = (HttpServletRequest) requests.get(request.getRequestURI());
		if (value == null) {
			try {
				req.getSession().removeAttribute(key);
			} catch (Exception e) {
				//e.printStackTrace();
			}
			return;
		}
		try {
			req.getSession().setAttribute(key, value);
		} catch (Exception e) {
			//e.printStackTrace();
		}
	}
	@SuppressWarnings("unchecked")
	public static <T> T session(String key, Class<T> clazz) {
		Object value = session(key);
		if (value == null) return null;
		if (value instanceof Map) value = Common.mapToBean((Map<String, Object>) value, clazz);
		if (value != null && value.getClass() != clazz) return null;
		return (T) value;
	}
	public static DataList sessionDataList(String key) {
		Object ret = session(key);
		if (ret == null) return null;
		if (ret instanceof DataList) return (DataList) ret;
		return null;
	}
	public static DataMap sessionDataMap(String key) {
		Object ret = session(key);
		if (ret == null) return null;
		if (ret instanceof DataMap) return (DataMap) ret;
		return null;
	}
	
	//获取/设置Cookie
	public static String cookie(String key) {
		getServlet();
		HttpServletRequest req = (HttpServletRequest) requests.get(request.getRequestURI());
		try {
			Cookie[] cookies = req.getCookies();
			if (cookies != null) {
				for (Cookie cookie : cookies) {
					if (cookie.getName().equals(key)) return URLDecoder.decode(cookie.getValue(), "UTF-8");
				}
			}
		} catch (Exception e) {
			//e.printStackTrace();
		}
		return null;
	}
	public static void cookie(String key, String value) {
		cookie(key, value, -1);
	}
	public static void cookie(String key, String value, int expire) {
		getServlet();
		HttpServletResponse res = (HttpServletResponse) responses.get(request.getRequestURI());
		if (value == null) {
			try {
				Cookie cookie = new Cookie(key, "");
				cookie.setMaxAge(0);
				cookie.setPath("/");
				res.addCookie(cookie);
			} catch (Exception e) {
				//e.printStackTrace();
			}
			return;
		}
		try {
			Cookie cookie = new Cookie(key, URLEncoder.encode(value, "UTF-8"));
			if (expire > 0) cookie.setMaxAge(expire); //有效时长(单位秒), 默认为-1, 页面关闭就失效
			cookie.setPath("/"); //设置访问该域名下某个路径时生效
			res.addCookie(cookie);
		} catch (UnsupportedEncodingException e) {
			//e.printStackTrace();
		}
	}
	
	//读取yml文件
	public static Map<String, Object> getYmls() {
		return getYmls("application.yml");
	}
	public static Map<String, Object> getYmls(String filename) {
		if (ymls == null) ymls = new HashMap<>();
		if (ymls.get(filename) == null) {
			try {
				Yaml yaml = new Yaml();
				URL url;
				if (filename.startsWith("file:///")) {
					filename = filename.replace("file:///", "");
					String filepath;
					if (filename.contains("/")) {
						filepath = filename;
					} else {
						filepath = root_path() + filename;
					}
					if (!new File(filepath).exists()) return null;
					url = new URL("file:///" + filepath);
				} else {
					url = Common.class.getClassLoader().getResource(filename);
				}
				if (url == null) return null;
				Map<String, Object> map = new LinkedHashMap<>();
				JSONObject obj = JSONObject.parseObject(JSON.toJSONString(yaml.load(Files.newInputStream(Paths.get(url.getFile())))));
				setYmlInline(obj, "", map);
				ymls.put(filename, map);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		return ymls.get(filename);
	}
	private static void setYmlInline(JSONObject obj, String tmpKey, Map<String, Object> map) {
		for (String key : obj.keySet()) {
			String value = obj.getString(key);
			Object res;
			try {
				res = JSONObject.parse(value);
			} catch (Exception e) {
				//如果解析出错，就说明已经到头了，放入map然后继续解析
				map.put(tmpKey + key, json_decode(value));
				continue;
			}
			//如果是集合，需要特殊解析
			if (res instanceof Collection<?>) {
				List<?> list = (List<?>) res;
				for (int i = 0; i < list.size(); i++) {
					String itemKey = tmpKey + key + "[" + i + "]" + ".";
					if (list.get(i) instanceof JSONObject) {
						JSONObject itemValue = (JSONObject) list.get(i);
						setYmlInline(itemValue, itemKey, map);
					} else {
						map.put(tmpKey + key, json_decode(value));
					}
				}
			} else if (res instanceof JSONObject) {
				JSONObject json = JSONObject.parseObject(value);
				setYmlInline(json, tmpKey + key + ".", map);
			} else {
				map.put(tmpKey + key, json_decode(value));
			}
		}
	}
	//读取yml文件指定值
	public static Object getYml(String key) {
		return getYml(key, null, Object.class);
	}
	@SuppressWarnings("unchecked")
	public static <T> T getYml(String key, T defaultValue) {
		if (defaultValue == null) defaultValue = (T) new Object();
		return getYml(key, defaultValue, defaultValue.getClass());
	}
	@SuppressWarnings("unchecked")
	public static <T> T getYml(String key, T defaultValue, Class<?> clazz) {
		Map<String, Object> map = getYmls();
		if (map == null) return defaultValue;
		Object res = map.get(key);
		if (res == null) return defaultValue;
		if (clazz == Integer.class) {
			res = Integer.parseInt(String.valueOf(res));
		} else if (clazz == Long.class) {
			res = Long.parseLong(String.valueOf(res));
		} else if (clazz == Float.class) {
			res = Float.parseFloat(String.valueOf(res));
		} else if (clazz == Double.class) {
			res = Double.parseDouble(String.valueOf(res));
		} else if (clazz == Boolean.class) {
			res = String.valueOf(res).equalsIgnoreCase("true");
		} else if (clazz == String.class) {
			if (String.valueOf(res).length() == 0) return defaultValue;
		}
		return (T) res;
	}

	//读取properties文件
	public static Map<String, Object> getProperties() {
		return getProperties("application.properties");
	}
	public static Map<String, Object> getProperties(String filename) {
		if (properties == null) properties = new HashMap<>();
		if (properties.get(filename) == null) {
			Map<String, Object> map = new LinkedHashMap<>();
			try {
				Properties p = new Properties();
				if (filename.contains("/")) {
					p.load(Files.newInputStream(Paths.get(filename)));
				} else {
					p.load(Common.class.getClassLoader().getResourceAsStream(filename));
				}
				for (String key: p.stringPropertyNames()) {
					map.put(key, json_decode(p.getProperty(key)));
				}
				properties.put(filename, map);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		return properties.get(filename);
	}
	//读取properties文件指定值
	public static Object getProperty(String key) {
		return getProperty(key, null, Object.class);
	}
	@SuppressWarnings("unchecked")
	public static <T> T getProperty(String key, T defaultValue) {
		if (defaultValue == null) defaultValue = (T) new Object();
		return getProperty(key, defaultValue, defaultValue.getClass());
	}
	@SuppressWarnings("unchecked")
	public static <T> T getProperty(String key, T defaultValue, Class<?> clazz) {
		Map<String, Object> map = getProperties();
		if (map == null) return defaultValue;
		Object res = map.get(key);
		if (res == null) return defaultValue;
		if (clazz == Integer.class) {
			res = Integer.parseInt(String.valueOf(res));
		} else if (clazz == Long.class) {
			res = Long.parseLong(String.valueOf(res));
		} else if (clazz == Float.class) {
			res = Float.parseFloat(String.valueOf(res));
		} else if (clazz == Double.class) {
			res = Double.parseDouble(String.valueOf(res));
		} else if (clazz == Boolean.class) {
			res = String.valueOf(res).equalsIgnoreCase("true");
		} else if (clazz == String.class) {
			if (String.valueOf(res).length() == 0) return defaultValue;
		}
		return (T) res;
	}
	//读取自定义properties文件
	public static Map<String, Object> getMyProperty(String filepath) {
		Map<String, Object> map = new HashMap<>();
		try {
			Properties p = new Properties();
			p.load(Files.newInputStream(Paths.get(filepath)));
			for (String key: p.stringPropertyNames()) {
				String value = p.getProperty(key);
				if (value.startsWith("[") || value.startsWith("{")) {
					map.put(key, json_decode(value));
				} else {
					map.put(key, value);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return map;
	}
	//生成自定义properties文件
	public static void saveMyProperty(Map<String, Object> params, String filepath) {
		StringBuilder content = new StringBuilder();
		for (Map.Entry<String, Object> param : params.entrySet()) {
			Object value = param.getValue();
			content.append(param.getKey()).append(" = ").append((value instanceof String) ? value : JSON.toJSONString(value)).append("\n");
		}
		writeFile(content.toString(), root_path() + "/" + trim(filepath.replace(root_path(), ""), "/"));
	}
	
	//当前是否jar运行
	public static boolean isJarRun() {
		return new File(getJarPath()).isFile();
	}
	
	//获取jar路径
	public static String getJarPath() {
		return Common.class.getProtectionDomain().getCodeSource().getLocation().getFile();
	}
	
	//获取jar内的文件
	public static JarEntry getJarFile(String jarPath, String filepath) {
		try {
			try (JarFile jarFile = new JarFile(jarPath)) {
				return jarFile.getJarEntry(trim(filepath, "/"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	//获取jar内文件的内容
	public static String getJarFileContent(String jarPath, String filepath) {
		StringBuilder content = new StringBuilder();
		try {
			JarFile jarFile = new JarFile(jarPath);
			JarEntry entry = jarFile.getJarEntry(trim(filepath, "/"));
			if (entry.isDirectory()) return null;
			InputStream input = jarFile.getInputStream(entry);
			InputStreamReader in = new InputStreamReader(input);
			BufferedReader reader = new BufferedReader(in);
			String line;
			while ((line = reader.readLine()) != null) content.append(line).append("\n");
			input.close();
			in.close();
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return content.toString();
	}
	
	//历遍jar内文件与目录, separateType: -1所有, 0文件夹, 1文件
	public static List<String> getJarFilePaths(String jarPath) {
		return getJarFilePaths(jarPath, -1);
	}
	public static List<String> getJarFilePaths(String jarPath, int separateType) {
		List<String> list = new ArrayList<>();
		try {
			Enumeration<JarEntry> entrys;
			try (JarFile jarFile = new JarFile(jarPath)) {
				entrys = jarFile.entries();
			}
			while (entrys.hasMoreElements()) {
				JarEntry entry = entrys.nextElement();
				if (separateType == 0 && !entry.isDirectory()) continue;
				if (separateType == 1 && entry.isDirectory()) continue;
				list.add(entry.getName());
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return list;
	}

	//生成序列号
	public static String generate_sn() {
		return date("yyMMddHHmmss") + rand(10000, 99999);
	}

	//生成sign
	public static String generate_token() {
		return md5(md5(String.valueOf(rand(100000, 999999))) + time());
	}

	//生成密码盐值salt
	public static String generate_salt() {
		return String.valueOf(rand(100000, 999999));
	}
	
	//生成指定位数的随机整数
	public static int generate_code() {
		return generate_code(4);
	}
	public static int generate_code(int length) {
		ArrayList<String> list = new ArrayList<>();
		//将0-9的数字加入集合
		for (int i = 0; i < 10; i++) list.add(i + "");
		Random random = new Random();
		StringBuilder res = new StringBuilder();
		for (int i = 0; i < length; i++) res.append(list.get(random.nextInt(list.size())));
		return Integer.parseInt(res.toString());
	}
	
	//生成md5密码与盐值
	public static String[] generate_password(String password) {
		if (password == null || password.length() == 0) return null;
		String salt = generate_salt();
		password = crypt_password(password, salt);
		return new String[]{password, salt};
	}

	//根据盐值生成加密密码
	public static String crypt_password(String password, String salt) {
		if (password == null || password.length() == 0 || salt == null || salt.length() == 0) return "";
		return md5(md5(password) + salt);
	}

	//清除两端字符串
	public static String trim(String str) {
		return trim(str, " ");
	}
	public static String trim(String str, String symbol) {
		return str.replaceAll("(^" + symbol.replaceAll("([|\\[]\\(\\)\\^\\$\\\\])", "\\\\$1") + "|" + symbol.replaceAll("([|\\[]\\(\\)\\^\\$\\\\])", "\\\\$1") + "$)", "");
	}
	public static String ltrim(String str, String symbol) {
		return str.replaceAll("(^" + symbol.replaceAll("([|\\[]\\(\\)\\^\\$\\\\])", "\\\\$1") + ")", "");
	}
	public static String rtrim(String str, String symbol) {
		return str.replaceAll("(" + symbol.replaceAll("([|\\[]\\(\\)\\^\\$\\\\])", "\\\\$1") + "$)", "");
	}

	//保留指定小数位
	public static String round(float number, int digits) {
		return String.format("%."+digits+"f", number);
		//%.2f %.表示小数点前任意位数, 2表示两位小数 格式后的结果为f 表示浮点型
	}

	//是否POST请求
	public static boolean isPost() {
		getServlet();
		HttpServletRequest req = (HttpServletRequest) requests.get(request.getRequestURI());
		return req.getMethod().equalsIgnoreCase("POST");
	}

	//是否PUT请求
	public static boolean isPut() {
		getServlet();
		HttpServletRequest req = (HttpServletRequest) requests.get(request.getRequestURI());
		return req.getMethod().equalsIgnoreCase("PUT");
	}

	//是否DELETE请求
	public static boolean isDelete() {
		getServlet();
		HttpServletRequest req = (HttpServletRequest) requests.get(request.getRequestURI());
		return req.getMethod().equalsIgnoreCase("DELETE");
	}

	//是否WAP
	public static boolean isWap() {
		return isMobileWeb();
	}

	//是否WEB
	public static boolean isWeb() {
		return (!isAjax() && !isWap());
	}

	//是否微信端打开
	public static boolean isWX() {
		return StringUtils.containsIgnoreCase(getHeaders("user-agent"), "MicroMessenger");
	}

	//是否微信小程序打开
	public static boolean isMini() {
		return (StringUtils.containsIgnoreCase(getHeaders("referer"), "https://servicewechat.com/wx") && isWX());
	}

	//是否微信开发者工具打开
	public static boolean isDevTools() {
		return StringUtils.containsIgnoreCase(getHeaders("user-agent"), "wechatdevtools");
	}

	//是否AJAX
	public static boolean isAjax() {
		getServlet();
		HttpServletRequest req = (HttpServletRequest) requests.get(request.getRequestURI());
		return (req.getHeader("x-requested-with") != null && req.getHeader("x-requested-with").equalsIgnoreCase("XMLHttpRequest")) || request().get("output").equals("json");
	}

	//判断移动端浏览器打开
	public static boolean isMobileWeb() {
		String[] keywords = new String[]{
			"nokia", "sony", "ericsson", "mot", "samsung", "htc", "sgh", "lg", "sharp", "sie-", "philips", "panasonic", "alcatel", "lenovo", "blackberry",
			"meizu", "netfront", "symbian", "ucweb", "windowsce", "palm", "operamini", "operamobi", "openwave", "nexusone", "cldc", "midp", "wap", "mobile",
			"smartphone", "windows ce", "windows phone", "ipod", "iphone", "ipad", "android"
		};
		return Pattern.compile("(" + StringUtils.join(keywords, "|") + ")").matcher(getHeaders("user-agent")).find();
	}

	//字符串是否数字
	public static boolean isNumeric(Object str) {
		return Pattern.compile("^[-+]?\\d+$").matcher(String.valueOf(str)).find();
	}

	//验证手机号
	public static boolean isMobile(String str) {
		return Pattern.compile("^13\\d{9}$|^14[5,7]\\d{8}$|^15[^4]\\d{8}$|^17[03678]\\d{8}$|^18\\d{9}$").matcher(str).find();
	}

	//验证座机
	public static boolean isTel(String str) {
		return Pattern.compile("^((\\d{3,4}-)?\\d{8}(-\\d+)?|(\\(\\d{3,4}\\))?\\d{8}(-\\d+)?)$").matcher(str).find();
	}

	//验证电话号码(包括手机号与座机)
	public static boolean isPhone(String str) {
		return isMobile(str) || isTel(str);
	}

	//验证邮箱
	public static boolean isEmail(String str) {
		return Pattern.compile("^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$").matcher(str).find();
	}

	//验证is_date
	public static boolean isDate(String str) {
		return Pattern.compile("^(?:(?!0000)\\d{4}[/-](?:(?:0?[1-9]|1[0-2])[/-](?:0?[1-9]|1\\d|2[0-8])|(?:0?[13-9]|1[0-2])[/-](?:29|30)|(?:0?[13578]|1[02])[/-]31)|(?:\\d{2}(?:0[48]|[2468][048]|[13579][26])|(?:0[48]|[2468][048]|[13579][26])00)[/-]0?2[/-]29)$").matcher(str).find();
	}

	//验证邮箱
	public static boolean isIdcard(String str) {
		int ID_LENGTH = 17;
		boolean vIDNumByRegex =  str.matches("(\\d{17}[\\dxX]|\\d{14}[\\dxX])");
		//系数列表
		int[] ratioArr = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
		//校验码列表
		char[] checkCodeList = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};
		//获取身份证号字符数组
		char[] cIds = str.toCharArray();
		//获取最后一位（身份证校验码）
		char oCode = cIds[ID_LENGTH];
		int[] iIds = new int[ID_LENGTH];
		int idSum = 0;// 身份证号第1-17位与系数之积的和
		int residue;// 余数(用加出来和除以11，看余数是多少？)
		for (int i = 0; i < ID_LENGTH; i++) {
			iIds[i] = cIds[i] - '0';
			idSum += iIds[i] * ratioArr[i];
		}
		residue = idSum % 11;// 取得余数
		boolean vIDNumByCode = Character.toUpperCase(oCode) == checkCodeList[residue];
		return vIDNumByRegex && vIDNumByCode;
	}
	
	//整数/浮点类型字符串转数值
	public static Object toNumber(String str) {
		if (!isNumeric(str)) return str;
		if (str.contains(".")) return Float.valueOf(str);
		return Integer.valueOf(str);
	}

	//获取主机头信息
	public static String getHeaders(String key) {
		Map<String, String> headers = getHeaders();
		return headers.get(key);
	}
	public static Map<String, String> getHeaders() {
		getServlet();
		HttpServletRequest req = (HttpServletRequest) requests.get(request.getRequestURI());
		Map<String, String> headers = new HashMap<>();
		Enumeration<String> headerNames = req.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String key = headerNames.nextElement();
			String value = req.getHeader(key);
			headers.put(key, value);
		}
		return headers;
	}

	//MD5
	public static String md5(String str) {
		try {
			//生成一个MD5加密计算摘要
			MessageDigest md = MessageDigest.getInstance("MD5");
			//计算md5函数
			md.update(str.getBytes());
			//digest()最后确定返回md5 hash值，返回值为8位字符串。因为md5 hash值是16位的hex值，实际上就是8位的字符
			//BigInteger函数则将8位的字符串转换成16位hex值，用字符串来表示；得到字符串形式的hash值
			//一个byte是八位二进制，也就是2位十六进制字符(2的8次方等于16的2次方)
			return new BigInteger(1, md.digest()).toString(16);
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	//SHA1
	public static String sha1(String filePath) {
		try {
			FileInputStream in = new FileInputStream(filePath);
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] buffer = new byte[1024 * 1024 * 10];
			int len;
			while ((len = in.read(buffer)) > 0) md.update(buffer, 0, len);
			in.close();
			StringBuilder sha1 = new StringBuilder(new BigInteger(1, md.digest()).toString(16));
			int length = 40 - sha1.length();
			if (length > 0) {
				for (int i = 0; i < length; i++) sha1.insert(0, "0");
			}
			return sha1.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	//base64 encode
	public static String base64_encode(String str) {
		return Base64.getEncoder().encodeToString(str.getBytes(StandardCharsets.UTF_8));
	}
	public static String base64_encode(byte[] bytes) {
		return Base64.getEncoder().encodeToString(bytes);
	}

	//base64 decode
	public static String base64_decode(String str) {
		return new String(Base64.getDecoder().decode(str), StandardCharsets.UTF_8);
	}
	public static byte[] base64_decode(String str, boolean returnByte) {
		return Base64.getDecoder().decode(str);
	}

	//json_encode
	public static String json_encode(Object obj) {
		return JSON.toJSONString(obj, SerializerFeature.WriteMapNullValue);
	}

	//json_decode
	@SuppressWarnings("unchecked")
	public static <T> T json_decode(String value) {
		if (value == null || value.length() == 0) return null;
		if (value.contains("=>")) {
			Matcher matcher = Pattern.compile("(['\"])(\\w+)\\1\\s*=>").matcher(value.replace("[", "{").replace("]", "}"));
			StringBuffer str = new StringBuffer();
			while (matcher.find()) {
				matcher.appendReplacement(str, "\""+matcher.group(2)+"\":");
			}
			matcher.appendTail(str);
			matcher = Pattern.compile("'([^']+)'\\s*([,}])").matcher(str.toString());
			str = new StringBuffer();
			while (matcher.find()) {
				matcher.appendReplacement(str, "\""+matcher.group(1)+"\""+matcher.group(2)+"");
			}
			matcher.appendTail(str);
			matcher = Pattern.compile("\\{(\\s*\"[^\"]+\"(\\s*,\\s*\"[^\"]+\")*\\s*)}").matcher(str.toString());
			str = new StringBuffer();
			while (matcher.find()) {
				matcher.appendReplacement(str, "["+matcher.group(1)+"]");
			}
			matcher.appendTail(str);
			value = str.toString();
		}
		if (Pattern.compile("^\\[.*]$").matcher(value).find()) {
			return (T) JSONObject.parseArray(value);
		} else if (Pattern.compile("^\\{.*}$").matcher(value).find()) {
			return (T) JSON.parseObject(value);
		}
		return (T) value;
	}

	//url_encode
	public static String url_encode(String url) {
		try {
			return URLEncoder.encode(url, "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
			return ""; //非UTF-8编码
		}
	}

	//url_decode
	public static String url_decode(String url) {
		try {
			return URLDecoder.decode(url.replaceAll("%(?![\\da-fA-F]{2})", "%25"), "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	
	//下划线转驼峰
	public static String camelize(String value) {
		StringBuilder res = new StringBuilder();
		String[] words = value.replaceAll("_", " ").split(" ");
		for (String word : words) {
			res.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
		}
		return res.toString();
	}
	
	//驼峰转下划线
	public static String uncamelize(String value) {
		return value.replaceAll("\\s+", "").replaceAll("(.)(?=[A-Z])", "$1_").toLowerCase();
	}

	//数组去重
	public static <T> List<T> array_unique(List<T> list) {
		Set<T> set = new HashSet<>(list);
		return new ArrayList<>(set);
	}
	public static <T> T[] array_unique(T[] array) {
		Set<T> set = new HashSet<>(Arrays.asList(array));
		return set.toArray(array);
	}
	public static int[] array_unique(int[] array) {
		Set<Integer> set = new HashSet<>();
		for (int k : array) set.add(k);
		Integer[] arr = set.toArray(new Integer[0]);
		int[] ret = new int[arr.length];
		for (int i = 0; i < arr.length; i++) ret[i] = arr[i];
		return ret;
	}
	public static long[] array_unique(long[] array) {
		Set<Long> set = new HashSet<>();
		for (long k : array) set.add(k);
		Long[] arr = set.toArray(new Long[0]);
		long[] ret = new long[arr.length];
		for (int i = 0; i < arr.length; i++) ret[i] = arr[i];
		return ret;
	}
	public static float[] array_unique(float[] array) {
		Set<Float> set = new HashSet<>();
		for (float k : array) set.add(k);
		Float[] arr = set.toArray(new Float[0]);
		float[] ret = new float[arr.length];
		for (int i = 0; i < arr.length; i++) ret[i] = arr[i];
		return ret;
	}
	public static double[] array_unique(double[] array) {
		Set<Double> set = new HashSet<>();
		for (double k : array) set.add(k);
		Double[] arr = set.toArray(new Double[0]);
		double[] ret = new double[arr.length];
		for (int i = 0; i < arr.length; i++) ret[i] = arr[i];
		return ret;
	}

	//implode
	public static <T> String join(String symbol, T[] arr) {
		return implode(symbol, arr);
	}
	public static <T> String implode(String symbol, T[] arr) {
		if (arr.length == 0) return "";
		return StringUtils.join(arr, symbol);
	}
	public static <T> String join(String symbol, List<T> arr) {
		return implode(symbol, arr);
	}
	public static <T> String implode(String symbol, List<T> arr) {
		if (arr.size() == 0) return "";
		StringBuilder res = new StringBuilder();
		for (Object item : arr) res.append(symbol).append(item);
		return trim(res.toString(), symbol);
	}

	//数组转List
	@SuppressWarnings("unchecked")
	public static <T> List<T> arrayToList(Object array, Class<T> type) {
		List<T> list = new ArrayList<>(((T[]) array).length);
		Collections.addAll(list, ((T[]) array));
		return list;
	}

	//List转数组
	@SuppressWarnings("all")
	public static <T> T[] listToArray(List<T> list) {
		Class<?> type = list.get(0).getClass();
		if (type == Integer.class) return (T[]) list.toArray(new Integer[0]);
		if (type == Long.class) return (T[]) list.toArray(new Long[0]);
		if (type == Float.class) return (T[]) list.toArray(new Float[0]);
		if (type == Double.class) return (T[]) list.toArray(new Double[0]);
		if (type == String.class) return (T[]) list.toArray(new String[0]);
		return (T[]) list.toArray();
		//Integer[] ret = Arrays.stream(arr).boxed().toArray(Integer[]::new); //int[]转Integer[]
		//int[] ret = Arrays.stream(arr).mapToInt(Integer::valueOf).toArray(); //Integer[]转int[]
	}

	//时间戳
	public static long time() {
		return time(date("yyyy-MM-dd HH:mm:ss"));
	}
	public static long time(String date) {
		try {
			SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			return time(dateformat.parse(date));
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
	}
	public static long time(Date date) {
		return date.getTime() / 1000;
	}

	//日期格式化
	public static String date(String format) {
		return date(format, System.currentTimeMillis()/1000);
	}
	public static String date(String format, String timestamp) {
		return date(format, Long.parseLong(timestamp));
	}
	public static String date(String format, long timestamp) {
		format = format.replace("Y", "yyyy").replace("-m", "-M").replace("h", "H").replace("i", "m");
		format = preg_replace("\\by\\b", () -> "yy", format);
		format = preg_replace("\\bM\\b", () -> "MM", format);
		format = preg_replace("\\bd\\b", () -> "dd", format);
		format = preg_replace("\\bH\\b", () -> "HH", format);
		format = preg_replace("\\bm\\b", () -> "mm", format);
		format = preg_replace("\\bs\\b", () -> "ss", format);
		SimpleDateFormat dateformat = new SimpleDateFormat(format);
		return dateformat.format(new Date(String.valueOf(timestamp).length()<13 ? Long.parseLong(timestamp+"000") : timestamp));
	}

	//获取当前时间的指定部分
	public static int getCalendar(String interval) {
		interval = interval.toLowerCase();
		Calendar c = Calendar.getInstance();
		switch (interval) {
			case "y":return c.get(Calendar.YEAR);
			case "m":return c.get(Calendar.MONTH);
			case "w":return c.get(Calendar.DAY_OF_WEEK);
			case "d":return c.get(Calendar.DATE);
			case "h":return c.get(Calendar.HOUR_OF_DAY);
			case "n":return c.get(Calendar.MINUTE);
			case "s":return c.get(Calendar.SECOND);
		}
		return 0;
	}

	//利用UUID生成伪随机字符串
	public static String uuid() {
		return UUID.randomUUID().toString();
	}

	//指定范围随机数
	public static int rand(int min, int max) {
		Random random = new Random();
		return random.nextInt(max) % (max - min + 1) + min;
	}

	//指定长度随机字符串
	public static String random_str(int length) {
		ArrayList<String> list = new ArrayList<>();
		int begin = 97;
		//生成小写字母,并加入集合
		for (int i = begin; i < begin + 26; i++) list.add((char)i + "");
		//生成大写字母,并加入集合
		begin = 65;
		for (int i = begin; i < begin + 26; i++) list.add((char)i + "");
		//将0-9的数字加入集合
		for (int i = 0; i < 10; i++) list.add(i + "");
		Random random = new Random();
		StringBuilder res = new StringBuilder();
		for (int i = 0; i < length; i++) res.append(list.get(random.nextInt(list.size())));
		return res.toString();
	}

	//构造URL参数字符串
	public static String http_build_query(Map<String, Object> data) {
		StringBuilder res = new StringBuilder();
		for (String key : data.keySet()) res.append("&").append(key).append("=").append(url_encode(String.valueOf(data.get(key))));
		return trim(res.toString(), "&");
	}

	//URL参数字符串解析为Map
	public static Map<String, Object> parse_str(String data) {
		Map<String, Object> map = new HashMap<>();
		if (data != null && data.length() > 0) {
			String[] params = data.split("&");
			for (String param : params) {
				String[] p = param.split("=");
				map.put(p[0], p.length > 1 ? url_decode(p[1]) : "");
			}
		}
		return map;
	}

	//将时间转换成刚刚、分钟、小时
	public static String get_time_word(String date) {
		return get_time_word(time(date));
	}
	public static String get_time_word(Date date) {
		return get_time_word(time(date));
	}
	public static String get_time_word(long timestamp) {
		long between = time() - timestamp;
		if (between < 60) return "刚刚";
		if (between < 3600) return Math.floor((double) (between / 60)) + "分钟前";
		if (between < 86400) return Math.floor((double) (between/3600)) + "小时前";
		if (between <= 864000) return Math.floor((double) (between/86400)) + "天前";
		return date("Y-m-d", timestamp);
	}

	//列出文件夹下所有文件
	public static List<String> listFiles(String directoryPath, boolean isAddDirectory) {
		List<String> list = new ArrayList<>();
		File baseFile = new File(directoryPath);
		if (baseFile.isFile() || !baseFile.exists()) return list;
		File[] files = baseFile.listFiles();
		assert files != null;
		for (File file : files) {
			if (file.isDirectory()) {
				if (isAddDirectory) list.add(file.getAbsolutePath());
				list.addAll(listFiles(file.getAbsolutePath(), isAddDirectory));
			} else {
				list.add(file.getAbsolutePath());
			}
		}
		return list;
	}

	//创建文件夹
	public static void makedir(String dir) {
		dir = trim(dir.replace(root_path(), "").replace("\\", "/"), "/");
		File path = new File(root_path(), dir);
		if (path.exists() && path.isDirectory()) return;
		String[] dirs = dir.split("/");
		String filePath = root_path();
		for (String d : dirs) {
			try {
				path = new File(filePath, d);
				if (!path.exists() || !path.isDirectory()) Files.createDirectory(path.toPath());
				filePath += "/" + d;
			} catch (Exception e) {
				error("#error?tips=FILE PATH CREATE FAIL:\n" + filePath + "/" + d);
			}
		}
	}
	public static boolean makedirs(String dir) {
		String filePath = root_path() + dir.replace(root_path(), "");
		File path = new File(filePath);
		if (path.exists() && path.isDirectory()) return true;
		if (!path.mkdirs()) error("#error?tips=FILE PATH CREATE FAIL:\n" + filePath);
		return false;
	}

	//复制文件夹
	public static void copyDir(String resource, String target) {
		copyDir(resource, target, true);
	}
	public static void copyDir(String resource, String target, boolean reCopy) {
		if (resource.contains(".jar!")) {
			File targetDir = new File(target);
			if (targetDir.exists()) {
				if (!reCopy) return;
			} else {
				if (!targetDir.mkdirs()) error("#error?tips=FILE PATH CREATE FAIL:\n" + target);
			}
			resource = ltrim(resource.split("\\.jar!")[1], "/");
			List<JarEntry> resourceFiles = new ArrayList<>();
			String jarPath = getJarPath();
			try {
				Enumeration<JarEntry> entrys;
				try (JarFile jarFile = new JarFile(jarPath)) {
					entrys = jarFile.entries();
				}
				while (entrys.hasMoreElements()) {
					JarEntry entry = entrys.nextElement();
					if (!entry.getName().equals(resource) && entry.getName().startsWith(resource)) resourceFiles.add(entry);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (resourceFiles.size() == 0) return;
			for (JarEntry entry : resourceFiles) {
				String fileName = entry.getName().replace(resource, "");
				File item = new File(targetDir.getAbsolutePath() + "/" + fileName);
				if (entry.isDirectory()) {
					if (!item.exists()) {
						if (!item.mkdirs()) error("#error?tips=FILE PATH CREATE FAIL:\n" + item.getPath());
					}
				} else {
					if (item.exists()) {
						if (!item.delete()) throw new IllegalArgumentException("FILE DELETE FAIL:\n" + item.getPath());
					}
					String content = getJarFileContent(jarPath, entry.getName());
					if (content == null) continue;
					writeFile(content, item);
				}
			}
			return;
		}
		File resourceDir = new File(resource);
		if (!resourceDir.exists()) error("#error?tips=RESOURCE FOLDER IS NOT EXIST:\n" + resource);
		File targetDir = new File(target);
		if (targetDir.exists()) {
			if (!reCopy) return;
		} else {
			if (!targetDir.mkdirs()) error("#error?tips=FILE PATH CREATE FAIL:\n" + target);
		}
		File[] resourceFiles = resourceDir.listFiles();
		if (resourceFiles == null || resourceFiles.length == 0) return;
		for (File file : resourceFiles) {
			File item = new File(targetDir.getAbsolutePath() + "/" + file.getName());
			if (file.isDirectory()) {
				copyDir(file.getAbsolutePath(), item.getAbsolutePath());
			} else if (file.isFile()) {
				if (item.exists()) {
					if (!item.delete()) error("#error?tips=FILE DELETE FAIL:\n" + item.getPath());
				}
				copyFile(file, item);
			}
		}
	}

	//复制文件
	public static void copyFile(File resource, File target) {
		try {
			//文件输入流并进行缓冲
			FileInputStream inputStream = new FileInputStream(resource);
			BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
			//文件输出流并进行缓冲
			FileOutputStream outputStream = new FileOutputStream(target);
			BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
			byte[] bytes = new byte[1024 * 2];
			int len;
			while ((len = inputStream.read(bytes)) != -1) bufferedOutputStream.write(bytes, 0, len);
			//刷新输出缓冲流
			bufferedOutputStream.flush();
			bufferedInputStream.close();
			bufferedOutputStream.close();
			inputStream.close();
			outputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//正则替换字符串
	public static String preg_replace(String pattern, Callback callback, String str) {
		Matcher matcher = Pattern.compile(pattern).matcher(str);
		StringBuffer res = new StringBuffer();
		while (matcher.find()) {
			String newStr = String.valueOf(callback.get());
			matcher.appendReplacement(res, newStr.replaceAll("([$\\\\])", "\\\\$1"));
		}
		matcher.appendTail(res);
		return res.toString();
	}
	
	//正则匹配字符串
	public static boolean preg_match(String pattern, String str) {
		return Pattern.compile(pattern).matcher(str).find();
	}

	//获取ip
	public static String ip() {
		getServlet();
		HttpServletRequest req = (HttpServletRequest) requests.get(request.getRequestURI());
		String ip = req.getHeader("x-forwarded-for");
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) ip = req.getHeader("Proxy-Client-IP");
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) ip = req.getHeader("WL-Proxy-Client-IP");
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) ip = req.getHeader("HTTP_CLIENT_IP");
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) ip = req.getHeader("HTTP_X_FORWARDED_FOR");
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) ip = req.getRemoteAddr();
		return ip;
	}

	//当前协议
	public static String https() {
		getServlet();
		return request.getScheme().equals("https") ? "https://" : request.getScheme() + "://";
	}

	//当前主机名
	public static String host() {
		getServlet();
		return request.getServerName() + (request.getServerPort() != 80 ? ":" + request.getServerPort() : "");
	}

	//当前域名
	public static String domain() {
		return https() + host();
	}

	//当前网址
	public static String url() {
		return domain() + request.getRequestURI() + ((request.getQueryString() != null && request.getQueryString().length() > 0) ? "?" + request.getQueryString() : "");
	}
	
	//获取路由网址
	public static String routeUrl() {
		getServlet();
		return routeUrl(request.getRequestURI());
	}
	public static String routeUrl(String url) {
		getServlet();
		Map<String, String> moduleMap = getModule(request);
		String module = moduleMap.get("module");
		String app = moduleMap.get("app");
		String act = moduleMap.get("act");
		String actions = trim(url, "/").toLowerCase();
		String[] arr = actions.split("/");
		if (url.startsWith("/")) {
			if (arr.length == 1) {
				return "/" + actions + "/" + app + "/" + act;
			} else if (arr.length == 2) {
				return "/" + actions + "/" + act;
			} else {
				return "/" + actions;
			}
		}
		if (arr.length == 1) {
			return "/" + module + "/" + app + "/" + actions;
		} else if (arr.length == 2) {
			return "/" + module + "/" + actions;
		} else {
			return "/" + actions;
		}
	}

	//组合网址
	public static String url(String str) {
		return url(str, null);
	}
	public static String url(String str, String param) {
		if (str == null || str.length() == 0) return "";
		str = routeUrl(str);
		if (param != null && param.length() > 0) {
			Object qs = json_decode(param);
			if (qs instanceof JSONObject) {
				Map<String, Object> innerMap = ((JSONObject)qs).getInnerMap();
				List<String> map = new ArrayList<>();
				for (String key : innerMap.keySet()) {
					map.add(key + "=" + innerMap.get(key));
				}
				qs = StringUtils.join(map, "&");
			} else if (qs instanceof JSONArray) {
				qs = StringUtils.join(((JSONArray)qs).toArray(new Object[0]), "&");
			}
			str += (Pattern.compile("[?&]").matcher(str).find() ? "&" : "?") + qs;
		}
		return str;
	}

	//格式化URL,suffix增加网址后缀, 如七牛?imageMogr2/thumbnail/200x200, 又拍云(需自定义)!logo
	public static String add_domain(String url) {
		return add_domain(url, "");
	}
	public static String add_domain(String url, String suffix) {
		if (imgDomain == null) imgDomain = getYml("sdk.image.domain", "");
		String host = domain();
		if (url != null && url.length() > 0 && !url.matches("^https?://.+$")) {
			if (url.startsWith("//")) {
				url = https() + url.substring(2);
			} else {
				if (url.contains("%domain%") && !url.contains(host)) {
					url = url.replace("%domain%", host);
				} else {
					url = url.replace("%domain%", "");
					if (url.charAt(0) == '/') {
						url = (imgDomain.length() > 0 ? imgDomain : host) + url;
					} else {
						if (Pattern.compile("^((http|https|ftp)://)?[\\w-_]+(\\.[\\w\\-_]+)+([\\w\\-.,@?^=%&:/~+#]*[\\w\\-@?^=%&/~+#])?$").matcher(host + "/" + url).find()) {
							url = (imgDomain.length() > 0 ? imgDomain : host) + "/" + url;
						} else {
							url = url.replace("\"/uploads/", "\"" + host + "/uploads/");
						}
					}
				}
			}
		}
		if (url != null && url.length() > 0 && !url.contains("/images/") && suffix.length() > 0 && !url.contains(suffix)) url += suffix;
		if (url != null) url = url.replace("%domain%", "");
		return url;
	}

	//递归一个数组/对象的属性加上域名
	public static <T> T add_domain_deep(T obj, String field) {
		String[] fields = field.split(",");
		for (int i = 0; i < fields.length; i++) fields[i] = fields[i].trim();
		return add_domain_deep(obj, fields);
	}
	@SuppressWarnings("unchecked")
	public static <T> T add_domain_deep(T obj, String[] fields) {
		if (obj instanceof List) {
			List<Object> o = (List<Object>)obj;
			for (int i = 0; i < o.size(); i++) {
				Object e = add_domain_deep(o.get(i), fields);
				((List<Object>) obj).set(i, e);
			}
		} else if (obj instanceof Map) {
			Map<String, Object> map = new HashMap<>((Map<String, Object>) obj);
			for (String key : map.keySet()) {
				if (!Arrays.asList(fields).contains(key)) continue;
				if (map.get(key).getClass() != String.class) continue;
				((Map<String, Object>) obj).put(key, add_domain((String) map.get(key)));
			}
		} else if (obj instanceof String) {
			obj = (T) add_domain((String) obj);
		} else if (obj instanceof DataList) {
			List<DataMap> list = add_domain_deep(((DataList) obj).list, fields);
			obj = (T) new DataList(list);
		} else if (obj instanceof DataMap) {
			Map<String, Object> map = add_domain_deep(((DataMap) obj).data, fields);
			obj = (T) new DataMap(map);
		} else if (obj != null) {
			try {
				Class<?> clazz = obj.getClass();
				Field[] _fields = clazz.getDeclaredFields();
				for (Field f : _fields) {
					if (!Arrays.asList(fields).contains(f.getName())) continue;
					String getterName = "get" + Character.toUpperCase(f.getName().charAt(0)) + f.getName().substring(1);
					Method getter = clazz.getMethod(getterName);
					String url = (String) getter.invoke(obj);
					String setterName = "set" + Character.toUpperCase(f.getName().charAt(0)) + f.getName().substring(1);
					Method setter = clazz.getMethod(setterName, f.getType());
					setter.invoke(obj, add_domain(url));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return obj;
	}

	//生成又拍云缩略图url
	public static String get_upyun_thumb_url(String url, String size) {
		if (url.length() > 0 && size.length() > 0 && !is_my_domain(url)) {
			//产品缩略图
			if (url.contains(getYml("spring.servlet.upload.path", ""))) url = url + '!' + size;
		}
		return url;
	}

	//是否本站域名
	public static boolean is_my_domain(String url) {
		if (imgDomain == null) imgDomain = getYml("sdk.image.domain", "");
		String img_domain = imgDomain;
		if (img_domain.length() == 0) img_domain = domain();
		return url.contains(img_domain);
	}

	//输出script
	public static String script(String msg) {
		return script(msg, "");
	}
	public static String script(String msg, String url) {
		try {
			String html = "<meta charset=\"UTF-8\"><script>";
			if (msg != null && msg.length() > 0) html += "alert('" + msg + "');";
			if (url != null && url.length() > 0) {
				if (url.startsWith("javascript:")) {
					html += url.substring(11);
				} else if (url.startsWith("js:")) {
					html += url.substring(3);
				} else {
					html += "location.href = '" + url + "';";
				}
			}
			html += "</script>";
			return html;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
	public static void writeScript(String msg) {
		writeScript(msg, "");
	}
	public static void writeScript(String msg, String url) {
		String html = script(msg, url);
		outputHtml(html);
	}
	public static String historyBack() {
		return historyBack("");
	}
	public static String historyBack(String msg) {
		return script(msg, "javascript:history.back()");
	}
	public static void writeHistoryBack() {
		writeHistoryBack("");
	}
	public static void writeHistoryBack(String msg) {
		writeScript(msg, "javascript:history.back()");
	}

	//跳转网址
	public static String location(String url) {
		return "tourl:" + url.replaceFirst("tourl:", "");
	}

	//写log
	public static void writeLog(String content) {
		writeLog(content, "log.txt");
	}
	public static void writeLog(String content, String file) {
		if (runtimeDir == null) runtimeDir = getYml("sdk.runtime.dir", "runtime");
		//String path = request.getSession().getServletContext().getRealPath(runtimeDir);
		String path = root_path() + "/" + runtimeDir;
		File filePath = new File(path);
		if (!filePath.exists()) {
			if (!filePath.mkdirs()) {
				error("#error?tips=FILE PATH CREATE FAIL:\n" + path);
				return;
			}
		}
		writeFile(date("yyyy-MM-dd HH:mm:ss") + "\n" + content + "\n\n", path + "/" + file, true);
	}
	public static void writeError(String content) {
		writeLog(content, "error.txt");
	}

	//写文件
	public static void writeFile(String content, String filepath) {
		writeFile(content, filepath, false);
	}
	public static void writeFile(String content, String filepath, boolean append) {
		writeFile(content, new File(filepath), append);
	}
	public static void writeFile(String content, File file) {
		writeFile(content, file, false);
	}
	public static void writeFile(String content, File file, boolean append) {
		try {
			FileWriter fileWritter = new FileWriter(file, append);
			fileWritter.write(content);
			fileWritter.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//输出文件内容
	public static void outputFile(String filepath, HttpServletResponse response) {
		try {
			File file = new File(filepath);
			if (!file.exists()) return;
			FileInputStream ips = new FileInputStream(file);
			ContentInfo contentInfo = ContentInfoUtil.findExtensionMatch(filepath);
			String mimeType = contentInfo != null ? contentInfo.getMimeType() : null;
			if (mimeType != null) response.setContentType(mimeType);
			ServletOutputStream out = response.getOutputStream();
			int len;
			byte[] buffer = new byte[1024 * 10];
			while ((len = ips.read(buffer)) != -1) out.write(buffer, 0, len);
			out.flush();
			out.close();
			ips.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//输出页面内容
	public static void outputHtml(String html) {
		try {
			getServlet();
			HttpServletResponse res = (HttpServletResponse) responses.get(request.getRequestURI());
			PrintWriter writer = res.getWriter();
			writer.write(html);
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//初始化Redis
	public static Redis redis() {
		 if (redis == null) redis = new Redis();
		 return redis;
	}

	//实例化模型
	public static <T> T model(String model, Object...initargs) {
		if (model == null || model.length() == 0) {
			error("#error?tips=MODEL's NAME IS EMPTY");
			return null;
		}
		try {
			return model(Class.forName("com.app.model."+Character.toUpperCase(model.charAt(0))+model.substring(1)), initargs);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	@SuppressWarnings("unchecked")
	public static <T> T model(Class<?> model, Object...initargs) {
		if (model == null) {
			error("#error?tips=MODEL's CLASS IS NULL");
			return null;
		}
		return (T) instance(model, initargs);
	}

	//实例化插件
	@SuppressWarnings("unchecked")
	public static <T> T plugin(String plugin, String type, Object...initargs) {
		if (plugin == null || plugin.length() == 0 || type == null || type.length() == 0) {
			error("#error?tips=PLUGIN's NAME OR TYPE IS EMPTY");
			return null;
		}
		try {
			return (T) plugin("com.framework.tool.plugins."+plugin+"."+Character.toUpperCase(type.charAt(0))+type.substring(1), initargs);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	//反射实例化插件
	public static Object plugin(String packageName, Object...initargs) {
		if (packageName == null || packageName.length() == 0) {
			error("#error?tips=PLUGIN PACKAGE NAME IS EMPTY");
			return null;
		}
		try {
			return plugin(Class.forName(packageName), initargs);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	public static Object plugin(Class<?> clazz, Object...initargs) {
		if (clazz == null) {
			error("#error?tips=PLUGIN CLASS IS EMPTY");
			return null;
		}
		Object instance;
		if (plugins == null || plugins.get(clazz.getName()) == null) {
			instance = instance(clazz, initargs);
			if (instance != null) {
				if (plugins == null) plugins = new HashMap<>();
				plugins.put(clazz.getName(), instance);
			}
		} else {
			instance = plugins.get(clazz.getName());
		}
		return instance;
	}

	//反射初始化实例
	public static Object instance(String packageName, Object...initargs) {
		if (packageName == null || packageName.length() == 0) {
			error("#error?tips=INSTANCE PACKAGE NAME IS EMPTY");
			return null;
		}
		try {
			return instance(Class.forName(packageName), initargs);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	public static Object instance(Class<?> clazz, Object...initargs) {
		if (clazz == null) {
			error("#error?tips=INSTANCE CLASS IS EMPTY");
			return null;
		}
		getServlet();
		HttpServletRequest req = (HttpServletRequest) requests.get(request.getRequestURI());
		HttpServletResponse res = (HttpServletResponse) responses.get(request.getRequestURI());
		Object instance = null;
		try {
			Class<?>[] parameterTypes = new Class<?>[initargs.length];
			for (int i = 0; i < initargs.length; i++) parameterTypes[i] = initargs[i].getClass();
			instance = clazz.getConstructor(parameterTypes).newInstance(initargs);
			try {
				clazz.getMethod("__construct", HttpServletRequest.class, HttpServletResponse.class).invoke(instance, req, res);
			} catch (NoSuchMethodException e) {
				//Method不存在
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return instance;
	}

	//反射调用方法
	public static void method(Object instance, String method, Object...args) {
		if (instance == null) return;
		if (instance instanceof String) instance = instance((String) instance);
		if (instance == null) return;
		Class<?>[] parameterTypes = new Class<?>[args.length];
		for (int i = 0; i < args.length; i++) parameterTypes[i] = args[i].getClass();
		try {
			instance.getClass().getMethod(method, parameterTypes).invoke(instance, args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//反射调用返回值方法
	@SuppressWarnings("unchecked")
	public static <T> T getMethod(Object instance, String method, Object...args) {
		if (instance == null) return null;
		if (instance instanceof String) return runMethod((String) instance, method, args);
		Class<?>[] parameterTypes = new Class<?>[args.length];
		for (int i = 0; i < args.length; i++) parameterTypes[i] = args[i].getClass();
		try {
			return (T) instance.getClass().getMethod(method, parameterTypes).invoke(instance, args);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	//反射调用返回值方法
	public static <T> T runMethod(String packageName, String method, Object...args) {
		return getMethod(instance(packageName), method, args);
	}
	public static <T> T runMethod(Class<?> clazz, String method, Object...args) {
		return getMethod(instance(clazz), method, args);
	}

	//上传文件
	public static String uploadFile(String key) {
		return uploadFile(key, "");
	}
	public static String uploadFile(String key, String dir) {
		return uploadFile(key, dir, "jpg,png,gif,bmp");
	}
	public static String uploadFile(String key, String dir, String fileType) {
		return uploadFile(key, dir, fileType, null);
	}
	public static String uploadFile(String key, String dir, String fileType, Map<String, Object> thirdParty) {
		Map<String, Object> files = uploadFile(dir, fileType, thirdParty, false);
		if (files == null || files.keySet().size() == 0) return "";
		return (String) files.get(key);
	}
	public static Map<String, Object> uploadFile(String dir, String fileType, boolean returnDetail) {
		return uploadFile(dir, fileType, null, returnDetail);
	}
	public static Map<String, Object> uploadFile(String dir, String fileType, Map<String, Object> thirdParty, boolean returnDetail) {
		dir += date("/yyyy/mm/dd");
		Upload upload = new Upload();
		return upload.file(dir, fileType, thirdParty, returnDetail);
	}

	//404
	public static Object error404() {
		getServlet();
		HttpServletResponse res = (HttpServletResponse) responses.get(request.getRequestURI());
		return error404(res);
	}
	public static Object error404(HttpServletResponse response) {
		response.setStatus(HttpStatus.NOT_FOUND.value());
		return null;
	}

	//503
	public static Object error503() {
		getServlet();
		HttpServletResponse res = (HttpServletResponse) responses.get(request.getRequestURI());
		return error503(res);
	}
	public static Object error503(HttpServletResponse response) {
		response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
		return null;
	}

	//字符串转任何类型
	@SuppressWarnings("unchecked")
	public static <T> T stringToBean(String value, Class<T> clazz) {
		if (value == null || value.length() == 0 || clazz == null) return null;
		if (clazz == Integer.class) {
			return (T) Integer.valueOf(value);
		} else if (clazz == Long.class) {
			return (T) Long.valueOf(value);
		} else if (clazz == Float.class) {
			return (T) Float.valueOf(value);
		} else if (clazz == Double.class) {
			return (T) Double.valueOf(value);
		} else if (clazz == BigDecimal.class) {
			return (T) BigDecimal.valueOf(Long.parseLong(value));
		} else if (clazz == String.class) {
			return (T) value;
		} else {
			try {
				return JSON.toJavaObject(JSON.parseObject(value), clazz);
			} catch (Exception e) {
				return JSON.toJavaObject(JSON.parseArray(value), clazz);
			}
		}
	}

	//任何类型转字符串
	public static <T> String beanToString(T value) {
		if (value == null) return null;
		if (value.getClass() == Integer.class || value.getClass() == Long.class ||
				value.getClass() == Float.class || value.getClass() == Double.class ||
				value.getClass() == BigDecimal.class || value.getClass() == String.class) {
			return String.valueOf(value);
		} else {
			return JSON.toJSONString(value);
		}
	}

	//Map转对象
	public static <T> T mapToBean(Map<String, Object> map, Class<T> clazz) {
		return stringToBean(JSON.toJSONString(map), clazz);
	}

	//对象转Map
	public static Map<String, Object> beanToMap(Object obj) {
		return new HashMap<>(JSON.parseObject(JSON.toJSONString(obj)));
	}

	//Map键排序
	public static <T> Map<String, T> sortMapByKey(Map<String, T> map) {
		if (map == null || map.isEmpty()) return null;
		Map<String, T> sortMap = new TreeMap<>(String::compareTo);
		sortMap.putAll(map);
		return sortMap;
	}

	//Map值排序
	public static <T, R> Map<T, R> sortMapByValue(Map<T, R> map) {
		if (map == null || map.isEmpty()) return null;
		List<Map.Entry<T, R> > list = new LinkedList<>(map.entrySet());
		list.sort((o1, o2) -> {
			if (o1.getValue().getClass() == String.class) return ((String) o1.getValue()).compareTo(((String) o2.getValue()));
			if (o1.getValue().getClass() == Integer.class || o1.getValue().getClass() == Long.class ||
					o1.getValue().getClass() == Float.class || o1.getValue().getClass() == Double.class || o1.getValue().getClass() == BigDecimal.class) {
				return new BigDecimal(String.valueOf(o1.getValue())).compareTo(new BigDecimal(String.valueOf(o2.getValue())));
			}
			return 0;
		});
		Map<T, R> sortMap = new LinkedHashMap<>();
		for (Map.Entry<T, R> item : list) sortMap.put(item.getKey(), item.getValue());
		return sortMap;
	}

	//请求
	public static String requestUrl(String method, String url) {
		return requestUrl(method, url, "", false, null);
	}
	public static String requestUrl(String method, String url, String data) {
		return requestUrl(method, url, data, false, null);
	}
	public static JSONObject requestUrl(String method, String url, String data, boolean returnJson) {
		Map<String, Object> map = parse_str(data);
		return requestUrl(method, url, map, returnJson);
	}
	public static JSONObject requestUrl(String method, String url, Map<String, Object> data, boolean returnJson) {
		String res = requestUrl(method, url, data, false, null);
		return JSON.parseObject(res);
	}
	public static String requestUrl(String method, String url, Map<String, Object> data) {
		return requestUrl(method, url, data, false, null);
	}
	public static String requestUrl(String method, String url, String data, boolean postJson, Map<String, String> headers) {
		Map<String, Object> map = parse_str(data);
		return requestUrl(method, url, map, postJson, headers);
	}
	public static String requestUrl(String method, String url, Map<String, Object> data, boolean postJson, Map<String, String> headers) {
		return requestUrl(method, url, data, postJson, headers, false);
	}
	public static String requestUrl(String method, String url, Map<String, Object> data, boolean postJson, Map<String, String> headers, boolean async) {
		if (async) {
			//异步执行
			String finalMethod = method;
			String finalUrl = url;
			new Thread(() -> requestUrl(finalMethod, finalUrl, data, postJson, headers, false)).start();
			return null;
		}
		getServlet();
		method = method.toUpperCase();
		if (!url.startsWith("http:") && !url.startsWith("https:")) url = trim(domain(), "/") + "/" + trim(url, "/");
		HttpURLConnection conn = null;
		BufferedReader reader = null;
		StringBuilder res = new StringBuilder();
		try {
			if (url.startsWith("https:")) {
				//忽略安全证书
				try {
					TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
						public X509Certificate[] getAcceptedIssuers() {return null;}
						public void checkClientTrusted(X509Certificate[] certs, String authType) {}
						public void checkServerTrusted(X509Certificate[] certs, String authType) {}
					}};
					SSLContext sc = SSLContext.getInstance("SSL");
					sc.init(null, trustAllCerts, new java.security.SecureRandom());
					HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
					HostnameVerifier allHostsValid = (hostname, session) -> true;
					HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			conn = (HttpURLConnection) new URL(url).openConnection();
			conn.setConnectTimeout(5000);
			conn.setUseCaches(false); //禁止缓存
			conn.setRequestProperty("Accept-Charset", "utf-8"); //设置接收编码
			conn.setRequestProperty("Connection", "keep-alive"); //开启长连接可以持续传输
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");
			//conn.setRequestProperty("Accept", "application/json"); //设置接收返回值的格式
			if (headers != null) {
				for (String key : headers.keySet()) conn.setRequestProperty(key, headers.get(key));
			}
			if (method.equalsIgnoreCase("POST")) {
				conn.setRequestMethod("POST");
				conn.setDoOutput(true); //运行写入默认为false, 置为true, 发送POST请求必须设置
				conn.setDoInput(true);
				boolean isMultipart = false;
				for (String key : data.keySet()) {
					if ((data.get(key) instanceof String) && ((String) data.get(key)).startsWith("@")) {
						isMultipart = true;
						break;
					}
				}
				if (isMultipart) {
					String boundary = "WebKitFormBoundary" + random_str(16);
					conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
					OutputStream stream = new DataOutputStream(conn.getOutputStream());
					for (String key : data.keySet()) {
						Object value = data.get(key);
						if (value == null) continue;
						StringBuilder strBuf = new StringBuilder();
						if ((data.get(key) instanceof String) && ((String) data.get(key)).startsWith("@")) {
							//file
							File file = new File(((String) value).substring(1));
							String filename = file.getName();
							ContentInfo contentInfo = ContentInfoUtil.findExtensionMatch(filename);
							String mimeType = contentInfo != null ? contentInfo.getMimeType() : null;
							if (mimeType == null) mimeType = "application/octet-stream";
							strBuf.append("\r\n").append("--").append(boundary).append("\r\n");
							strBuf.append("Content-Disposition: form-data; name=\"").append(key).append("\"; filename=\"").append(filename).append("\"\r\n");
							strBuf.append("Content-Type: ").append(mimeType).append("\r\n\r\n");
							stream.write(strBuf.toString().getBytes());
							DataInputStream in = new DataInputStream(Files.newInputStream(file.toPath()));
							int bytes;
							byte[] bufferOut = new byte[1024];
							while ((bytes = in.read(bufferOut)) != -1) {
								stream.write(bufferOut, 0, bytes);
							}
							in.close();
						} else {
							//input
							strBuf.append("\r\n").append("--").append(boundary).append("\r\n");
							strBuf.append("Content-Disposition: form-data; name=\"").append(key).append("\"\r\n\r\n");
							strBuf.append(value);
							stream.write(strBuf.toString().getBytes());
						}
					}
					byte[] endData = ("\r\n--" + boundary + "--\r\n").getBytes();
					stream.write(endData);
					stream.flush();
					stream.close();
				} else {
					String postData;
					//使用JSON提交
					if (postJson) {
						conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
						postData = JSONObject.toJSONString(data);
					} else {
						postData = http_build_query(data);
					}
					byte[] bytes = postData.getBytes(StandardCharsets.UTF_8);
					conn.setRequestProperty("Content-Length", String.valueOf(bytes.length));
					conn.getOutputStream().write(bytes);
				}
			} else {
				conn.connect();
			}
			/*// 获取所有响应头字段
			Map<String, List<String>> fields = conn.getHeaderFields();
			for (String key : fields.keySet()) System.out.println(key + ": " + fields.get(key));*/
			if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
				reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
				String line;
				while ((line = reader.readLine()) != null) res.append(line).append("\n");
			}
		} catch (Exception e) {
			System.out.println("发送 " + method + " 请求异常\n" + url);
			e.printStackTrace();
		} finally {
			try {
				if (reader != null) reader.close();
				if (conn != null) conn.disconnect();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return res.toString();
	}

	//获取MODULE、APP、ACT
	public static Map<String, String> getModule(HttpServletRequest req) {
		String uri = req.getRequestURI();
		if (moduleMap == null) moduleMap = new HashMap<>();
		if (moduleMap.get(uri) == null) {
			String module = null;
			String default_module = null;
			String[] routes = getYml("sdk.host.module.route", "").split(",");
			String[] modulers = new String[routes.length];
			String setup = "false"; //当前域名是否为域名绑定模块入口(sdk.host.module.route)
			for (int i = 0; i < routes.length; i++) {
				if (routes[i].startsWith("*=")) default_module = routes[i].split("=")[1];
				modulers[i] = routes[i].split("=")[1];
			}
			for (String route : routes) {
				if (route.startsWith((req.getServerName() + (req.getServerPort() != 80 ? ":" + req.getServerPort() : "")) + "=")) {
					module = route.split("=")[1];
					setup = "true";
					break;
				}
			}
			if (module == null || module.length() == 0) module = default_module;
			if (module == null || module.length() == 0) throw new IllegalArgumentException("Properties sdk.host.module.route IS ERROR");
			String moduler = module;
			String app = getYml("sdk.app.default.controller", "index");
			String act = getYml("sdk.app.default.action", "index");
			if (!uri.matches("^/(" + StringUtils.join(modulers, "|") + ").*")) uri = "/" + module + uri;
			Matcher matcher = Pattern.compile("^/("+StringUtils.join(modulers, "|")+")(/\\w\\w+)?(/\\w\\w+)?").matcher(uri);
			if (matcher.find()) {
				if (matcher.group(1) != null) moduler = matcher.group(1);
				if (matcher.group(2) != null) app = matcher.group(2).substring(1);
				if (matcher.group(3) != null) act = matcher.group(3).substring(1);
			}
			Map<String, String> map = new HashMap<>();
			map.put("module", moduler.toLowerCase()); //Package|Module
			map.put("app", app.toLowerCase()); //Class|Controller
			map.put("act", act.toLowerCase()); //Method|Action
			map.put("modules", StringUtils.join(modulers, "|")); //All packages (application.properties - sdk.host.module.route)
			map.put("setup", setup); //Whether the current domain is a setup package
			moduleMap.put(uri, map);
		}
		return moduleMap.get(uri);
	}

	//模板输出
	public static Object view(Object data, String webPath) {
		return view(data, webPath, null);
	}
	public static Object view(Object data, String webPath, boolean isWriter) {
		return view(data, webPath, null, isWriter);
	}
	public static Object view(Object data, String webPath, Object extend) {
		return view(data, webPath, extend, false);
	}
	public static Object view(Object data, String webPath, Object extend, boolean isWriter) {
		getServlet();
		HttpServletRequest req = (HttpServletRequest) requests.get(request.getRequestURI());
		HttpServletResponse res = (HttpServletResponse) responses.get(request.getRequestURI());
		if (webPath == null || webPath.length() == 0) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		ModelAndView mv = new ModelAndView(webPath);
		if (!preg_match("<[^>]+>", webPath)) {
			String[] webPaths = trim(webPath, "/").replace("\n", "<br>").split("\\?");
			webPath = webPaths[0];
			Map<String, Object> staticElement = null;
			if (webPaths.length > 1) staticElement = parse_str(webPaths[1]);
			if (staticElement != null) {
				for (String key : staticElement.keySet()) mv.addObject(key, staticElement.get(key));
			}
		}
		if (webPath.equals("error")) {
			return fetch(mv, webPath, isWriter);
		}
		if (data != null) {
			mv.addObject("data", DataMap.dataToMap(data));
		}
		Enumeration<String> attribute = req.getAttributeNames();
		while (attribute.hasMoreElements()) {
			Object obj = attribute.nextElement();
			if (obj.toString().contains(".")) continue;
			mv.addObject(obj.toString(), DataMap.dataToMap(req.getAttribute(obj.toString())));
		}
		Map<String, String[]> map = req.getParameterMap();
		if (map != null) {
			for (String key : map.keySet()) {
				String[] values = map.get(key);
				if (values != null && values.length > 0) mv.addObject(key, values.length == 1 ? values[0] : values);
			}
		}
		if (extend instanceof Map) {
			JSONObject extendObj = JSONObject.parseObject(JSON.toJSONString(DataMap.dataToMap(extend)));
			for (String key : extendObj.keySet()) mv.addObject(key, extendObj.get(key));
		}
		Object output = request().get("output");
		if (output != null) {
			mv.getModel().remove("output");
			if (String.valueOf(output).equals("model")) return mv.getModel();
		}
		return fetch(mv, webPath, isWriter);
	}
	public static Object fetch(ModelAndView mv, String webPath, boolean isWriter) {
		getServlet();
		HttpServletRequest req = (HttpServletRequest) requests.get(request.getRequestURI());
		Map<String, String> moduleMap = getModule(req);
		String module = moduleMap.get("module");
		String app = moduleMap.get("app");
		String act = moduleMap.get("act");
		boolean isError = webPath.equals("error"); //错误页面
		if (getYml("spring.mvc.view.type", "").equalsIgnoreCase("Tengine")) {
			Tengine engine = new Tengine();
			JSONObject view_replace_str = getYml("spring.mvc.view.replace-str", new JSONObject());
			if (!view_replace_str.isEmpty()) {
				for (String key : view_replace_str.keySet()) {
					String value = (String) view_replace_str.get(key);
					value = value.replace("__DOMAIN__", domain());
					engine.setReplace("{"+key+"}", value);
				}
			}
			boolean isExcludeCache = req.getServerName().equals("localhost");
			if (!isError) {
				String className = "com.app."+module+"."+Character.toUpperCase(app.charAt(0)) + app.substring(1);
				try {
					engine.classForCustomFunction(Class.forName(className));
				} catch (ClassNotFoundException e) {
					writeError("Tengine: " + className + " IS NOT FOUND");
				}
				JSONObject not_check_login = getYml("sdk.not.check.login", new JSONObject());
				if ( !not_check_login.isEmpty() ) {
					JSONArray param = not_check_login.getJSONArray(app);
					if ( param == null || param.isEmpty() ) {
						isExcludeCache = true;
					} else {
						if ( !param.contains("*") && !param.contains(act) ) {
							isExcludeCache = true;
						} else if ( getHeaders("Authorization") != null && getHeaders("Authorization").length() > 0 ) {
							isExcludeCache = true;
						}
					}
				}
			}
			String prefix = getYml("spring.mvc.view.prefix", "");
			String suffix = getYml("spring.mvc.view.suffix", "");
			String path = getYml("spring.mvc.view.path", "");
			String templateRoot = null;
			if (path.length() > 0) {
				templateRoot = root_path() + "/" + trim(path, "/");
				File file = new File(templateRoot);
				if (!file.exists() || !file.isDirectory()) templateRoot = null;
			}
			if (templateRoot == null) {
				templateRoot = Objects.requireNonNull(Common.class.getResource(prefix)).getPath();
			}
			templateRoot = rtrim(templateRoot, "/") + "/";
			if (!isError) {
				templateRoot += module;
			}
			engine.assigns(mv.getModel());
			String html;
			if (preg_match("<[^>]+>", webPath)) { //html标签即按模板内容输出
				html = engine.display(null, webPath, false);
			} else {
				html = engine.display(templateRoot, trim(webPath, "/").replace(module + "/", "") + suffix, isExcludeCache);
			}
			if (isWriter) {
				outputHtml(html);
				return null;
			}
			return html;
		}
		return mv;
	}

	//success
	public static Object success() {
		return success(null);
	}
	public static Object success(Object data) {
		return success(data, "success");
	}
	public static Object success(Object data, String msg) {
		return success(data, msg, null);
	}
	public static Object success(Object data, String msg, Object extend) {
		getServlet();
		HttpServletRequest req = (HttpServletRequest) requests.get(request.getRequestURI());
		String dataValue = String.valueOf(data);
		boolean isStart = Pattern.compile("^[@#]").matcher(dataValue).find();
		//保持tourl:|stay:在data, .html|.view|<tag>|json:|xml:|view:|display:在msg
		if ((data instanceof String) &&
				(isStart || Pattern.compile("\\.html$").matcher(dataValue).find() || Pattern.compile("<[^>]+>").matcher(dataValue).find() ||
						Pattern.compile("^(json|xml|view|display):/").matcher(dataValue).find() || Pattern.compile("^(tourl|redirect|stay):").matcher(dataValue).find() )) {
			String tmp = String.valueOf(data);
			data = isStart ? null : msg;
			msg = tmp;
		}
		if (isAjax()) {
			try {
				Map<String, Object> json = new HashMap<>();
				if (data != null) {
					json.put("data", DataMap.dataToMap(data));
				}
				if (Pattern.compile("^(tourl|redirect):").matcher(msg).find()) {
					json.put("toUrl", msg.replaceAll("^(tourl|redirect):", ""));
					msg = "success";
				} else if (Pattern.compile("^(stay):").matcher(msg).find()) {
					json.put("stay", 1);
					msg = "success";
				}
				json.put("msg", Pattern.compile("^[@#]").matcher(msg).find() ? "success" : msg);
				json.put("code", 0);
				if (extend instanceof Map) json.putAll(JSONObject.parseObject(JSON.toJSONString(DataMap.dataToMap(extend))));
				return json;
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (Pattern.compile("^(tourl|redirect):").matcher(msg).find()) {
			return "redirect:"+msg.replaceAll("^(tourl|redirect):", "");
		} else if (msg.startsWith("@") || msg.startsWith("#")) {
			return view(data, msg.substring(1), extend, msg.startsWith("#"));
		} else {
			boolean isWriter = false;
			if (preg_match("\\.html$", msg)) { //.html结尾按完整路径处理
				msg = msg.substring(0, msg.length() - 5);
			} else if (preg_match("<[^>]+>", msg)) { //html标签即按模板内容输出
				isWriter = true;
			} else {
				Map<String, String> moduleMap = getModule(req);
				msg = moduleMap.get("module") + "/" + moduleMap.get("app") + "/" + moduleMap.get("act");
			}
			return view(data, trim(msg, "/"), extend, isWriter);
		}
		return null;
	}

	//error
	public static Object error() {
		return error("error");
	}
	public static Object error(String msg) {
		return error(msg, 0);
	}
	public static Object error(String msg, String url) {
		return script(msg, url);
	}
	public static Object error(String msg, int code) {
		if (code == -9 || code == -100) code = -10;
		if (isAjax()) {
			Map<String, Object> json = new HashMap<>();
			json.put("msg", msg.matches("[@#]/?error") ? "error" : msg.replaceAll("^[@#]/?error\\?tips=", "").replace("<br>", " "));
			json.put("code", code);
			return json;
		} else if (Pattern.compile("^[@#]").matcher(msg).find()) {
			return view(null, msg.substring(1).replace(root_path(), ""), msg.startsWith("#"));
		} else {
			switch (code) {
				case -10:return "redirect:/passport/login";
				case -1:return "redirect:/";
				default:return historyBack(msg);
			}
		}
	}

}
