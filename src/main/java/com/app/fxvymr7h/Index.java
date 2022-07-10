package com.app.fxvymr7h;

import com.framework.tool.*;
import java.util.*;

public class Index extends Core {
	
	public Object index() {
		//会员数量
		Map<String, Object> map = new HashMap<>();
		map.put("total", Db.name("member").count());
		map.put("today", Db.name("member").where("reg_time>=? and reg_time<=?", Common.date("Y-m-d 00:00:00"), Common.date("Y-m-d 23:59:59")).count());
		return this.render(new HashMap<String, Object>(){
			{
				put("user", map);
			}
		});
	}
	
}
