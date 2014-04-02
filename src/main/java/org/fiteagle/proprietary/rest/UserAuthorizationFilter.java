package org.fiteagle.proprietary.rest;

import java.io.IOException;

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
    
    String subjectUsername = (String) request.getAttribute(UserAuthenticationFilter.SUBJECT_USERNAME_ATTRIBUTE);
    String resourceUsername = (String) request.getAttribute(UserAuthenticationFilter.RESOURCE_USERNAME_ATTRIBUTE);
    String action = (String) request.getAttribute(UserAuthenticationFilter.ACTION_ATTRIBUTE);
    Role role = Role.USER;
    if(subjectUsername != null){
      role = manager.get(subjectUsername).getRole();
    }
    Boolean isAuthenticated = (Boolean) request.getAttribute(UserAuthenticationFilter.IS_AUTHENTICATED_ATTRIBUTE);
    Boolean requiresAdminRights = requiresAdminRights(request);
    Boolean requiresTBOwnerRights = requiresTBOwnerRights(request);
    
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
    if(request.getRequestURI().endsWith("/role/ADMIN") || request.getRequestURI().endsWith("/role/TBOWNER")){
      return true;
    }
    return false;
  }
  
  private Boolean requiresTBOwnerRights(HttpServletRequest request) {
    if(request.getRequestURI().endsWith("/api/user/")){
      return true;
    }
    return false;
  }

  @Override
  public void destroy() {
    
  }


}
