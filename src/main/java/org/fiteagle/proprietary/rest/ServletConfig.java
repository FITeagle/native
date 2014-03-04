package org.fiteagle.proprietary.rest;
//package org.fiteagle.fnative.rest;
//
//import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
//
//import com.google.inject.Guice;
//import com.google.inject.Injector;
//import com.google.inject.Scopes;
//import com.google.inject.servlet.GuiceServletContextListener;
//import com.sun.jersey.guice.JerseyServletModule;
//import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
// 
//public class ServletConfig extends GuiceServletContextListener {
//  
//   @Override
//   protected Injector getInjector() {
//     
//      return Guice.createInjector(new JerseyServletModule() {
//        
//         @Override
//         protected void configureServlets() {            
//            
//            bind(UserPresenter.class).in(Scopes.SINGLETON);
////            bind(UserManagerBoundary.class).toInstance(UserManager.getInstance());
////            filter("/native/v1/user/*").through(UserAuthenticationFilter.getInstance());
////            filter("/native/v1/user/*").through(UserAuthorizationFilter.getInstance());
//            
//            bind(JacksonJsonProvider.class).in(Scopes.SINGLETON);
//        
//            
//            serve("/v1/*").with(GuiceContainer.class);
//         }
//      });
//      
//   }
//}