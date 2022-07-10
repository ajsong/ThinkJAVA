package com.app.model;


public class Commission extends Core {
	
	public static String tablename() {
		String clazz = new Object() {
			public String get() {
				String clazz = this.getClass().getName();
				return clazz.substring(0, clazz.lastIndexOf('$'));
			}
		}.get();
		return com.framework.tool.Common.uncamelize(clazz.substring(clazz.lastIndexOf(".")+1));
	}

}
