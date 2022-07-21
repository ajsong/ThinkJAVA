package com.framework;

import com.framework.tool.*;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.*;

@RestController
public class Start {
	@RequestMapping("/")
	Object index(HttpServletResponse response) {
		try {
			response.sendRedirect("/index");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@RequestMapping(value = {"/index/**", "/admin/**", "/error"})
	Object reflect(HttpServletRequest request, HttpServletResponse response) {
		String ban = (String) request.getSession().getAttribute("appActMissing");
		int count = (ban == null || ban.length() == 0) ? 0 : Integer.parseInt(ban);
		if (count >= 5) return Common.error404(response);
		Map<String, String> moduleMap = Common.getModule(request);
		if (moduleMap.get("setup").equals("true")) {
			if (request.getRequestURI().matches("^/(" + moduleMap.get("modules") + ")\\b.*")) return Common.error404(response);
		}
		String module = moduleMap.get("module");
		String app = moduleMap.get("app");
		String act = moduleMap.get("act");
		try {
			Class<?> clazz = Class.forName("com.app." + module + "." + Character.toUpperCase(app.charAt(0)) + app.substring(1));
			Object instance;
			try {
				instance = clazz.getConstructor(HttpServletRequest.class, HttpServletResponse.class).newInstance(request, response);
			} catch (Exception e) {
				instance = clazz.getConstructor().newInstance();
			}
			try {
				clazz.getMethod("__construct", HttpServletRequest.class, HttpServletResponse.class).invoke(instance, request, response);
			} catch (NoSuchMethodException e) {
				//Method not exist
			}
			if (!((boolean) clazz.getMethod("getAppKeepRunning").invoke(instance))) return null;
			Object ret = clazz.getMethod(act).invoke(instance);
			if (ret instanceof String) {
				if (Common.preg_match("^(tourl|redirect):", String.valueOf(ret))) return Common.redirect(String.valueOf(ret).replaceFirst("^(tourl|redirect):", ""));
			}
			return ret;
		} catch (ClassNotFoundException | NoSuchMethodException e) {
			//e.printStackTrace();
			//count++;
			//request.getSession().setAttribute("appActMissing", String.valueOf(count));
			return Common.error("@error?type=404&tips=HERE IS NO PAGES CALLED "+app+"/"+act);
		} catch (Exception e) {
			System.out.println("Error in url: " + request.getRequestURI());
			e.printStackTrace();
		}
		return Common.error("@error");
	}

	/*@RequestMapping("/s/{id:\\d+}")
	Object indexCode(@PathVariable int id) {
		return Common.runMethod(Index.class, "code", id);
	}*/
}
