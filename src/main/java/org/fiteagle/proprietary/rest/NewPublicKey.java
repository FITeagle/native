package org.fiteagle.proprietary.rest;

public class NewPublicKey{
  
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

}
