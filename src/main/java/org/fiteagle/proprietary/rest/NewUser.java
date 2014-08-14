package org.fiteagle.proprietary.rest;

import java.util.List;

import org.fiteagle.api.core.usermanagement.Node;
import org.fiteagle.api.core.usermanagement.UserPublicKey;

public class NewUser{
  
  private String username;
  private String firstName;
  private String lastName;
  private String email;
  private String affiliation;
  private String password;
  private List<UserPublicKey> publicKeys;
  private Node node;
  
  public NewUser(){};
  
  public String getUsername() {
    return username;
  }
  public void setUsername(String username) {
    this.username = username;
  }
  public String getFirstName() {
    return firstName;
  }
  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }
  public String getLastName() {
    return lastName;
  }
  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getAffiliation() {
    return affiliation;
  }

  public void setAffiliation(String affiliation) {
    this.affiliation = affiliation;
  }

  protected String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
  
  public List<UserPublicKey> getPublicKeys() {
    return publicKeys;
  }

  public void setPublicKeys(List<UserPublicKey> publicKeys) {
    this.publicKeys = publicKeys;
  }

  public Node getNode() {
    return node;
  }

  public void setNode(Node node) {
    this.node = node;
  }
 
}
