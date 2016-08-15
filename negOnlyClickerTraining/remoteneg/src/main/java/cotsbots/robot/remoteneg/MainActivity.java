package cotsbots.robot.remoteneg;


import cotsbots.devicedescovery.BluetoothDiscoveryActivity;

import android.os.Bundle;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity {
	
	public String robotAddress;
	public remoteSend remote;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.remotemenu, menu);
		return true;
	}
	
	 public boolean onOptionsItemSelected(MenuItem item){
	         
	        switch (item.getItemId()){
				case R.id.Start:
		        	remote.stateClick('6');
		        	return true;
		        case R.id.TrainANN:
		        	remote.stateClick('7');
		        	return true;
		        case R.id.Autonomous:
		        	remote.stateClick('8');
		        	return true;
		        case R.id.Done:
		        	remote.sendDone();
		        case R.id.Save:
		        	remote.saveTraining();
		        	return true;
		        case R.id.Load:
		        	remote.loadTraining();
		        	return true;
		        case R.id.SaveANN:
		        	remote.saveANN();
		        	return true;
		        case R.id.LoadANN:
		        	remote.loadANN();
		        	return true;
		        case R.id.Evolve10:
		        	remote.evolveTen();
		        	return true;
	        }
	        return true;
	 }
	
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == BluetoothDiscoveryActivity.sf_REQUEST_CODE_BLUETOOTH) {
			setContentView(R.layout.activity_main);
			if (resultCode == RESULT_OK) {
				
				robotAddress = data.getStringExtra(BluetoothDiscoveryActivity.sf_SELECTED_MAC_ADDRESS);
			}
		}
		
		
	}
	
	public void selectMac(View view){
		Intent macIntent = new Intent(this, BluetoothDiscoveryActivity.class);
		startActivityForResult(macIntent, BluetoothDiscoveryActivity.sf_REQUEST_CODE_BLUETOOTH);
	}
	public void startRemote(View view){
		BluetoothAdapter myAdapter = BluetoothAdapter.getDefaultAdapter();
		BluetoothDevice myDevice = myAdapter.getRemoteDevice(robotAddress);
		remote = new remoteSend(myDevice);
		remote.start();
		while(!remote.connected){
			;
		}
		setContentView(R.layout.remote_layout);
	}
	
	
	public void sendPositive(View view){remote.driveClick('p');	}


	public void sendNegative(View view){
		remote.driveClick('n');
	}
	
	public void sendPause(View view){
		remote.driveClick('0');
	}
	

}
