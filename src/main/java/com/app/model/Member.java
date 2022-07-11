package com.app.model;

import com.framework.tool.*;
import java.util.*;

public class Member extends Core {
	
	//积分抵扣
	//order_min_price, 订单满多少元才可用
	//order_min_integral, 会员现时最少多少积分才可用
	//order_integral_money, 多少积分抵扣1元
	//order_integral_total_percent, 积分只可抵扣总价格的百分率，小数形式
	public boolean order_integral_check(int member_id, float price){
		if (member_id<=0 ||
				this.configs.get("order_min_price") == null ||
				this.configs.get("order_min_integral") == null ||
				this.configs.get("order_integral_money") == null ||
				this.configs.get("order_integral_total_percent") == null) return false;
		//订单总价是否满使用积分的价格
		if (price < Float.parseFloat(this.configs.get("order_min_price"))) return false;
		//会员积分是否达标
		int integral = com.app.model.Member.where(member_id).value("integral", Integer.class);
		return integral >= Integer.parseInt(this.configs.get("order_min_integral"));
	}

	//检测使用积分抵扣
	//成功，返回对象，否则null
	public DataMap check_pay_with_integral(int member_id, float price) {
		if (member_id<=0 ||
				this.configs.get("order_min_price") == null ||
				this.configs.get("order_min_integral") == null ||
				this.configs.get("order_integral_money") == null ||
				this.configs.get("order_integral_total_percent") == null) return null;
		int integral = com.app.model.Member.where(member_id).value("integral", Integer.class);
		DataMap integral_pay = new DataMap();
		//积分最多可抵现
		float integral_money = price * Float.parseFloat(this.configs.get("order_integral_total_percent"));
		//用户积分不够扣除即获取积分最多可抵金额
		if (integral < Math.ceil(integral_money * Float.parseFloat(this.configs.get("order_integral_money")))) {
			integral_money = integral / Float.parseFloat(this.configs.get("order_integral_money"));
		}
		integral_pay.put("integral", Math.ceil(integral_money * Float.parseFloat(this.configs.get("order_integral_money"))));
		integral_pay.put("money", integral_money);
		return integral_pay;
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
	public static Db alias(String alias) {
		return Db.name(tablename()).alias(alias);
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
	public static Db whereDay(String field, String mark) {
		return Db.name(tablename()).whereDay(field, mark);
	}
	public static Db whereTime(String field, String value) {
		return Db.name(tablename()).whereTime(field, value);
	}
	public static Db whereTime(String field, String operator, String value) {
		return Db.name(tablename()).whereTime(field, operator, value);
	}
	public static Db whereTime(String interval, String field, String operator, Object value) {
		return Db.name(tablename()).whereTime(interval, field, operator, value);
	}
	public static Db field(Object field) {
		return Db.name(tablename()).field(field);
	}
	public static Db distinct(String field) {
		return Db.name(tablename()).distinct(field);
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
	public static Db pagination() {
		return Db.name(tablename()).pagination();
	}
	public static Db pagination(String paginationMark) {
		return Db.name(tablename()).pagination(paginationMark);
	}
	public static Db fetchSql() {
		return Db.name(tablename()).fetchSql();
	}
	public static boolean exist() {
		return Db.name(tablename()).exist();
	}
	public static int count() {
		return Db.name(tablename()).count();
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
