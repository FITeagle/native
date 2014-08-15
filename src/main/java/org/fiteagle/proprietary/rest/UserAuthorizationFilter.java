package org.fiteagle.proprietary.rest;

import java.io.IOException;
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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.usermanagement.PolicyEnforcementPoint;
import org.fiteagle.api.core.usermanagement.User;
import org.fiteagle.api.core.usermanagement.User.Role;
import org.fiteagle.api.core.usermanagement.UserManager;
import org.fiteagle.proprietary.rest.ObjectPresenter.FiteagleWebApplicationException;

import com.google.gson.Gson;

public class UserAuthorizationFilter implements Filter {

  @Inject
  private JMSContext context;
  @Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
  private Topic topic;
  private final static int TIMEOUT_TIME_MS = 10000;

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }
  
  @Override
  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException,
      ServletException {
    HttpServletRequest request = (HttpServletRequest) req;
    HttpServletResponse response = (HttpServletResponse) resp;
    
    String action = (String) request.getAttribute(AuthenticationFilter.ACTION_ATTRIBUTE);
    if(action.equals("PUT")){
      chain.doFilter(request, response);
      return;
    }
    
    String subjectUsername = (String) request.getAttribute(AuthenticationFilter.SUBJECT_USERNAME_ATTRIBUTE);
    String resourceUsername = (String) request.getAttribute(AuthenticationFilter.RESOURCE_USERNAME_ATTRIBUTE);
    
    Role role = getRole(subjectUsername);

    Boolean requiresAdminRights = requiresAdminRights(request);
    Boolean requiresTBOwnerRights = requiresClassOwnerRights(request);
    
    if(!isRequestAuthorized(subjectUsername, resourceUsername, action, role.name(), requiresAdminRights, requiresTBOwnerRights)){
      response.sendError(Response.Status.FORBIDDEN.getStatusCode());
      return;
    }
    
    chain.doFilter(request, response);
  }
  
  private Boolean isRequestAuthorized(String subjectUsername, String resourceUsername, String action, String role, Boolean requiresAdminRights, Boolean requiresTBOwnerRights){
    try{
      Message message = context.createMessage();
      message.setStringProperty(PolicyEnforcementPoint.TYPE_PARAMETER_SUBJECT_USERNAME, subjectUsername);
      message.setStringProperty(PolicyEnforcementPoint.TYPE_PARAMETER_RESOURCE_USERNAME, resourceUsername);
      message.setStringProperty(PolicyEnforcementPoint.TYPE_PARAMETER_ACTION, action);
      message.setStringProperty(PolicyEnforcementPoint.TYPE_PARAMETER_ROLE, role);
      message.setBooleanProperty(PolicyEnforcementPoint.TYPE_PARAMETER_REQUIRES_ADMIN_RIGHTS, requiresAdminRights);
      message.setBooleanProperty(PolicyEnforcementPoint.TYPE_PARAMETER_REQUIRES_TBOWNER_RIGHTS, requiresTBOwnerRights);
      message.setStringProperty(IMessageBus.TYPE_TARGET, PolicyEnforcementPoint.TARGET);
      message.setStringProperty(IMessageBus.TYPE_REQUEST, PolicyEnforcementPoint.IS_REQUEST_AUTHORIZED);
      message.setJMSCorrelationID(UUID.randomUUID().toString());
      String filter = "JMSCorrelationID='" + message.getJMSCorrelationID() + "'";
      context.createProducer().send(topic, message);
      
      Message rcvMessage = context.createConsumer(topic, filter).receive(TIMEOUT_TIME_MS);
      
      if(rcvMessage == null){
        throw new FiteagleWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "timeout while waiting for answer from JMS message bus");    
      }
      Boolean result = rcvMessage.getBooleanProperty(IMessageBus.TYPE_RESULT);
      return result;
    }catch(JMSException e) {
      throw new FiteagleWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "JMS Error: "+e.getMessage());    
    }
  }

  private Role getRole(String username){
    try{
      Message message = context.createMessage();
      message.setStringProperty(UserManager.TYPE_PARAMETER_USERNAME, username);
      message.setStringProperty(IMessageBus.TYPE_TARGET, UserManager.TARGET);
      message.setStringProperty(IMessageBus.TYPE_REQUEST, UserManager.GET_USER);
      message.setJMSCorrelationID(UUID.randomUUID().toString());
      String filter = "JMSCorrelationID='" + message.getJMSCorrelationID() + "'";
      context.createProducer().send(topic, message);
      
      Message rcvMessage = context.createConsumer(topic, filter).receive(TIMEOUT_TIME_MS);
      
      if(rcvMessage == null){
        throw new FiteagleWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "timeout while waiting for answer from JMS message bus");    
      }
      String exceptionMessage = rcvMessage.getStringProperty(IMessageBus.TYPE_EXCEPTION);
      if(exceptionMessage != null){
        throw new FiteagleWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), exceptionMessage);    
      }
      String resultJSON = rcvMessage.getStringProperty(IMessageBus.TYPE_RESULT);
      User user =  new Gson().fromJson(resultJSON, User.class);
      return user.getRole();
    }catch(JMSException e) {
      throw new FiteagleWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "JMS Error: "+e.getMessage());    
    }
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
