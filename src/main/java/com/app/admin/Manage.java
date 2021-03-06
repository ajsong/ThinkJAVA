package com.app.admin;

import com.app.model.*;
import com.framework.closure.*;
import com.framework.tool.*;
import java.util.*;

public class Manage extends Core {
	
	public Object group() {
		DataList list = ManageGroup.order("id").pagination().select().each((CallbackDataMap)(DataMap item) -> {
			item.put("add_time", Common.date("Y-m-d H:i", item.getInt("add_time")));
		});
		return this.render(new HashMap<String, Object>(){
			{
				put("list", list);
			}
		});
	}
}
