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


public class VirtualMoneyAccount implements MoneyAccountInterface{
  
  
  AccountInterface stub1;
  AccountInterface stub2;
  AccountInterface stub3;
  
  static boolean snapshotCompleted = false;
  
  private boolean isLeader; //so that each account knows whether it is the leader or not
  private boolean snapshotInitiated;
  
  private int balance;
  private int processId;
  private String ipAddress;
  
  private AccountInterface[] stubArray;
  
  
  public VirtualMoneyAccount(String[] otherIPs) {
    snapshotInitiated = false;
    balance = 200;    
    ipAddress = InetAddress.getLocalHost().getHostAddress();
    processId = generateProcessId;
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
      stubA = (AccountInterface) regA.lookup("Account");
      
      Registry regB = LocateRegistry.getRegistry(otherIPs[1]);
      stubB = (AccountInterface) regB.lookup("Account");
      
      Registry regC = LocateRegistry.getRegistry(otherIPs[2]);
      stubC = (AccountInterface) regC.lookup("Account");
      
      AccountInterface[] stubArray = {stubA, stubB, stubC};
      
      
      //leader election
      electLeader();      
      // SEMAPHORES HERE TO MAKE SURE ACCOUNT TRANSACTIONS DON'T OCCUR UNTIL EVERYONE IS ON SAME PAGE ABOUT LEADER
      
      

      //transactions loop
      Boolean snapshotInitiated = false;
      Boolean snapshotDone = false;
      int r = (int)(Math.random()*45000)+5000;
      int m = (int)(Math.random()*this.getBalance()-1)+1;
      int p = (int)(Math.random()*3); //randomly picks one of the other accts for a transaction
      
      while (shapShotDone == false){
        
        //if leader, initiate snapshot if haven't yet initiated
        if(this.isLeader && !snapshotInitiated){
          initiateSnapshot();
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
  
  private void generateProcessID(){}
  private void setIsLeader(){}
  private int getBalance(){}
  private void withdraw(){}
  //something void initiateSnapshot(){}
  public void deposit(int){}
  
  
  
  
  
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