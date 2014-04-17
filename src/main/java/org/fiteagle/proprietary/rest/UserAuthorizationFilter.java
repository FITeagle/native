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

import org.fiteagle.api.usermanagement.PolicyEnforcementPoint;
import org.fiteagle.api.usermanagement.User.Role;
import org.fiteagle.api.usermanagement.UserManager;
import org.fiteagle.api.usermanagement.UserManager.UserNotFoundException;

public class UserAuthorizationFilter implements Filter {

  private PolicyEnforcementPoint policyEnforcementPoint;
  private UserManager manager;

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    Context context;
    try {
      context = new InitialContext();
      manager = (UserManager) context.lookup("java:global/usermanagement/JPAUserManager!org.fiteagle.api.usermanagement.UserManager");
      policyEnforcementPoint = (PolicyEnforcementPoint) context.lookup("java:global/usermanagement/FiteaglePolicyEnforcementPoint!org.fiteagle.api.usermanagement.PolicyEnforcementPoint");
    } catch (NamingException e) {
      e.printStackTrace();
    }
  }
  
  @Override
  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException,
      ServletException {
    HttpServletRequest request = (HttpServletRequest) req;
    HttpServletResponse response = (HttpServletResponse) resp;
    
    if(request.getRequestURI().contains("/class/")){
      chain.doFilter(request, response);
      return;
    }
    
    String subjectUsername = (String) request.getAttribute(UserAuthenticationFilter.SUBJECT_USERNAME_ATTRIBUTE);
    String resourceUsername = (String) request.getAttribute(UserAuthenticationFilter.RESOURCE_USERNAME_ATTRIBUTE);
    String action = (String) request.getAttribute(UserAuthenticationFilter.ACTION_ATTRIBUTE);
    Role role = Role.STUDENT;
    if(subjectUsername != null && !action.equals("PUT")){
      try {
        role = manager.get(subjectUsername).getRole();
      } catch (EJBException e) {
        if(e.getCausedByException() instanceof UserNotFoundException){
          response.sendError(Response.Status.UNAUTHORIZED.getStatusCode());
          return;
        }
      }
    }
    Boolean isAuthenticated = (Boolean) request.getAttribute(UserAuthenticationFilter.IS_AUTHENTICATED_ATTRIBUTE);
    Boolean requiresAdminRights = requiresAdminRights(request);
    Boolean requiresTBOwnerRights = requiresClassOwnerRights(request);
    
    if(!policyEnforcementPoint.isRequestAuthorized(subjectUsername, resourceUsername, action, role.name(), isAuthenticated, requiresAdminRights, requiresTBOwnerRights)){
      if(isAuthenticated){
        response.sendError(Response.Status.FORBIDDEN.getStatusCode());
      }
      else{
        response.sendError(Response.Status.UNAUTHORIZED.getStatusCode());
      }
      return; 
    }
    
    chain.doFilter(request, response);
  }

  private Boolean requiresAdminRights(HttpServletRequest request) {
    if(request.getRequestURI().endsWith("/role/FEDERATION_ADMIN") || request.getRequestURI().endsWith("/role/CLASSOWNER") || request.getRequestURI().endsWith("/role/NODE_ADMIN")){
      return true;
    }
    return false;
  }
  
  private Boolean requiresClassOwnerRights(HttpServletRequest request) {
    if(request.getRequestURI().endsWith("/api/user/")){
      return true;
    }
    return false;
  }

  @Override
  public void destroy() {
    
  }


}
