//Developed by @mario 2.6.20220711
package com.framework.tool;

import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

public class Request {
	private RequestWrapper request;
	private HttpServletRequest originRequest;
	private Map<String, String[]> params;

	public Request() {
		ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		HttpServletRequest req = Objects.requireNonNull(servletRequestAttributes).getRequest();
		this.init(req);
	}
	public Request(HttpServletRequest request) {
		this.init(request);
	}

	public void init(HttpServletRequest request) {
		try {
			this.originRequest = request;
			this.request = new RequestWrapper(request);
			this.request.setCharacterEncoding("utf-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public HttpServletRequest getRequest() {
		return this.originRequest;
	}

	public Object getAttribute(String key) {
		return this.request.getAttribute(key);
	}
	public void setAttribute(String key, Object value) {
		this.request.setAttribute(key, value);
	}
	public void removeAttribute(String key) {
		this.request.removeAttribute(key);
	}

	public String domain() {
		String scheme = this.request.getScheme().equals("https") ? "https://" : this.request.getScheme() + "://";
		String server = this.request.getServerName() + (this.request.getServerPort() != 80 ? ":" + this.request.getServerPort() : "");
		return scheme + server;
	}
	
	public String url() {
		return this.request.getRequestURI();
	}

	public String get(String key) {
		return get(key, "");
	}
	public String get(String key, String defaultValue) {
		return act(key, defaultValue, "get");
	}
	public int get(String key, int defaultValue) {
		return act(key, defaultValue, "get");
	}
	public float get(String key, float defaultValue) {
		return act(key, defaultValue, "get");
	}
	public boolean get(String key, boolean defaultValue) {
		return act(key, defaultValue, "get");
	}
	public String[] get(String key, String[] defaultValue) {
		return act(key, defaultValue == null ? new String[0] : defaultValue, "get");
	}
	public DataList get(String key, DataList defaultValue) {
		return act(key, defaultValue, "get");
	}

	public String post(String key) {
		return post(key, "");
	}
	public String post(String key, String defaultValue) {
		return act(key, defaultValue, "post");
	}
	public int post(String key, int defaultValue) {
		return act(key, defaultValue, "post");
	}
	public float post(String key, float defaultValue) {
		return act(key, defaultValue, "post");
	}
	public boolean post(String key, boolean defaultValue) {
		return act(key, defaultValue, "post");
	}
	public String[] post(String key, String[] defaultValue) {
		return act(key, defaultValue == null ? new String[0] : defaultValue, "post");
	}
	public DataList post(String key, DataList defaultValue) {
		return act(key, defaultValue, "post");
	}

	public String param(String key) {
		return param(key, "");
	}
	public <T> T param(String key, T defaultValue) {
		return act(key, defaultValue, "param");
	}
	public String param(int index) {
		return param(index, "");
	}
	public <T> T param(int index, T defaultValue) {
		return act(String.valueOf(index), defaultValue, "param");
	}

	public String path() {
		return this.getRequestURI();
	}
	public String path(int index) {
		return path(index, "");
	}
	public <T> T path(int index, T defaultValue) {
		return path(String.valueOf(index), defaultValue);
	}
	public String path(String index) {
		return path(index, "");
	}
	public <T> T path(String index, T defaultValue) {
		return act(index, defaultValue, "path");
	}

	public String session(String key) {
		return session(key, "");
	}
	public String session(String key, String defaultValue) {
		return act(key, defaultValue, "session");
	}
	public int session(String key, int defaultValue) {
		return act(key, defaultValue, "session");
	}
	public float session(String key, float defaultValue) {
		return act(key, defaultValue, "session");
	}
	public <T> T[] session(String key, T[] defaultValue) {
		return act(key, defaultValue, "session");
	}
	public Object session(String key, boolean defaultValue) {
		return act(key, null, "session");
	}

	public String cookie(String key) {
		return cookie(key, "");
	}
	public <T> T cookie(String key, T defaultValue) {
		return act(key, defaultValue, "cookie");
	}

	public String server(String key) {
		return act(key, "", "server");
	}
	public Map<String, String> server() {
		return act("", new HashMap<>(), "server");
	}

	public String header(String key) {
		return act(key, "", "header");
	}

	public InputStream input() {
		try {
			return act("", InputStream.class.getConstructor().newInstance(), "input");
		} catch (Exception e) {
			return null;
		}
	}

	public String file(String key) {
		return file(key, "");
	}
	public String file(String key, String dir) {
		return file(key, dir, "jpg,png,gif,bmp");
	}
	public String file(String key, String dir, String fileType) {
		return file(key, dir, fileType, null);
	}
	public String file(String key, String dir, String fileType, Map<String, Object> thirdParty) {
		Map<String, Object> files = file(dir, fileType, thirdParty, false);
		if (files == null || files.keySet().size() == 0) return "";
		return (String) files.get(key);
	}
	public Map<String, Object> file(String dir, String fileType, boolean returnDetail) {
		return file(dir, fileType, null, returnDetail);
	}
	public Map<String, Object> file(String dir, String fileType, Map<String, Object> thirdParty, boolean returnDetail) {
		dir += Common.date("/yyyy/mm/dd");
		return act(dir, new HashMap<>(), "file", fileType, thirdParty, returnDetail);
	}

	public <T> T act(String key, T defaultValue, String method) {
		return act(key, defaultValue, method, "", null, false);
	}
	@SuppressWarnings("unchecked")
	public <T> T act(String key, T defaultValue, String method, String fileType, Map<String, Object> thirdParty, boolean returnDetail) {
		if (key.contains("/")) key = key.split("/")[0];
		Object[] values = null;
		switch (method.toUpperCase()) {
			case "GET":
			case "POST": {
				Map<String, String[]> params = getParameterMap();
				if (defaultValue instanceof DataList) {
					//param[0][id]
					List<String> list = new ArrayList<>();
					for (String k : params.keySet()) {
						if (Pattern.compile(key+"\\[\\d+]\\[\\w+]").matcher(Common.url_decode(k)).find()) list.add(k);
					}
					if (list.size() == 0) return null;
					list.sort((o1, o2) -> {
						Pattern pattern = Pattern.compile("\\d+|[a-z]+|[A-Z]+|[\\u4E00-\\u9FFF]");
						Matcher matcher1 = pattern.matcher(Common.url_decode(o1));
						Matcher matcher2 = pattern.matcher(Common.url_decode(o2));
						List<String> o1List = new ArrayList<>();
						List<String> o2List = new ArrayList<>();
						while (matcher1.find()) o1List.add(matcher1.group());
						while (matcher2.find()) o2List.add(matcher2.group());
						int size = Math.min(o1List.size(), o2List.size());
						for (int i = 0; i < size; i++) {
							int o1Code = o1List.get(i).hashCode();
							int o2Code = o2List.get(i).hashCode();
							if (o1Code != o2Code) return o1Code - o2Code;
						}
						return 0;
					});
					DataList res = new DataList();
					for (String k : list) {
						Matcher matcher = Pattern.compile("^\\w+\\[(\\d+)]\\[(\\w+)]$").matcher(Common.url_decode(k));
						if (matcher.find()) {
							int index = Integer.parseInt(matcher.group(1));
							if (res.hasIndex(index)) {
								DataMap data = res.get(index);
								data.put(matcher.group(2), params.get(k)[0]);
								res.set(index, data);
							} else {
								DataMap data = new DataMap();
								data.put(matcher.group(2), params.get(k)[0]);
								res.add(data);
							}
						}
					}
					if (res.size() == 0) return null;
					return (T) res;
				}
				values = params.get(key);
				break;
			}
			case "SESSION": {
				Object value = this.request.getSession().getAttribute(key);
				if (value == null) return defaultValue;
				if (defaultValue == null) return (T) value;
				values = new Object[]{value};
				break;
			}
			case "COOKIE": {
				Cookie[] cookies = this.request.getCookies();
				if (cookies == null) return defaultValue;
				try {
					for (Cookie cookie : cookies) {
						if (cookie.getName().equals(key)) {
							values = new Object[]{URLDecoder.decode(cookie.getValue(), "UTF-8")};
							break;
						}
					}
				} catch (Exception e) {
					return defaultValue;
				}
				break;
			}
			case "PARAM": {
				String uri = this.request.getRequestURI();
				if (!uri.matches("^/\\w+/\\w+/\\w+/\\w+.*")) return defaultValue;
				uri = uri.replaceAll("^/\\w+/\\w+/\\w+/", "");
				String[] params = uri.split("/");
				if (key.matches("^-?\\d+$")) {
					int index = Integer.parseInt(key);
					if (index >= params.length || index < -params.length) return defaultValue;
					if (index < 0) index += params.length;
					values = new Object[]{params[index]};
				} else {
					for (int i = 0; i < params.length; i+=2) {
						if (params[i].equals(key)) {
							if (i == params.length - 1) {
								values = new Object[]{""};
							} else {
								values = new Object[]{params[i + 1]};
							}
							break;
						}
					}
				}
				break;
			}
			case "PATH": {
				String uri = Common.trim(this.request.getRequestURI(), "/");
				String[] params = uri.split("/");
				if (!key.matches("^-?\\d+$")) return null;
				int index = Integer.parseInt(key);
				if (index >= params.length || index < -params.length) return defaultValue;
				if (index < 0) index += params.length;
				values = new Object[]{params[index]};
				break;
			}
			case "SERVER": {
				Properties properties = System.getProperties();
				if (defaultValue instanceof Map) {
					Set<Object> set = properties.keySet();
					Map<String, String> map = new HashMap<>();
					for (Object k : properties.keySet()) {
						map.put(String.valueOf(k), (String) properties.get(k));
					}
					return (T) map;
				} else {
					return (T) properties.get(key);
				}
			}
			case "HEADER": {
				Map<String, String> headers = new HashMap<>();
				Enumeration<String> headerNames = this.request.getHeaderNames();
				while (headerNames.hasMoreElements()) {
					String _key = headerNames.nextElement();
					String value = this.request.getHeader(_key);
					headers.put(_key, value);
				}
				if (defaultValue.getClass().isArray()) {
					values = new Object[headers.size()];
					int i = 0;
					for (String _key : headers.keySet()) {
						values[i] = headers.get(_key);
						i++;
					}
				} else {
					return (T) headers.get(key);
				}
				break;
			}
			case "INPUT": {
				return (T) this.request.getInputStream();
			}
			case "FILE": {
				Upload upload = new Upload();
				return (T) upload.file(key, fileType, thirdParty, returnDetail);
			}
		}
		if (values == null || values.length == 0) return defaultValue;
		if (defaultValue.getClass() == Integer.class) {
			if (String.valueOf(values[0]).length() == 0) return defaultValue;
			return (T) Integer.valueOf((String) values[0]);
		} else if (defaultValue.getClass() == Float.class) {
			if (String.valueOf(values[0]).length() == 0) return defaultValue;
			return (T) Float.valueOf((String) values[0]);
		} else if (defaultValue.getClass() == Boolean.class) {
			if (String.valueOf(values[0]).length() == 0) return (T) Boolean.FALSE;
			return (T) Boolean.valueOf(((String) values[0]).equalsIgnoreCase("true"));
		} else if (defaultValue.getClass().isArray()) {
			if (defaultValue.getClass() == Integer[].class || defaultValue.getClass() == int[].class ||
					defaultValue.getClass() == Long[].class || defaultValue.getClass() == long[].class) {
				int[] res = new int[values.length];
				for (int i = 0; i < values.length; i++) {
					if (String.valueOf(values[i]).length() == 0) values[i] = "0";
					res[i] = Integer.parseInt(String.valueOf(values[i]));
				}
				return (T) res;
			} else if (defaultValue.getClass() == Float[].class || defaultValue.getClass() == float[].class ||
					defaultValue.getClass() == Double[].class || defaultValue.getClass() == double[].class) {
				float[] res = new float[values.length];
				for (int i = 0; i < values.length; i++) {
					if (String.valueOf(values[i]).length() == 0) values[i] = "0";
					res[i] = Float.parseFloat(String.valueOf(values[i]));
				}
				return (T) res;
			}
			return (T) values;
		}
		return (T) values[0];
	}

	public Map<String, String[]> getParameterMap() {
		if (this.params != null) return this.params;
		Map<String, String[]> params = new HashMap<>();
		if (ServletFileUpload.isMultipartContent(this.request)) {
			//Multipart form
			FileItemFactory factory = new DiskFileItemFactory();
			ServletFileUpload upload = new ServletFileUpload(factory);
			try {
				List<FileItem> fileItems = upload.parseRequest(this.request);
				for (FileItem item : fileItems) {
					if (item.isFormField()) {
						String name = item.getFieldName();
						String value = item.getString("utf-8");
						params.put(name, new String[]{value.trim()});
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			//Simple form
			Map<String, String[]> map = this.request.getParameterMap();
			if (map != null) {
				for (String name : map.keySet()) {
					String[] _values = map.get(name);
					String[] values = new String[_values.length];
					for (int i = 0; i < _values.length; i++) values[i] = _values[i].replaceAll("(['\"\\\\])", "\\\\$1");
					params.put(name, values);
				}
			}
		}
		this.params = params;
		return params;
	}

	public String getQueryString() {
		return this.request.getQueryString();
	}
	public String getRequestURI() {
		return this.request.getRequestURI();
	}
	public HttpSession getSession() {
		return this.request.getSession();
	}
	public Cookie[] getCookies() {
		return this.request.getCookies();
	}

	//自定义HttpServletRequestWrapper, 让request输入流可重复使用多次
	public static class RequestWrapper extends HttpServletRequestWrapper {
		private static final String UTF_8 = "UTF-8";
		private final Map<String, String[]> paramsMap;
		private byte[] body; // 报文

		public RequestWrapper(HttpServletRequest request) {
			super(request);
			try {
				body = readBytes(request.getInputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}

			// 首先从POST中获取数据
			if ("POST".equalsIgnoreCase(request.getMethod())) {
				paramsMap = getParamMapFromPost(this);
			} else {
				paramsMap = getParamMapFromGet(this);
			}
		}

		@Override
		public Map<String, String[]> getParameterMap() {
			return paramsMap;
		}

		@Override
		public String getParameter(String name) {// 重写getParameter，代表参数从当前类中的map获取
			String[] values = paramsMap.get(name);
			if (values == null || values.length == 0) {
				return null;
			}
			return values[0];
		}

		@Override
		public String[] getParameterValues(String name) {// 同上
			return paramsMap.get(name);
		}

		@Override
		public Enumeration<String> getParameterNames() {
			return Collections.enumeration(paramsMap.keySet());
		}

		@Override
		public BufferedReader getReader() {
			return new BufferedReader(new InputStreamReader(getInputStream()));
		}

		@Override
		public ServletInputStream getInputStream() {
			final ByteArrayInputStream bais = new ByteArrayInputStream(body);
			return new ServletInputStream() {
				@Override
				public boolean isFinished() {
					return false;
				}
				@Override
				public boolean isReady() {
					return false;
				}
				@Override
				public void setReadListener(ReadListener readListener) {

				}
				@Override
				public int read() {
					return bais.read();
				}
			};
		}

		private Map<String, String[]> getParamMapFromGet(HttpServletRequest request) {
			return parseQueryString(request.getQueryString());
		}

		private HashMap<String, String[]> getParamMapFromPost(HttpServletRequest request) {
			String body = StringUtils.EMPTY;
			try {
				body = getRequestBody(request.getInputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
			HashMap<String, String[]> result = new HashMap<>();
			if (0 == body.length()) {
				return result;
			}
			return parseQueryString(body);
		}

		private String getRequestBody(InputStream stream) throws IOException {
			String line;
			StringBuilder body = new StringBuilder();
			int counter = 0;
			// 读取POST提交的数据内容
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			while ((line = reader.readLine()) != null) {
				if (counter > 0) {
					body.append("rn");
				}
				body.append(line);
				counter++;
			}
			reader.close();
			return body.toString();
		}

		public HashMap<String, String[]> parseQueryString(String s) {
			String[] valArray;
			if (s == null) {
				return null;
			}
			HashMap<String, String[]> ht = new HashMap<>();
			StringTokenizer st = new StringTokenizer(s, "&");
			while (st.hasMoreTokens()) {
				String pair = st.nextToken();
				int pos = pair.indexOf('=');
				if (pos == -1) {
					continue;
				}
				String key = pair.substring(0, pos);
				String val = pair.substring(pos + 1);
				if (ht.containsKey(key)) {
					String[] oldVals = ht.get(key);
					valArray = new String[oldVals.length + 1];
					System.arraycopy(oldVals, 0, valArray, 0, oldVals.length);
					valArray[oldVals.length] = decodeValue(val);
				} else {
					valArray = new String[1];
					valArray[0] = decodeValue(val);
				}
				ht.put(key, valArray);
			}
			return ht;
		}

		private static byte[] readBytes(InputStream in) {
			try {
				BufferedInputStream bufin = new BufferedInputStream(in);
				final int buffSize = 1024;
				ByteArrayOutputStream out = new ByteArrayOutputStream(buffSize);
				byte[] temp = new byte[buffSize];
				int size;
				while ((size = bufin.read(temp)) != -1) {
					out.write(temp, 0, size);
				}
				out.flush();
				byte[] content = out.toByteArray();
				bufin.close();
				out.close();
				return content;
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}

		//自定义解码函数
		private String decodeValue(String value) {
			try {
				if (value.contains("%u")) {
					return URLEncoder.encode(value, UTF_8);
				} else {
					return URLDecoder.decode(value.replaceAll("%(?![0-9a-fA-F]{2})", "%25"), UTF_8);
				}
			} catch (UnsupportedEncodingException e) {
				return StringUtils.EMPTY;// 非UTF-8编码
			}
		}
	}
}
