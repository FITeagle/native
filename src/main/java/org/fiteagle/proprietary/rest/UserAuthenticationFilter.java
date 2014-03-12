package org.fiteagle.proprietary.rest;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.ejb.EJBException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
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
import javax.xml.bind.DatatypeConverter;

import net.iharder.Base64;

import org.fiteagle.api.usermanagement.User;
import org.fiteagle.api.usermanagement.User.Role;
import org.fiteagle.api.usermanagement.UserManager;
import org.fiteagle.api.usermanagement.UserManager.UserNotFoundException;
import org.fiteagle.core.config.preferences.InterfaceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class UserAuthenticationFilter implements Filter{

  public final static String COOKIE_NAME = "fiteagle_user_cookie";
  protected final static String SUBJECT_USERNAME_ATTRIBUTE = "subjectUsername";
  protected final static String RESOURCE_USERNAME_ATTRIBUTE = "resourceUsername";
  protected final static String ACTION_ATTRIBUTE = "action";
  protected final static String IS_AUTHENTICATED_ATTRIBUTE = "isAuthenticated";
  
  private Logger log = LoggerFactory.getLogger(getClass());
  
  public UserAuthenticationFilter(UserManager manager){
    this.manager = manager;
  }
  
  public UserAuthenticationFilter(){};
 
  private UserManager manager;
  
  protected HashMap<String, Cookie> cookies = new HashMap<>();
  
  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    Context context;
    try {
      context = new InitialContext();
      manager = (UserManager) context.lookup("java:global/usermanagement/JPAUserManager!org.fiteagle.api.usermanagement.UserManager");
    } catch (NamingException e) {
      e.printStackTrace();
    }
    if(!databaseContainsAdminUser()){
      createFirstAdminUser();
    }

  }
  
  private void createFirstAdminUser() {
    log.info("Creating First Admin User");
    User admin = User.createAdminUser("admin", "admin");
    manager.add(admin);
  }
  
  private boolean databaseContainsAdminUser() {
    List<User> users = manager.getAllUsers();
    for (User u : users) {
      if (u.getRole().equals(Role.ADMIN)) {
        return true;
      }
    }
    return false;
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
    
    try {
      if (!manager.verifyCredentials(subjectUsername, credentials[1])) {
        return false;
      }
    } catch (EJBException e) {
      if(e.getCausedByException() instanceof UserNotFoundException){
        return false;
      }
    } catch (NoSuchAlgorithmException e) {
      log.error(e.getMessage());
      return false;
    } catch (UserNotFoundException e) {
      return false;
    }
    
    request.setAttribute(SUBJECT_USERNAME_ATTRIBUTE, subjectUsername);
    return true;
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
//  TODO:  cookie.setHttpOnly(true);
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
    if(target == null){
      return "";
    }
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
    return null;
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
