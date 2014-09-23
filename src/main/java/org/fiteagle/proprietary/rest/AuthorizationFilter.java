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
import org.jboss.logging.Logger;

public class AuthorizationFilter implements Filter {

  @Inject
  private JMSContext context;
  @Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
  private Topic topic;

  private final static Logger log = Logger.getLogger(AuthorizationFilter.class.toString());
  
  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }
  
  @Override
  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException,
      ServletException {
    HttpServletRequest request = (HttpServletRequest) req;
    HttpServletResponse response = (HttpServletResponse) resp;
    
    String action = (String) request.getAttribute(AuthenticationFilter.ACTION_ATTRIBUTE);
    String subjectUsername = (String) request.getAttribute(AuthenticationFilter.SUBJECT_USERNAME_ATTRIBUTE);
    String resource = (String) request.getAttribute(AuthenticationFilter.RESOURCE_ATTRIBUTE);
    
    if(AuthenticationFilter.requestDoesNotNeedAuth(action, request.getRequestURI())){
      chain.doFilter(request, response);
      return;
    }
    
    if(!isRequestAuthorized(subjectUsername, resource, action, response)){
      response.sendError(Response.Status.FORBIDDEN.getStatusCode());
      return;
    }
    
    chain.doFilter(request, response);
  }
  
  private Boolean isRequestAuthorized(String subjectUsername, String resource, String action, HttpServletResponse response) throws IOException{
    try{
      Message message = context.createMessage();
      message.setStringProperty(PolicyEnforcementPoint.TYPE_PARAMETER_SUBJECT_USERNAME, subjectUsername);
      message.setStringProperty(PolicyEnforcementPoint.TYPE_PARAMETER_RESOURCE, resource);
      message.setStringProperty(PolicyEnforcementPoint.TYPE_PARAMETER_ACTION, action);
      message.setStringProperty(IMessageBus.TYPE_TARGET, PolicyEnforcementPoint.TARGET);
      message.setStringProperty(IMessageBus.TYPE_REQUEST, PolicyEnforcementPoint.IS_REQUEST_AUTHORIZED);
      message.setJMSCorrelationID(UUID.randomUUID().toString());
      String filter = "JMSCorrelationID='" + message.getJMSCorrelationID() + "'";
      
      Message rcvMessage = null;
      while(rcvMessage == null){
        context.createProducer().send(topic, message);
        rcvMessage = context.createConsumer(topic, filter).receive(IMessageBus.TIMEOUT);
      }
      Boolean result = rcvMessage.getBooleanProperty(IMessageBus.TYPE_RESULT);
      return result;
    }catch(JMSException e) {
      log.error(e);
      response.sendError(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
      return false;
    }
  }

  @Override
  public void destroy() {
    
  }

}
