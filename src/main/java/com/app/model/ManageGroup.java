package com.app.model;

import com.framework.tool.*;
import java.util.*;

public class ManageGroup extends Core {

	public Integer id;
	public String name;
	public String menu;
	public String permission;
	public Integer power;
	public Integer add_time;

	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public String getMenu() {
		return menu;
	}
	public void setMenu(String menu) {
		this.menu = menu;
	}

	public String getPermission() {
		return permission;
	}
	public void setPermission(String permission) {
		this.permission = permission;
	}

	public Integer getPower() {
		return power;
	}
	public void setPower(Integer power) {
		this.power = power;
	}

	public Integer getAdd_time() {
		return add_time;
	}
	public void setAdd_time(Integer add_time) {
		this.add_time = add_time;
	}

	//数据库操作(自动设定表名)===================================================
	public static String tablename() {
		String clazz = new Object() {
			public String get() {
				String clazz = this.getClass().getName();
				return clazz.substring(0, clazz.lastIndexOf('$'));
			}
		}.get();
		return Common.uncamelize(clazz.substring(clazz.lastIndexOf(".")+1));
	}
	public static Db leftJoin(String table, String on) {
		return Db.name(tablename()).leftJoin(table, on);
	}
	public static Db rightJoin(String table, String on) {
		return Db.name(tablename()).rightJoin(table, on);
	}
	public static Db innerJoin(String table, String on) {
		return Db.name(tablename()).innerJoin(table, on);
	}
	public static Db crossJoin(String table) {
		return Db.name(tablename()).crossJoin(table);
	}
	public static Db where(Object where, Object...whereParams) {
		return Db.name(tablename()).where(where, whereParams);
	}
	public static Db whereOr(Object where, Object...whereParams) {
		return Db.name(tablename()).whereOr(where, whereParams);
	}
	public static Db field(Object field) {
		return Db.name(tablename()).field(field);
	}
	public static Db distinct(String field) {
		return Db.name(tablename()).distinct(field);
	}
	public static Db whereTime(String interval, String field, String operatorAndValue) {
		return Db.name(tablename()).whereTime(interval, field, operatorAndValue);
	}
	public static Db whereTime(String interval, String field, String operatorAndValue, String now) {
		return Db.name(tablename()).whereTime(interval, field, operatorAndValue, now);
	}
	public static Db like(String field, String str) {
		return Db.name(tablename()).like(field, str);
	}
	public static Db like(String field, String str, String escape) {
		return Db.name(tablename()).like(field, str, escape);
	}
	public static Db order(String field) {
		return Db.name(tablename()).order(field);
	}
	public static Db order(String field, String order) {
		return Db.name(tablename()).order(field, order);
	}
	public static Db orderField(String field, String value) {
		return Db.name(tablename()).orderField(field, value);
	}
	public static Db group(String group) {
		return Db.name(tablename()).group(group);
	}
	public static Db having(String having) {
		return Db.name(tablename()).having(having);
	}
	public static Db offset(int offset) {
		return Db.name(tablename()).offset(offset);
	}
	public static Db pagesize(int pagesize) {
		return Db.name(tablename()).pagesize(pagesize);
	}
	public static Db limit(int offset, int pagesize) {
		return Db.name(tablename()).limit(offset, pagesize);
	}
	public static Db cached(int cached) {
		return Db.name(tablename()).cached(cached);
	}
	public static Db pagination(boolean pagination) {
		return Db.name(tablename()).pagination(pagination);
	}
	public static Db pagination(boolean pagination, String paginationMark) {
		return Db.name(tablename()).pagination(pagination, paginationMark);
	}
	public static Db fetchSql() {
		return Db.name(tablename()).fetchSql();
	}
	public static DataList select(Object field) {
		return Db.name(tablename()).select(field);
	}
	public static DataList select() {
		return Db.name(tablename()).select();
	}
	public static int insert(String data, Object...dataParams) {
		return Db.name(tablename()).insert(data, dataParams);
	}
	public static int insert(List<String> data, Object...dataParams) {
		return Db.name(tablename()).insert(data, dataParams);
	}
	public static int insert(String[] data, Object...dataParams) {
		return Db.name(tablename()).insert(data, dataParams);
	}
	public static int insert(Map<String, Object> datas) {
		return Db.name(tablename()).insert(datas);
	}
	public static int insert(String[] data, List<Object> dataParams) {
		return Db.name(tablename()).insert(data, dataParams);
	}
	public static int insertGetId(String data, Object...dataParams) {
		return Db.name(tablename()).insertGetId(data, dataParams);
	}
	public static int insertGetId(List<String> data, Object...dataParams) {
		return Db.name(tablename()).insertGetId(data, dataParams);
	}
	public static int insertGetId(String[] data, Object...dataParams) {
		return Db.name(tablename()).insertGetId(data, dataParams);
	}
	public static int insertGetId(Map<String, Object> datas) {
		return Db.name(tablename()).insertGetId(datas);
	}
	public static int insertGetId(String[] data, List<Object> dataParams) {
		return Db.name(tablename()).insertGetId(data, dataParams);
	}

}