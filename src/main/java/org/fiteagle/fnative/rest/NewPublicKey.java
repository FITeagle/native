package org.fiteagle.fnative.rest;

import java.security.PublicKey;
import java.util.Date;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.fiteagle.api.User;
import org.fiteagle.api.UserPublicKey;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NewPublicKey implements UserPublicKey {
  
  private static final long serialVersionUID = -7716521444498210880L;
  private String publicKeyString;  
  private String description;  
  
  public NewPublicKey(){};   

  public String getPublicKeyString() {
    return publicKeyString;
  }
  
  public void setPublicKeyString(String publicKey) {
    this.publicKeyString = publicKey;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public PublicKey getPublicKey() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Date getCreated() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public User getOwner() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setOwner(User owner) {
    // TODO Auto-generated method stub
    
  }
}
