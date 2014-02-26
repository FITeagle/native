package org.fiteagle.fnative.rest;

import java.util.Date;
import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.fiteagle.api.User;
import org.fiteagle.api.UserPublicKey;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NewUser implements User {
  
  private static final long serialVersionUID = -1830926707750453582L;
  private String username;
  private String firstName;
  private String lastName;
  private String email;
  private String affiliation;
  private String password;
  private List<NewPublicKey> publicKeys;
  
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

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
  
  @SuppressWarnings("unchecked")
  public List<UserPublicKey> getPublicKeys() {
    return (List<UserPublicKey>) (List<?>) publicKeys;
  }

  public void setPublicKeys(List<NewPublicKey> publicKeys) {
    this.publicKeys = publicKeys;
  }

  @Override
  public void updateAttributes(String firstName, String lastName, String email, String affiliation, String password,
      List<UserPublicKey> publicKeys) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void addPublicKey(UserPublicKey publicKey) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void deletePublicKey(String description) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void renamePublicKey(String description, String newDescription) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public UserPublicKey getPublicKey(String description) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Role getRole() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setRole(Role role) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public Date getCreated() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Date getLastModified() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getPasswordHash() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getPasswordSalt() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean hasKeyWithDescription(String description) {
    // TODO Auto-generated method stub
    return false;
  }
 
}
