package com.app.index;

import java.util.*;

public class Index extends Core {
	
	public Object index() {
		//if (!isAjax()) return "redirect:/mobile";
		return success(new HashMap<String, Object>() {
			{
				put("MdexTxs", com.app.model.MdexTxs.limit(5).select());
				put("WebData", com.app.model.WebData.select());
			}
		});
	}
	
}
