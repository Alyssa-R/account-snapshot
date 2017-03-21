import java.rmi.*;
import java.rmi.server.*;
import java.rmi.RemoteException;

import java.util.*;

public interface AccountInterface extends Remote {
  
  public void deposit(int amount, int callingID) throws RemoteException; 

  public int getProcessID() throws RemoteException;
  
  public void sendLeaderConfirmation(int leaderPID) throws RemoteException;
  
  public void passLeaderID(int value) throws RemoteException;
  
  public boolean isLeaderDecided() throws RemoteException;
 
  public void recordStates(int callingPID) throws RemoteException;
  
  public void saveBalance(int callingPID, int callingBalance) throws RemoteException;
  
  public void updateSnapshotStatus(int pID) throws RemoteException;
  
  public void saveChannel(int channelFrom, int channelTo, int channelValue) throws RemoteException;

  public void addChannelState(String channelInfo) throws RemoteException;
}