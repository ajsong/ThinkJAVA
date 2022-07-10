package com.app.model;

import com.framework.Model;
import org.springframework.web.context.request.*;
import javax.servlet.http.*;
import java.util.*;

public class Core extends Model {
	
	public Core() {
		ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		HttpServletRequest request = Objects.requireNonNull(servletRequestAttributes).getRequest();
		HttpServletResponse response = Objects.requireNonNull(servletRequestAttributes).getResponse();
		super.__construct(request, response);
		
	}

}
