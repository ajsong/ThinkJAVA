package com;

import com.alibaba.fastjson.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.system.ApplicationHome;
import org.yaml.snakeyaml.Yaml;
import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.regex.*;

@SpringBootApplication
public class ThinkApplication {
	
	public static void main(String[] args) {
		if (args.length > 0) {
			String command = args[0];
			String[] commands = command.split(":");
			if (commands[0].equals("make")) {
				if (commands.length > 1 && args.length > 1) {
					String argument = args[1];
					boolean detail = (args.length > 2 && args[2].equals("--detail"));
					if (argument.contains("--") && args.length > 2) {
						detail = argument.equals("--detail");
						argument = args[2];
					}
					if (commands[1].equals("controller")) {
						String[] arguments = argument.split(",");
						for (String arg : arguments) {
							String filepath = createControllerFile(arg, detail);
							if (filepath != null) System.out.println("Controller:\033[32m" + filepath + "\033[m created successfully.");
						}
						System.out.println();
						System.exit(0);
					} else if (commands[1].equals("model")) {
						String[] arguments = argument.split(",");
						for (String arg : arguments) {
							String filepath = createModelFile(uncamelize(arg), detail);
							if (filepath != null) System.out.println("Model:\033[32m" + filepath + "\033[m created successfully.");
						}
						System.out.println();
						System.exit(0);
					}
				}
			}
		}
		
		System.out.println();
		System.out.println("\033[33mUsage:\033[m\n" +
				"  java -jar think.jar [commands] [options]\n" +
				"\n" +
				"\033[33mAvailable commands:\033[m\n" +
				" \033[33mmake\033[m\n" +
				"  \033[32mmake:controller\033[m   Create a new resource controller class\n" +
				"  \033[32mmake:model\033[m        Create a new model class\n" +
				"\n" +
				"\033[33mOptions:\033[m\n" +
				"  \033[32m--detail\033[m   Create the method controller / database field model class");
		System.out.println();
		System.exit(0);
		
		SpringApplication.run(ThinkApplication.class, args);
	}
	
	public static String root_path() {
		ApplicationHome ah = new ApplicationHome(ThinkApplication.class);
		return ah.getSource().getParentFile().getPath().replaceAll("(/$)", "");
	}
	public static Map<String, Object> getYmls() {
		return getYmls("file:///" + root_path() + "/src/main/resources/application.yml");
	}
	public static Map<String, Object> getYmls(String filename) {
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
				url = ThinkApplication.class.getClassLoader().getResource(filename);
			}
			if (url == null) return null;
			Map<String, Object> map = new LinkedHashMap<>();
			JSONObject obj = JSONObject.parseObject(JSON.toJSONString(yaml.load(Files.newInputStream(Paths.get(url.getFile())))));
			setYmlInline(obj, "", map);
			return map;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
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
	public static boolean makedir(String dir) {
		String root_path = root_path();
		dir = dir.replace(root_path, "").replace("\\", "/").replaceAll("(/$)", "");
		File path = new File(root_path, dir);
		if (path.exists() && path.isDirectory()) return true;
		String[] dirs = dir.split("/");
		String filePath = root_path;
		for (String d : dirs) {
			try {
				path = new File(filePath, d);
				if (!path.exists() || !path.isDirectory()) Files.createDirectory(path.toPath());
				filePath += "/" + d;
			} catch (Exception e) {
				System.out.println("FILE PATH CREATE FAIL:\n" + filePath + "/" + d);
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}
	public static String camelize(String value) {
		StringBuilder res = new StringBuilder();
		String[] words = value.replaceAll("_", " ").split(" ");
		for (String word : words) {
			res.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
		}
		return res.toString();
	}
	public static String uncamelize(String value) {
		return value.replaceAll("\\s+", "").replaceAll("(.)(?=[A-Z])", "$1_").toLowerCase();
	}
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
	
	//生成controller文件
	public static String createControllerFile(String mark, boolean detail) {
		try {
			String packagePath = "controller";
			String clazz = mark;
			if (mark.contains("@")) {
				String[] marks = mark.split("@");
				packagePath = marks[0].toLowerCase();
				clazz = marks[1];
			}
			clazz = camelize(clazz);
			String path = root_path() + "/src/main/java/com/app/" + packagePath;
			if (!makedir(path)) return null;
			String filepath = path + "/" + clazz + ".java";
			if (new File(filepath).exists()) {
				System.out.println("Controller:\033[31m" + filepath + "\033[m already exist.\n");
				return null;
			}
			StringBuilder sb = new StringBuilder("package com.app."+packagePath+";\n\n").append("public class ").append(clazz).append(" extends Core {\n\n");
			if (detail) {
				sb.append("\t");
				sb.append("//显示资源列表\n" +
						"\tpublic Object index() {\n" +
						"\t\treturn this.render(null);\n" +
						"\t}\n" +
						"\t\n" +
						"\t//显示创建资源表单页\n" +
						"\tpublic Object create() {\n" +
						"\t\tint id = this.request.get(\"id\", 0);\n" +
						"\t\treturn this.render(null);\n" +
						"\t}\n" +
						"\t\n" +
						"\t//保存新建的资源\n" +
						"\tpublic Object save() {\n" +
						"\t\tint id = this.request.post(\"id\", 0);\n" +
						"\t\treturn this.render(null);\n" +
						"\t}\n" +
						"\t\n" +
						"\t//显示指定的资源\n" +
						"\tpublic Object read() {\n" +
						"\t\tint id = this.request.get(\"id\", 0);\n" +
						"\t\treturn this.render(null);\n" +
						"\t}\n" +
						"\n" +
						"\t//显示编辑资源表单页\n" +
						"\tpublic Object edit() {\n" +
						"\t\tint id = this.request.get(\"id\", 0);\n" +
						"\t\treturn this.render(null);\n" +
						"\t}\n" +
						"\t\n" +
						"\t//保存更新的资源\n" +
						"\tpublic Object update() {\n" +
						"\t\tint id = this.request.post(\"id\", 0);\n" +
						"\t\treturn this.render(null);\n" +
						"\t}\n" +
						"\t\n" +
						"\t//删除指定资源\n" +
						"\tpublic Object delete() {\n" +
						"\t\tint id = this.request.post(\"id\", 0);\n" +
						"\t\treturn this.render(null);\n" +
						"\t}");
			}
			sb.append("\n\n}");
			FileWriter writer = new FileWriter(filepath);
			writer.write(sb.toString());
			writer.close();
			return filepath;
		} catch (Exception e) {
			System.out.println("生成controller异常");
			e.printStackTrace();
		}
		return null;
	}
	
	//生成model文件
	public static Connection ConnInit() {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			return DriverManager.getConnection(getYml("spring.datasource.url", ""), getYml("spring.datasource.username", ""), getYml("spring.datasource.password", ""));
		} catch (Exception e) {
			System.out.println("SQL驱动程序初始化失败：" + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}
	public static String createModelFile(String table, boolean detail) {
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			String clazz = camelize(table);
			String path = root_path() + "/src/main/java/com/app/model";
			if (!makedir(path)) return null;
			String filepath = path + "/" + clazz + ".java";
			if (new File(filepath).exists()) {
				System.out.println("Model:\033[31m" + filepath + "\033[m already exist.\n");
				return null;
			}
			StringBuilder sb = new StringBuilder("package com.app.model;\n\nimport com.framework.tool.*;\nimport java.lang.reflect.*;\nimport java.util.*;\n\n")
					.append("public class ").append(clazz).append(" extends Core {\n");
			if (detail) {
				sb.append("\n");
				StringBuilder method = new StringBuilder();
				String sql = "SHOW COLUMNS FROM " + getYml("spring.datasource.prefix", "") + table;
				conn = ConnInit();
				assert conn != null;
				ps = conn.prepareStatement(sql);
				ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					String field = rs.getString("Field");
					String type = rs.getString("Type");
					if (type.startsWith("int(")) type = "Integer";
					else if (type.startsWith("varchar(") || type.equals("text")) type = "String";
					else if (type.startsWith("decimal(")) type = "Double";
					sb.append("\tpublic ").append(type).append(" ").append(field).append(";\n");
					String Field = Character.toUpperCase(field.charAt(0)) + field.substring(1);
					method.append("\n\tpublic ").append(type).append(" get").append(Field).append("() {\n\t\treturn ").append(field).append(";\n\t}\n");
					method.append("\tpublic void set").append(Field).append("(").append(type).append(" ").append(field).append(") {\n\t\tthis.")
							.append(field).append(" = ").append(field).append(";\n\t}\n");
				}
				sb.append(method);
			}
			sb.append("\n\t");
			sb.append("//数据库操作(自动设定表名)===================================================\n" +
					"\tpublic static String connectname() {\n" +
					"\t\tString connection = \"\";\n" +
					"\t\tField[] fields = "+clazz+".class.getDeclaredFields();\n" +
					"\t\ttry {\n" +
					"\t\t\tfor (Field field : fields) {\n" +
					"\t\t\t\tfield.setAccessible(true);\n" +
					"\t\t\t\tif (Modifier.isStatic(field.getModifiers())) {\n" +
					"\t\t\t\t\tif (field.getName().equals(\"connection\")) connection = (String) field.get("+clazz+".class);\n" +
					"\t\t\t\t}\n" +
					"\t\t\t}\n" +
					"\t\t} catch (Exception e) {\n" +
					"\t\t\te.printStackTrace();\n" +
					"\t\t}\n" +
					"\t\tif (connection.length() > 0) return connection;\n" +
					"\t\treturn null;\n" +
					"\t}\n" +
					"\tpublic static String tablename() {\n" +
					"\t\tString name = \"\";\n" +
					"\t\tString table = \"\";\n" +
					"\t\tField[] fields = "+clazz+".class.getDeclaredFields();\n" +
					"\t\ttry {\n" +
					"\t\t\tfor (Field field : fields) {\n" +
					"\t\t\t\tfield.setAccessible(true);\n" +
					"\t\t\t\tif (Modifier.isStatic(field.getModifiers())) {\n" +
					"\t\t\t\t\tif (field.getName().equals(\"name\")) name = (String) field.get("+clazz+".class);\n" +
					"\t\t\t\t\tif (field.getName().equals(\"table\")) table = (String) field.get("+clazz+".class);\n" +
					"\t\t\t\t}\n" +
					"\t\t\t}\n" +
					"\t\t} catch (Exception e) {\n" +
					"\t\t\te.printStackTrace();\n" +
					"\t\t}\n" +
					"\t\tif (name.length() > 0) return name;\n" +
					"\t\tif (table.length() > 0) return table;\n" +
					"\t\tString clazz = new Object() {\n" +
					"\t\t\tpublic String get() {\n" +
					"\t\t\t\tString clazz = this.getClass().getName();\n" +
					"\t\t\t\treturn clazz.substring(0, clazz.lastIndexOf('$'));\n" +
					"\t\t\t}\n" +
					"\t\t}.get();\n" +
					"\t\treturn Common.uncamelize(clazz.substring(clazz.lastIndexOf(\".\")+1));\n" +
					"\t}\n" +
					"\tpublic static Db alias(String alias) {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).alias(alias);\n" +
					"\t}\n" +
					"\tpublic static Db leftJoin(String table, String on) {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).leftJoin(table, on);\n" +
					"\t}\n" +
					"\tpublic static Db rightJoin(String table, String on) {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).rightJoin(table, on);\n" +
					"\t}\n" +
					"\tpublic static Db innerJoin(String table, String on) {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).innerJoin(table, on);\n" +
					"\t}\n" +
					"\tpublic static Db crossJoin(String table) {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).crossJoin(table);\n" +
					"\t}\n" +
					"\tpublic static Db where(Object where, Object...whereParams) {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).where(where, whereParams);\n" +
					"\t}\n" +
					"\tpublic static Db whereOr(Object where, Object...whereParams) {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).whereOr(where, whereParams);\n" +
					"\t}\n" +
					"\tpublic static Db whereDay(String field, String mark) {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).whereDay(field, mark);\n" +
					"\t}\n" +
					"\tpublic static Db whereTime(String field, String value) {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).whereTime(field, value);\n" +
					"\t}\n" +
					"\tpublic static Db whereTime(String field, String operator, String value) {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).whereTime(field, operator, value);\n" +
					"\t}\n" +
					"\tpublic static Db whereTime(String interval, String field, String operator, Object value) {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).whereTime(interval, field, operator, value);\n" +
					"\t}\n" +
					"\tpublic static Db field(Object field) {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).field(field);\n" +
					"\t}\n" +
					"\tpublic static Db distinct(String field) {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).distinct(field);\n" +
					"\t}\n" +
					"\tpublic static Db like(String field, String str) {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).like(field, str);\n" +
					"\t}\n" +
					"\tpublic static Db like(String field, String str, String escape) {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).like(field, str, escape);\n" +
					"\t}\n" +
					"\tpublic static Db order(String field) {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).order(field);\n" +
					"\t}\n" +
					"\tpublic static Db order(String field, String order) {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).order(field, order);\n" +
					"\t}\n" +
					"\tpublic static Db orderField(String field, String value) {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).orderField(field, value);\n" +
					"\t}\n" +
					"\tpublic static Db group(String group) {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).group(group);\n" +
					"\t}\n" +
					"\tpublic static Db having(String having) {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).having(having);\n" +
					"\t}\n" +
					"\tpublic static Db offset(int offset) {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).offset(offset);\n" +
					"\t}\n" +
					"\tpublic static Db pagesize(int pagesize) {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).pagesize(pagesize);\n" +
					"\t}\n" +
					"\tpublic static Db limit(int pagesize) {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).limit(pagesize);\n" +
					"\t}\n" +
					"\tpublic static Db limit(int offset, int pagesize) {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).limit(offset, pagesize);\n" +
					"\t}\n" +
					"\tpublic static Db cached(int cached) {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).cached(cached);\n" +
					"\t}\n" +
					"\tpublic static Db pagination() {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).pagination();\n" +
					"\t}\n" +
					"\tpublic static Db pagination(String paginationMark) {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).pagination(paginationMark);\n" +
					"\t}\n" +
					"\tpublic static Db fetchSql() {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).fetchSql();\n" +
					"\t}\n" +
					"\tpublic static boolean exist() {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).exist();\n" +
					"\t}\n" +
					"\tpublic static int count() {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).count();\n" +
					"\t}\n" +
					"\tpublic static DataList select(Object field) {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).select(field);\n" +
					"\t}\n" +
					"\tpublic static DataList select() {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).select();\n" +
					"\t}\n" +
					"\tpublic static int insert(String data, Object...dataParams) {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).insert(data, dataParams);\n" +
					"\t}\n" +
					"\tpublic static int insert(List<String> data, Object...dataParams) {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).insert(data, dataParams);\n" +
					"\t}\n" +
					"\tpublic static int insert(String[] data, Object...dataParams) {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).insert(data, dataParams);\n" +
					"\t}\n" +
					"\tpublic static int insert(Map<String, Object> datas) {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).insert(datas);\n" +
					"\t}\n" +
					"\tpublic static int insert(String[] data, List<Object> dataParams) {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).insert(data, dataParams);\n" +
					"\t}\n" +
					"\tpublic static int insertGetId(String data, Object...dataParams) {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).insertGetId(data, dataParams);\n" +
					"\t}\n" +
					"\tpublic static int insertGetId(List<String> data, Object...dataParams) {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).insertGetId(data, dataParams);\n" +
					"\t}\n" +
					"\tpublic static int insertGetId(String[] data, Object...dataParams) {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).insertGetId(data, dataParams);\n" +
					"\t}\n" +
					"\tpublic static int insertGetId(Map<String, Object> datas) {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).insertGetId(datas);\n" +
					"\t}\n" +
					"\tpublic static int insertGetId(String[] data, List<Object> dataParams) {\n" +
					"\t\tDb.connect(connectname());return Db.name(tablename()).insertGetId(data, dataParams);\n" +
					"\t}");
			sb.append("\n\n}");
			FileWriter writer = new FileWriter(filepath);
			writer.write(sb.toString());
			writer.close();
			return filepath;
		} catch (Exception e) {
			System.out.println("生成model异常");
			e.printStackTrace();
		} finally {
			try {
				if (ps != null) ps.close();
				if (conn != null) conn.close();
			} catch (Exception e) {
				//e.printStackTrace();
			}
		}
		return null;
	}
	
}
