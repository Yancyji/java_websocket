package com.eim.websocket.WbsUtil;

import java.util.Set;

import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpointConfig;

public class WEBConfig implements ServerApplicationConfig{

	//注解方式启动
	@Override
	public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> scaned) {
		System.out.println("启动"+scaned.size());
		//起到过滤作用
		return scaned;
	}

	//接口方式启动
	@Override
	public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> arg0) {
		return null;
	}

}
