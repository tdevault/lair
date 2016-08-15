/**
 * BluetoothDiscoveryActivity.java
 * 
 * Activity for discovering bluetooth devices.  This activity can only be called from other
 * activities for bluetooth discovery and is not a standalone project. 
 * 
 * This is a library activity that when called, allows users to select an address, search for new
 * addresses, or type a new address.
 * 
 * Result string is returned in the intent with the key
 * "BluetoothDiscoveryActivity.sf_SELECTED_MAC_ADDRESS"
 * 
 * NOTE: Place following line of code in your androidManifest.xml file to access the activity: 
 * <activity android:name="cotsbots.devicedescovery.BluetoothDiscoveryActivity" android:screenOrientation="landscape" android:launchMode="standard"></activity>
 *  
 * NOTE: Place following lines of code in your androidManifest.xml file for propper permissions
 * <uses-permission android:name="android.permission.BLUETOOTH" />
 * <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" /> 
 *  
 * NOTE: Add the CotsBotsDeviceDiscovery library to your project:
 * 1a. Project should be located inside your android workspace folder and should be open
 *     inside your package explorer before following steps below. 
 * 1. Right click project folder in package explorer window located on left side of screen
 * 2. Click on Properties -> Android
 * 3. Scroll down to bottom of window and click "Add" inside the Library section
 * 4. Select CotsBotsDeviceDiscovery library
 * 
 * An example function you would place in a new activity that utilizes 
 * this activity (CotsBotsDeviceDiscovery) is shown below. 
 * 
 * @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == BluetoothDiscoveryActivity.REQUEST_CODE_BLUETOOTH) {
			setContentView(R.layout.main);

			if (resultCode == RESULT_OK) {
				DO STUFF HERE WITH NEW ADDRESS obtained by line of code below
				data.getStringExtra(BluetoothDiscoveryActivity.sf_SELECTED_MAC_ADDRESS)
			} else {
				result was cancelled, no address returned
			}
		}
	}
 * 
 * @author stinger
 */
package cotsbots.devicedescovery;

import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class BluetoothDiscoveryActivity extends Activity {
	// Location where new mac address can be found within the intent that
	// is sent back to your activity
	public static final String sf_SELECTED_MAC_ADDRESS = "deviceAddress";
	// Request code your activity can use to start the activity
	// and make sure that the proper result came back
	public static final int sf_REQUEST_CODE_BLUETOOTH = 1;

	// Length of proper mac address
	public static final int sf_MAC_ADDRESS_LENGTH = 17;

	// /////Member variables///////
	private String m_SelectedAddress = "";
	private BluetoothAdapter m_BtAdapter;
	private ArrayAdapter<String> m_PairedDevicesArrayAdapter;
	private ArrayAdapter<String> m_NewDevicesArrayAdapter;
	private EditText m_TextBoxMAC;
	private Button m_NewMACButton;

	/**
	 * Initial function called for activity
	 */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setupViews();
	}

	/**
	 * Listener called when new mac button is pressed
	 */
	private OnClickListener newMACButtonListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			Editable textEdit = m_TextBoxMAC.getText();

			m_SelectedAddress = (String) textEdit.toString();
			// Make sure that mac address typed is the proper length
			// Warning! currently only checks length of address, not true
			// correctness
			if (m_SelectedAddress.length() == sf_MAC_ADDRESS_LENGTH) {
				Intent intent = new Intent();
				intent.putExtra(sf_SELECTED_MAC_ADDRESS, m_SelectedAddress);
				setResult(RESULT_OK, intent);
				finish();
			} else {
				// Incorrect mac address length, stay on current screen
				((Button) v).setText("New MAC (Invalid MAC)");
			}
		}
	};

	/**
	 * Initialize all buttons and their listeners
	 */
	private void setupButtons() {
		Button scanButton = (Button) findViewById(R.id.button_scan);

		scanButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				doDiscovery();
				((Button) v).setText("Scanning");

				((Button) v).setEnabled(false);
				Button cancelB = (Button) findViewById(R.id.button_cancel);
				cancelB.setText("Cancel Scan");
			}
		});

		Button cancelButton = (Button) findViewById(R.id.button_cancel);
		cancelButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (m_BtAdapter.isDiscovering()) {
					m_BtAdapter.cancelDiscovery();
					((Button) v).setText("Back");
					//setupViews();
				} else {
					setResult(RESULT_CANCELED);
					finish();
				}
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		// Make sure we're not doing discovery anymore
		if (m_BtAdapter != null) {
			m_BtAdapter.cancelDiscovery();
		}

		// Unregister broadcast listeners
		try {
			this.unregisterReceiver(receiver);
		} catch (Exception exc) {
			// DO nothing. Currently there are no API methods for checking
			// if receiver is registered.
			// Android needs to fix this problem
		}
	}

	/**
	 * Start device discover with the BluetoothAdapter
	 */
	private void doDiscovery() {
		// Indicate scanning in the title
		// setProgressBarIndeterminateVisibility(true);
		setTitle(R.string.scanning);

		// Turn on sub-title for new devices
		findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

		// If we're already discovering, stop it
		if (m_BtAdapter.isDiscovering()) {
			m_BtAdapter.cancelDiscovery();
		}

		// Request discover from BluetoothAdapter
		m_BtAdapter.startDiscovery();
	}

	// The on-click listener for all devices in the ListViews
	private OnItemClickListener deviceClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
			// Cancel discovery because it's costly and we're about to connect
			m_BtAdapter.cancelDiscovery();

			// Get the device MAC address, which is the last 17 chars in the
			// View
			String info = ((TextView) v).getText().toString();
			if (info.length() > sf_MAC_ADDRESS_LENGTH) {
				m_SelectedAddress = info.substring(info.length()
						- sf_MAC_ADDRESS_LENGTH);
				// mMainContext.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
				Intent intent = new Intent();
				intent.putExtra(sf_SELECTED_MAC_ADDRESS, m_SelectedAddress);
				setResult(RESULT_OK, intent);
				finish();
			}
		}
	};

	// The BroadcastReceiver that listens for discovered devices and
	// changes the title when discovery is finished
	private final BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			// When discovery finds a device
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// Get the BluetoothDevice object from the Intent
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				// If it's already paired, skip it, because it's been listed
				// already
				if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
					m_NewDevicesArrayAdapter.add(device.getName() + "\n"
							+ device.getAddress());
				}
				// When discovery is finished, change the Activity title
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
					.equals(action)) {
				// setProgressBarIndeterminateVisibility(false);
				// setTitle(R.string.select_device);
				if (m_NewDevicesArrayAdapter.getCount() == 0) {
					String noDevices = getResources().getText(
							R.string.none_found).toString();
					m_NewDevicesArrayAdapter.add(noDevices);
				}

				Button scanButton = (Button) findViewById(R.id.button_scan);
				if (scanButton != null) {
					scanButton.setText("Scan for devices");
					scanButton.setEnabled(true);
				}
				Button cancelButton = (Button) findViewById(R.id.button_cancel);
				if (cancelButton != null) {
					cancelButton.setText("Back");
				}
			}
		}
	};
	
	private void setupViews() {
		setContentView(R.layout.device_list);

		m_TextBoxMAC = (EditText) findViewById(R.id.editText1);
		m_NewMACButton = (Button) findViewById(R.id.buttonNewMAC);
		m_NewMACButton.setOnClickListener(newMACButtonListener);
		InputFilter inputFilter[] = new InputFilter[1];

		// Create text input filter so only numbers and colons are input
		// characters
		inputFilter[0] = new InputFilter() {

			@Override
			public CharSequence filter(CharSequence source, int start, int end,
					Spanned dest, int dstart, int dend) {
				if (source.length() > 0) {
					char newChar = source.charAt(source.length() - 1);
					// Bluetooth MAC address can only be digits
					if (((newChar >= '0') && (newChar <= '9'))
							|| (newChar == ':')) {
						if (dstart > 16) {
							return source.subSequence(0, 0);
						}
						return source;
					} else {
						return source.subSequence(0, 0);
					}
				}
				return null;
			}
		};
		m_TextBoxMAC.setFilters(inputFilter);
		// look for enter key press to close soft keyboard
		m_TextBoxMAC.setOnKeyListener(new View.OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if ((event.getAction() == KeyEvent.ACTION_DOWN)
						&& (keyCode == KeyEvent.KEYCODE_ENTER)) {
					InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(m_TextBoxMAC.getWindowToken(), 0);
					return true;
				}
				return false;
			}
		});
		setupButtons();
		// Initialize array adapters. One for already paired devices and
		// one for newly discovered devices
		m_PairedDevicesArrayAdapter = new ArrayAdapter<String>(this,
				R.layout.device_name);
		m_NewDevicesArrayAdapter = new ArrayAdapter<String>(this,
				R.layout.device_name);

		// Find and set up the ListView for paired devices
		ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
		pairedListView.setAdapter(m_PairedDevicesArrayAdapter);
		pairedListView.setOnItemClickListener(deviceClickListener);

		// Find and set up the ListView for newly discovered devices
		ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
		newDevicesListView.setAdapter(m_NewDevicesArrayAdapter);
		newDevicesListView.setOnItemClickListener(deviceClickListener);

		// Register for broadcasts when a device is discovered
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		this.registerReceiver(receiver, filter);

		// Register for broadcasts when discovery has finished
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		this.registerReceiver(receiver, filter);

		// Get the local Bluetooth adapter
		m_BtAdapter = BluetoothAdapter.getDefaultAdapter();

		// Get a set of currently paired devices
		Set<BluetoothDevice> pairedDevices = m_BtAdapter.getBondedDevices();

		// If there are paired devices, add each one to the ArrayAdapter
		if (pairedDevices.size() > 0) {
			findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
			for (BluetoothDevice device : pairedDevices) {
				m_PairedDevicesArrayAdapter.add(device.getName() + "\n"
						+ device.getAddress());
			}
		} else {
			String noDevices = getResources().getText(R.string.none_paired)
					.toString();
			m_PairedDevicesArrayAdapter.add(noDevices);
		}
	}

}