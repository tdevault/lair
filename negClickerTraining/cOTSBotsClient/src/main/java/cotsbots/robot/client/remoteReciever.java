package cotsbots.robot.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;


public class remoteReciever extends Thread{
	private final BluetoothServerSocket mmServerSocket;
    public BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private static final UUID sf_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B35FB");
    public InputStream m_input;
    public String message;
    public boolean connected = false;
    public boolean inCreated = false;
    
    
    public remoteReciever() {
        // Use a temporary object that is later assigned to mmServerSocket,
        // because mmServerSocket is final
    	
        BluetoothServerSocket tmp = null;
        try {
            // MY_UUID is the app's UUID string, also used by the client code
            tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(mBluetoothAdapter.getName(), sf_UUID);
            //Log.e("in acceptThread", "seversocket made");
        } catch (IOException e) { 
        	//Log.e("in acceptThread", "server socket failed");
        	}
        mmServerSocket = tmp;
    }
 
    public void run() {
        BluetoothSocket socket = null;
        // Keep listening until exception occurs or a socket is returned
        while (!connected && !inCreated) {
        	Log.e("in acceptThread", "Trying to accept");
        	
            try {
                Log.e("in acceptThread", "Trying to accept2");
                socket = mmServerSocket.accept();
                Log.e("in acceptThread", "accepted");
            } catch (IOException e) {
            	Log.e("in acceptThread", "not accepted");
            }
            // If a connection was accepted
            if (socket != null) {
                // Do work to manage the connection (in a separate thread)
            	connected = true;
                try {
                	Log.e("in acceptThread", "connection accepted, opening input stream");
					m_input = socket.getInputStream();
					inCreated = true;
				} catch (IOException e) {
					
				}                
            }
        }
    }
    
    public char read(){
    	
    	try {
				if(m_input.available() > 0){
					
					return (char) m_input.read();
					
				}
				else{
					return '0';
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				
			}
		return '0';
    	
    }
    
    
    public boolean ready(){
    	try {
			if ( m_input.available() > 0 ){
				return true;
			}
			else{
				return false;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
    }
   
    /** Will cancel the listening socket, and cause the thread to finish */
    public void cancel() {
        try {
            mmServerSocket.close();
        } catch (IOException e) { }
    }
}