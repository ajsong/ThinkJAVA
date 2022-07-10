package com.app.fxvymr7h;

import com.framework.closure.*;
import com.framework.tool.*;
import java.util.*;

public class Menu extends Core {
	public Object index() {
		DataList list = Db.name("menu").where("parent_id", 0).field("*, null as children").order("sort, id").select().each((CallbackDataMap)(DataMap item)->{
			item.put("children", Db.name("menu").where("parent_id", item.getInt("id")).select());
		});
		return this.render(new HashMap<String, Object>(){
			{
				put("list", list);
				put("parent", list);
			}
		});
	}
	public Object add() {
		return this.edit();
	}
	public Object edit() {
		int id = this.request.get("id", 0);
		if (Common.isPost()) {
			id = this.request.post("id", 0);
			String name = this.request.post("name");
			int parent_id = this.request.post("parent_id", 0);
			int sort = this.request.post("sort", 999);
			String path = this.request.post("path");
			String icon = this.request.post("icon");
			Map<String, Object> data = new HashMap<>();
			data.put("name", name);
			data.put("sort", sort);
			data.put("path", path);
			data.put("icon", icon);
			int level = 0;
			if (parent_id == 0) {
				data.put("parent_id", parent_id);
				level = 1;
			} else if (parent_id > 0) {
				data.put("parent_id", parent_id);
				level = 2;
			}
			data.put("level", level);
			if (id > 0) {
				Db.name("menu").where("id", id).update(data);
			} else {
				Db.name("menu").insert(data);
			}
			Db.name("manage").where("status", 1).field("id").select().each((CallbackDataMap)(DataMap item)->{
				Cache.delete("manage:menu:" + item.getString("id"));
			});
			return success("tourl:menu/index", "提交成功");
		}
		DataMap row;
		if (id > 0) {
			row = Db.name("menu").where("id", id).find();
		} else {
			row = Db.createInstanceDataMap("menu");
		}
		DataList parent = Db.name("menu").where("parent_id", 0).order("sort, id").select();
		return this.render(new HashMap<String, Object>() {
			{
				put("row", row);
				put("parent", parent);
			}
		}, "edit");
	}
	
	public Object edit_all() {
		DataList param = this.request.post("param", new DataList());
		if (param == null) return error("数据错误");
		for (DataMap g : param) {
			Map<String, Object> data = new HashMap<>();
			data.put("name", g.getString("name"));
			data.put("sort", g.getString("sort"));
			data.put("path", g.getString("path", ""));
			data.put("icon", g.getString("icon", ""));
			int level = 0;
			if (g.getInt("parent_id") == 0) {
				data.put("parent_id", g.getInt("parent_id"));
				level = 1;
			} else if (g.getInt("parent_id") > 0) {
				data.put("parent_id", g.getInt("parent_id"));
				level = 2;
			}
			data.put("level", level);
			if (g.getInt("id", 0) > 0) {
				Db.name("menu").where("id", g.getInt("id")).update(data);
			//} else {
				//Db.name("menu").insert(data);
			}
		}
		Db.name("manage").where("status", 1).field("id").select().each((CallbackDataMap)(DataMap item)->{
			Cache.delete("manage:menu:" + item.getString("id"));
		});
		return success(null, "提交成功");
	}
	
	public Object delete() {
		int id = this.request.get("id", 0);
		if (id <= 0) return error("数据错误");
		Db.name("menu").where("id", id).delete();
		Db.name("menu").where("parent_id", id).delete();
		Db.name("manage").where("status", 1).field("id").select().each((CallbackDataMap)(DataMap item)->{
			Cache.delete("manage:menu:" + item.getString("id"));
		});
		return success(null, "操作成功");
	}
}
