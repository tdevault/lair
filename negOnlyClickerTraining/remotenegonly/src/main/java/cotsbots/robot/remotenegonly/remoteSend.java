package cotsbots.robot.remotenegonly;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;


public class remoteSend extends Thread {
    private final BluetoothSocket mmSocket;
    public boolean connected = false;
    private static final UUID sf_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B35FB");
    
    public OutputStream output;
    
    
    public remoteSend( BluetoothDevice device) {
        // Use a temporary object that is later assigned to mmSocket,
        // because mmSocket is final
        BluetoothSocket tmp = null;
        //Log.e("in connect thread", "device name: "+ device.getName() + " UUID" + sf_UUID.toString());
        // Get a BluetoothSocket to connect with the given BluetoothDevice
        try {
            // MY_UUID is the app's UUID string, also used by the server code
        	
            tmp = device.createRfcommSocketToServiceRecord(sf_UUID);
        } catch (IOException e) { }
        mmSocket = tmp;
    }
 
    
    
    public void driveClick(char dir){
    	try {
			output.write(dir);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			
		}
    }
    public void stateClick(char state){
    	try {
			output.write(state);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			
		}
    }

    public void saveTraining(){
    	try {
			output.write('s');
		} catch (IOException e) {
			// TODO Auto-generated catch block
			
		}
    }
    public void sendDone(){
    	try {
			output.write('d');
		} catch (IOException e) {
			// TODO Auto-generated catch block
			
		}
    }
    public void saveANN(){
    	try {
			output.write('a');
		} catch (IOException e) {
			// TODO Auto-generated catch block
			
		}
    }
    public void loadTraining(){
    	try {
			output.write('l');
		} catch (IOException e) {
			// TODO Auto-generated catch block
			
		}
    }
    public void loadANN(){
    	try {
			output.write('b');
		} catch (IOException e) {
			// TODO Auto-generated catch block
			
		}
    }
    public void setupEvolution(){
    	try {
			output.write('e');
		} catch (IOException e) {
			// TODO Auto-generated catch block
			
		}
    }
    public void reset(){
    	try {
			output.write('r');
		} catch (IOException e) {
			// TODO Auto-generated catch block
			
		}
    }
	public void evolveTen(){
		try {
			output.write('t');
		} catch (IOException e) {
			// TODO Auto-generated catch block

		}
	}
  
    
    public void run() {
        // Cancel discovery because it will slow down the connection
        
    	Log.e("in connect thread", "trying to connect");
    	while(!connected){
	        try {
	            // Connect the device through the socket. This will block
	            // until it succeeds or throws an exception
	            mmSocket.connect();
	            connected = true;
	            Log.e("in connect thread", "connected");
	        } catch (IOException connectException) {
	            // Unable to connect; close the socket and get out
	        	 Log.e("in connect thread", " not connected");
	            try {
	                mmSocket.close();
	            } catch (IOException closeException) { }
	            
	        }
    	}
 
        try {
			output = mmSocket.getOutputStream();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.e("in connect thread", "output stream failed");
			
		}
        
    }
 
    /** Will cancel an in-progress connection, and close the socket */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }
}