package com.app.index;

import com.alibaba.fastjson.*;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.framework.Base;
import com.framework.tool.*;
import javax.servlet.http.*;
import java.io.PrintWriter;
import java.util.*;

public class Core extends Base {
	public Integer memberId;
	public DataMap memberObj;
	public String token;
	public static JSONObject not_check_login;

	public void __construct(HttpServletRequest request, HttpServletResponse response) {
		super.__construct(request, response);

		Object access_allow = Common.getYml("sdk.access.allow", null, Object.class);
		if ( access_allow != null ) {
			String accessAllowHost = null;
			JSONArray access_allow_host = Common.getYml("sdk.access.allow.host", null, JSONArray.class);
			if ( access_allow_host != null ) {
				if (access_allow_host.size() == 1 && access_allow_host.get(0).equals("*")) accessAllowHost = "*";
				else {
					String host = Common.domain();
					if (access_allow_host.contains(host)) accessAllowHost = host;
				}
			}
			boolean isContains = false;
			if (access_allow instanceof JSONArray) {
				JSONArray accessAllow = (JSONArray) access_allow;
				if (accessAllow.size() == 1 && (accessAllow.get(0) instanceof String) && accessAllow.get(0).equals("*")) isContains = true;
			} else if (access_allow instanceof JSONObject) {
				JSONObject accessAllow = (JSONObject) access_allow;
				if (accessAllow.getJSONArray(this.app) != null && accessAllow.getJSONArray(this.app).size() > 0 &&
						(accessAllow.getJSONArray(this.app).contains("*") || accessAllow.getJSONArray(this.app).contains(this.act))) isContains = true;
			}
			if (accessAllowHost != null && isContains) {
				this.response.setHeader("Access-Control-Allow-Origin", accessAllowHost);
				//this.response.setHeader("Access-Control-Allow-Origin", "*"); //允许所有地址跨域请求
				this.response.setHeader("Access-Control-Allow-Methods", "*"); //设置允许的请求方法, *表示所有, POST,GET,OPTIONS,DELETE
				this.response.setHeader("Access-Control-Allow-Credentials", "true"); //设置允许请求携带cookie, 此时origin不能用*
				this.response.setHeader("Access-Control-Allow-Headers", "x-requested-with,content-type,token,sign"); //设置头
			}
		}

		this.memberId = 0;
		this.token = this.headers.get("token");
		if (this.token == null || this.token.length() == 0) this.token = this.request.get("token");
		if (this.token == null) this.token = "";
		if (this.token.length() > 0) this._check_login();
		
		DataMap member = sessionDataMap("member");
		if (member != null) {
			this.token = member.getString("token");
			this.memberId = member.getInt("id");
			this.memberObj = member;
			member = Db.name("member").where("id=?", this.memberId).field("status").find();
			if (member == null) {
				session("member", null);
				this.token = "";
				this.memberId = 0;
				this.memberObj = null;
			} else if (member.getInt("status") != 1) {
				this.not_login("账号已被冻结", -1);
			}
		}
		if (this.memberId <= 0) {
			if (not_check_login == null) not_check_login = Common.getYml("sdk.not.check.login", new JSONObject());
			if ( !not_check_login.isEmpty() ) {
				JSONArray param = not_check_login.getJSONArray(this.app);
				if ( param == null || param.isEmpty() ) {
					this.check_login();
				} else {
					if ( !param.contains("*") && !param.contains(this.act) ) {
						this.check_login();
					} else if ( this.headers.get("Authorization") != null && this.headers.get("Authorization").length() > 0 ) {
						this.check_login();
					}
				}
			}
		}
	}

	//get member info from token
	public DataMap get_member_from_token(String token) {
		return get_member_from_token(token, false);
	}
	public DataMap get_member_from_token(String token, boolean is_session) {
		if (token == null || token.length() == 0) return null;
		if (this.memberObj == null || is_session) {
			DataMap member = Db.name("member").where("token='" + token + "'").field("*, null as grade").find();
			if (member == null) {
				member = sessionDataMap("member");
				if (member == null) {
					if (is_session) {
						errorWrite("该账号已在其他设备登录", -9);
					}
					return null;
				}
			}
			/*DataMap shop = Db.name("shop s").leftJoin("member m", "s.memberId=m.id").where("m.id='" + member.get("id") + "'").field("s.*").find();
			if (shop != null) {
				member.put("shop_id", shop.getInt("id"));
				member.put("shop", shop);
			}
			DataMap grade = Db.name("grade").where(Integer.parseInt(String.valueOf(member.get("grade_id")))).find();
			if (grade != null) {
				member.put("grade", grade);
			}*/
			DataList thirdparty = Db.name("member_thirdparty").where(member.getInt("id")).select();
			if (thirdparty != null) {
				for (DataMap t : thirdparty) {
					String type = t.getString("type") + "_openid";
					member.put(type, t.getString("mark"));
					if (t.getString("type").equals("wechat")) member.put("openid", t.getString("mark"));
				}
			}
			this.memberId = member.getInt("id");
			this.token = member.getString("token");
			member.put("total_price", member.getDouble("money") + member.getDouble("commission")); //总财富
			member = Common.add_domain_deep(member, new String[]{"avatar", "pic"});
			member.remove("origin_password");
			member.remove("salt");
			member.remove("withdraw_salt");
			session("member", member);
			this.memberObj = member;
		} else {
			DataMap member = this.memberObj;
			this.memberId = member.getInt("id");
			this.token = member.getString("sign");
		}
		return this.memberObj;
	}

	//是否登录
	public boolean _check_login() {
		DataMap member = sessionDataMap("member");
		if (member != null && member.getInt("id") > 0 && this.token.length() == 0) {
			return this.get_member_from_token(member.getString("token"), true) != null;
		} else if (this.token.length() > 0) {
			return this.get_member_from_token(this.token) != null;
		} else if (cookie("member_token") != null) {
			return this.get_member_from_token(cookie("member_token")) != null;
		} else if (this.headers.get("Authorization") != null && this.headers.get("Authorization").length() > 0) {
			if (this.headers.get("Authorization").toLowerCase().contains("basic")) {
				String sign = Common.base64_decode(this.headers.get("Authorization").substring(6));
				if (sign.length() > 0) return this.get_member_from_token(sign) != null;
			}
		}
		return false;
	}

	//对是否登录函数的封装，如果登录了，则返回true，
	//否则，返回错误信息：-100，APP需检查此返回值，判断是否需要重新登录
	public boolean check_login(){
		if (!this._check_login()) {
			this.appKeepRunning = false;
			session("api_gourl", Common.url());
			Object ret = Common.error("请登录", -100);
			try {
				if (ret instanceof String) {
					if (((String)ret).startsWith("tourl:")) {
						this.response.sendRedirect(((String) ret).replaceFirst("tourl:", ""));
					} else {
						PrintWriter out = this.response.getWriter();
						out.write((String) ret);
						out.close();
					}
				} else {
					PrintWriter out = this.response.getWriter();
					out.write(JSON.toJSONString(ret, SerializerFeature.WriteMapNullValue));
					out.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return false;
		}
		return true;
	}
	
	//没有登录处理
	public void not_login() {
		this.not_login("请登录", -2);
	}
	public void not_login(String msg, int code) {
		if (Common.isAjax()) {
			error(msg, code);
		} else {
			this.redirect("/" + this.module + "/passport/login");
		}
	}
	
	//输出模板
	public Object render(Object data) {
		return this.render(data, "");
	}
	public Object render(Object data, String template_file) {
		if (Common.isAjax()) {
			return success(data);
		}
		JSONObject dataObj = null;
		if (data instanceof Map) {
			dataObj = JSONObject.parseObject(JSON.toJSONString(data));
		}
		if (dataObj == null) dataObj = new JSONObject();
		dataObj.put("member", this.memberObj);
		if (template_file != null && strlen(template_file)) {
			if (preg_match("\\.html$", template_file)) { //.html结尾自动获取完整路径
				template_file = Common.routeUrl(template_file.substring(0, template_file.length() - 5)) + ".html";
			} else {
				template_file = Common.routeUrl(template_file) + ".html";
			}
		} else {
			template_file = "success";
		}
		return success(null, template_file, dataObj);
	}

}
