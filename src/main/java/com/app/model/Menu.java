package com.app.model;

import com.framework.tool.Common;

public class Menu extends Core {
	
	//获取下级总数
	public int getChildTotal() {
		/*if ($this->level == 1) {
			return self::where(['parent_id'=>$this->id, 'level'=>2])->count() + 1;
		}*/
		return 1;
	}
	
	//获取当前路径
	/*public function checkActive() {
		if (!strlen($this->path)) return false;
		$trueUrl = self::getRoutePath();
		$trueUrl = explode('/', $trueUrl);
		$trueUrl = $trueUrl[0] . '/' . $trueUrl[1];
		$url = explode('/', trim($this->path, '/'));
		$url = $url[0] . '/' . $url[1];
		return $trueUrl == strtolower($url);
	}*/
	
	public static String getRoutePath() {
		String actions = Common.trim(Common.request().url(), "/");
		String[] arr = actions.split("/");
		if (arr.length == 1) {
			return actions.toLowerCase() + "/index/index";
		} else if (arr.length == 2) {
			return actions.toLowerCase() + "/index";
		} else {
			return actions.toLowerCase();
		}
	}
}
