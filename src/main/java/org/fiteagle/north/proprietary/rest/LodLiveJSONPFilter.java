package org.fiteagle.north.proprietary.rest;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * surrounds incoming JSONPRequest with JSONP wrapping, also resets content type
 */
public class LodLiveJSONPFilter implements Filter {
  
  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }
  
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
      ServletException {
    
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    
    if (isJSONPRequest(httpRequest)) {
      ServletOutputStream out = response.getOutputStream();
      
      out.println(getCallbackMethod(httpRequest) + "(");
      chain.doFilter(request, response);
      out.println(")");
      
      response.setContentType("application/sparql-results+json");
    } else {
      chain.doFilter(request, response);
    }
  }
  
  private String getCallbackMethod(HttpServletRequest httpRequest) {
    return httpRequest.getParameter("callback");
  }
  
  private boolean isJSONPRequest(HttpServletRequest httpRequest) {
    return getCallbackMethod(httpRequest) != null && getCallbackMethod(httpRequest).length() > 0;
  }
  
  @Override
  public void destroy() {
  }
}