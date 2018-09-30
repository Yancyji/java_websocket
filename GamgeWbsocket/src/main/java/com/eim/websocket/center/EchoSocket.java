package com.eim.websocket.center;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSession;
import javax.websocket.ClientEndpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.eim.service.GetDataServiceImpl;
import com.eim.websocket.WbsUtil.GetHttpSessionConfigurator;
/**
 * value="/echo"  访问websocket路径
 * configurator=GetHttpSessionConfigurator.class  session共享
 * @author Administrator
 *
 */
@ClientEndpoint
@ServerEndpoint(value="/ddd/echo.do",configurator=GetHttpSessionConfigurator.class)
public class EchoSocket extends HttpServlet{
	
	private HttpSession httpSession;
	
	//线程安全的静态变量，表示在线连接数
	private static volatile int  onlineCount = 0;
	
	// httpsession 和 WebSocketSession之间的关系
	private static final Map<HttpSession, Session> SESSIONS = new HashMap<>();
	
	//进入**匹配**等待的用户  sesession集合
	private static final Set<Session> waitMatchUsers = new HashSet<>();
	
	//**匹配**比赛对手  比赛选手1---选手2
	private static final Map<Session, Session>  gameingMap = new HashMap<>();
	
	
	//注入service层
//    private GetDataServiceImpl getDataServiceImpl = (GetDataServiceImpl) ContextLoader.getCurrentWebApplicationContext().getBean("getDataServiceImpl");
	
	@OnOpen
    public void start(Session session,EndpointConfig config) {
		//session共享
		HttpSession httpSession= (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
		SESSIONS.put(httpSession, session);
		onlineCount++;
		System.out.println("当前连接数："+onlineCount);
        
    }
	
	@OnMessage
	public void message(Session session,String msg) throws IOException{
		String jiexiMsg = this.jiexiMsg(session, msg);
		if(StringUtils.isNotBlank(jiexiMsg)){
			switch (jiexiMsg) {
			case "pipei":
				this.pipeiGame(session);
				break;
			case "toMe":
				this.SendMessageToOne(session, msg);
				break;
			case "toEnemy":
				Session enemySessionBySessionFGameingMap = this.getEnemySessionBySessionFGameingMap(session);
				this.SendMessageToOne(enemySessionBySessionFGameingMap, msg);
				break;
			case "toBoth":
				break;
			default:
				break;
			}
		}
		
		
	}
	
	//匹配方法
	public void pipeiGame(Session session){
		waitMatchUsers.add(session);//进入即为匹配
		if(waitMatchUsers.size()>=2){
			Session newSessionOutme = this.getNewSessionOutme(session);//取出一个新对手立即移除
			waitMatchUsers.remove(session);
			waitMatchUsers.remove(newSessionOutme);
			//移除等待队列后  添加到gameMap
			gameingMap.put(session, newSessionOutme);
			net.sf.json.JSONObject jsonObject = new net.sf.json.JSONObject();
			jsonObject.put("type", 0);
			jsonObject.put("mainfupalyer", 0);
			List<Integer> randomlist = new ArrayList<>();
			for (int i = 0; i < 30; i++) {
				Integer randomdata = (int) Math.ceil(Math.random()*400-200);
				randomlist.add(randomdata);
			}
			jsonObject.put("mapdata", randomlist);
			net.sf.json.JSONObject jsonObject2 = new net.sf.json.JSONObject();
			jsonObject2.put("type", 0);
			jsonObject2.put("mainfupalyer", 1);
			jsonObject2.put("mapdata", randomlist);
			this.SendMessageToOne(session, jsonObject.toString());
			this.SendMessageToOne(newSessionOutme, jsonObject2.toString());
		
		}
	}
	
	 /**
     * 通过session查找 在线匹配  gameingMap map中的 对手session  
     */
    public Session getEnemySessionBySessionFGameingMap(Session session){
    	 Set<Entry<Session, Session>> entrySet = gameingMap.entrySet();
    	 if(entrySet == null){
    		 return null;
    	 }
    	 for (Entry<Session, Session> entry : entrySet) {
    		 Session key = entry.getKey();
    		 Session value = entry.getValue();
    		 if(key.equals(session)){
    			 return value;
    		 }
    		 if(value.equals(session)){
    			 return key;
    		 }
		}
    	return null;
    }
	
	//解析msg 
	public String jiexiMsg(Session session,String msg) {
		String msgstr = "";
		if(msg.contains("{")){ 
			JSONObject parseObject = JSON.parseObject(msg);
			try {
				msgstr=parseObject.getString("msg");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}else{
			//非json格式
			this.SendMessageToOne(session, "非json数据");
		}
		return msgstr;
	}
	
    /**
     * //零时创建一个除了自己的 新的list集合 并随机取出来除了自己的session对手
     */
    public Session getNewSessionOutme(Session session){
    	LinkedList<Session> newlist = new LinkedList<>();
    	for (Session session2 : waitMatchUsers) {
    		if(!session.equals(session2)){
    			newlist.add(session2);
    		}
    	}
    	Collections.shuffle(newlist);
    	return newlist.get(0);
    }
    
    @OnClose
	public void close(Session session) {
		this.removeMapOnError(session);
	}
	
	@OnError
	public void Error(Session session,Throwable error){
		this.removeMapOnError(session);
	}
	
	 /**
     * 移除map中的  对象
     */
    public void removeMapOnError(Session session){
    	SESSIONS.remove(httpSession);
    	waitMatchUsers.remove(session);
    	gameingMap.remove(session);
    	if(onlineCount>0){
    		onlineCount --;
    	}
    	System.out.println("当前连接数："+onlineCount);
    }
	
    /**            
    * 群发
    */
    public void SendMessageToAll(String msg){
    	Set<Entry<HttpSession, Session>> entrySet = SESSIONS.entrySet();
		for (Entry<HttpSession, Session> entry : entrySet) {
			Session value = entry.getValue();
			try {
				value.getBasicRemote().sendText(msg);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
    }
    /**
     * 单发
     */
    public void SendMessageToOne(Session toSession,String msg){
    	try {
    		toSession.getBasicRemote().sendText(msg);
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
	
    

}

