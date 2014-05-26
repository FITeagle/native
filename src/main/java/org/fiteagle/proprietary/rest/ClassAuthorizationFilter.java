package org.fiteagle.proprietary.rest;

import java.io.IOException;

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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import org.fiteagle.api.core.usermanagement.User.Role;
import org.fiteagle.api.core.usermanagement.UserManager;
import org.fiteagle.api.core.usermanagement.UserManager.UserNotFoundException;

public class ClassAuthorizationFilter implements Filter{

  private UserManager manager;
  
  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    Context context;
    try {
      context = new InitialContext();
      manager = (UserManager) context.lookup("java:global/usermanagement/JPAUserManager!org.fiteagle.api.core.usermanagement.UserManager");
    } catch (NamingException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException,
      ServletException {
    HttpServletRequest request = (HttpServletRequest) req;
    HttpServletResponse response = (HttpServletResponse) resp;
    
    String subjectUsername = (String) request.getAttribute(AuthenticationFilter.SUBJECT_USERNAME_ATTRIBUTE);
    String action = (String) request.getAttribute(AuthenticationFilter.ACTION_ATTRIBUTE);
    Role role = Role.STUDENT;
    if(!action.equals("GET")){
      if(subjectUsername == null){
        response.sendError(Response.Status.UNAUTHORIZED.getStatusCode());
        return;
      }
      try {
        role = manager.get(subjectUsername).getRole();
      } catch (EJBException e) {
        if(e.getCausedByException() instanceof UserNotFoundException){
          response.sendError(Response.Status.UNAUTHORIZED.getStatusCode());
          return;
        }
      }
      if(role.equals(Role.STUDENT)){
        response.sendError(Response.Status.FORBIDDEN.getStatusCode());
        return;
      }
    }
    
    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {
  }
  
}
