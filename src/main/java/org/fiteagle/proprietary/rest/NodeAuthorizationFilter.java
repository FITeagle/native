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
import org.fiteagle.api.core.usermanagement.User;
import org.fiteagle.api.core.usermanagement.UserManager;
import org.fiteagle.api.core.usermanagement.User.Role;
import org.fiteagle.proprietary.rest.UserPresenter.FiteagleWebApplicationException;

import com.google.gson.Gson;

public class NodeAuthorizationFilter implements Filter{

  @Inject
  private JMSContext context;
  @Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
  private Topic topic;
  private final static int TIMEOUT_TIME_MS = 4000;
  
  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
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
        role = getRole(subjectUsername);
      } catch (Exception e) {
        response.sendError(Response.Status.UNAUTHORIZED.getStatusCode());
        return;
      }
      if(role.equals(Role.STUDENT) || role.equals(Role.CLASSOWNER)){
        response.sendError(Response.Status.FORBIDDEN.getStatusCode());
        return;
      }
    }
    chain.doFilter(request, response);
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

  @Override
  public void destroy() {
  }
  
}
