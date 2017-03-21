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
import java.util.Hashtable;

import java.io.*;  
import java.net.*; 


public class VirtualMoneyAccount implements AccountInterface{
  
  
  AccountInterface stubA;
  AccountInterface stubB;
  AccountInterface stubC;
  private Hashtable<Integer, AccountInterface> stubs;
  
  private Hashtable<Integer, Integer> channels;
  
  private String channelsAsString;
  
  private boolean snapshotCompleted;
  private boolean snapshotInitiated;
  private boolean firstTimeReceivingMarker; 
  private Hashtable<Integer, Integer> processesStates;
  
  private String processesChannelStates;
  private Hashtable<Integer, Integer> snapshotStatus;
  
  private boolean isLeader; //so that each account knows whether it is the leader or not
  private int leaderID;
  private AccountInterface leader;
  private boolean leaderDecided;
  
  private int balance;
  private int processID;
  private String ipAddress;
  
  //change to hashtable so that we have processIDs (keys) associated with stubs (values)
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
    processID = generateProcessID();
    isLeader = false; //start with no process the leader
    
    
    stubs = new Hashtable();
    channels = new Hashtable();
    processesStates = new Hashtable();
    processesChannelStates = "";
    snapshotStatus = new Hashtable();
    
    snapshotInitiated = false;
    snapshotCompleted = false;
    firstTimeReceivingMarker = true;
    
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
      
      
      boolean connected = false;
      System.out.println("I'm here!");
      while (!connected){
        try {
          //process has to lookup the other three processes in the registry
          Registry regA = LocateRegistry.getRegistry(otherIPs[0]);
          stubA = (AccountInterface) regA.lookup("Account");
          
          System.out.println("I'm here!");
            
          Registry regB = LocateRegistry.getRegistry(otherIPs[1]);
          stubB = (AccountInterface) regB.lookup("Account");
          
          Registry regC = LocateRegistry.getRegistry(otherIPs[2]);
          stubC = (AccountInterface) regC.lookup("Account");
          
          AccountInterface[] stubArray = {stubA, stubB, stubC};
          
          connected = true;
        }
        catch (NotBoundException e) {
          System.out.println("Not bound exception: " + e.toString());
          e.printStackTrace();
        }
      }
      System.out.println("I'm here!");
      //populate hashtable
      stubs.put(stubA.getProcessID(), stubA);
      stubs.put(stubB.getProcessID(), stubB);
      stubs.put(stubC.getProcessID(), stubC);
      
      
      //leader election
      electLeader();      
      
      //transactions loop
      int r = (int)(Math.random()*45000)+5000;
      int m = (int)(Math.random()*this.getBalance()-1)+1;
      int p = (int)(Math.random()*3); //randomly picks one of the other accts for a transaction
      
      while (snapshotCompleted == false){
        
        TimeUnit.MILLISECONDS.sleep(r); //wait random time
        stubArray[p].deposit(m, this.getProcessID()); //transfer random money to random other acct
        this.withdraw(m); //deduct from balance
        
        r = (int)(Math.random()*45000)+5000;
        m = (int)(Math.random()*this.getBalance()-1)+1;
        p = (int)(Math.random()*3);
        
        //if leader, initiate snapshot if haven't yet initiated
        if(this.isLeader && !snapshotInitiated){
          initiateSnapshot();
          snapshotInitiated = true;
        }
        if(this.isLeader){
          
          if (snapshotStatus.get(this.getProcessID())==4 && snapshotStatus.get(stubA.getProcessID())==4 && snapshotStatus.get(stubB.getProcessID())==4 && snapshotStatus.get(stubC.getProcessID())==4){
            snapshotCompleted = true;
          }
        }
      }
      
      //after loop, snapshot has been completed
      if (isLeader){
        recordSnapshotResult();
      }
      
    } catch (Exception e) {
      System.out.println("Account exception: " + e.toString());
      e.printStackTrace();
    }
    
  }
  
  
//LEADER ELECTION CODE
//----------------------------------------------------------------------------
  private void electLeader(){
    leaderID = getProcessID();
    Boolean leaderDecided = false;
    int lastSentID = getProcessID();
    
    
    while (!leaderDecided){
      try{
        if (isLeader){
        stubA.sendLeaderConfirmation(getProcessID());
      }
      stubA.passLeaderID(leaderID);
    
    
    //waits for leader to receive confirmation of election back from ring before returning from leader election method
    stubA.sendLeaderConfirmation(leaderID);
    if (!isLeader){
      while (!stubs.get(leaderID).isLeaderDecided()){
      }
    }
      }
      catch (RemoteException e){}
     }
  }
  
  public void passLeaderID(int value) throws RemoteException {
    if (value>leaderID && !leaderDecided){
      setLeaderID(value);
    }
    else if (value == getProcessID()){
      isLeader = true;
    }
  }
  
  public void sendLeaderConfirmation(int leaderPID){

      setLeaderID(leaderPID);
    setLeaderDecided();

  }
  
  public int getProcessID(){
    return processID; 
  }
  
  public void setLeaderID(int value){
    leaderID = value;
    return;
  }
  
  public void setLeader(){
    leader = stubs.get(leaderID);
  }
  
  public int getLeaderID(){
    return leaderID;
  }
  
  public boolean isLeaderDecided(){
    return leaderDecided;
  }
  
  public void setLeaderDecided(){
   this.leaderDecided = true; 
  }
  
  //-----------------------------------------------------------------------------
  
  
//SNAPSHOT CODE
//----------------------------------------------------------------------------
  public void initiateSnapshot(){
    saveBalance(getProcessID(), getBalance());
    updateSnapshotStatus(getProcessID());
    
    try {
    stubA.recordStates(this.getProcessID());
    stubB.recordStates(this.getProcessID());
    stubC.recordStates(this.getProcessID());
    } catch (RemoteException e){}
    
  }
  
//called on recipient of snapshot marker
  public void recordStates(int callingPID) throws RemoteException{
    if (firstTimeReceivingMarker){
      //-save balance by calling record method on leader -- leader.saveBalance(this.processID, this.balance)
      leader.saveBalance(callingPID, this.getBalance());
      leader.updateSnapshotStatus(callingPID);
      
      //-send own markers out to all other processes (basically call this method on other processes)
      stubA.recordStates(this.getProcessID());
      stubB.recordStates(this.getProcessID());
      stubC.recordStates(this.getProcessID());
      
      //-save channel from initPID to this proccess as empty -- leader.saveChannel(from, to, "");
      leader.saveChannel(callingPID, this.getProcessID(), 0);
      leader.updateSnapshotStatus(callingPID);
      
      //-begin recording all other channels into this process
      channels.clear(); 
    }
    else if (isLeader){
      int channel = channels.get(callingPID);
      saveChannel(callingPID, this.getProcessID(), channel);
      updateSnapshotStatus(callingPID);
    }
    else {
      //stop recording channel from initPID to this process
      int channel = channels.get(callingPID);
      //leader.saveChannel(initPID, this.PID, ChannelString);
      leader.saveChannel(callingPID, this.getProcessID(), channel);
      leader.updateSnapshotStatus(callingPID);
    }
  }
  
  
  public void saveBalance(int callingPID, int callingBalance){
    processesStates.put(callingPID, callingBalance);
  }
  
  public void saveChannel(int channelFrom, int channelTo, int channelValue){
    channelsAsString+="Channel from process ID " + channelFrom + " to process ID " + channelTo + ": $" + channelValue + "/n";
    try {leader.addChannelState(this.channelsAsString);} catch (RemoteException e){}
    
  }
  
  public void addChannelState(String channelInfo){
    processesChannelStates+=channelInfo;
  }
  
  public void updateSnapshotStatus(int pID){
    if (this.snapshotStatus.contains(pID)){
      int currentStatus = this.snapshotStatus.get(pID);
      this.snapshotStatus.put(pID, currentStatus++);
    }
        else {
          this.snapshotStatus.put(pID, 1);
        }
        
        }
  
  public void recordSnapshotResult(){
    try{
    System.out.println(getProcessID() + " state: " + processesStates.get(getProcessID()));
    
    int aIP = stubA.getProcessID();
    System.out.println(aIP + " state: " + processesStates.get(aIP));
    
    int bIP = stubB.getProcessID();
    System.out.println(bIP + " state: " + processesStates.get(bIP));
    
    int cIP = stubC.getProcessID();
    System.out.println(cIP + " state: " + processesStates.get(cIP));
  
    } catch (RemoteException e){}
  }
  
 
  
//----------------------------------------------------------------------------
  
  private int generateProcessID(){
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
  
  public void deposit(int amt, int callingPID){
    this.balance = this.balance + amt;
    if (this.channels.contains(callingPID)){
      int channelPrev = this.channels.get(callingPID);
      this.channels.put(callingPID, channelPrev + amt);                                          
    }
        else {
          this.channels.put(callingPID, amt);
        }
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