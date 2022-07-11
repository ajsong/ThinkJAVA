package com.app.admin;

import com.framework.Base;
import com.framework.tool.*;

public class Login extends Base {
	public Object index() {
		if (Common.isPost()) {
			String username = this.request.post("username");
			String password = this.request.post("password");
			int remember = this.request.post("remember", 0);
			if (username.length()==0 || password.length()==0) return error("请输入账号与密码");
			DataMap manage = com.app.model.Manage.where("name=?", username).find();
			if (manage == null) return error("用户不存在");
			if (!Common.crypt_password(password, manage.getString("salt")).equals(manage.getString("password"))) return error("账号或密码错误");
			if (manage.getInt("status") == 0) return error("当前账号已被冻结");
			manage.remove("password");
			manage.remove("salt");
			String token = Common.generate_token();
			com.app.model.Manage.where(manage.getInt("id")).update("token", token);
			session("manage", manage);
			if (remember == 1) {
				cookie("manage_token", token, 60*60*24*365);
			}
			String url = session("manage_gourl", String.class);
			if (url != null && url.length() > 0) {
				session("manage_gourl", null);
				return success("tourl:"+url);
			}
			return success("tourl:/"+this.module+"/index");
		}
		return success();
	}
	
	public void logout() {
		if (cookie("manage_token") != null) cookie("manage_token", null);
		session("manage", null);
		this.redirect("/"+this.module+"/login");
	}
}
