package com.app.model;

import com.framework.tool.*;
import java.lang.reflect.*;
import java.util.*;

public class Article extends Core {
	
	//获取文章详情
	public DataMap detail(int id) {
		return com.app.model.Article.where("status=1 AND id="+id).find();
	}

	//分类列表
	public DataList categories() {
		return categories(0);
	}
	public DataList categories(int parent_id) {
		DataList rs = com.app.model.ArticleCategory.where("status=1 AND parent_id="+parent_id).order("sort ASC, id ASC").field("*, NULL as categories").select();
		if (rs != null) {
			for (DataMap g : rs) {
				if (g.getInt("parent_id") > 0) g.put("categories", this.categories(g.getInt("parent_id")));
			}
			rs = Common.add_domain_deep(rs, "pic");
		}
		return rs;
	}

	//关联图片
	public DataList pics(int article_id, int limit) {
		return null;
		//return com.app.model.ArticlePic.where("article_id="+article_id).order("id ASC").pagesize(limit).field("pic").select();
	}

	//关联商品
	public DataList goods(int article_id) {
		return null;
		//return com.app.model.ArticleGoods.alias("ag").leftJoin("goods g", "goods_id=g.id").where("article_id="+article_id).order("ag.id ASC").select("g.id, g.name, g.model, g.pic, g.price");
	}

	//是否点赞
	public int liked(int member_id, int article_id) {
		if (member_id == 0) return 0;
		return 0;
		//return com.app.model.ArticleLike.where("article_id='"+article_id+"' AND member_id='"+member_id+"'").count();
	}
	
	//数据库操作(自动设定表名)===================================================
	public static String connectname() {
		String connection = "";
		Field[] fields = Article.class.getDeclaredFields();
		try {
			for (Field field : fields) {
				field.setAccessible(true);
				if (Modifier.isStatic(field.getModifiers())) {
					if (field.getName().equals("connection")) connection = (String) field.get(Article.class);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (connection.length() > 0) return connection;
		return null;
	}
	public static String tablename() {
		String name = "";
		String table = "";
		Field[] fields = Article.class.getDeclaredFields();
		try {
			for (Field field : fields) {
				field.setAccessible(true);
				if (Modifier.isStatic(field.getModifiers())) {
					if (field.getName().equals("name")) name = (String) field.get(Article.class);
					if (field.getName().equals("table")) table = (String) field.get(Article.class);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (name.length() > 0) return name;
		if (table.length() > 0) return table;
		String clazz = new Object() {
			public String get() {
				String clazz = this.getClass().getName();
				return clazz.substring(0, clazz.lastIndexOf('$'));
			}
		}.get();
		return Common.uncamelize(clazz.substring(clazz.lastIndexOf(".")+1));
	}
	public static Db alias(String alias) {
		Db.connect(connectname());return Db.name(tablename()).alias(alias);
	}
	public static Db leftJoin(String table, String on) {
		Db.connect(connectname());return Db.name(tablename()).leftJoin(table, on);
	}
	public static Db rightJoin(String table, String on) {
		Db.connect(connectname());return Db.name(tablename()).rightJoin(table, on);
	}
	public static Db innerJoin(String table, String on) {
		Db.connect(connectname());return Db.name(tablename()).innerJoin(table, on);
	}
	public static Db crossJoin(String table) {
		Db.connect(connectname());return Db.name(tablename()).crossJoin(table);
	}
	public static Db where(Object where, Object...whereParams) {
		Db.connect(connectname());return Db.name(tablename()).where(where, whereParams);
	}
	public static Db whereOr(Object where, Object...whereParams) {
		Db.connect(connectname());return Db.name(tablename()).whereOr(where, whereParams);
	}
	public static Db whereDay(String field, String mark) {
		Db.connect(connectname());return Db.name(tablename()).whereDay(field, mark);
	}
	public static Db whereTime(String field, String value) {
		Db.connect(connectname());return Db.name(tablename()).whereTime(field, value);
	}
	public static Db whereTime(String field, String operator, String value) {
		Db.connect(connectname());return Db.name(tablename()).whereTime(field, operator, value);
	}
	public static Db whereTime(String interval, String field, String operator, Object value) {
		Db.connect(connectname());return Db.name(tablename()).whereTime(interval, field, operator, value);
	}
	public static Db field(Object field) {
		Db.connect(connectname());return Db.name(tablename()).field(field);
	}
	public static Db distinct(String field) {
		Db.connect(connectname());return Db.name(tablename()).distinct(field);
	}
	public static Db like(String field, String str) {
		Db.connect(connectname());return Db.name(tablename()).like(field, str);
	}
	public static Db like(String field, String str, String escape) {
		Db.connect(connectname());return Db.name(tablename()).like(field, str, escape);
	}
	public static Db order(String field) {
		Db.connect(connectname());return Db.name(tablename()).order(field);
	}
	public static Db order(String field, String order) {
		Db.connect(connectname());return Db.name(tablename()).order(field, order);
	}
	public static Db orderField(String field, String value) {
		Db.connect(connectname());return Db.name(tablename()).orderField(field, value);
	}
	public static Db group(String group) {
		Db.connect(connectname());return Db.name(tablename()).group(group);
	}
	public static Db having(String having) {
		Db.connect(connectname());return Db.name(tablename()).having(having);
	}
	public static Db offset(int offset) {
		Db.connect(connectname());return Db.name(tablename()).offset(offset);
	}
	public static Db pagesize(int pagesize) {
		Db.connect(connectname());return Db.name(tablename()).pagesize(pagesize);
	}
	public static Db limit(int pagesize) {
		Db.connect(connectname());return Db.name(tablename()).limit(pagesize);
	}
	public static Db limit(int offset, int pagesize) {
		Db.connect(connectname());return Db.name(tablename()).limit(offset, pagesize);
	}
	public static Db cached(int cached) {
		Db.connect(connectname());return Db.name(tablename()).cached(cached);
	}
	public static Db pagination() {
		Db.connect(connectname());return Db.name(tablename()).pagination();
	}
	public static Db pagination(String paginationMark) {
		Db.connect(connectname());return Db.name(tablename()).pagination(paginationMark);
	}
	public static Db fetchSql() {
		Db.connect(connectname());return Db.name(tablename()).fetchSql();
	}
	public static boolean exist() {
		Db.connect(connectname());return Db.name(tablename()).exist();
	}
	public static int count() {
		Db.connect(connectname());return Db.name(tablename()).count();
	}
	public static DataList select(Object field) {
		Db.connect(connectname());return Db.name(tablename()).select(field);
	}
	public static DataList select() {
		Db.connect(connectname());return Db.name(tablename()).select();
	}
	public static int insert(String data, Object...dataParams) {
		Db.connect(connectname());return Db.name(tablename()).insert(data, dataParams);
	}
	public static int insert(List<String> data, Object...dataParams) {
		Db.connect(connectname());return Db.name(tablename()).insert(data, dataParams);
	}
	public static int insert(String[] data, Object...dataParams) {
		Db.connect(connectname());return Db.name(tablename()).insert(data, dataParams);
	}
	public static int insert(Map<String, Object> datas) {
		Db.connect(connectname());return Db.name(tablename()).insert(datas);
	}
	public static int insert(String[] data, List<Object> dataParams) {
		Db.connect(connectname());return Db.name(tablename()).insert(data, dataParams);
	}
	public static int insertGetId(String data, Object...dataParams) {
		Db.connect(connectname());return Db.name(tablename()).insertGetId(data, dataParams);
	}
	public static int insertGetId(List<String> data, Object...dataParams) {
		Db.connect(connectname());return Db.name(tablename()).insertGetId(data, dataParams);
	}
	public static int insertGetId(String[] data, Object...dataParams) {
		Db.connect(connectname());return Db.name(tablename()).insertGetId(data, dataParams);
	}
	public static int insertGetId(Map<String, Object> datas) {
		Db.connect(connectname());return Db.name(tablename()).insertGetId(datas);
	}
	public static int insertGetId(String[] data, List<Object> dataParams) {
		Db.connect(connectname());return Db.name(tablename()).insertGetId(data, dataParams);
	}
	
}
