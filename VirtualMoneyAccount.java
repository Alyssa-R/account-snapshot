import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

import java.io.*;  
import java.net.*; 


public class VirtualMoneyAccount implements AccountInterface{
  
  
  AccountInterface stub1;
  AccountInterface stub2;
  AccountInterface stub3;
  
  static boolean shapshotCompleted = false;
  
  private boolean isLeader; //so that each account knows whether it is the leader or not
  private boolean snapshotInitiated;
  
  private int balance;
  private int processId;
  private String ipAddress;
  
  private AccountInterface[] stubArray;
  
  
 // public VirtualMoneyAccount(String[] otherIPs) {
    public VirtualMoneyAccount() {
    snapshotInitiated = false;
    balance = 200;
    try{
      ipAddress = InetAddress.getLocalHost().getHostAddress();
    }catch (Exception e){
      System.err.print(e);
    }
    processId = generateProcessId();
    isLeader = false; //start with no process the leader
    
    
    AccountInterface[] stubArray = new AccountInterface[3];
    
  }
  
  
  private void run(String[] otherIPs) throws IOException {
    
    //prepare given IPs to lookup in registry
    
    try {
      
      //process has to put itself in the registry
      
      VirtualMoneyAccount obj = new VirtualMoneyAccount();
      AccountInterface skeleton = (AccountInterface) UnicastRemoteObject.exportObject(obj, 0);
      
      // Bind the remote object's skeleton in the registry
      Registry reg = LocateRegistry.getRegistry();
      reg.bind("Account", skeleton); 
      
      //process has to lookup the other three processes in the registry
      Registry regA = LocateRegistry.getRegistry(otherIPs[0]);
      AccountInterface stubA = (AccountInterface) regA.lookup("Account");
      
      Registry regB = LocateRegistry.getRegistry(otherIPs[1]);
      AccountInterface stubB = (AccountInterface) regB.lookup("Account");
      
      Registry regC = LocateRegistry.getRegistry(otherIPs[2]);
      AccountInterface stubC = (AccountInterface) regC.lookup("Account");
      
      AccountInterface[] stubArray = {stubA, stubB, stubC};
      
      
      //leader election
      electLeader();      
      // SEMAPHORES HERE TO MAKE SURE ACCOUNT TRANSACTIONS DON'T OCCUR UNTIL EVERYONE IS ON SAME PAGE ABOUT LEADER
      
      
      
      //transactions loop
      Boolean snapshotInitiated = false;
      Boolean shapshotCompleted = false;
      int r = (int)(Math.random()*45000)+5000;
      int m = (int)(Math.random()*this.getBalance()-1)+1;
      int p = (int)(Math.random()*3); //randomly picks one of the other accts for a transaction
      
      while (shapshotCompleted == false){
        
        //if leader, initiate snapshot if haven't yet initiated
        if(this.isLeader && !snapshotInitiated){
          //initiateSnapshot();
        }
        
        
        TimeUnit.MILLISECONDS.sleep(r); //wait random time
        stubArray[p].deposit(m); //transfer random money to random other acct
        this.withdraw(m);
        
        r = (int)(Math.random()*45000)+5000;
        m = (int)(Math.random()*this.getBalance()-1)+1;
        p = (int)(Math.random()*3);
        
      }    
      
    } catch (Exception e) {
      System.out.println("Account exception: " + e.toString());
      e.printStackTrace();
    }
    
  }
  
  
  private void electLeader(){
    
    Boolean leaderDecided = false;
    while (leaderDecided == false){
      
    }
  }
  
  private int generateProcessId(){
    int sum = 0;
    try{
      //String raw_ip = InetAddress.getLocalHost().getHostAddress();
      String raw_ip = this.ipAddress; 
      //System.out.println(raw_ip);
      String parsed_ip = raw_ip.replaceAll("[.]","");
      //System.out.println(parsed_ip);
      char[] ip_chars = parsed_ip.toCharArray();
      for (char digit: ip_chars){
        sum+= Character.getNumericValue(digit);
      }
      System.out.println(sum);
    }catch(Exception e){
      System.err.print(e);
    }
    
    return sum;
    
  }
  
  private void setIsLeader(boolean status){
    this.isLeader = status;
    return;
  }
  
  private int getBalance(){
    return this.balance;
  }
  
  private void withdraw(int amt){
    this.balance = this.balance - amt;
    return;
  }
  //something void initiateSnapshot(){}
  public void deposit(int amt){
    this.balance = this.balance + amt;
    return;
  }
  
  
  
  
  
  public static void main(String[] args) {

    VirtualMoneyAccount account = new VirtualMoneyAccount();
    
    try {
      account.run(args); 
    } catch (Exception e) {
      System.out.println("Account exception: " + e.toString());
      e.printStackTrace();
    }
  }
}