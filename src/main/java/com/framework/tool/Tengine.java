//Developed by @mario 3.2.20220711
package com.framework.tool;

import com.alibaba.fastjson.*;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.math.*;
import java.util.*;
import java.util.regex.*;

public class Tengine {
	
	static String prefix;
	static String suffix;
	static boolean cacheEnabled;
	static String cacheDir;
	static String cacheSplitChar; //模板数据MD5、模板文件路径SHA1、模板内容的分隔符
	static String runtimeDir;
	static String rootPath;
	
	private final Request request;
	private final Map<String, Object> data = new HashMap<>();
	private final Map<String, String> replace = new HashMap<>();
	private final Map<String, String> originMap = new HashMap<>();
	private final Map<String, String> normalMap = new HashMap<>();
	private Class<?> clazz;
	private String currentMark = "";

	static {
		prefix = Common.getYml("spring.mvc.view.prefix", "/META-INF/resources/");
		suffix = Common.getYml("spring.mvc.view.suffix", ".html");
		cacheEnabled = Common.getYml("spring.mvc.view.cache.enabled", false);
		cacheDir = Common.getYml("spring.mvc.view.cache-dir", "templates");
		cacheSplitChar = Common.getYml("spring.mvc.view.cache-split-char", "!@#$%^&*(^^^^^^%$#");
		runtimeDir = Common.getYml("sdk.runtime.dir", "runtime");
		rootPath = Common.root_path();
	}
	
	public Tengine() {
		this.request = Common.request();
	}

	private String setSuffix(String path) {
		if (path == null || path.length() == 0) return "";
		if (!path.endsWith(suffix)) path += suffix;
		return path;
	}

	//模板自定义替换字符
	public void setReplace(String key, String value) {
		this.replace.put(key, value);
	}

	//当前运行的Controller类, 模板自定义函数用
	public void classForCustomFunction(Class<?> clazz) {
		this.clazz = clazz;
	}

	public void assign(String key, Object value) {
		this.data.put(key, value);
	}
	public void assigns(Map<String, Object> data) {
		this.data.putAll(data);
	}

	public String display(String templateRoot, String templatePath, boolean isExcludeCache) {
		if (templateRoot == null) {
			return displayExtend(null, templatePath);
		}
		templateRoot = Common.rtrim(templateRoot, "/");
		String path = templateRoot + "/" + templatePath;
		File template = new File(this.setSuffix(path));
		if (!template.exists()) throw new IllegalArgumentException("TEMPLATE FILE IS NOT EXIST:\n" + path);
		String dataMd5 = Common.md5(JSON.toJSONString(this.data, SerializerFeature.WriteMapNullValue));
		String cachePath = rootPath + "/" + runtimeDir + "/" + cacheDir;
		String cacheFile = template.getName() + "." + dataMd5;
		String cacheFilePath = cachePath + "/" + cacheFile;
		if (cacheEnabled && !isExcludeCache) {
			File file = new File(cacheFilePath);
			if (file.exists()) {
				try {
					int len;
					byte[] buffer = new byte[1024 * 2];
					StringBuilder sbf = new StringBuilder();
					FileInputStream ips = new FileInputStream(file);
					while ((len = ips.read(buffer)) != -1) sbf.append(new String(buffer, 0, len));
					ips.close();
					String[] param = sbf.toString().split(cacheSplitChar.replaceAll("([\\^$?*+(\\[\\\\])", "\\\\$1"));
					if (param[0].equals(dataMd5) && param[1].equals(Common.sha1(path)) && param.length > 2) return param[2];
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}
		}
		String html = displayExtend(templateRoot, templatePath);
		if (cacheEnabled && !isExcludeCache) {
			if (!Common.makedir(cachePath)) return null;
			File file = new File(cacheFilePath);
			try {
				FileWriter fileWritter = new FileWriter(file);
				fileWritter.write(dataMd5 + cacheSplitChar + Common.sha1(path) + cacheSplitChar + (html == null ? "" : html));
				fileWritter.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return html;
	}
	public String displayExtend(String templateRoot, String templatePath) {
		File file = null;
		if (templateRoot != null) {
			String templateFilepath = templateRoot + "/" + templatePath;
			file = new File(this.setSuffix(templateFilepath));
			if (!file.exists()) throw new IllegalArgumentException("FILE IS NOT EXIST:\n" + templateFilepath);
		}
		ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		HttpServletRequest req = Objects.requireNonNull(servletRequestAttributes).getRequest();
		Map<String, String> moduleMap = Common.getModule(req);
		String module = moduleMap.get("module");
		String templateDir = templateRoot == null ? Objects.requireNonNull(Tengine.class.getResource(prefix)).getPath() + module : templateRoot;
		String templateHtml = "";
		try {
			StringBuilder sbf = new StringBuilder();
			if (templateRoot == null) {
				sbf = new StringBuilder(templatePath);
			} else {
				FileInputStream ips = new FileInputStream(file);
				int len;
				byte[] buffer = new byte[1024 * 2];
				while ((len = ips.read(buffer)) != -1) {
					sbf.append(new String(buffer, 0, len));
				}
				ips.close();
			}
			
			//{extend name=""}
			StringBuffer html = new StringBuffer(sbf.toString());
			Matcher matcher = Pattern.compile("\\{extend name=\"([^\"]+)\"\\s*/?}").matcher(sbf.toString());
			while (matcher.find()) {
				this.currentMark = matcher.group(0);
				String path;
				String matcherDir = templateDir;
				if (matcher.group(1).startsWith("./")) {
					// ./file.html
					path = matcherDir + "/" + matcher.group(1).replace("./", "");
				} else if (matcher.group(1).startsWith("../")) {
					// ../file.html
					Matcher m = Pattern.compile("(\\.\\./)").matcher(matcher.group(1));
					while (m.find()) matcherDir = new File(matcherDir).getParent();
					path = matcherDir + "/" + matcher.group(1).replace("../", "");
				} else if (matcher.group(1).startsWith("~") || matcher.group(1).startsWith("/")) {
					// ~file.html /file.html
					path = rootPath + "/" + matcher.group(1).substring(1);
				} else {
					// file.html
					path = matcherDir + "/" + matcher.group(1);
				}
				
				//{block name=""}{/block}
				File extendFile = new File(this.setSuffix(path));
				if (!extendFile.exists()) throw new IllegalArgumentException("FILE IS NOT EXIST:\n" + path);
				FileInputStream ip = new FileInputStream(extendFile);
				StringBuilder extendSbf = new StringBuilder();
				int len;
				byte[] buffer = new byte[1024 * 2];
				while ((len = ip.read(buffer)) != -1) {
					extendSbf.append(new String(buffer, 0, len));
				}
				ip.close();
				Matcher blockMatcher = Pattern.compile("\\{block name=\"(\\w+)\"\\s*}[\\s\\S]*?\\{/block}").matcher(extendSbf.toString());
				StringBuffer extendHtml = new StringBuffer();
				while (blockMatcher.find()) {
					Matcher templateMatcher = Pattern.compile("\\{block name=\""+blockMatcher.group(1)+"\"\\s*}([\\s\\S]*?)\\{/block}").matcher(sbf.toString());
					while (templateMatcher.find()) {
						blockMatcher.appendReplacement(extendHtml, templateMatcher.group(1).replaceAll("([$\\\\])", "\\\\$1"));
					}
				}
				blockMatcher.appendTail(extendHtml);
				
				html = extendHtml;
			}
			
			//{include file=""}
			matcher = Pattern.compile("\\{include file=\"([^\"]+)\"\\s*/?}").matcher(html.toString());
			html = new StringBuffer();
			while (matcher.find()) {
				this.currentMark = matcher.group(0);
				String path;
				String matcherDir = templateDir;
				if (matcher.group(1).startsWith("./")) {
					// ./file.html
					path = matcherDir + "/" + matcher.group(1).replace("./", "");
				} else if (matcher.group(1).startsWith("../")) {
					// ../file.html
					Matcher m = Pattern.compile("(\\.\\./)").matcher(matcher.group(1));
					while (m.find()) matcherDir = new File(matcherDir).getParent();
					path = matcherDir + "/" + matcher.group(1).replace("../", "");
				} else if (matcher.group(1).startsWith("~") || matcher.group(1).startsWith("/")) {
					// ~file.html /file.html
					path = rootPath + "/" + matcher.group(1).substring(1);
				} else {
					// file.html
					path = matcherDir + "/" + matcher.group(1);
				}
				String ret = displayInclude(templateDir, path);
				matcher.appendReplacement(html, ret.replaceAll("([$\\\\])", "\\\\$1"));
			}
			matcher.appendTail(html);
			
			for (String key : this.replace.keySet()) html = new StringBuffer(html.toString().replace(key, this.replace.get(key)));
			
			matcher = Pattern.compile("\\{(origin|literal)\\s*}([\\s\\S]+?)\\{/\\1\\s*}").matcher(html.toString());
			html = new StringBuffer();
			while (matcher.find()) {
				String key = "___ORIGIN_"+this.originMap.keySet().size()+"___";
				this.originMap.put(key, matcher.group(2));
				matcher.appendReplacement(html, key.replaceAll("([$\\\\])", "\\\\$1"));
			}
			matcher.appendTail(html);
			
			templateHtml = displayTag(html);
		} catch (Exception e) {
			System.out.println(this.currentMark+"\n");
			e.printStackTrace();
		}
		return templateHtml;
	}
	public String displayInclude(String templateRoot, String templatePath) {
		return displayInclude(templateRoot, templatePath, 0);
	}
	public String displayInclude(String templateRoot, String templatePath, int level) {
		File file = new File(this.setSuffix(templatePath));
		if (!file.exists()) throw new IllegalArgumentException("FILE IS NOT EXIST:\n" + templatePath);
		StringBuffer html = new StringBuffer();
		try {
			FileInputStream ips = new FileInputStream(file);
			StringBuilder sbf = new StringBuilder();
			int len;
			byte[] buffer = new byte[1024 * 2];
			while ((len = ips.read(buffer)) != -1) {
				sbf.append(new String(buffer, 0, len));
			}
			ips.close();
			Matcher matcher = Pattern.compile("\\{include file=\"([^\"]+)\"\\s*/?}").matcher(sbf.toString());
			while (matcher.find()) {
				this.currentMark = matcher.group(0);
				String path;
				String matcherDir = templateRoot;
				if (matcher.group(1).startsWith("./")) {
					// ./file.html
					path = matcherDir + "/" + matcher.group(1).replace("./", "");
				} else if (matcher.group(1).startsWith("../")) {
					// ../file.html
					Matcher m = Pattern.compile("(\\.\\./)").matcher(matcher.group(1));
					while (m.find()) matcherDir = new File(matcherDir).getParent();
					path = matcherDir + "/" + matcher.group(1).replace("../", "");
				} else if (matcher.group(1).startsWith("~") || matcher.group(1).startsWith("/")) {
					// ~file.html /file.html
					path = rootPath + "/" + matcher.group(1).substring(1);
				} else {
					// file.html
					path = matcherDir + "/" + matcher.group(1);
				}
				String ret = displayInclude(templateRoot, path, level + 1);
				matcher.appendReplacement(html, ret.replaceAll("([$\\\\])", "\\\\$1"));
			}
			matcher.appendTail(html);
		} catch (Exception e) {
			System.out.println(this.currentMark+"\n");
			e.printStackTrace();
		}
		return html.toString();
	}
	public String displayTag(StringBuffer html) {
		return displayTag(html, 0);
	}
	public String displayTag(StringBuffer html, int level) {
		Map<String, String[]> foreachMap = new HashMap<>();
		Matcher matcher = Pattern.compile("\\{foreach:(\\S+)\\s+as\\s+\\$(\\w+)\\s*}([\\s\\S]+)\\{/foreach:\\1\\s*}").matcher(html.toString());
		html = new StringBuffer();
		while (matcher.find()) {
			String key = "{==foreachIndex:" + foreachMap.keySet().size() + "==}";
			foreachMap.put(key, new String[]{matcher.group(1).replace("->", "."), matcher.group(2), matcher.group(3)});
			matcher.appendReplacement(html, key.replaceAll("([$\\\\])", "\\\\$1"));
		}
		matcher.appendTail(html);
		
		Map<String, String[]> forMap = new HashMap<>();
		matcher = Pattern.compile("\\{for:(\\S+)\\s+(\\S+)\\s+to\\s+([^\\s|}]+?)(\\s+step\\s*=\\s*(\\d+))?\\s*}([\\s\\S]+?)\\{/for:\\1\\s*}").matcher(html.toString());
		html = new StringBuffer();
		while (matcher.find()) {
			String key = "{==forIndex:" + forMap.keySet().size() + "==}";
			forMap.put(key, new String[]{matcher.group(1).replace("->", "."), matcher.group(2), matcher.group(3), matcher.group(5), matcher.group(6)});
			matcher.appendReplacement(html, key.replaceAll("([$\\\\])", "\\\\$1"));
		}
		matcher.appendTail(html);

		//{foreach:$rs as $g}{$g.name}{$rs.index}{$rs.iteration}{$rs.total}{if $rs.first}{if $rs.last}{/foreach:$rs}
		html = parseForeach(foreachMap, html);
		
		//{for:k 0 to 5 [step=2]}{$k}{$k.total}{if $k.first}{if $k.last}{/for:k}
		html = parseFor(forMap, html);
		
		//{switch count($rs)}{case 0}xx{case 1}yy{default}zz{/switch}
		html = parseSwitch(html);
		
		//{if aa==bb [&& xx!=yy]}content{/if}
		html = parseIf(html);

		//{$title} {trim($g.name)}
		html = new StringBuffer(parseVariable(html.toString()));

		String content = html.toString();
		if (content.contains("{switch") || content.contains("{if") || content.contains("{for:") || content.contains("{foreach:") ||
				Pattern.compile("\\{==").matcher(content).find()) {
			html = new StringBuffer(displayTag(html, level + 1));
		}
		
		if (level == 0) {
			//{origin}{/origin} {literal}{/literal}
			matcher = Pattern.compile("___ORIGIN_\\d+___").matcher(html.toString());
			html = new StringBuffer();
			while (matcher.find()) {
				matcher.appendReplacement(html, this.originMap.containsKey(matcher.group(0)) ? this.originMap.get(matcher.group(0)).replaceAll("([$\\\\])", "\\\\$1") : "");
			}
			matcher.appendTail(html);
			
			//正常代码, css:.aa{float:left;} js:{aa:'bb'}
			matcher = Pattern.compile("___NORMAL_\\d+___").matcher(html.toString());
			html = new StringBuffer();
			while (matcher.find()) {
				matcher.appendReplacement(html, this.normalMap.containsKey(matcher.group(0)) ? this.normalMap.get(matcher.group(0)).replaceAll("([$\\\\])", "\\\\$1") : "");
			}
			matcher.appendTail(html);
		}
		
		return html.toString();
	}

	private StringBuffer parseForeach(Map<String, String[]> map, StringBuffer html) {
		return parseForeach(map, html, this.data, null);
	}
	private StringBuffer parseForeach(Map<String, String[]> map, StringBuffer html, Object obj, String item) {
		Matcher matcher = Pattern.compile("\\{==foreachIndex:([^=]+)==}").matcher(html.toString());
		html = new StringBuffer();
		while (matcher.find()) {
			this.currentMark = matcher.group(0);
			try {
				if (map.get(matcher.group(0)) == null) {
					matcher.appendReplacement(html, "");
					continue;
				}
				String[] param = map.get(matcher.group(0));
				Object items;
				if (item != null && !Pattern.compile("\\$"+item+"\\.").matcher(param[0]).find()) {
					items = parse(param[0], this.data, null);
				} else {
					items = parse(param[0], obj, item);
				}
				if (items == null || (!(items instanceof List) && !items.getClass().isArray())) {
					matcher.appendReplacement(html, "");
					continue;
				}
				StringBuilder ret = new StringBuilder();
				if (items instanceof List) {
					try {
						int len = (int) items.getClass().getMethod("size").invoke(items);
						for (int i = 0; i < len; i++) {
							Object o = items.getClass().getMethod("get", int.class).invoke(items, i);
							String content = parseForeachContent(param[2], param[0], i, len);
							Matcher ifMatcher = Pattern.compile("\\{(\\$"+param[1]+"(\\.|->)((\\w|\\.|->)+))}").matcher(content);
							StringBuffer sbi = new StringBuffer();
							while (ifMatcher.find()) {
								Object res = parse(ifMatcher.group(1), o, param[1]);
								if ((res instanceof JSONObject) || (res instanceof JSONArray)) {
									ifMatcher.appendReplacement(sbi, ifMatcher.group(0).replaceAll("([$\\\\])", "\\\\$1"));
								} else {
									ifMatcher.appendReplacement(sbi, Common.isNumeric(res) ? String.valueOf(res) : String.valueOf(res).replaceAll("([$\\\\])", "\\\\$1"));
								}
							}
							ifMatcher.appendTail(sbi);
							ifMatcher = Pattern.compile("(\\$"+param[1]+"(\\.|->)((\\w|\\.|->)+))").matcher(sbi.toString());
							sbi = new StringBuffer();
							while (ifMatcher.find()) {
								Object res = parse(ifMatcher.group(1), o, param[1]);
								if ((res instanceof JSONObject) || (res instanceof JSONArray)) {
									ifMatcher.appendReplacement(sbi, ifMatcher.group(0).replaceAll("([$\\\\])", "\\\\$1"));
								} else {
									ifMatcher.appendReplacement(sbi, Common.isNumeric(res) ? String.valueOf(res) : ("'"+res+"'").replaceAll("([$\\\\])", "\\\\$1"));
								}
							}
							ifMatcher.appendTail(sbi);
							content = sbi.toString();
							if (content.contains("{foreach:")) {
								Matcher m = Pattern.compile("\\{foreach:(\\S+)\\s+as\\s+\\$(\\w+)\\s*}([\\s\\S]+?)\\{/foreach:\\1\\s*}").matcher(content);
								StringBuffer sb = new StringBuffer();
								while (m.find()) {
									String key = "{==foreachIndex:" + map.keySet().size() + "==}";
									map.put(key, new String[]{m.group(1).replace("->", "."), m.group(2), m.group(3)});
									m.appendReplacement(sb, key.replaceAll("([$\\\\])", "\\\\$1"));
								}
								m.appendTail(sb);
								content = parseForeach(map, sb, o, param[1]).toString();
							}
							ret.append(content);
						}
						if (item == null) {
							ret = new StringBuilder(parseIf(new StringBuffer(ret.toString()), obj, param[1]).toString());
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					if (items instanceof String[]) {
						int len = ((String[]) items).length;
						for (int i = 0; i < len; i++) {
							ret.append(parseForeachArray(map, ((String[]) items)[i], param, i, len));
						}
					} else if (items instanceof Integer[]) {
						int len = ((Integer[]) items).length;
						for (int i = 0; i < len; i++) {
							ret.append(parseForeachArray(map, ((Integer[]) items)[i], param, i, len));
						}
					} else if (items instanceof Long[]) {
						int len = ((Long[]) items).length;
						for (int i = 0; i < len; i++) {
							ret.append(parseForeachArray(map, ((Long[]) items)[i], param, i, len));
						}
					} else if (items instanceof Float[]) {
						int len = ((Float[]) items).length;
						for (int i = 0; i < len; i++) {
							ret.append(parseForeachArray(map, ((Float[]) items)[i], param, i, len));
						}
					} else if (items instanceof Double[]) {
						int len = ((Double[]) items).length;
						for (int i = 0; i < len; i++) {
							ret.append(parseForeachArray(map, ((Double[]) items)[i], param, i, len));
						}
					} else if (items instanceof BigDecimal[]) {
						int len = ((BigDecimal[]) items).length;
						for (int i = 0; i < len; i++) {
							ret.append(parseForeachArray(map, ((BigDecimal[]) items)[i], param, i, len));
						}
					} else {
						throw new IllegalArgumentException(String.valueOf(items.getClass()));
					}
					if (item == null) {
						ret = new StringBuilder(parseIf(new StringBuffer(ret.toString()), obj, param[1]).toString());
					}
				}
				matcher.appendReplacement(html, ret.toString().replaceAll("([$\\\\])", "\\\\$1"));
			} catch (Exception e) {
				StackTraceElement ste = new Throwable().getStackTrace()[0];
				System.out.println(ste.getFileName() + ": Line " + ste.getLineNumber());
				System.out.println(matcher.group(0)+"\n");
				e.printStackTrace();
			}
		}
		matcher.appendTail(html);
		return html;
	}
	private String parseForeachContent(String str, String search, int i, int len) {
		search = search.replaceAll("(\\$)", "\\\\$1");
		str = str.replaceAll("\\{" + search + "\\.index\\s*}", String.valueOf(i));
		str = str.replaceAll(search + "\\.index\\b", String.valueOf(i));
		str = str.replaceAll("\\{" + search + "\\.iteration\\s*}", String.valueOf((i + 1)));
		str = str.replaceAll(search + "\\.iteration\\b", String.valueOf((i + 1)));
		str = str.replaceAll(search + "\\.first\\b", (i == 0 ? "true" : "false"));
		str = str.replaceAll(search + "\\.last\\b", (i == len - 1 ? "true" : "false"));
		return str.replaceAll(search + "\\.total\\b", String.valueOf(len));
	}
	private String parseForeachArray(Map<String, String[]> map, Object o, String[] param, int i, int len) {
		String content = parseForeachContent(param[2], param[0], i, len);
		Matcher ifMatcher = Pattern.compile("\\{(\\$"+param[1]+")}").matcher(content);
		StringBuffer sbi = new StringBuffer();
		while (ifMatcher.find()) {
			Object res = parse(ifMatcher.group(1), o, param[1]);
			if ((res instanceof JSONObject) || (res instanceof JSONArray)) {
				ifMatcher.appendReplacement(sbi, ifMatcher.group(0).replaceAll("([$\\\\])", "\\\\$1"));
			} else {
				ifMatcher.appendReplacement(sbi, Common.isNumeric(res) ? String.valueOf(res) : String.valueOf(res).replaceAll("([$\\\\])", "\\\\$1"));
			}
		}
		ifMatcher.appendTail(sbi);
		ifMatcher = Pattern.compile("(\\$"+param[1]+")").matcher(sbi.toString());
		sbi = new StringBuffer();
		while (ifMatcher.find()) {
			Object res = parse(ifMatcher.group(1), o, param[1]);
			if ((res instanceof JSONObject) || (res instanceof JSONArray)) {
				ifMatcher.appendReplacement(sbi, ifMatcher.group(0).replaceAll("([$\\\\])", "\\\\$1"));
			} else {
				ifMatcher.appendReplacement(sbi, Common.isNumeric(res) ? String.valueOf(res) : ("'"+res+"'").replaceAll("([$\\\\])", "\\\\$1"));
			}
		}
		ifMatcher.appendTail(sbi);
		content = sbi.toString();
		if (content.contains("{foreach")) {
			Matcher m = Pattern.compile("\\{foreach:(\\S+)\\s+as\\s+\\$(\\w+)\\s*}([\\s\\S]+?)\\{/foreach:\\1\\s*}").matcher(content);
			StringBuffer sb = new StringBuffer();
			while (m.find()) {
				String key = "{==foreachIndex:" + map.keySet().size() + "==}";
				map.put(key, new String[]{m.group(1).replace("->", "."), m.group(2), m.group(3)});
				m.appendReplacement(sb, key.replaceAll("([$\\\\])", "\\\\$1"));
			}
			m.appendTail(sb);
			content = parseForeach(map, sb, o, param[1]).toString();
		}
		return content;
	}
	
	private StringBuffer parseFor(Map<String, String[]> map, StringBuffer html) {
		return parseFor(map, html, this.data, null);
	}
	private StringBuffer parseFor(Map<String, String[]> map, StringBuffer html, Object obj, String item) {
		Matcher matcher = Pattern.compile("\\{==forIndex:([^=]+)==}").matcher(html.toString());
		html = new StringBuffer();
		while (matcher.find()) {
			this.currentMark = matcher.group(0);
			try {
				if (map.get(matcher.group(0)) == null) {
					matcher.appendReplacement(html, "");
					continue;
				}
				String[] param = map.get(matcher.group(0));
				Object start = parseCompute(param[1], obj, item);
				Object end = parseCompute(param[2], obj, item);
				Object step = param[3] == null ? 1 : parseCompute(param[3], obj, item);
				if ((start instanceof Float) || (start instanceof Double)) start = Double.valueOf(String.valueOf(start)).intValue();
				if ((end instanceof Float) || (end instanceof Double)) end = Double.valueOf(String.valueOf(end)).intValue();
				if ((step instanceof Float) || (step instanceof Double)) step = Double.valueOf(String.valueOf(step)).intValue();
				if (Integer.parseInt(String.valueOf(end)) < Integer.parseInt(String.valueOf(start))) {
					int tmp = Integer.parseInt(String.valueOf(end));
					end = start;
					start = tmp;
				}
				StringBuilder ret = new StringBuilder();
				int len = Integer.parseInt(String.valueOf(end));
				int total = len - Integer.parseInt(String.valueOf(start));
				for (int i = Integer.parseInt(String.valueOf(start)); i < len; i += Integer.parseInt(String.valueOf(step))) {
					String content = param[4].replaceAll("\\{\\$" + param[0] + "\\s*}", String.valueOf(i));
					content = content.replaceAll("\\$" + param[0] + "\\b", String.valueOf(i));
					content = content.replaceAll("\\$" + param[0] + "\\.first\\b", (i == 0 ? "true" : "false"));
					content = content.replaceAll("\\$" + param[0] + "\\.last\\b", (i == len - 1 ? "true" : "false"));
					content = content.replaceAll("\\$" + param[0] + "\\.total\\b", String.valueOf(total));
					if (content.contains("{for:")) {
						Matcher m = Pattern.compile("\\{for:(\\S+)\\s+(\\S+)\\s+to\\s+([^\\s|}]+?)(\\s+step\\s+(\\d+))?\\s*}([\\s\\S]+?)\\{/for:\\1\\s*}").matcher(content);
						StringBuffer sb = new StringBuffer();
						while (m.find()) {
							String key = "{==forIndex:" + map.keySet().size() + "==}";
							map.put(key, new String[]{m.group(1).replace("->", "."), m.group(2), m.group(3), m.group(5), m.group(6)});
							m.appendReplacement(sb, key.replaceAll("([$\\\\])", "\\\\$1"));
						}
						m.appendTail(sb);
						content = parseFor(map, sb, obj, item).toString();
					}
					ret.append(content);
				}
				ret = new StringBuilder(parseIf(new StringBuffer(ret.toString())).toString());
				matcher.appendReplacement(html, ret.toString().replaceAll("([$\\\\])", "\\\\$1"));
			} catch (Exception e) {
				StackTraceElement ste = new Throwable().getStackTrace()[0];
				System.out.println(ste.getFileName() + ": Line " + ste.getLineNumber());
				System.out.println(matcher.group(0)+"\n");
				e.printStackTrace();
			}
		}
		matcher.appendTail(html);
		return html;
	}
	
	private StringBuffer parseSwitch(StringBuffer html) {
		return parseSwitch(html, false);
	}
	private StringBuffer parseSwitch(StringBuffer html, boolean nonMark) {
		Matcher matcher;
		if (nonMark) {
			matcher = Pattern.compile("\\{switch(\\s+)([^}]+)}([\\s\\S]*?)\\{/switch\\s*}").matcher(html.toString());
		} else {
			matcher = Pattern.compile("\\{switch(:\\S+)\\s+([^}]+)}([\\s\\S]*?)\\{/switch\\1\\s*}").matcher(html.toString());
		}
		html = new StringBuffer();
		while (matcher.find()) {
			this.currentMark = matcher.group(0);
			try {
				String mark = matcher.group(1) == null ? "" : matcher.group(1).trim();
				String content = "";
				Object expression = parse(matcher.group(2).replace("->", "."));
				boolean isCase = false;
				Matcher caseMatcher = Pattern.compile("\\{case"+mark+"\\s+([^}]+)}([\\s\\S]+?)(?=\\{case"+mark+"\\s|\\{default"+mark+"\\s*}|$)").matcher(matcher.group(3));
				while (caseMatcher.find()) {
					Object label = parse(caseMatcher.group(1).replace("->", "."));
					boolean res = parseSwitchJudge(expression, label);
					if (res) {
						content = caseMatcher.group(2);
						isCase = true;
						break;
					}
				}
				if (!isCase) {
					Matcher defaultMatcher = Pattern.compile("\\{default"+mark+"\\s*}([\\s\\S]+)$").matcher(matcher.group(3));
					if (defaultMatcher.find()) {
						content = defaultMatcher.group(1);
					}
				}
				matcher.appendReplacement(html, content.replaceAll("([$\\\\])", "\\\\$1"));
			} catch (Exception e) {
				StackTraceElement ste = new Throwable().getStackTrace()[0];
				System.out.println(ste.getFileName() + ": Line " + ste.getLineNumber());
				System.out.println(matcher.group(0)+"\n");
				e.printStackTrace();
			}
		}
		matcher.appendTail(html);
		if (!nonMark) html = parseSwitch(html, true);
		return html;
	}
	private boolean parseSwitchJudge(Object leftRet, Object rightRet) {
		boolean res;
		boolean isNumber = (leftRet instanceof Integer) || (leftRet instanceof Long) || (leftRet instanceof Float) || (leftRet instanceof Double) || (leftRet instanceof BigDecimal) ||
				(rightRet instanceof Integer) || (rightRet instanceof Long) || (rightRet instanceof Float) || (rightRet instanceof Double) || (rightRet instanceof BigDecimal);
		if (leftRet == null && rightRet == null) return true;
		if (leftRet == null || rightRet == null) return false;
		if (isNumber) {
			res = Float.parseFloat(String.valueOf(leftRet)) == Float.parseFloat(String.valueOf(rightRet));
		} else {
			res = leftRet.equals(rightRet);
		}
		return res;
	}
	
	private StringBuffer parseIf(StringBuffer html) {
		return parseIf(html, this.data, null);
	}
	private StringBuffer parseIf(StringBuffer html, Object obj, String item) {
		return parseIf(html, obj, item, false);
	}
	private StringBuffer parseIf(StringBuffer html, Object obj, String item, boolean nonMark) {
		Matcher matcher;
		if (nonMark) {
			matcher = Pattern.compile("\\{if(\\s+)([^}]+)}([\\s\\S]*?)\\{/if\\s*}").matcher(html.toString());
		} else {
			matcher = Pattern.compile("\\{if(:\\S+)\\s+([^}]+)}([\\s\\S]*?)\\{/if\\1\\s*}").matcher(html.toString());
		}
		html = new StringBuffer();
		while (matcher.find()) {
			this.currentMark = matcher.group(0);
			try {
				String mark = matcher.group(1) == null ? "" : matcher.group(1).trim();
				String content = parseIfContent(matcher.group(3), mark);
				Matcher logicMatcher = Pattern.compile("^(.+?)((&&|\\|\\||and|or|&amp;&amp;)\\s+.+)?$").matcher(matcher.group(2));
				if (logicMatcher.find()) {
					boolean judge = parseIfJudge(logicMatcher.group(1), obj, item);
					if (logicMatcher.group(2) != null) judge = parseIfLogic(judge, logicMatcher.group(2), obj, item);
					if (!judge) content = parseIfElse(matcher.group(3), mark, obj, item);
				}
				if (content.length() > 0) {
					StringBuffer sb = parseIf(new StringBuffer(content));
					content = sb.toString();
				}
				matcher.appendReplacement(html, content.replaceAll("([$\\\\])", "\\\\$1"));
			} catch (Exception e) {
				StackTraceElement ste = new Throwable().getStackTrace()[0];
				System.out.println(ste.getFileName() + ": Line " + ste.getLineNumber());
				System.out.println(matcher.group(0)+"\n");
				e.printStackTrace();
			}
		}
		matcher.appendTail(html);
		if (!nonMark) html = parseIf(html, obj, item, true);
		return html;
	}
	/*private List<String> parseIfSub(String str) { //elseif、else暂时无法实现
		List<String> list = new ArrayList<>();
		StringBuilder sb = new StringBuilder();
		int k = 0;
		char[] arr = str.toCharArray();
		for (int i = 0; i < arr.length; i++) {
			char ch = arr[i];
			if (ch == '{' && i < arr.length - 3) {
				if (arr[i+1] == 'i' && arr[i+2] == 'f') k++;
				if (arr[i+1] == '/' && arr[i+2] == 'i' && arr[i+3] == 'f') k--;
			}
			if (k == 0) {
				list.add(sb.toString());
				sb.delete(0, sb.length());
			} else {
				sb.append(ch);
			}
		}
		if (sb.length() > 0) list.add(sb.toString());
		list = list.stream().filter(string -> !string.isEmpty()).collect(Collectors.toList());
		List<String> l = new ArrayList<>(list);
		for (String string : l) {
			Matcher matcher = Pattern.compile("^\\{if[^}]+}([\\s\\S]+)$").matcher(string);
			if (matcher.find()) {
				if (matcher.group(1).contains("{if")) {
					List<String> items = parseIfSub(matcher.group(1));
					list.addAll(items);
				}
			}
		}
		return list;
	}*/
	private boolean parseIfJudge(String str, Object obj, String item) {
		str = str.trim().replace("->", ".");
		boolean res = false;
		Matcher matcher = Pattern.compile("^(.+?)(==|!=|<>|&lt;&gt;|<=|&lt;=|>=|&gt;=|<|&lt;|>|&gt;|=)(.+)$").matcher(str);
		if (matcher.find()) {
			Object leftRet = parseCompute(matcher.group(1), obj, item);
			Object rightRet = parseCompute(matcher.group(3), obj, item);
			try {
				boolean isNumber = Common.isNumeric(String.valueOf(leftRet)) || Common.isNumeric(String.valueOf(rightRet));
				switch (matcher.group(2)) {
					case "==":case "=": //byte,short,char,int,long,float,double,boolean
						if (leftRet == null && rightRet == null) return true;
						if (leftRet == null || rightRet == null) return false;
						if (isNumber) {
							res = Float.parseFloat(String.valueOf(leftRet)) == Float.parseFloat(String.valueOf(rightRet));
						} else {
							res = leftRet.equals(rightRet);
						}
						break;
					case "!=":case "<>":case "&lt;&gt;":
						if (leftRet == null && rightRet == null) return false;
						if (leftRet == null || rightRet == null) return true;
						if (isNumber) {
							res = Float.parseFloat(String.valueOf(leftRet)) != Float.parseFloat(String.valueOf(rightRet));
						} else {
							res = !leftRet.equals(rightRet);
						}
						break;
					case "<=":case "&lt;=":
						if (leftRet == null || rightRet == null) return false;
						if ((leftRet instanceof Boolean) || (rightRet instanceof Boolean)) return false;
						if ((leftRet instanceof String) || (rightRet instanceof String)) return false;
						res = Float.parseFloat(String.valueOf(leftRet)) <= Float.parseFloat(String.valueOf(rightRet));
						break;
					case "<":case "&lt;":
						if (leftRet == null || rightRet == null) return false;
						if ((leftRet instanceof Boolean) || (rightRet instanceof Boolean)) return false;
						if ((leftRet instanceof String) || (rightRet instanceof String)) return false;
						res = Float.parseFloat(String.valueOf(leftRet)) < Float.parseFloat(String.valueOf(rightRet));
						break;
					case ">=":case "&gt;=":
						if (leftRet == null || rightRet == null) return false;
						if ((leftRet instanceof Boolean) || (rightRet instanceof Boolean)) return false;
						if ((leftRet instanceof String) || (rightRet instanceof String)) return false;
						res = Float.parseFloat(String.valueOf(leftRet)) >= Float.parseFloat(String.valueOf(rightRet));
						break;
					case ">":case "&gt;":
						if (leftRet == null || rightRet == null) return false;
						if ((leftRet instanceof Boolean) || (rightRet instanceof Boolean)) return false;
						if ((leftRet instanceof String) || (rightRet instanceof String)) return false;
						res = Float.parseFloat(String.valueOf(leftRet)) > Float.parseFloat(String.valueOf(rightRet));
						break;
				}
			} catch (Exception e) {
				StackTraceElement ste = new Throwable().getStackTrace()[0];
				System.out.println(ste.getFileName() + ": Line " + ste.getLineNumber());
				System.out.println(matcher.group(0));
				System.out.println(this.currentMark+"\n");
				e.printStackTrace();
			}
		} else {
			boolean isReverse = str.startsWith("!");
			if (isReverse) str = str.substring(1);
			Object ret = parse(str, obj, item);
			if (Common.isNumeric(ret)) {
				res = Float.parseFloat(String.valueOf(ret)) > 0;
			} else if (ret instanceof Boolean) {
				res = Boolean.TRUE.equals(ret);
			} else if (ret instanceof String) {
				res = ((String) ret).length() > 0;
			} else {
				res = ret != null;
			}
			if (isReverse) res = !res;
		}
		return res;
	}
	private boolean parseIfLogic(boolean logic, String str, Object obj, String item) {
		str = str.trim().replace("->", ".");
		Matcher matcher = Pattern.compile("^(&&|\\|\\||and|or|&amp;&amp;)(\\s+.+?)((&&|\\|\\||and|or|&amp;&amp;)\\s+.+)?$").matcher(str);
		if (matcher.find()) {
			try {
				switch (matcher.group(1)) {
					case "&&":case "and":case "&amp;&amp;": {
						if (!logic) return false;
						boolean judge = parseIfJudge(matcher.group(2), obj, item);
						if (judge) {
							if (matcher.group(3) != null) judge = parseIfLogic(true, matcher.group(3), obj, item);
							return judge;
						}
						break;
					}
					case "||":case "or": {
						if (logic) return true;
						boolean judge = parseIfJudge(matcher.group(2), obj, item);
						if (judge) return true;
						if (matcher.group(3) != null) judge = parseIfLogic(false, matcher.group(3), obj, item);
						if (judge) return true;
						break;
					}
				}
			} catch (Exception e) {
				StackTraceElement ste = new Throwable().getStackTrace()[0];
				System.out.println(ste.getFileName() + ": Line " + ste.getLineNumber());
				System.out.println(matcher.group(0));
				System.out.println(this.currentMark+"\n");
				e.printStackTrace();
			}
		}
		return false;
	}
	private String parseIfElse(String str, String mark, Object obj, String item) {
		boolean nonElseIf = true;
		if (mark == null) mark = "";
		Matcher matcher = Pattern.compile("^[\\s\\S]*?\\{else\\s*if"+mark+"\\s+([^}]+)}([\\s\\S]*)$").matcher(str);
		if (!matcher.find()) {
			nonElseIf = false;
			matcher = Pattern.compile("^[\\s\\S]*?\\{else"+mark+"(\\s*)}([\\s\\S]*)$").matcher(str);
		}
		if (!matcher.find()) return "";
		String content = parseIfContent(matcher.group(2), mark);
		if (!nonElseIf) return content;
		Matcher logicMatcher = Pattern.compile("^(.+?)((&&|\\|\\||and|or|&amp;&amp;)\\s+.+)?$").matcher(matcher.group(1).trim());
		if (logicMatcher.find()) {
			boolean logic = parseIfJudge(logicMatcher.group(1), obj, item);
			if (logicMatcher.group(2) != null) logic = parseIfLogic(logic, logicMatcher.group(2), obj, item);
			if (!logic) content = parseIfElse(matcher.group(2), mark, obj, item);
		}
		return content;
	}
	private String parseIfContent(String str, String mark) {
		if (mark == null) mark = "";
		Matcher matcher = Pattern.compile("^([\\s\\S]*?)(\\{else"+mark+"[\\s\\S]+)?$").matcher(str);
		if (matcher.find()) return matcher.group(1);
		return "";
	}

	private String parseVariable(String str) {
		return parseVariable(str, this.data);
	}
	private String parseVariable(String str, Object obj) {
		Matcher matcher = Pattern.compile("\\{(\\S[^}]+)}").matcher(str);
		StringBuffer html = new StringBuffer();
		while (matcher.find()) {
			String mark = matcher.group(1).replace("->", ".");
			if (Pattern.compile("switch(:\\S+)?\\s+").matcher(mark).find() || Pattern.compile("/?switch\\s*").matcher(mark).find() ||
					Pattern.compile("if(:\\S+)?\\s+").matcher(mark).find() || Pattern.compile("/?if\\s*").matcher(mark).find() ||
					Pattern.compile("/?foreach:").matcher(mark).find() || Pattern.compile("/?for:").matcher(mark).find()) {
				matcher.appendReplacement(html, matcher.group(0).replaceAll("([$\\\\])", "\\\\$1"));
				continue;
			}
			try {
				Object ret = parse(mark, obj, null);
				if (ret == null) ret = "";
				matcher.appendReplacement(html, String.valueOf(ret).replaceAll("([$\\\\])", "\\\\$1").replace("{", "{ "));
			} catch (Exception e) {
				StackTraceElement ste = new Throwable().getStackTrace()[0];
				System.out.println(ste.getFileName() + ": Line " + ste.getLineNumber());
				System.out.println(matcher.group(0));
				System.out.println(this.currentMark+"\n");
				e.printStackTrace();
			}
		}
		matcher.appendTail(html);
		return html.toString();
	}

	private Object parseCompute(String str, Object obj, String item) {
		if (str.startsWith("'") || str.startsWith("\"")) return parse(str, obj, item);
		str = str.trim().replace("->", ".");
		Object res = null;
		Matcher matcher = Pattern.compile("^(.+?)(([+\\-*/])(.+))?$").matcher(str);
		if (matcher.find()) {
			try {
				Object leftRet = parse(matcher.group(1), obj, item);
				if (matcher.group(2) == null || matcher.group(4) == null || matcher.group(4).length() == 0) return leftRet;
				Object rightRet = parse(matcher.group(4), obj, item);
				if (leftRet == null || rightRet == null) return null;
				boolean isNumber = (((leftRet instanceof Integer) || (leftRet instanceof Long) || (leftRet instanceof Float) || (leftRet instanceof Double) || (leftRet instanceof BigDecimal)) && ((rightRet instanceof Integer) || (rightRet instanceof Long) || (rightRet instanceof Float) || (rightRet instanceof Double) || (rightRet instanceof BigDecimal)));
				BigDecimal left;
				BigDecimal right;
				switch (matcher.group(3)) {
					case "+":
						if (isNumber) {
							left = new BigDecimal(String.valueOf(leftRet));
							right = new BigDecimal(String.valueOf(rightRet));
							res = left.add(right).doubleValue();
						} else {
							res = String.valueOf(leftRet) + rightRet;
						}
						break;
					case "-":
						if (!isNumber) return null;
						left = new BigDecimal(String.valueOf(leftRet));
						right = new BigDecimal(String.valueOf(rightRet));
						res = left.subtract(right).doubleValue();
						break;
					case "*":
						if (!isNumber) return null;
						left = new BigDecimal(String.valueOf(leftRet));
						right = new BigDecimal(String.valueOf(rightRet));
						res = left.multiply(right).doubleValue();
						break;
					case "/":
						if (!isNumber) return null;
						left = new BigDecimal(String.valueOf(leftRet));
						right = new BigDecimal(String.valueOf(rightRet));
						res = left.divide(right, 2, RoundingMode.HALF_UP).doubleValue();
						break;
				}
			} catch (Exception e) {
				StackTraceElement ste = new Throwable().getStackTrace()[0];
				System.out.println(ste.getFileName() + ": Line " + ste.getLineNumber());
				System.out.println(matcher.group(0));
				System.out.println(this.currentMark+"\n");
				e.printStackTrace();
			}
		}
		return res;
	}
	
	private Object parse(String str) {
		return parse(str, this.data, null);
	}
	@SuppressWarnings("unchecked")
	private Object parse(String str, Object obj, String item) {
		if (str == null) return null;
		Object newObj;
		try {
			newObj = obj == null ? null : ((obj instanceof Map) ? JSONObject.parseObject(JSON.toJSONString(obj)) : (((obj instanceof List) || obj.getClass().isArray()) ? JSONArray.parseArray(JSON.toJSONString(obj)) : obj));
		} catch (Exception e) {
			System.out.println(str);
			System.out.println(obj);
			e.printStackTrace();
			return null;
		}
		str = str.trim().replace("->", ".");
		Object ret = "{" + str + "}"; //正常代码, css:.aa{float:left;} js:{aa:'bb'}
		if (Pattern.compile("^\\$\\w+").matcher(str).find() && !str.matches("^(.+?)([+\\-*/])(.+)$")) { //$aa
			if (item != null) {
				Matcher itemMatcher = Pattern.compile("\\$"+item+"\\.(\\w+)").matcher(str);
				if (!itemMatcher.find()) return str;
			}
			String field = str.substring(1);
			if (newObj instanceof Map) {
				Matcher matcher = Pattern.compile("^([^.]+)(.+)?$").matcher(field);
				if (!matcher.find()) return null;
				Object o = newObj;
				String key = matcher.group(1);
				String _item = key;
				if (item == null) {
					try {
						if (!((Map<String, Object>) o).containsKey(key)) return null;
					} catch (ClassCastException e) {
						StackTraceElement ste = new Throwable().getStackTrace()[0];
						System.out.println(ste.getFileName() + ": Line " + ste.getLineNumber());
						System.out.println(field);
						System.out.println(Common.json_encode(o));
						e.printStackTrace();
						return null;
					}
					o = ((Map<String, Object>) o).get(key);
				}
				if (matcher.group(2) != null) {
					Matcher m = Pattern.compile("(\\.([^.]+))").matcher(matcher.group(2));
					while (m.find()) {
						key = m.group(2);
						if (item == null || _item.equals(item)) {
							if (!(o instanceof Map)) return null;
							try {
								if (!((Map<String, Object>) o).containsKey(key)) return null;
								o = ((Map<String, Object>) o).get(key);
							} catch (ClassCastException e) {
								StackTraceElement ste = new Throwable().getStackTrace()[0];
								System.out.println(ste.getFileName() + ": Line " + ste.getLineNumber());
								System.out.println(field);
								System.out.println(Common.json_encode(o));
								e.printStackTrace();
								return null;
							}
						}
					}
				}
				ret = o;
			} else {
				ret = newObj;
			}
			return ret;
		} else if (str.startsWith("'") || str.startsWith("\"")) { //'aa' "bb"
			Matcher matcher = Pattern.compile("^(['\"])(.*)\\1$").matcher(str);
			if (matcher.find()) ret = matcher.group(2);
			return ret;
		} else if (str.startsWith("count(")) { //count()
			Matcher matcher = Pattern.compile("^count\\((.+)\\)$").matcher(str);
			if (!matcher.find()) return 0;
			ret = parseCompute(matcher.group(1), newObj, item);
			if (ret == null) return 0;
			if (ret instanceof List) {
				try {
					ret = ret.getClass().getMethod("size").invoke(ret);
				} catch (Exception e) {
					e.printStackTrace();
					ret = 0;
				}
			} else if (ret.getClass().isArray()) {
				if (ret instanceof String[]) {
					return Arrays.asList((String[]) ret).size();
				} else if (ret instanceof Integer[]) {
					return Arrays.asList((Integer[]) ret).size();
				} else if (ret instanceof Long[]) {
					return Arrays.asList((Long[]) ret).size();
				} else if (ret instanceof Float[]) {
					return Arrays.asList((Float[]) ret).size();
				} else if (ret instanceof Double[]) {
					return Arrays.asList((Double[]) ret).size();
				} else if (ret instanceof BigDecimal[]) {
					return Arrays.asList((BigDecimal[]) ret).size();
				} else {
					throw new IllegalArgumentException(String.valueOf(ret.getClass()));
				}
			} else if (ret instanceof String) {
				ret = ((String) ret).length();
			} else {
				ret = 0;
			}
			return ret;
		} else if (str.startsWith("url(")) { //url()
			Matcher matcher = Pattern.compile("^url\\(([^,]+)(,\\s*(.+))?\\)$").matcher(str);
			if (matcher.find()) {
				if (matcher.group(1) == null) return "";
				String url = matcher.group(1).replaceAll("[\"']", "");
				if (url.length() == 0) return "";
				String param = null;
				if (matcher.group(2) != null && matcher.group(3) != null) {
					param = matcher.group(3);
					if (item != null) {
						Matcher m = Pattern.compile("\\$"+item+"\\.(\\w+)").matcher(param);
						StringBuffer h = new StringBuffer();
						if (m.find()) {
							Object r = parse(m.group(0), newObj, item);
							m.appendReplacement(h, Common.isNumeric(String.valueOf(r)) ? String.valueOf(r) : "'"+String.valueOf(r).replaceAll("([$\\\\])", "\\\\$1")+"'");
						}
						m.appendTail(h);
						param = h.toString();
					}
				}
				ret = Common.url(url, param);
			}
			return ret;
		} else if (str.startsWith("strlen(")) { //strlen()
			Matcher matcher = Pattern.compile("^strlen\\((.+)\\)$").matcher(str);
			if (matcher.find()) {
				try {
					Object r = parseCompute(matcher.group(1), newObj, item);
					if (!(r instanceof String)) return 0;
					ret = ((String) r).length();
				} catch (Exception e) {
					e.printStackTrace();
					ret = 0;
				}
			}
			return ret;
		} else if (str.matches("^(number_format|round)\\(([^,]+),\\s*(.+)\\)$")) { //number_format(aa, 2) round(aa, 2)
			Matcher matcher = Pattern.compile("^(number_format|round)\\(([^,]+),\\s*(.+)\\)$").matcher(str);
			if (matcher.find()) {
				double number = Double.parseDouble(String.valueOf(parseCompute(matcher.group(2), newObj, item)));
				int digits = (int) Float.parseFloat(String.valueOf(parseCompute(matcher.group(3), newObj, item)));
				ret = String.format("%."+digits+"f", number);
			}
		} else if (str.matches("^substring\\(([^,]+),\\s*(\\d+)(\\s*,\\s*(\\d+))?\\s*\\)$")) { //substring(aa, 0, 1) substring(aa, 1)
			Matcher matcher = Pattern.compile("^substring\\(([^,]+),\\s*(\\d+)(\\s*,\\s*(\\d+))?\\s*\\)$").matcher(str);
			if (matcher.find()) {
				Object element = parse(matcher.group(1), newObj, item);
				int beginIndex = Integer.parseInt(matcher.group(2));
				if (matcher.group(4) == null) {
					ret = ((String) element).substring(beginIndex);
				} else {
					ret = ((String) element).substring(beginIndex, Integer.parseInt(matcher.group(4)));
				}
			}
			return ret;
		} else if (str.startsWith("is_array(")) { //is_array()
			Matcher matcher = Pattern.compile("^is_array\\((.+)\\)$").matcher(str);
			if (matcher.find()) {
				Object o = parse(matcher.group(1), newObj, item);
				if (o == null) return Boolean.FALSE;
				return (o instanceof List) || o.getClass().isArray();
			}
			return Boolean.FALSE;
		} else if (str.startsWith("in_array(")) { //in_array()
			Matcher matcher = Pattern.compile("^in_array\\(([^,]+),\\s*(.+)\\)$").matcher(str);
			if (matcher.find()) {
				Object element = parse(matcher.group(1), newObj, item);
				Object array = parse(matcher.group(2), newObj, item);
				if (element == null || array == null) return Boolean.FALSE;
				if (array instanceof List) {
					try {
						return array.getClass().getMethod("contains", Object.class).invoke(array, element);
					} catch (Exception e) {
						e.printStackTrace();
						return Boolean.FALSE;
					}
				} else if (array.getClass().isArray()) {
					if (array instanceof String[]) {
						return Arrays.asList((String[]) array).contains((String) element);
					} else if (array instanceof Integer[]) {
						return Arrays.asList((Integer[]) array).contains((int) element);
					} else if (array instanceof Long[]) {
						return Arrays.asList((Long[]) array).contains((long) element);
					} else if (array instanceof Float[]) {
						return Arrays.asList((Float[]) array).contains((float) element);
					} else if (array instanceof Double[]) {
						return Arrays.asList((Double[]) array).contains((double) element);
					} else if (array instanceof BigDecimal[]) {
						return Arrays.asList((BigDecimal[]) array).contains((BigDecimal) element);
					} else {
						throw new IllegalArgumentException(array.getClass().getName() + " IS NOT SUPPORT");
					}
				}
				return Boolean.FALSE;
			}
			return Boolean.FALSE;
		} else if (str.startsWith("isset(")) { //isset()
			Matcher matcher = Pattern.compile("^isset\\((.+)\\)$").matcher(str);
			if (matcher.find()) {
				Object o = parse(matcher.group(1), newObj, item);
				return o != null;
			}
			return Boolean.FALSE;
		} else if (str.startsWith("json_encode(")) { //json_encode()
			Matcher matcher = Pattern.compile("^json_encode\\((.+)\\)$").matcher(str);
			if (matcher.find()) {
				Object o = parse(matcher.group(1), newObj, item);
				if (o == null) return null;
				ret = JSON.toJSONString(o, SerializerFeature.WriteMapNullValue);
			}
			return ret;
		} else if (Pattern.compile("^(strto)?lower\\(").matcher(str).find()) { //strtolower() lower()
			Matcher matcher = Pattern.compile("^(strto)?lower\\((.+)\\)$").matcher(str);
			if (matcher.find()) {
				Object o = parseCompute(matcher.group(2), newObj, item);
				if (!(o instanceof String)) return null;
				ret = ((String) o).toLowerCase();
			}
			return ret;
		} else if (Pattern.compile("^(strto)?upper\\(").matcher(str).find()) { //strtoupper() upper()
			Matcher matcher = Pattern.compile("^(strto)?upper\\((.+)\\)$").matcher(str);
			if (matcher.find()) {
				Object o = parseCompute(matcher.group(2), newObj, item);
				if (!(o instanceof String)) return null;
				ret = ((String) o).toUpperCase();
			}
			return ret;
		} else if (str.startsWith("intval(")) { //intval()
			Matcher matcher = Pattern.compile("^intval\\((.+)\\)$").matcher(str);
			if (matcher.find()) {
				float number = Float.parseFloat(String.valueOf(parse(matcher.group(1), newObj, item)));
				ret = (int) number;
			}
			return ret;
		} else if (str.startsWith("nl2br(")) { //nl2br()
			Matcher matcher = Pattern.compile("^nl2br\\((.+)\\)$").matcher(str);
			if (matcher.find()) {
				String res = (String) parse(matcher.group(1), newObj, item);
				ret = res.replace("\n", "<br>");
			}
			return ret;
		} else if (Common.isNumeric(str)) { //123.00
			ret = Float.parseFloat(str);
			return ret;
		} else if (str.equalsIgnoreCase("true")) { //true
			return Boolean.TRUE;
		} else if (str.equalsIgnoreCase("false")) { //false
			return Boolean.FALSE;
		} else if (Pattern.compile("^(\\w+)\\((([^,]+)(,.+)?)?\\)$").matcher(str).find()) { //testFun('merge', 'string')
			//自定义方法, 在当前 Controller 内, 且为 public static 方法, 或者使用 Common 的方法
			if (this.clazz != null) {
				Matcher matcher = Pattern.compile("^(\\w+)\\((([^,]+)(,.+)?)?\\)$").matcher(str);
				if (matcher.find()) {
					Class<?>[] parameterTypes = new Class<?>[0];
					Object[] args = new Object[0];
					String funName = matcher.group(1);
					if (matcher.group(2) != null) {
						Object param = parseCompute(matcher.group(3), newObj, item);
						if (param == null) param = new Object();
						parameterTypes = new Class<?>[]{param.getClass()};
						args = new Object[]{param};
						if (matcher.group(4) != null) {
							List<Object> list = new ArrayList<>();
							list.add(param);
							Matcher m = Pattern.compile("(,\\s*(.+))").matcher(matcher.group(4));
							while (m.find()) {
								Object r = parseCompute(m.group(2), newObj, item);
								if (r == null) r = new Object();
								list.add(r);
							}
							parameterTypes = new Class<?>[list.size()];
							args = new Object[list.size()];
							for (int i = 0; i < list.size(); i++) {
								parameterTypes[i] = list.get(i).getClass();
								args[i] = list.get(i);
							}
						}
					}
					try {
						ret = this.clazz.getMethod(funName, parameterTypes).invoke(null, args);
					} catch (Exception e) {
						String message = e.getMessage();
						try {
							ret = Common.class.getMethod(funName, parameterTypes).invoke(null, args);
						} catch (Exception ex) {
							Common.writeError("Tengine template variable error: " + str + "\n" + message + "\n" + ex.getMessage() + "\n");
							return null;
						}
					}
					return ret;
				}
			}
		} else if (str.matches("^([^?]+)\\?((.+?)(:(.+))?)$")) { //aa ? bb : cc
			Matcher matcher = Pattern.compile("^([^?]+)\\?((.+?)(:(.+))?)$").matcher(str);
			if (matcher.find()) {
				if (parseIfJudge(matcher.group(1), newObj, item)) {
					ret = parseCompute(matcher.group(3), newObj, item);
				} else if (matcher.group(5) != null) {
					ret = parseCompute(matcher.group(5), newObj, item);
				} else {
					ret = null;
				}
			}
			return ret;
		} else if (str.matches("^(.+?)([+\\-*/])(.+)$")) { //aa[+-*/]bb
			ret = parseCompute(str, newObj, item);
			return ret;
		} else if (str.startsWith("Tengine.")) {
			String[] sys = str.split("\\.");
			if (sys.length < 2 || sys[1].length() == 0) return null;
			switch (sys[1].toLowerCase()) {
				case "time": { //Tengine.time
					ret = new Date().getTime() / 1000;
					break;
				}
				case "get":
				case "post": { //Tengine.get.id Tengine.post.id
					if (sys.length < 3) return null;
					ret = this.request.get(sys[2]);
					if (ret == null) return null;
					break;
				}
				case "param": { //Tengine.param.id
					if (sys.length < 3) return null;
					ret = this.request.param(sys[2]);
					if (ret == null) return null;
					break;
				}
				case "path": { //Tengine.path.0
					if (sys.length < 3) return null;
					ret = this.request.path(sys[2]);
					if (ret == null) return null;
					break;
				}
				case "session": { //Tengine.session.id
					if (sys.length < 3) return null;
					if (sys[2].equals("getId")) return this.request.getSession().getId();
					ret = this.request.session(sys[2], true);
					if (ret == null) return null;
					break;
				}
				case "cookie": { //Tengine.cookie.id
					if (sys.length < 3) return null;
					ret = this.request.cookie(sys[2]);
					if (ret == null) return null;
					break;
				}
				case "server": { //Tengine.server.java.version
					if (sys.length < 3) return null;
					ret = this.request.server(str.replace("tengine.server.", ""));
					if (ret == null) return null;
					break;
				}
				case "header": { //Tengine.header.referer
					if (sys.length < 3) return null;
					ret = this.request.header(sys[2]);
					if (ret == null) return null;
					break;
				}
			}
			return ret;
		}
		String key = "___NORMAL_"+this.normalMap.keySet().size()+"___";
		this.normalMap.put(key, String.valueOf(ret));
		return key;
	}
}
