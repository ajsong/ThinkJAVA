package com.app.fxvymr7h;

import com.framework.tool.*;
import java.util.*;

public class Index extends Core {
	
	public Object index() {
		//会员数量
		Map<String, Object> map = new HashMap<>();
		map.put("total", com.app.model.Member.count());
		map.put("today", com.app.model.Member.whereTime("reg_time", "today").count());
		return this.render(new HashMap<String, Object>(){
			{
				put("user", map);
			}
		});
	}
	
}
