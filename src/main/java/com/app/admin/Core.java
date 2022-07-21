package com.app.admin;

import com.alibaba.fastjson.*;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.framework.Base;
import com.framework.closure.*;
import com.framework.tool.*;
import org.apache.commons.lang3.StringUtils;
import javax.servlet.http.*;
import java.io.PrintWriter;
import java.util.*;

public class Core extends Base {
	public Integer manageId;
	public DataMap manageObj;
	
	public void __construct(HttpServletRequest request, HttpServletResponse response) {
		super.__construct(request, response);
		
		this.manageId = 0;
		
		//判断登录
		if (!this.check_login()) {
			return;
		}
		
		//判断冻结
		if (this.manageObj.getInt("status") != 1) {
			this.appKeepRunning = false;
			error("账号已被禁用", -1);
		}
		
		//判断权限
		if (!this.check_permission(this.app, this.act)) {
			this.appKeepRunning = false;
			error("没有操作权限，请联系超级管理员");
		}
	}
	
	//是否登录
	public boolean _check_login() {
		DataMap manage = sessionDataMap("manage");
		if ( manage != null ) {
			this.manageId = manage.getInt("id");
			this.manageObj = manage;
			return true;
		} else if ( cookie("manage_token") != null ) {
			manage = com.app.model.Manage.where("token=?", cookie("manage_token")).find();
			if (manage != null) {
				manage.remove("password");
				manage.remove("salt");
				this.manageId = manage.getInt("id");
				this.manageObj = manage;
				session("manage", manage);
				return true;
			}
			cookie("manage_token", null);
		}
		return false;
	}
	
	//对是否登录函数的封装，如果登录了，则返回true，
	//否则，返回错误信息：-100，APP需检查此返回值，判断是否需要重新登录
	public boolean check_login(){
		if (!this._check_login()) {
			this.appKeepRunning = false;
			session("manage_gourl", Common.url());
			Object ret = error("登录失效", -2);
			try {
				if (ret instanceof String) {
					if (preg_match("^(tourl|redirect):", String.valueOf(ret))) {
						this.redirect("/" + this.module + "/login");
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
	
	private DataList menu() {
		String cacheKey = "manage:menu:" + this.manageId;
		return Cache.remember(cacheKey, () -> {
			String[] menuIds = Core.this.getMenuIds();
			if (Arrays.asList(menuIds).contains("all")) {
				return Core.this.getAllMenus();
			} else {
				return Core.this.getAllMenus(0, menuIds);
			}
		});
	}
	
	public String[] getMenuIds() {
		DataMap group = com.app.model.ManageGroup.where(this.manageObj.getInt("group_id")).field("menu").find();
		if (group == null) return new String[]{"0"};
		if (group.getString("menu").equals("all")) {
			return new String[]{"all"};
		} else {
			return group.getString("menu").split(",");
		}
	}
	
	public DataList getAllMenus() {
		return getAllMenus(0, null);
	}
	public DataList getAllMenus(int parentId, String[] menuIds) {
		Db menu = com.app.model.Menu.where("parent_id=? and level>0", parentId);
		if (menuIds != null) menu.where("id in ("+ StringUtils.join(menuIds, ",") +")");
		return menu.order("sort, id").field("*, null as children").select().each((CallbackDataMap)(DataMap item)->{
			Db children = com.app.model.Menu.where("parent_id=? and level>0", item.getInt("id"));
			if (menuIds != null) children.where("id in ("+ StringUtils.join(menuIds, ",") +")");
			int count = children.count();
			if (count > 0) {
				item.put("children", this.getAllMenus(item.getInt("id"), menuIds));
			}
		});
	}
	
	//检查权限
	public boolean check_permission(String app, String act) {
		if (this.manageObj.getInt("super") == 1) return true;
		if (app.equals("index")) return true;
		if (app.equals("upload")) return true;
		if (app.equals("menu") || (app.equals("config") && act.equals("log"))) return false;
		String[] menuIds = this.getMenuIds();
		if (Arrays.asList(menuIds).contains("all")) return true;
		String path = com.app.model.Menu.getRoutePath();
		DataMap power = com.app.model.Menu.where("path=?", "/"+path).field("id").find();
		if (power != null && Arrays.asList(menuIds).contains(power.getString("id"))) return true;
		power = com.app.model.ManageGroup.where("id="+this.manageObj.getInt("group_id")+" and CONCAT('|',permission,'|') LIKE '%|"+app+":"+act+"|%'").find();
		return power != null;
	}
	
	//输出模板
	public Object render(Object data) {
		return this.render(data, "");
	}
	public Object render(Object data, String template_file) {
		JSONObject dataObj = null;
		if (data instanceof Map) {
			dataObj = JSONObject.parseObject(JSON.toJSONString(DataMap.dataToMap(data)));
		}
		if (dataObj == null) dataObj = new JSONObject();
		dataObj.put("manage", this.manageObj);
		dataObj.put("leftMenu", this.menu());
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
	
	//模板用方法==================================================
	//获取当前路径
	public static boolean checkMenuPath(String path) {
		if (path == null || path.length() == 0) return false;
		String routeUrl = Common.routeUrl();
		String[] routeArr = Common.trim(routeUrl, "/").split("/");
		routeUrl = routeArr[0] + "/" + routeArr[1];
		path = Common.routeUrl(path);
		String[] pathArr = Common.trim(path, "/").split("/");
		path = pathArr[0] + "/" + pathArr[1];
		return routeUrl.equals(path);
	}
	//检查权限
	public static boolean permission(String app, String act) {
		DataMap manage = Common.sessionDataMap("manage");
		if (manage == null) return false;
		if (manage.getInt("super") == 1) return true;
		DataMap group = com.app.model.ManageGroup.where("id", manage.getInt("group_id")).field("menu").find();
		if (group == null) return false;
		String[] menuIds = group.getString("menu").equals("all") ? new String[]{"all"} : group.getString("menu").split(",");
		if (Arrays.asList(menuIds).contains("all")) return true;
		DataMap power = com.app.model.ManageGroup.where("id", manage.getInt("group_id")).where("CONCAT('|',permission,'|') LIKE '%|"+app+":"+act+"|%'").find();
		return power != null;
	}
	
}
