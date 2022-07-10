package com.framework.response;

import com.framework.tool.Common;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class ResponseMvcConfig implements WebMvcConfigurer {
	
	@Value("${spring.servlet.upload.path}")
	private String uploadPath;
	
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/" + uploadPath + "/**").addResourceLocations("file:///" + Common.root_path() + "/" + uploadPath + "/");
	}
	
}
