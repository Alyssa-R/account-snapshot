import java.rmi.*;
import java.rmi.server.*;
import java.rmi.RemoteException;

import java.util.*;

public interface AccountInterface extends Remote {
  
  public void deposit(int amount) throws RemoteException; //if needed, take sender info to record channel statuses
  //potential snapshot related methods -- sendMarker, 
  //leadership election methods -- 
 
}