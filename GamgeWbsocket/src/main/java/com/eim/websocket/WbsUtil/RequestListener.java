package com.eim.websocket.WbsUtil;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
@WebListener
public class RequestListener implements ServletRequestListener {
    @Override
    public void requestDestroyed(ServletRequestEvent sre) {

    }
    @Override
    public void requestInitialized(ServletRequestEvent sre) {
    	 //将所有request请求都携带上httpSession
      HttpSession session = ((HttpServletRequest) sre.getServletRequest()).getSession();
      System.out.println("初始化sessionid:"+session.getId());
    }
}
