package com.dineug.file.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
@EnableAsync
public class WebMvcConfig extends WebMvcConfigurerAdapter {

}