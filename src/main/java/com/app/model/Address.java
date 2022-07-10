package com.app.model;

import com.framework.tool.*;

public class Address extends Core {
	
	public static String tablename() {
		String clazz = new Object() {
			public String get() {
				String clazz = this.getClass().getName();
				return clazz.substring(0, clazz.lastIndexOf('$'));
			}
		}.get();
		return com.framework.tool.Common.uncamelize(clazz.substring(clazz.lastIndexOf(".")+1));
	}
	
	//获取默认地址，已经登录的情况下才返回
	public DataMap default_address(int member_id) {
		DataMap address;
		if (member_id>0) {
			address = Db.name("address").where("member_id='"+member_id+"'").order("is_default DESC, id DESC").find();
			if (address == null) address = this._init_address();
		} else {
			address = this._init_address();
		}
		return address;
	}

	//初始化一个地址对象。
	private DataMap _init_address() {
		return Db.createInstanceDataMap("address");
	}
}
