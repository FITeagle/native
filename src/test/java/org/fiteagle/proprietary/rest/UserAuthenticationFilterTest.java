//package org.fiteagle.proprietary.rest;
//
//import static org.easymock.EasyMock.createMock;
//import static org.easymock.EasyMock.expect;
//import static org.easymock.EasyMock.expectLastCall;
//import static org.easymock.EasyMock.replay;
//import static org.easymock.EasyMock.verify;
//
//import java.io.IOException;
//import java.security.NoSuchAlgorithmException;
//import java.util.ArrayList;
//
//import javax.servlet.FilterChain;
//import javax.servlet.ServletException;
//import javax.servlet.http.Cookie;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//
//import org.fiteagle.api.usermanagement.User;
//import org.fiteagle.api.usermanagement.UserManager;
//import org.fiteagle.api.usermanagement.UserManager.DuplicateUsernameException;
//import org.fiteagle.api.usermanagement.UserManager.UserNotFoundException;
//import org.fiteagle.api.usermanagement.UserPublicKey;
//import org.junit.AfterClass;
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.Test;
//
//import com.sun.jersey.core.util.Base64;
//
//public class UserAuthenticationFilterTest {
//  
//  private static UserAuthenticationFilter filter;
//  private static HttpServletRequest req;
//  private static HttpServletResponse resp;
//  private static FilterChain chain;
//  private static UserManager manager;
//  
////  private static UserManagerBoundary userManager;
//  
////  @BeforeClass
////  public static void setUp() throws DuplicateUsernameException, DatabaseException, User.NotEnoughAttributesException, User.InValidAttributeException, NoSuchAlgorithmException {
////     userManager = UserManager.getInstance();
////     try{
////       userManager.delete("test");
////     } catch(UserNotFoundException e){}
////     
////     userManager.add(new User("test", "test", "test", "test@test.de", "testAffiliation", "test", new ArrayList<UserPublicKey>()));     
////  }
//
//  @Before
//  public void createMocks(){
//    req = createMock(HttpServletRequest.class);
//    resp = createMock(HttpServletResponse.class);   
//    chain = createMock(FilterChain.class);  
//    manager = createMock(UserManager.class);
//    filter = new UserAuthenticationFilter(manager);
//  }
//  
//  @Test
//  public void testAuthenticateWithUserNameAndPassword() throws IOException, ServletException, NoSuchAlgorithmException, UserNotFoundException{
////    expect(req.isSecure()).andReturn(true);
//    
//    String auth = "Basic "+new String(Base64.encode("test:test".getBytes()));
//    expect(req.getHeader("authorization")).andReturn(auth); 
//    
//    expect(manager.verifyCredentials("test", "test")).andReturn(true);
//    
//    expect(req.getRequestURI()).andReturn("/api/v1/user/test");
//    
//    expect(req.getMethod()).andReturn("GET");
//    expectLastCall().times(2);
//    
//    expect(req.getParameter("setCookie")).andReturn(null);
//    expect(req.getSession(false)).andReturn(null);
//    expect(req.getSession(true)).andReturn(null);
//    
//    expect(req.getCookies()).andReturn(null);
//    
//    req.setAttribute("isAuthenticated", true);
//    req.setAttribute("action", "GET");
//    req.setAttribute("subjectUsername", "test@localhost");
//    req.setAttribute("resourceUsername", "test@localhost");
//    
//    chain.doFilter(req, resp);
//    
//    replay(req, chain);
//    
//    filter.doFilter(req, resp, chain);
//    verify(req, chain);
//  }
//  
//  @Test
//  public void testAuthenticateWithCookie() throws IOException, ServletException{
//    Cookie cookie = new Cookie("fiteagle_user_cookie", "ZGJjYTM3YzYtY2U3My00YzJmLWI5YTYtZWM1MzYyYTg1ZDFhLXVzZXJuYW1lOm1uaWtvbGF1c0Bsb2NhbGhvc3Q=");
//
//    filter.saveCookie("mnikolaus@localhost", cookie);  
//    
//    expect(req.getMethod()).andReturn("GET");
//    expectLastCall().times(2);
//    
//    expect(req.isSecure()).andReturn(true);
//    
//    expect(req.getCookies()).andReturn(new Cookie[]{cookie});
//    
//    expect(req.getRequestURI()).andReturn("/api/v1/user/mnikolaus");
//    
//    expect(req.getParameter("setCookie")).andReturn(null);
//    expect(req.getSession(false)).andReturn(null);
//    expect(req.getSession(true)).andReturn(null);
//    
//    req.setAttribute("isAuthenticated", true);
//    req.setAttribute("action", "GET");
//    req.setAttribute("subjectUsername", "mnikolaus@localhost");
//    req.setAttribute("resourceUsername", "mnikolaus@localhost");
//    
//    chain.doFilter(req, resp);
//    
//    replay(req, chain);
//    
//    filter.doFilter(req, resp, chain);
//    verify(req, chain);
//  }
//  
////  @AfterClass
////  public static void cleanUp(){
////    try{
////      userManager.delete("test");
////    } catch(UserNotFoundException e){}
////  }
//}
