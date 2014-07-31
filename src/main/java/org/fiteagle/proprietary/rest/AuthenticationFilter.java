package org.fiteagle.proprietary.rest;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Topic;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;
import javax.xml.bind.DatatypeConverter;

import net.iharder.Base64;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.usermanagement.UserManager;
import org.fiteagle.core.config.preferences.InterfaceConfiguration;
import org.fiteagle.proprietary.rest.UserPresenter.FiteagleWebApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AuthenticationFilter implements Filter{

  public final static String COOKIE_NAME = "fiteagle_user_cookie";
  protected final static String SUBJECT_USERNAME_ATTRIBUTE = "subjectUsername";
  protected final static String RESOURCE_USERNAME_ATTRIBUTE = "resourceUsername";
  protected final static String ACTION_ATTRIBUTE = "action";
  protected final static String IS_AUTHENTICATED_ATTRIBUTE = "isAuthenticated";
  
  private Logger log = LoggerFactory.getLogger(getClass());
  
  public AuthenticationFilter(){};
 
  private static AuthenticationFilter instance;
  
  @Inject
  private JMSContext context;
  @Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
  private Topic topic;
  private final static int TIMEOUT_TIME_MS = 10000;
  
  protected HashMap<String, Cookie> cookies = new HashMap<>();
  
  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    instance = this;
  }

  public static AuthenticationFilter getInstance(){
    return instance;
  }
  
  @Override
  public void destroy() {}
  
  @Override
  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException,
      ServletException {
    HttpServletRequest request = (HttpServletRequest) req;
    HttpServletResponse response = (HttpServletResponse) resp;
    
//    if(!request.isSecure()){
//      response.sendError(Response.Status.BAD_REQUEST.getStatusCode());      
//      return;   
//    }
    
    request.setAttribute(ACTION_ATTRIBUTE, request.getMethod());
    request.setAttribute(RESOURCE_USERNAME_ATTRIBUTE, getTarget(request));
    
    if(authenticateWithSession(request) || authenticateWithCookie(request) || authenticateWithUsernamePassword(request, response)){
      request.setAttribute(IS_AUTHENTICATED_ATTRIBUTE, true);
      addCookieOnLogin(request, response);
      createSession(request);  
    }
    else{
      request.setAttribute(IS_AUTHENTICATED_ATTRIBUTE, false);
    }
      
    chain.doFilter(request, response);
    deleteSessionAndCookieOnLogout(request, response);
  }
  
  private void createSession(HttpServletRequest request) {
    HttpSession session = request.getSession(true);
    if(session != null){
      session.setAttribute("username", request.getAttribute(SUBJECT_USERNAME_ATTRIBUTE));
    }
  }

  private void addCookieOnLogin(HttpServletRequest request, HttpServletResponse response) {    
    boolean setCookie = Boolean.parseBoolean(request.getParameter("setCookie"));
    if(setCookie == true && getAuthCookieFromRequest(request) == null){      
      response.addCookie(createNewCookie(getTarget(request)));      
    }
  }

  private void deleteSessionAndCookieOnLogout(HttpServletRequest request, HttpServletResponse response) {
    if(request.getMethod().equals("DELETE") && request.getRequestURI().endsWith("/cookie")){
      request.getSession().invalidate();      
      addNullCookies(request, response);
    }
  }

  private void addNullCookies(HttpServletRequest request, HttpServletResponse response) {
    Cookie authCookie = getAuthCookieFromRequest(request);
    if(authCookie != null){
      authCookie.setMaxAge(0);
      authCookie.setValue(null);
      authCookie.setPath("/");
      response.addCookie(authCookie);
    }    
    
    Cookie sessionCookie = getSessionCookieFromRequest(request);
    if(sessionCookie != null){
      sessionCookie.setMaxAge(0);
      sessionCookie.setValue(null);
      sessionCookie.setPath("/");
      response.addCookie(sessionCookie);
    }
  }
  
  private boolean authenticateWithSession(HttpServletRequest request){
    HttpSession session = request.getSession(false);    
    if(session == null){
      return false;
    }
    String subjectUsername = session.getAttribute("username").toString();
    request.setAttribute(SUBJECT_USERNAME_ATTRIBUTE, subjectUsername);
    return true;
  }

  private boolean authenticateWithCookie(HttpServletRequest request){
    Cookie authCookieFromRequest = getAuthCookieFromRequest(request);
    if(authCookieFromRequest == null){
      return false;
    }
    
    String subjectUsername = getUsernameFromCookie(authCookieFromRequest);
    
    Cookie cookieFromStorage = (subjectUsername == null)? null : cookies.get(subjectUsername);
    if(cookieFromStorage == null || !authCookieFromRequest.getValue().equals(cookieFromStorage.getValue())){
      return false;
    }    
    
    request.setAttribute(SUBJECT_USERNAME_ATTRIBUTE, subjectUsername);
    return true;
  }
  
  private boolean authenticateWithUsernamePassword(HttpServletRequest request, HttpServletResponse response) throws IOException{
    String auth = request.getHeader("authorization");
    String[] credentials = decode(auth);
    if (credentials == null || credentials.length != 2) {
      return false;
    }
    String subjectUsername = addDomain(credentials[0]);
    
    if (!verifyCredentials(subjectUsername, credentials[1])) {
      return false;
    }
    request.setAttribute(SUBJECT_USERNAME_ATTRIBUTE, subjectUsername);
    return true;
  }
  
  private boolean verifyCredentials(String username, String password){
    try{
      Message message = context.createMessage();
      message.setStringProperty(UserManager.TYPE_PARAMETER_USERNAME, username);
      message.setStringProperty(UserManager.TYPE_PARAMETER_PASSWORD, password);
      message.setStringProperty(IMessageBus.TYPE_TARGET, UserManager.TARGET);
      message.setStringProperty(IMessageBus.TYPE_REQUEST, UserManager.VERIFY_CREDENTIALS);
      message.setJMSCorrelationID(UUID.randomUUID().toString());
      String filter = "JMSCorrelationID='" + message.getJMSCorrelationID() + "'";
      context.createProducer().send(topic, message);
      
      Message rcvMessage = context.createConsumer(topic, filter).receive(TIMEOUT_TIME_MS);
      
      if(rcvMessage == null){
        throw new FiteagleWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "timeout while waiting for answer from JMS message bus");    
      }
      String exceptionMessage = rcvMessage.getStringProperty(IMessageBus.TYPE_EXCEPTION);
      if(exceptionMessage != null){
        return false;
      }
      return rcvMessage.getBooleanProperty(IMessageBus.TYPE_RESULT);
    }catch(JMSException e) {
      throw new FiteagleWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "JMS Error: "+e.getMessage());    
    }
  }
  
  private String getUsernameFromCookie(Cookie cookie){
    String s;
    try {
      s = new String(Base64.decode(cookie.getValue()));
    } catch (IOException e) {
      log.error(e.getMessage());
      return null;
    }
    String[] splitted = s.split("-username:");
    return splitted[1];
  }

  private Cookie getAuthCookieFromRequest(HttpServletRequest request) {
    Cookie[] cookiesFromRequest = request.getCookies();
    if(cookiesFromRequest != null){
      for(Cookie cookie : cookiesFromRequest){
        if(cookie.getName().equals(COOKIE_NAME)){
          return cookie;
        }
      }
    }
    return null;
  }
  
  private Cookie getSessionCookieFromRequest(HttpServletRequest request) {
    Cookie[] cookiesFromRequest = request.getCookies();
    if(cookiesFromRequest != null){
      for(Cookie cookie : cookiesFromRequest){
        if(cookie.getName().equals("JSESSIONID")){
          return cookie;
        }
      }
    }
    return null;
  }
  
  private Cookie createNewCookie(String username){
    String authToken = createRandomAuthToken("-username:"+username);
    Cookie cookie = new Cookie(COOKIE_NAME, authToken);
    cookie.setSecure(true);
    cookie.setHttpOnly(true);
    cookie.setMaxAge(365 * 24 * 60 * 60);
    cookie.setPath("/");
    cookies.put(username, cookie);
    return cookie;
  } 
  
  protected void deleteCookie(String username){
    cookies.remove(addDomain(username));
  }
  
  protected String getTarget(HttpServletRequest request) {
    String path = request.getRequestURI();
    String target = getTargetNameFromURI(path, "user");
    return addDomain(target);
  } 
  
  protected String[] decode(String auth) {
    if (auth == null || (!auth.startsWith("Basic ") && !auth.startsWith("basic "))) {
      return null;
    }
    auth = auth.replaceFirst("[B|b]asic ", "");
    byte[] decoded = DatatypeConverter.parseBase64Binary(auth);
    if (decoded == null || decoded.length == 0) {
      return null;
    }
    return new String(decoded).split(":", 2);
  }
  
  protected String getTargetNameFromURI(String path, String targetIdentifier) {
    String[] splitted = path.split("/");
    for (int i = 0; i < splitted.length - 1; i++) {
      if (splitted[i].equals(targetIdentifier)) {
        return splitted[i+1];
      }
    }
    return "";
  }
  
  protected String createRandomAuthToken(String postfix) {
    String u = UUID.randomUUID().toString();
    u+=postfix;
    return new String(Base64.encodeBytes(u.getBytes()));
  }
  
  protected void saveCookie(String target, Cookie cookie) {
    cookies.put(target, cookie);    
  }

  private String addDomain(String username) {
    InterfaceConfiguration configuration = null;
    if (!username.contains("@")) {
      configuration = InterfaceConfiguration.getInstance();
      username = username + "@" + configuration.getDomain();
    }
    return username;
  }
  
}