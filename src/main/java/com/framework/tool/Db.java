//Developed by @mario 3.1.20220722
package com.framework.tool;

import com.alibaba.fastjson.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.*;
import javax.servlet.http.*;
import java.io.*;
import java.lang.reflect.*;
import java.math.*;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.regex.*;

public class Db {
	public static final String MASTER = "master";
	public static final String SLAVER = "slaver";
	
	static int dbType = 0; //0:Mysql, 1:SQLite
	static String sqliteDatabase = "";
	static String sqliteDir = "sqlite";
	static String default_host;
	static String default_username;
	static String default_password;
	static String default_prefix;
	static String host;
	static String username;
	static String password;
	static String prefix;
	static String slaverHost;
	static String slaverUsername;
	static String slaverPassword;
	static String cacheType;
	static String cacheDir;
	static String runtimeDir;
	static String rootPath;
	static Connection conn = null;
	static PreparedStatement ps =  null;

	private String table = "";
	private String alias = "";
	private List<String> left = null;
	private List<String> right = null;
	private List<String> inner = null;
	private List<String> cross = null;
	private String where = "";
	private List<Object> whereParams = null;
	private String field = "";
	private String distinct = "";
	private String order = "";
	private String group = "";
	private String having = "";
	private int offset = 0;
	private int pagesize = 0;
	private int cached = 0;
	private boolean pagination = false;
	private String paginationMark = "";
	private boolean fetchSql = false;

	static {
		default_host = Common.getYml("spring.datasource.default.url", "");
		default_username = Common.getYml("spring.datasource.default.username", "");
		default_password = Common.getYml("spring.datasource.default.password", "");
		default_prefix = Common.getYml("spring.datasource.default.prefix", "");
		host = default_host;
		username = default_username;
		password = default_password;
		prefix = default_prefix;
		slaverHost = Common.getYml("spring.datasource.slaver.url", host);
		slaverUsername = Common.getYml("spring.datasource.slaver.username", username);
		slaverPassword = Common.getYml("spring.datasource.slaver.password", password);
		cacheType = Common.getYml("sdk.cache.type", "");
		cacheDir = Common.getYml("spring.datasource.cache-dir", "sql");
		runtimeDir = Common.getYml("sdk.runtime.dir", "runtime");
		rootPath = Common.root_path();
	}

	//数据库连接, deployType:部署方式[0读|1写]
	public static void init(String deployType) {
		try {
			if (dbType == 0) {
				Class.forName("com.mysql.cj.jdbc.Driver");
				if (deployType.equals(MASTER)) {
					conn = DriverManager.getConnection(host, username, password);
				} else {
					conn = DriverManager.getConnection(slaverHost, slaverUsername, slaverPassword);
				}
			} else if (dbType == 1) {
				String sqlitePath = rootPath + "/" + sqliteDir;
				File paths = new File(sqlitePath);
				if (!paths.exists()) {
					if (!paths.mkdirs()) throw new IllegalArgumentException("FILE PATH CREATE FAIL:\n" + sqlitePath);
				}
				Class.forName("org.sqlite.JDBC");
				conn = DriverManager.getConnection("jdbc:sqlite:" + sqlitePath + "/" + sqliteDatabase + ".sqlite");
			}
		} catch (Exception e) {
			System.out.println("SQL驱动程序初始化失败：" + e.getMessage());
			e.printStackTrace();
		}
	}
	//切换连接参数
	public static void connect() {
		Db.connect("default");
	}
	public static void connect(String key) {
		if (key == null || key.length() == 0) key = "default";
		if (key.startsWith("~")) {
			//连接SQLite数据库
			/*<dependency>
				<groupId>org.xerial</groupId>
				<artifactId>sqlite-jdbc</artifactId>
			</dependency>*/
			dbType = 1;
			sqliteDatabase = key.substring(1);
		} else {
			//连接Mysql数据库
			dbType = 0;
			host = Common.getYml("spring.datasource."+key+".url", "");
			username = Common.getYml("spring.datasource."+key+".username", "");
			password = Common.getYml("spring.datasource."+key+".password", "");
			prefix = Common.getYml("spring.datasource."+key+".prefix", "");
		}
	}
	//指定表名
	public static Db name(String name) {
		return Db.table(Db.replaceTable(name));
	}
	//指定表名, 可设置别名, 如: table('table t'), 支持双减号转表前缀(不区分大小写), 如: table('--TABLE-- t')
	public static Db table(String table) {
		Db db = new Db();
		boolean restore = true;
		if (table.startsWith("!")) { //表名前加!代表不restore
			restore = false;
			table = table.substring(1);
		}
		if (restore) db.restore();
		if (table.contains(" ")) {
			String[] tables = table.replaceAll("\\s+", " ").split(" ");
			table = tables[0];
			db.alias = tables[1];
		}
		db.table = table;
		return db;
	}
	//表别名
	public Db alias(String alias) {
		this.alias = alias.length() > 0 ? alias : "";
		return this;
	}
	//左联接
	public Db leftJoin(String table, String on) {
		String sql = " LEFT JOIN " + Db.replaceTable(table) + " ON " + on;
		if (this.left == null) this.left = new ArrayList<>();
		this.left.add(sql);
		return this;
	}
	//右联接
	public Db rightJoin(String table, String on) {
		String sql = " RIGHT JOIN " + Db.replaceTable(table) + " ON " + on;
		if (this.right == null) this.right = new ArrayList<>();
		this.right.add(sql);
		return this;
	}
	//等值联接
	public Db innerJoin(String table, String on) {
		String sql = " INNER JOIN " + Db.replaceTable(table) + " ON " + on;
		if (this.inner == null) this.inner = new ArrayList<>();
		this.inner.add(sql);
		return this;
	}
	//多联接
	public Db crossJoin(String table) {
		if (this.cross == null) this.cross = new ArrayList<>();
		this.cross.add(", " + Db.replaceTable(table));
		return this;
	}
	//条件
	public Db where(Object where, Object...whereParams) {
		return whereAdapter(where, " AND ", whereParams);
	}
	public Db whereOr(Object where, Object...whereParams) {
		return whereAdapter(where, " OR ", whereParams);
	}
	@SuppressWarnings("unchecked")
	public Db whereAdapter(Object where, String andOr, Object...whereParams) {
		String wheres = this.where;
		if ((where instanceof Integer) || Pattern.compile("^\\d+$").matcher(String.valueOf(where)).find()) { //数值默认为id
			wheres += (wheres.length() > 0 ? andOr : "") + "id=" + where;
		} else if (where instanceof String[]) {
			String[] items = new String[((String[]) where).length];
			for (int i = 0; i < ((String[]) where).length; i++) {
				String item = ((String[]) where)[i];
				items[i] = Pattern.compile("^[\\w.]+$").matcher(item).find() ? (item.contains(".") ? item + "=?" : "`" + item + "`=?") : item;
			}
			String w = StringUtils.join(items, " AND ");
			if (andOr.equals(" OR ")) w = "(" + w + ")";
			wheres += (wheres.length() > 0 ? andOr : "") + w;
		} else if (where instanceof Map) {
			Map<String, Object> entry = (Map<String, Object>) where;
			String[] items = new String[entry.keySet().size()];
			whereParams = new Object[entry.keySet().size()];
			int i = 0;
			for (String key : entry.keySet()) {
				items[i] = key.contains("=") ? key : (key.contains(".") ? key + "=?" : "`" + key + "`=?");
				whereParams[i] = entry.get(key);
				i++;
			}
			if (wheres.length() == 0 && andOr.equals(" OR ")) {
				wheres = StringUtils.join(items, " OR ");
			} else {
				String w = StringUtils.join(items, " AND ");
				if (andOr.equals(" OR ")) w = "(" + w + ")";
				wheres += (wheres.length() > 0 ? andOr : "") + w;
			}
		} else if ((where instanceof String) && ((String)where).length() > 0){
			String _where = (String) where;
			if (_where.contains("&") || _where.contains("|")) {
				Matcher matcher = Pattern.compile("([a-z_.]+([A-Z_.!%<>=]+)?)").matcher(_where);
				StringBuffer res = new StringBuffer();
				while (matcher.find()) {
					String item = matcher.group(1);
					String mark = matcher.group(2);
					String operator = "=?";
					if (mark != null) {
						switch (mark) {
							case "!":operator = "!=?";break; //field!
							case "IN":operator = " IN (?)";break; //fieldIN
							case "!IN":case "NOTIN":operator = " NOT IN (?)";break; //field!IN fieldNOTIN
							case "NULL":operator = " IS NULL";break; //fieldNULL
							case "!NULL":case "NOTNULL":operator = " IS NOT NULL";break; //field!NULL fieldNOTNULL
							default:
								if (mark.contains("%")) { //field%LIKE fieldLIKE% field%LIKE% field%uploads_LIKE%
									operator = " LIKE '" + mark.replace("LIKE", "?") + "'";
								} else {
									operator = mark + "?"; //field< field<= field> field>=
								}
						}
						item = item.substring(0, item.length() - mark.length());
					}
					item = item.contains(".") ? item + operator : "`" + item + "`" + operator;
					matcher.appendReplacement(res, item);
				}
				matcher.appendTail(res);
				_where = res.toString().replace("&", " AND ").replace("|", " OR ");
			} else if (_where.matches("\\w+")) {
				_where = "`" + _where + "`=?";
			}
			String w = _where.replaceFirst("^ AND ", "");
			if (andOr.equals(" OR ")) w = "(" + w + ")";
			wheres += (wheres.length() > 0 ? andOr : "") + w;
		}
		this.where = wheres;
		//绑定参数
		if (whereParams.length > 0) {
			if (this.whereParams == null) this.whereParams = new ArrayList<>();
			this.whereParams.addAll(Arrays.asList(whereParams));
		}
		return this;
	}
	//时间对比查询
	//whereDay("add_time", "today") //查询add_time今天的记录
	public Db whereDay(String field, String mark) {
		return whereTime(field, "=", Common.date("Y-m-d HH:ii:ss", Common.strtotime(mark)));
	}
	//whereTime("add_time", "2022-7-10") //查询add_time等于指定日期的记录
	public Db whereTime(String field, String value) {
		return whereTime(field, "=", Common.date("Y-m-d HH:ii:ss", Common.strtotime(value)));
	}
	//whereTime("add_time", "<", "2022-7-10") //查询add_time小于指定日期的记录
	public Db whereTime(String field, String operator, String value) {
		if (operator.contains("<")) {
			long timestamp = Common.time(Common.date("Y-m-d 00:00:00", Common.time(value)));
			this.where += (this.where.length() > 0 ? " AND " : "") + "`"+field+"`" + operator + timestamp;
		} else if (operator.contains(">")) {
			long timestamp = Common.time(Common.date("Y-m-d 23:59:59", Common.time(value)));
			this.where += (this.where.length() > 0 ? " AND " : "") + "`"+field+"`" + operator + timestamp;
		} else {
			long start = Common.time(Common.date("Y-m-d 00:00:00", Common.time(value)));
			long end = Common.time(Common.date("Y-m-d 23:59:59", Common.time(value)));
			this.where += (this.where.length() > 0 ? " AND " : "") + "`"+field+"`>=" + start + " AND `"+field+"`<=" + end;
		}
		return this;
	}
	//whereTime("d", "add_time", "<", 1) //查询add_time小于1天的记录
	public Db whereTime(String interval, String field, String operator, Object value) {
		return whereTime(interval, field, operator, value, "");
	}
	public Db whereTime(String interval, String field, String operator, Object value, String now) {
		switch (interval) {
			case "y":interval = "YEAR";break;
			case "q":interval = "QUARTER";break;
			case "m":interval = "MONTH";break;
			case "w":interval = "WEEK";break;
			case "d":interval = "DAY";break;
			case "h":interval = "HOUR";break;
			case "n":interval = "MINUTE";break;
			case "s":interval = "SECOND";break;
		}
		interval = interval.toUpperCase();
		String his = "";
		switch (interval) {
			case "HOUR":his = " %H";break;
			case "MINUTE":his = " %H:%i";break;
			case "SECOND":his = " %H:%i:%s";break;
		}
		if (now.length() == 0) {
			if (his.length() == 0) now = "DATE_FORMAT(NOW(),'%Y-%m-%d')";
			else {
				if (interval.equals("HOUR")) now = "DATE_FORMAT(NOW(),'%Y-%m-%d %H')";
				else if (interval.equals("MINUTE")) now = "DATE_FORMAT(NOW(),'%Y-%m-%d %H:%i')";
				else now = "NOW()";
			}
		}
		//fieldOpe = "IF(ISNUMERIC(" + field + "),FROM_UNIXTIME(" + field + ",'%Y-%m-%d" + his + "')," + field + ")";
		String fieldOpe = "FROM_UNIXTIME(" + field + ",'%Y-%m-%d" + his + "')";
		this.where += (this.where.length() > 0 ? " AND " : "") + "TIMESTAMPDIFF(" + interval + "," + fieldOpe + "," + now + ")" + operator + value;
		return this;
	}
	//要查询的字段
	//.field(new String[]{"id", "name"}) or .field("id, name")
	@SuppressWarnings("unchecked")
	public Db field(Object field) {
		String fields = this.field;
		if (field instanceof String[]) {
			String[] f = new String[((String[]) field).length];
			for (int i = 0; i < ((String[]) field).length; i++) {
				String _field = ((String[]) field)[i];
				f[i] = _field.contains(".") ? _field : "`" + _field + "`";
			}
			fields += (fields.length() > 0 ? ", " : "") + StringUtils.join(f, ", ");
		} else if (field instanceof Map) { //指定别名
			Map<String, String> entry = (Map<String, String>) field;
			String[] items = new String[entry.size()];
			int i = 0;
			for (Map.Entry<String, String> item : entry.entrySet()) {
				String _field = item.getKey();
				items[i] = (_field.contains(".") ? _field : "`" + _field + "`") + (item.getValue().length() > 0 ? " as " + item.getValue() : "");
				i++;
			}
			fields += (fields.length() > 0 ? ", " : "") + StringUtils.join(items, ", ");
		} else if ((field instanceof String) && ((String)field).length() > 0){
			if (((String)field).trim().matches("^\\w+(\\|\\w+)+$")) { //排除不需要的字段
				try {
					List<String> fieldArray = new ArrayList<>();
					String[] items = ((String)field).split("\\|");
					String sql = Db.replaceTable("SHOW COLUMNS FROM " + this.table);
					if (conn == null) Db.init(MASTER);
					ps = conn.prepareStatement(sql);
					ResultSet rs = ps.executeQuery();
					while (rs.next()) {
						if (!Arrays.asList(items).contains(rs.getString("Field"))) fieldArray.add(rs.getString("Field"));
					}
					fields = StringUtils.join(fieldArray, ", ");
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					Db.close();
				}
			} else {
				fields += (fields.length() > 0 ? ", " : "") + ((String)field).trim();
			}
		}
		this.field = fields;
		return this;
	}
	//去重查询
	public Db distinct(String field) {
		this.distinct = "DISTINCT(" + field + ")";
		return this;
	}
	//LIKE查询, 如: name LIKE 'G_ARTICLE/_%' ESCAPE '/'
	public Db like(String field, String str) {
		return like(field, str, "");
	}
	public Db like(String field, String str, String escape) {
		String where = field + " LIKE '" + str + "'";
		if (escape.length() > 0) where += " ESCAPE '" + escape + "'";
		this.where += (this.where.length() > 0 ? " AND " : "") + where;
		return this;
	}
	//排序
	public Db order(String field) {
		return order(field, null);
	}
	public Db order(String field, String order) {
		this.order = field.length() > 0 ? " ORDER BY " + field + ((order != null && order.length() > 0) ? " " + order : "") : "";
		return this;
	}
	//按字段排序, 如: ORDER BY FIELD(`id`, 1, 9, 8, 4)
	public Db orderField(String field, String value) {
		this.order = field.length() > 0 ? " ORDER BY FIELD(`" + field + "`, " + value + ")" : "";
		return this;
	}
	//分组(聚合)
	public Db group(String group) {
		this.group = group.length() > 0 ? " GROUP BY " + group : "";
		return this;
	}
	//聚合筛选, 语法与where一样
	public Db having(String having) {
		this.having = having.length() > 0 ? " HAVING " + having : "";
		return this;
	}
	//记录偏移量
	public Db offset(int offset) {
		this.offset = offset;
		return this;
	}
	//返回记录最大数目
	public Db pagesize(int pagesize) {
		this.pagesize = pagesize;
		return this;
	}
	//设定记录偏移量与返回记录最大数目
	public Db limit(int pagesize) {
		this.pagesize = pagesize;
		return this;
	}
	public Db limit(int offset, int pagesize) {
		this.offset = offset;
		this.pagesize = pagesize;
		return this;
	}
	//使用缓存查询结果, 0不缓存, -1永久缓存, >0缓存时间(单位秒)
	public Db cached(int cached) {
		this.cached = cached;
		return this;
	}
	//设置分页
	public Db pagination() {
		return pagination("page");
	}
	public Db pagination(String paginationMark) {
		this.pagination = true;
		this.paginationMark = paginationMark;
		return this;
	}
	//打印sql语句
	public Db fetchSql() {
		this.fetchSql = true;
		return this;
	}
	//记录是否存在
	public boolean exist() {
		return (Long)count() > 0;
	}
	//记录数量
	public <T> T count() {
		return count("COUNT(*)");
	}
	public <T> T count(String field) {
		return count(field, Integer.class);
	}
	@SuppressWarnings("unchecked")
	public <T> T count(String field, Class<? extends Number> type) {
		DataMap ret = this.field(field).find();
		if (ret == null) {
			if (type == Integer.class) return (T) Integer.valueOf("0");
			else if (type == Long.class) return (T) Long.valueOf("0");
			else if (type == Float.class) return (T) Float.valueOf("0");
			else if (type == Double.class) return (T) Double.valueOf("0");
			else if (type == BigDecimal.class) return (T) new BigDecimal("0");
			return null;
		}
		T res = (T) ret.getOne();
		if (res.getClass() != type || res.getClass() == BigDecimal.class) {
			try {
				String t = type.toString().substring(type.toString().lastIndexOf(".") + 1);
				if (t.equals("Integer")) {
					t = "Int";
					if (res.getClass() == Double.class) res = (T) String.valueOf(res).split("\\.")[0];
				}
				String parseName = "parse" + t;
				Method parse = type.getMethod(parseName, String.class);
				res = (T) parse.invoke(null, String.valueOf(res));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return res;
	}
	//查询字段总和
	public <T> T sum(String field) {
		return sum(field, Integer.class);
	}
	public <T> T sum(String field, Class<? extends Number> type) {
		return count("SUM(" + field + ")", type);
	}
	//查询字段平均值
	public <T> T avg(String field) {
		return avg(field, Double.class);
	}
	public <T> T avg(String field, Class<? extends Number> type) {
		return count("AVG(" + field + ")", type);
	}
	//查询字段最小值
	public <T> T min(String field) {
		return min(field, Integer.class);
	}
	public <T> T min(String field, Class<? extends Number> type) {
		return count("MIN(" + field + ")", type);
	}
	//查询字段最大值
	public <T> T max(String field) {
		return max(field, Integer.class);
	}
	public <T> T max(String field, Class<? extends Number> type) {
		return count("MAX(" + field + ")", type);
	}
	//查询字段
	public String value(String field) {
		return value(field, String.class);
	}
	@SuppressWarnings("unchecked")
	public <T> T value(String field, Class<T> type) {
		DataMap obj = this.field(field).find();
		if (obj == null) {
			if (type == Integer.class) return (T) Integer.valueOf("0");
			else if (type == Long.class) return (T) Long.valueOf("0");
			else if (type == Float.class) return (T) Float.valueOf("0");
			else if (type == Double.class) return (T) Double.valueOf("0");
			else if (type == BigDecimal.class) return (T) new BigDecimal("0");
			else if (type == String.class) return (T) "";
			return null;
		}
		if (field.contains(".")) field = field.substring(field.lastIndexOf(".") + 1);
		return (T) obj.get(field);
	}
	//查询某列值
	@SuppressWarnings("unchecked")
	public <T> T[] column(String field) {
		try {
			DataList list = field(field).select();
			if (list == null) return null;
			Object[] columns = new Object[list.size()];
			for (int i = 0; i < list.size(); i++) {
				DataMap obj = list.get(i);
				columns[i] = obj.get(field);
			}
			return (T[]) columns;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	//查询单条记录
	public DataMap find(Object field) {
		return field(field).find();
	}
	public DataMap find() {
		DataList list = this.pagesize(1).select();
		return list == null ? null : list.get(0);
	}
	//查询
	public DataList select(Object field) {
		return field(field).select();
	}
	public DataList select() {
		String sql = _createSql();
		try {
			DataList res = new DataList();
			if (this.fetchSql) System.out.println(sql);
			if (this.cached != 0) {
				DataList r = _cacheSql(sql);
				if (r != null) return r;
			} else if (this.pagination && this.pagesize != 1) {
				_setPagination();
			}
			if (conn == null) Db.init(MASTER);
			ps = conn.prepareStatement(sql);
			if (this.whereParams != null) {
				for (int i = 0; i < this.whereParams.size(); i++) { //绑定参数
					ps.setObject(i + 1, this.whereParams.get(i));
				}
			}
			ResultSet rs = ps.executeQuery();
			String[] columnNames = Db.getColumnNames(rs);
			while (rs.next()) {
				DataMap item = new DataMap();
				for (int i = 0; i < columnNames.length; i++) {
					String columnName = columnNames[i];
					Object value = Pattern.compile(".*(COUNT|SUM|AVG|MIN|MAX|DISTINCT)\\(.*", Pattern.CASE_INSENSITIVE).matcher(this.field).matches() ? rs.getObject(i + 1) : rs.getObject(columnName);
					if (value != null) {
						if (value.getClass() == Double.class || value.getClass() == BigDecimal.class) value = Float.parseFloat(String.valueOf(value));
					}
					item.put(columnName, value);
				}
				res.add(item);
			}
			if (res.size() > 0) {
				if (this.cached != 0 && sql.length() > 0) _cacheSql(sql, res);
				return res;
			}
		} catch (Exception e) {
			System.out.println("DB查询异常");
			System.out.println(sql);
			e.printStackTrace();
		} finally {
			Db.close();
		}
		return null;
	}
	//查询字段(使用对象)
	//String name = Db.name("user").where("id=1").value("name", User.class, String.class);
	@SuppressWarnings("unchecked")
	public <T, R> R value(String field, Class<T> clazz, Class<R> type) {
		if (field == null || field.length() == 0 || field.equals("*")) return count();
		try {
			T obj = field(field).find(clazz);
			if (obj == null) {
				if (type == Integer.class) return (R) Integer.valueOf("0");
				else if (type == Long.class) return (R) Long.valueOf("0");
				else if (type == Float.class) return (R) Float.valueOf("0");
				else if (type == Double.class) return (R) Double.valueOf("0");
				else if (type == BigDecimal.class) return (R) new BigDecimal("0");
				else if (type == String.class) return (R) "";
				return null;
			}
			if (field.contains(".")) field = field.substring(field.lastIndexOf(".") + 1);
			R res;
			try {
				Field f = obj.getClass().getDeclaredField(field);
				String getterName = "get" + Character.toUpperCase(f.getName().charAt(0)) + f.getName().substring(1);
				Method getter = clazz.getMethod(getterName);
				res = (R) getter.invoke(obj);
			} catch (NoSuchFieldException e) {
				if (type != null) {
					Method getter = clazz.getMethod("get", String.class, type);
					res = (R) getter.invoke(obj, field, type);
				} else {
					Method getter = clazz.getMethod("get", String.class);
					res = (R) getter.invoke(obj, field);
				}
			}
			return res;
		} catch (Exception e) {
			System.out.println("DB查询字段异常");
			e.printStackTrace();
		}
		return null;
	}
	//查询某列值(使用对象)
	@SuppressWarnings("unchecked")
	public <T, R> R[] column(String field, Class<T> clazz) {
		try {
			List<T> list = field(field).select(clazz);
			if (list == null) return null;
			Object[] columns = new Object[list.size()];
			for (int i = 0; i < list.size(); i++) {
				T obj = list.get(i);
				String getterName = "get" + Character.toUpperCase(field.charAt(0)) + field.substring(1);
				Method getter = clazz.getMethod(getterName);
				columns[i] = getter.invoke(obj);
			}
			return (R[]) columns;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	//查询单条记录(返回对象)
	//User user = Db.name("user").find(User.class);
	public <T> T find(Class<T> clazz) {
		List<T> list = this.pagesize(1).select(clazz);
		return list == null ? null : list.get(0);
	}
	//查询(返回List<对象>)
	//List<User> user = Db.name("user").select(User.class);
	public <T> List<T> select(Class<T> clazz) {
		String sql = _createSql();
		List<T> res = new ArrayList<>();
		try {
			if (this.fetchSql) System.out.println(sql);
			if (this.cached != 0) {
				List<T> r = _cacheSql(sql, clazz);
				if (r != null) return r;
			} else if (this.pagination && this.pagesize != 1) {
				_setPagination();
			}
			if (conn == null) Db.init(MASTER);
			ps = conn.prepareStatement(sql);
			if (this.whereParams != null) {
				for (int i = 0; i < this.whereParams.size(); i++) { //绑定参数
					ps.setObject(i + 1, this.whereParams.get(i));
				}
			}
			ResultSet rs = ps.executeQuery();
			String[] columnNames = Db.getColumnNames(rs);
			Constructor<T> constructor = clazz.getConstructor();
			//Field[] fields = clazz.getDeclaredFields(); //获取所有属性
			while (rs.next()) {
				boolean seted = false;
				T obj = constructor.newInstance(); //创建一个实例
				for (String columnName : columnNames) {
					Object value = rs.getObject(columnName);
					if (value != null) {
						if (value.getClass() == Double.class || value.getClass() == BigDecimal.class) value = Float.parseFloat(String.valueOf(value));
					}
					try {
						Field f = obj.getClass().getDeclaredField(columnName);
						String setterName = "set" + Character.toUpperCase(f.getName().charAt(0)) + f.getName().substring(1); //构造 setter 方法名
						Method setter = clazz.getMethod(setterName, f.getType()); //调用对应实例的 setter 方法给它设置属性
						if (value != null && f.getType() != value.getClass() && f.getType() != Object.class) {
							if (f.getType() == Integer.class) {
								value = Integer.parseInt(String.valueOf(value));
							} else if (f.getType() == Long.class) {
								value = Long.parseLong(String.valueOf(value));
							} else if (f.getType() == Float.class) {
								value = Float.parseFloat(String.valueOf(value));
							} else if (f.getType() == Double.class) {
								value = Double.parseDouble(String.valueOf(value));
							} else if (f.getType() == String.class) {
								value = String.valueOf(value);
							} else {
								System.out.println(clazz.getName()+"     "+setterName+"      "+f.getName()+" = "+f.getType().getName()+"        data = "+value.getClass().getName());
							}
						}
						if (value != null) setter.invoke(obj, value);
						seted = true;
					} catch (NoSuchFieldException e) {
						Method setter = clazz.getMethod("set", String.class, Object.class);
						if (value != null) setter.invoke(obj, columnName, value);
						seted = true;
					}
				}
				/*for (Field f : fields) {
					if (!Arrays.asList(columnNames).contains(f.getName())) continue;
					Object value = rs.getObject(f.getName());
					if (value != null && value.getClass().equals(BigDecimal.class)) value = ((BigDecimal)value).doubleValue();
					String setterName = "set" + Character.toUpperCase(f.getName().charAt(0)) + f.getName().substring(1); //构造 setter 方法名
					Method setter = clazz.getMethod(setterName, f.getType()); //调用对应实例的 setter 方法给它设置属性
					if (value != null && !f.getType().equals(value.getClass()) && !f.getType().getName().equals("java.lang.Object")) {
						System.out.println(clazz.getName()+"     "+setterName+"      "+f.getName()+" = "+f.getType().getName()+"        data = "+value.getClass().getName());
					}
					if (value != null) setter.invoke(obj, value);
					seted = true;
				}*/
				if (seted) res.add(obj);
			}
		} catch (Exception e) {
			System.out.println("DB查询异常");
			e.printStackTrace();
		} finally {
			Db.close();
		}
		if (res.size() > 0) {
			if (this.cached != 0 && sql.length() > 0) _cacheSql(sql, res);
			return res;
		}
		return null;
	}
	//分页用获取总记录数
	private void _setPagination() {
		String sql = _createSql(true);
		if (sql.matches("SELECT DISTINCT\\(")) {
			sql = sql.replaceAll("SELECT DISTINCT\\(([^)]+)\\).*\\s+FROM\\b", "SELECT COUNT(DISTINCT($1)) FROM");
		} else {
			sql = sql.replaceAll("^SELECT.*\\s+FROM\\b", "SELECT COUNT(*) FROM");
			sql = sql.replaceAll(" ORDER BY (\\s*,?.+?(A|DE)SC)+", "");
			if (sql.contains("GROUP BY")) sql = "SELECT COUNT(*) FROM (" + sql + ") gb";
		}
		try {
			if (this.fetchSql) System.out.println(sql);
			if (conn == null) Db.init(MASTER);
			ps = conn.prepareStatement(sql);
			if (this.whereParams != null) {
				for (int i = 0; i < this.whereParams.size(); i++) {
					ps.setObject(i + 1, this.whereParams.get(i));
				}
			}
			ResultSet rs = ps.executeQuery();
			int records = 0;
			while (rs.next()) {
				records = rs.getInt(1);
			}
			Pagination p = new Pagination(records).isCn();
			ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
			HttpServletRequest request = Objects.requireNonNull(servletRequestAttributes).getRequest();
			request.setAttribute(this.paginationMark, p);
		} catch (Exception e) {
			System.out.println("DB设置分页异常");
			e.printStackTrace();
		} finally {
			Db.close();
		}
	}
	//获取所有列名
	public static String[] getColumnNames(ResultSet rs) {
		String[] names = new String[0];
		try {
			ResultSetMetaData metaData = rs.getMetaData();
			int count = metaData.getColumnCount();
			names = new String[count];
			for (int i = 0; i < count; i++) {
				names[i] = metaData.getColumnName(i+1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return names;
	}
	//判断查询结果集中是否存在某列
	public static boolean isExistColumn(ResultSet rs, String columnName) {
		try {
			if (rs.findColumn(columnName) > 0) return true;
		} catch (SQLException e) {
			return false;
		}
		return false;
	}
	//创建SQL语句
	private String _createSql() {
		return _createSql(false);
	}
	private String _createSql(boolean isPagination) {
		String field = this.field;
		if (field.length() == 0) {
			if (this.distinct.length() == 0) field = "*";
			else field = this.distinct;
		} else if (this.distinct.length() > 0) {
			if (!field.trim().equals("*")) field = this.distinct + ", " + field;
			else field = this.distinct;
		}
		StringBuilder sql = new StringBuilder("SELECT ").append(field).append(" FROM ").append(this.table);
		if (this.alias.length() > 0) sql.append(" ").append(this.alias);
		if (this.left != null) sql.append(StringUtils.join(this.left, ""));
		if (this.right != null) sql.append(StringUtils.join(this.right, ""));
		if (this.inner != null) sql.append(StringUtils.join(this.inner, ""));
		if (this.cross != null) sql.append(StringUtils.join(this.cross, ""));
		if (this.where.length() > 0) sql.append(" WHERE ").append(this.where);
		if (this.group.length() > 0) sql.append(this.group);
		if (this.having.length() > 0) sql.append(this.having);
		if (!isPagination) {
			if (this.order.length() > 0) sql.append(this.order);
			if (this.pagesize == 1) sql.append(" LIMIT 1");
			else if (this.pagesize > 0) sql.append(" LIMIT ").append(this.offset).append(",").append(this.pagesize);
		}
		return Db.replaceTable(sql.toString());
	}
	//获取/设置sql缓存
	private DataList _cacheSql(String sql) {
		if (cacheType.equals("redis")) {
			Redis redis = new Redis();
			boolean hasRedis = redis.ping();
			if (hasRedis) {
				if (redis.hasKey(sql)) {
					return Common.json_decode((String) redis.get(sql));
				}
				return null;
			}
		}
		ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		HttpServletRequest request = Objects.requireNonNull(servletRequestAttributes).getRequest();
		Map<String, String> moduleMap = Common.getModule(request);
		String module = moduleMap.get("module");
		String cachePath = rootPath + "/" + runtimeDir + "/" + module + "/" + cacheDir;
		File file = new File(cachePath + "/" + _md5(sql));
		if (file.exists()) {
			if (this.cached == -1 || (new Date().getTime()/1000 - file.lastModified()/1000) <= this.cached) {
				StringBuilder res = new StringBuilder();
				BufferedReader reader = null;
				try {
					reader = new BufferedReader(new FileReader(file));
					String line;
					while ((line = reader.readLine()) != null) {
						res.append(line);
					}
					return new DataList(Common.json_decode(res.toString()));
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					try{
						if (reader != null) reader.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return null;
	}
	private void _cacheSql(String sql, Object res) {
		if (cacheType.equals("redis")) {
			Redis redis = new Redis();
			boolean hasRedis = redis.ping();
			if (hasRedis) {
				if (res instanceof DataList) {
					List<Map<String, Object>> list = new ArrayList<>();
					for (DataMap map : ((DataList) res).list) list.add(map.data);
					redis.set(sql, JSON.toJSONString(list), this.cached);
				} else if (res instanceof DataMap) {
					redis.set(sql, JSON.toJSONString(((DataMap)res).data), this.cached);
				} else {
					redis.set(sql, JSON.toJSONString(res), this.cached);
				}
				return;
			}
		}
		ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		HttpServletRequest request = Objects.requireNonNull(servletRequestAttributes).getRequest();
		Map<String, String> moduleMap = Common.getModule(request);
		String module = moduleMap.get("module");
		String cachePath = rootPath + "/" + runtimeDir + "/" + module + "/" + cacheDir;
		if (!Common.makedir(cachePath)) return;
		File file = new File(cachePath + "/" + _md5(sql));
		try {
			FileWriter fileWritter = new FileWriter(file);
			if (res instanceof DataList) {
				List<Map<String, Object>> list = new ArrayList<>();
				for (DataMap map : ((DataList) res).list) list.add(map.data);
				fileWritter.write(JSON.toJSONString(list));
			} else if (res instanceof DataMap) {
				fileWritter.write(JSON.toJSONString(((DataMap)res).data));
			} else {
				fileWritter.write(JSON.toJSONString(res));
			}
			fileWritter.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private <T> List<T> _cacheSql(String sql, Class<T> clazz) {
		if (cacheType.equals("redis")) {
			Redis redis = new Redis();
			boolean hasRedis = redis.ping();
			if (hasRedis) {
				if (redis.hasKey(sql)) {
					return JSONArray.parseArray((String) redis.get(sql), clazz);
				}
				return null;
			}
		}
		ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		HttpServletRequest request = Objects.requireNonNull(servletRequestAttributes).getRequest();
		Map<String, String> moduleMap = Common.getModule(request);
		String module = moduleMap.get("module");
		String cachePath = rootPath + "/" + runtimeDir + "/" + module + "/" + cacheDir;
		if (!Common.makedir(cachePath)) return null;
		File file = new File(cachePath + "/" + _md5(sql));
		if (file.exists()) {
			if (this.cached == -1 || (new Date().getTime()/1000 - file.lastModified()/1000) <= this.cached) {
				StringBuilder res = new StringBuilder();
				BufferedReader reader = null;
				try {
					reader = new BufferedReader(new FileReader(file));
					String line;
					while ((line = reader.readLine()) != null) {
						res.append(line);
					}
					return JSONArray.parseArray(res.toString(), clazz);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					try{
						if (reader != null) reader.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return null;
	}
	//MD5
	private String _md5(String str) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(str.getBytes());
			return new BigInteger(1, md.digest()).toString(16);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	//清除两端字符串
	private String _trim(String str) {
		return str.replaceAll("(^,|,$)", "");
	}
	//插入记录, 失败返回-1
	//int row = Db.name("user").insert(new String[]{"name", "age"}, name, age);
	public int insert(String data, Object...dataParams) {
		List<String> map = new ArrayList<>();
		String[] fields = data.split(",");
		for (String field : fields) map.add(field.trim());
		return insert(map, dataParams);
	}
	public int insert(List<String> data, Object...dataParams) {
		return insert(data.toArray(new String[0]), dataParams);
	}
	public int insert(String[] data, Object...dataParams) {
		return insert(data, new ArrayList<>(Arrays.asList(dataParams)));
	}
	public int insert(Map<String, Object> datas) {
		String[] data = new String[datas.keySet().size()];
		List<Object> dataParams = new ArrayList<>();
		int i = 0;
		for (String key : datas.keySet()) {
			data[i] = key;
			dataParams.add(datas.get(key));
		}
		return insert(data, dataParams);
	}
	public int insert(String[] data, List<Object> dataParams) {
		int row;
		try {
			StringBuilder sql = new StringBuilder("INSERT INTO " + this.table + " (");
			for (String d : data) sql.append(d).append(", ");
			sql = new StringBuilder(sql.toString().replaceAll("(^, |, $)", "")).append(") VALUES(");
			for (String ignored : data) sql.append("?, ");
			sql = new StringBuilder(sql.toString().replaceAll("(^, |, $)", "")).append(")");
			String sq = Db.replaceTable(sql.toString());
			if (this.fetchSql) System.out.println(sq);
			if (conn == null) Db.init(SLAVER);
			ps =  conn.prepareStatement(sq);
			int k = 0;
			if (dataParams != null) {
				for (Object dataParam : dataParams) {
					ps.setObject(k + 1, dataParam);
					k++;
				}
			}
			if (this.whereParams != null) {
				for (Object whereParam : this.whereParams) {
					ps.setObject(k + 1, whereParam);
					k++;
				}
			}
			row =  ps.executeUpdate();
		} catch (SQLException e) {
			System.out.println("DB插入异常");
			e.printStackTrace();
			row = -1;
		} finally {
			Db.close();
		}
		return row;
	}
	public int insertGetId(String data, Object...dataParams) {
		int res = insert(data, dataParams);
		if (res == -1) throw new IllegalArgumentException("Failed to create data");
		return Db.name(this.table).order("id DESC").value("id", Integer.class);
	}
	public int insertGetId(List<String> data, Object...dataParams) {
		int res = insert(data, dataParams);
		if (res == -1) throw new IllegalArgumentException("Failed to create data");
		return Db.name(this.table).order("id DESC").value("id", Integer.class);
	}
	public int insertGetId(String[] data, Object...dataParams) {
		int res = insert(data, dataParams);
		if (res == -1) throw new IllegalArgumentException("Failed to create data");
		return Db.name(this.table).order("id DESC").value("id", Integer.class);
	}
	public int insertGetId(Map<String, Object> datas) {
		int res = insert(datas);
		if (res == -1) throw new IllegalArgumentException("Failed to create data");
		return Db.name(this.table).order("id DESC").value("id", Integer.class);
	}
	public int insertGetId(String[] data, List<Object> dataParams) {
		int res = insert(data, dataParams);
		if (res == -1) throw new IllegalArgumentException("Failed to create data");
		return Db.name(this.table).order("id DESC").value("id", Integer.class);
	}
	//快捷更新某字段
	public int setField(String field, String value) {
		return update(field, value);
	}
	//字段递增
	public int incr(String field) {
		return incr(field, 1);
	}
	public int incr(String field, int step) {
		return setInc(field, step);
	}
	public int setInc(String field) {
		return setInc(field, 1);
	}
	public int setInc(String field, int step) {
		String value = String.valueOf(step);
		if (step > 0) value = "+" + value;
		return setField(field, value);
	}
	//字段递减
	public int decr(String field) {
		return decr(field, 1);
	}
	public int decr(String field, int step) {
		return setDec(field, step);
	}
	public int setDec(String field) {
		return setDec(field, 1);
	}
	public int setDec(String field, int step) {
		return setInc(field, -step);
	}
	//更新记录, 失败返回-1
	//int row = Db.name("user").where("id=?", id).update(new String[]{"name", "age"}, name, age);
	public int update(String data, Object...dataParams) {
		List<String> map = new ArrayList<>();
		String[] fields = data.split(",");
		for (String field : fields) map.add(field.trim());
		return update(map, dataParams);
	}
	public int update(List<String> data, Object...dataParams) {
		return update(data.toArray(new String[0]), dataParams);
	}
	public int update(String[] data, Object...dataParams) {
		return update(data, new ArrayList<>(Arrays.asList(dataParams)));
	}
	public int update(Map<String, Object> datas) {
		String[] data = new String[datas.keySet().size()];
		List<Object> dataParams = new ArrayList<>();
		int i = 0;
		for (String key : datas.keySet()) {
			data[i] = key;
			dataParams.add(datas.get(key));
			i++;
		}
		return update(data, dataParams);
	}
	public int update(String[] data, List<Object> dataParams) {
		int row;
		try {
			StringBuilder sql = new StringBuilder("UPDATE " + this.table + " SET ");
			int k = 0;
			for (String d : data) {
				if (dataParams != null && dataParams.size() > 0) sql.append("`");
				sql.append(d);
				if (dataParams != null && dataParams.size() > 0) {
					Object param = dataParams.get(k);
					if ((param instanceof String) && ((String)param).matches("^[+\\-*/]")) sql.append("`=`").append(d).append("`").append(param);
					else sql.append("`=?");
				}
				sql.append(", ");
				k++;
			}
			sql = new StringBuilder(sql.toString().replaceAll("(^, |, $)", ""));
			if (this.where.length() > 0) sql.append(" WHERE ").append(this.where);
			if (this.pagesize > 0) sql.append(" LIMIT ").append(this.pagesize);
			String sq = Db.replaceTable(sql.toString());
			if (this.fetchSql) System.out.println(sq);
			if (conn == null) Db.init(SLAVER);
			ps =  conn.prepareStatement(sq);
			k = 0;
			if (dataParams != null) {
				for (Object param : dataParams) {
					if (!(param instanceof String) || !((String) param).matches("^[+\\-*/]")) {
						ps.setObject(k + 1, param);
						k++;
					}
				}
			}
			if (this.whereParams != null) {
				for (Object whereParam : this.whereParams) {
					ps.setObject(k + 1, whereParam);
					k++;
				}
			}
			row =  ps.executeUpdate();
		} catch (SQLException e) {
			System.out.println("DB更新异常");
			e.printStackTrace();
			row = -1;
		} finally {
			Db.close();
		}
		return row;
	}
	//删除记录, 失败返回-1
	//int row = Db.name("user").where("id=?", id).delete();
	public int delete() {
		return delete(null);
	}
	public int delete(Object where, Object...whereParams) {
		if (where != null) this.where(where, whereParams);
		int row;
		try {
			StringBuilder sql = new StringBuilder("DELETE FROM " + this.table);
			if (this.where.length() > 0) sql.append(" WHERE ").append(this.where);
			String sq = Db.replaceTable(sql.toString());
			if (this.fetchSql) System.out.println(sq);
			if (conn == null) Db.init(SLAVER);
			ps =  conn.prepareStatement(sq);
			int k = 0;
			if (this.whereParams != null) {
				for (Object whereParam : this.whereParams) {
					ps.setObject(k + 1, whereParam);
					k++;
				}
			}
			row =  ps.executeUpdate();
		} catch (SQLException e) {
			System.out.println("DB删除异常");
			e.printStackTrace();
			row = -1;
		} finally {
			Db.close();
		}
		return row;
	}
	//原生查询
	public static DataList query(String sql, Object...dataParams) {
		DataList res = new DataList();
		try {
			sql = Db.replaceTable(sql);
			if (conn == null) Db.init(MASTER);
			ps =  conn.prepareStatement(sql);
			for (int i = 0; i < dataParams.length; i++) {
				ps.setObject(i + 1, dataParams[i]);
			}
			ResultSet rs = ps.executeQuery();
			String[] columnNames = Db.getColumnNames(rs);
			while (rs.next()) {
				DataMap item = new DataMap();
				for (String columnName : columnNames) {
					Object value = rs.getObject(columnName);
					if (value != null) {
						if (value.getClass() == Double.class || value.getClass() == BigDecimal.class) value = Float.parseFloat(String.valueOf(value));
					}
					item.put(columnName, value);
				}
				res.add(item);
			}
		} catch (SQLException e) {
			System.out.println("DB原生查询异常");
			e.printStackTrace();
		} finally {
			Db.close();
		}
		if (res.size() > 0) return res;
		return null;
	}
	//原生执行
	public static int execute(String sql, Object...dataParams) {
		int row = 0;
		try {
			sql = Db.replaceTable(sql);
			if (conn == null) Db.init(SLAVER);
			ps =  conn.prepareStatement(sql);
			for (int i = 0; i < dataParams.length; i++) {
				ps.setObject(i + 1, dataParams[i]);
			}
			row =  ps.executeUpdate();
		} catch (SQLException e) {
			System.out.println("DB原生增删改异常");
			e.printStackTrace();
		} finally {
			Db.close();
		}
		return row;
	}
	//替换SQL语句中表名双减号为表前缀
	public static String replaceTable(String sql) {
		if (sql.matches("^\\w+(\\s+\\w+)?$")) sql = prefix + sql.replace(prefix, "");
		Matcher matcher = Pattern.compile("(--(\\w+)--)").matcher(sql);
		StringBuffer res = new StringBuffer();
		while (matcher.find()) {
			matcher.appendReplacement(res, prefix + matcher.group(2).toLowerCase());
		}
		matcher.appendTail(res);
		return res.toString();
	}
	//获取指定列类型
	public static String getColumnType(String table, String column) {
		String type = "";
		try {
			String sql = "SHOW COLUMNS FROM " + Db.replaceTable(table);
			if (conn == null) Db.init(MASTER);
			ps = conn.prepareStatement(sql);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (rs.getString("Field").equals(column)) {
					type = rs.getString("Type");
					if (type.startsWith("int(")) type = "Integer";
					else if (type.startsWith("varchar(")) type = "String";
					else if (type.startsWith("decimal(")) type = "Double";
					break;
				}
			}
		} catch (Exception e) {
			System.out.println("DB获取指定列类型异常");
			e.printStackTrace();
		} finally {
			Db.close();
		}
		return type;
	}
	//是否存在表
	public boolean tableExist(String table) {
		//ALTER TABLE table ENGINE=InnoDB //修改数据表引擎为InnoDB
		String sql;
		boolean has_table = false;
		if (dbType == 1) {
			sql = "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='"+Db.replaceTable(table)+"'";
		} else {
			sql = "SHOW TABLES LIKE '"+Db.replaceTable(table)+"'";
		}
		try {
			if (conn == null) Db.init(MASTER);
			ps =  conn.prepareStatement(sql);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (dbType == 1) {
					if (rs.getInt(1) > 0) has_table = true;
				} else {
					has_table = true;
				}
			}
			return has_table;
		} catch (Exception e) {
			System.out.println("DB查询是否存在表异常");
			e.printStackTrace();
			return false;
		} finally {
			Db.close();
		}
	}
	//创建数据表,可创建sqlite3
	/*Db.name("~db").tableCreate(new LinkedHashMap<String, Object>(){{
		put("member", new LinkedHashMap<String, Object>(){{
			put("table_engine", "InnoDB");
			put("table_auto_increment", 10);
			put("table_comment", "表注释");
			put("id", new LinkedHashMap<String, String>(){{put("type", "key");}});
			put("name", new LinkedHashMap<String, String>(){{put("type", "varchar(255)");put("comment", "名称");put("charset", "utf8mb4");}});
			put("price", new LinkedHashMap<String, String>(){{put("type", "decimal(10,2)");put("default", "0.00");}});
			put("content", new LinkedHashMap<String, String>(){{put("type", "text");}});
			put("clicks", new LinkedHashMap<String, String>(){{put("type", "int");put("index", "clicks");}});
		}});
	}});*/
	public void tableCreate(Object tables) {
		tableCreate(tables, false);
	}
	@SuppressWarnings("unchecked")
	public void tableCreate(Object tables, boolean re_create) {
		String sql = "";
		if (!(tables instanceof String) && !(tables instanceof Map)) throw new IllegalArgumentException("tableCreate parament 1 must be String or Map<String, Map<String, Object>>");
		if (tables instanceof String) sql = (String) tables;
		else {
			Map<String, Object> infos = (Map<String, Object>) tables;
			for (String table_name : infos.keySet()) {
				Object table_info = infos.get(table_name);
				if (!(table_info instanceof Map)) throw new IllegalArgumentException("tableCreate parament 1 must be String or Map<String, Map<String, Object>>");
				if (!re_create && tableExist(table_name)) continue;
				String key_field = "";
				StringBuilder field_sql = new StringBuilder();
				List<String[]> index = new ArrayList<>(); //索引
				tableRemove(table_name);
				Map<String, Object> tableInfo = (Map<String, Object>) table_info;
				sql += "CREATE TABLE `"+table_name+"` (\n";
				for (String field_name : tableInfo.keySet()) {
					Object field_info = tableInfo.get(field_name);
					if (Arrays.asList(new String[]{"table_engine", "table_auto_increment", "table_comment"}).contains(field_name)) continue;
					field_sql.append("`").append(field_name).append("`");
					Map<String, String> fieldInfo = (Map<String, String>) field_info;
					if (fieldInfo.get("type") != null) {
						if (fieldInfo.get("type").equals("key")) {
							key_field = field_name;
							field_sql.append(dbType == 1 ? " integer NOT NULL PRIMARY KEY AUTOINCREMENT" : " int(11) NOT NULL AUTO_INCREMENT");
						}
						else if (dbType == 1 && fieldInfo.get("type").contains("varchar")) {
							field_sql.append(" text");
						}
						else if (dbType == 1 && fieldInfo.get("type").contains("int")) {
							field_sql.append(" integer");
						}
						else if (dbType == 1 && fieldInfo.get("type").contains("decimal")) {
							field_sql.append(" numeric");
						}
						else field_sql.append(" ").append(fieldInfo.get("type"));
					} else {
						field_sql.append(dbType == 1 ? " text" : " varchar(255)");
					}
					if (dbType != 1 && fieldInfo.get("charset") != null) field_sql.append(" CHARACTER SET ").append(fieldInfo.get("charset"));
					if (fieldInfo.get("default") != null) {
						field_sql.append(" DEFAULT '").append(fieldInfo.get("default")).append("'");
					} else if (fieldInfo.get("type") != null && (fieldInfo.get("type").contains("int") || fieldInfo.get("type").contains("decimal"))) {
						field_sql.append(fieldInfo.get("type").contains("decimal") ? " DEFAULT '0.00'" : " DEFAULT '0'");
					} else if (fieldInfo.get("type") != null && fieldInfo.get("type").contains("varchar")) {
						field_sql.append(" DEFAULT NULL");
					}
					if (dbType != 1 && fieldInfo.get("index") != null) index.add(new String[]{fieldInfo.get("index"), field_name});
					if (dbType != 1 && fieldInfo.get("comment") != null) field_sql.append(" COMMENT '").append(fieldInfo.get("comment").replace("'", "\\'")).append("'");
					field_sql.append(",\n");
				}
				if (dbType != 1 && key_field.length() > 0) field_sql.append("PRIMARY KEY (`").append(key_field).append("`)");
				field_sql = new StringBuilder(_trim(field_sql.toString().trim()));
				if (index.size() > 0) {
					for (String[] i : index) field_sql.append(",\n" + "KEY `").append(i[0]).append("` (`").append(i[1]).append("`)");
				}
				sql += _trim(field_sql.toString().trim()) + "\n";
				sql += ")";
				if (dbType != 1) {
					String engine = tableInfo.get("table_engine") != null ? (String) tableInfo.get("table_engine") : "InnoDB";
					sql += " ENGINE=" + engine;
					if (tableInfo.get("table_auto_increment") != null) sql += " AUTO_INCREMENT=" + tableInfo.get("table_auto_increment");
					sql += " DEFAULT CHARSET=utf8";
					if (tableInfo.get("table_comment") != null) sql += " COMMENT='" + ((String) tableInfo.get("table_comment")).replace("'", "\\'") + "'";
				}
				sql += ";";
				sql = Db.replaceTable(sql);
			}
		}
		if (sql.length() > 0) Db.execute(sql);
	}
	//删除表, Db.name().tableRemove("table");
	public void tableRemove(String table) {
		Db.execute("DROP TABLE IF EXISTS `" + table + "`");
	}
	//复原参数
	public void restore() {
		this.table = "";
		this.alias = "";
		this.left = null;
		this.right = null;
		this.inner = null;
		this.cross = null;
		this.where = "";
		this.whereParams = null;
		this.field = "";
		this.distinct = "";
		this.order = "";
		this.group = "";
		this.having = "";
		this.offset = 0;
		this.pagesize = 0;
		this.cached = 0;
		this.pagination = false;
		this.paginationMark = "";
		this.fetchSql = false;
	}
	//创建指定表Map
	public static Map<String, Object> createInstanceMap(String table) {
		Map<String, Object> map = new HashMap<>();
		try {
			String sql = "SHOW COLUMNS FROM " + Db.replaceTable(table);
			if (conn == null) Db.init(MASTER);
			ps = conn.prepareStatement(sql);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (rs.getString("Type").startsWith("int(")) {
					map.put(rs.getString("Field"), rs.getInt("Default"));
				} else if (rs.getString("Type").startsWith("decimal(")) {
					map.put(rs.getString("Field"), rs.getFloat("Default"));
				} else {
					String value = rs.getString("Default");
					if (value == null) value = "";
					map.put(rs.getString("Field"), value);
				}
			}
		} catch (Exception e) {
			System.out.println("DB创建指定表Map异常");
			e.printStackTrace();
		} finally {
			Db.close();
		}
		return map;
	}
	//创建指定表DataMap
	public static DataMap createInstanceDataMap(String table) {
		Map<String, Object> data = Db.createInstanceMap(table);
		return new DataMap(data);
	}
	//生成实例class文件
	//Db.createInstanceFile("member");
	public static void createInstanceFile(String table) {
		try {
			String clazz = Common.camelize(table);
			table = Db.replaceTable(table);
			String sql = "SHOW COLUMNS FROM " + table;
			if (conn == null) Db.init(MASTER);
			ps = conn.prepareStatement(sql);
			ResultSet rs = ps.executeQuery();
			StringBuilder content = new StringBuilder("package com.app.model;\n\n").append("public class ").append(clazz).append(" extends Core {\n\n");
			StringBuilder method = new StringBuilder();
			String clazzName = new Object() {
				public String getClassName() {
					String clazzName = this.getClass().getName();
					return clazzName.substring(0, clazzName.lastIndexOf('$'));
				}
			}.getClassName();
			String src = clazzName.substring(0, clazzName.indexOf(".app.framework")).replace(".", "/");
			while (rs.next()) {
				String field = rs.getString("Field");
				String type = rs.getString("Type");
				if (type.startsWith("int(")) type = "Integer";
				else if (type.startsWith("varchar(") || type.equals("text")) type = "String";
				else if (type.startsWith("decimal(")) type = "Double";
				content.append("\tpublic ").append(type).append(" ").append(field).append(";\n");
				String Field = Character.toUpperCase(field.charAt(0)) + field.substring(1);
				method.append("\n\tpublic ").append(type).append(" get").append(Field).append("() {\n\t\treturn ").append(field).append(";\n\t}\n");
				method.append("\tpublic void set").append(Field).append("(").append(type).append(" ").append(field).append(") {\n\t\tthis.")
						.append(field).append(" = ").append(field).append(";\n\t}\n");
			}
			content.append(method).append("\n}");
			FileWriter writer = new FileWriter(new File(rootPath).getParent()+"/src/main/java/"+src+"/app/model/" + clazz + ".java");
			writer.write(content.toString());
			writer.close();
		} catch (Exception e) {
			System.out.println("DB生成实例class文件异常");
			e.printStackTrace();
		} finally {
			Db.close();
		}
	}
	/*
	多个update操作共同协同工作时使用事务
	Db.startTransaction(); //开启事务
	if (Db.update(xxx) == -1) {
		Db.rollback();
	    return false; //失败，返回
	}
	if (Db.update(xxx) == -1) {
		Db.rollback();
	    return false; //失败，返回
	}
	Db.commit(); //成功，提交事务
	*/
	//开启事务
	public static void startTransaction() {
		try {
			if (conn == null) Db.init(SLAVER);
			conn.setAutoCommit(false);
		} catch (SQLException e) {
			System.out.println("DB事务关闭自动提交异常");
			e.printStackTrace();
		}
	}
	//回滚事务
	public static void rollback() {
		try {
			conn.rollback();
		} catch (SQLException e) {
			System.out.println("DB回滚事务异常");
			e.printStackTrace();
		} finally {
			try {
				conn.setAutoCommit(true);
			} catch (SQLException e) {
				System.out.println("DB事务开启自动提交异常");
				e.printStackTrace();
			}
			Db.close();
		}
	}
	//提交事务
	public static void commit() {
		try {
			conn.commit();
		} catch (SQLException e) {
			System.out.println("DB提交事务异常");
			e.printStackTrace();
		} finally {
			try {
				conn.setAutoCommit(true);
			} catch (SQLException e) {
				System.out.println("DB事务开启自动提交异常");
				e.printStackTrace();
			}
			Db.close();
		}
	}
	public static void close() {
		Db.connect();
		try {
			if (ps != null) { //仅需关闭 PreparedStatement，关闭它时 ResultSet 会自动关闭
				ps.close();
				ps = null;
			}
			if (conn != null) {
				conn.close();
				conn = null;
			}
		} catch (SQLException e) {
			System.out.println("DB关闭异常");
			e.printStackTrace();
		}
	}
}
