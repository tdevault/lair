/**
 * CotsBotsRobotController.java
 * 
 * Class used for driving robots using bluetooth connection between phone and
 * arduino.
 * TO Use:
 * Create an instance of CotsBotsRobotController(String bluetoothaddress)
 * For simple robot driving use one of the "Basic Driving Instructions"
 * For variable speed and turning navigation, use one of the following: 
 * 1. driveVehicle(double direction, double speed,SteeringType steerType)
 * 2. driveRobotNegativeOneToOne(double x, double y,SteeringType steerType)
 */
package cotsbots.graduate.robotcontroller;


import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

/**
 * @author stinger
 * 
 */

public class RobotController {
	public enum SteeringType {
		TRACKED, SERVO
	}

	private static final UUID sf_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static final char sf_FORWARD = 'F';
	private static final char sf_BACKWARD = 'B';
	private static final char sf_RIGHT_FORWARD = 'R';
	private static final char sf_RIGHT_BACKWARD = 'r';
	private static final char sf_LEFT_FORWARD = 'L';
	private static final char sf_LEFT_BACKWARD = 'l';
	private static final char sf_STOP = 'S';
	private static final char sf_INCREASE_SPEED = '+';
	private static final char sf_DECREASE_SPEED = '-';
	private static final String sf_TAG = "controller";
	private static final double sf_MOTOR_STALL_THRESHOLD = 0.4;

	/**
	 * Member variables
	 */
	private BluetoothSocket m_BlueToothSocket;
	private BluetoothAdapter m_BluetoothAdapter = BluetoothAdapter
			.getDefaultAdapter();
	private OutputStream m_OutStream;
	private BluetoothDevice m_BluetoothDevice = null;
	private String m_Address = "";
	private btThread m_BluetoothThread;
	private boolean m_Connected;

	/**
	 * Constructor for initializing communication between phone and arduino
	 * 
	 * @param buletoothAddress
	 *            bluetooth MAC address for arduino module
	 */
	public RobotController(String buletoothAddress) {
		m_Address = buletoothAddress;
		m_Connected = false;
		connect();
	}

	/**
	 * Function called to unpair phone and arduino module
	 */
	public void disconnect() {
		if (m_OutStream != null) {
			try {
				m_OutStream.close();
			} catch (IOException e) {
				Log.e(sf_TAG, "Failed to close output stream");
			}
		}
	}// end disconnect

	/*
	 * Use these public methods for robot movement
	 */

	// ///////////start- Basic Driving Instructions//////////////
	public void driveForward() {
		write(sf_FORWARD);
	}

	public void driveBackward() {
		write(sf_BACKWARD);
	}

	// use for turning tracked vehicle right
	public void turnRightForward() {
		write(sf_RIGHT_FORWARD);
	}

	// use for turning tracked vehicle left
	public void turnLeftForward() {
		write(sf_LEFT_FORWARD);
	}

	public void stop() {
		write(sf_STOP);
	}

	// Not used for tracked vehicle
	public void turnLeftBackward() {
		write(sf_LEFT_BACKWARD);
	}

	// Not used for tracked vehicle
	public void turnRightBackward() {
		write(sf_RIGHT_BACKWARD);
	}

	// This function increase arduino throttle value
	// for PWM driving vehicles
	public void increaseSpeed() {
		write(sf_INCREASE_SPEED);
	}

	// This funciton decreases arduino throttle value
	// for PWM driving vehicles
	public void decreaseSpeed() {
		write(sf_DECREASE_SPEED);
	}

	
	public void driveForwardSlow(){
		driveTracked(.5,.5);
	}
	
	public void trnLeftSlow(){
		driveTracked(-.75,.75);
	}
	public void turnRightSlow(){
		driveTracked(.75,-.75);
	}
	
	
	// /////////////////////////////////////////////////////////////////

	// /////////start- Variable Speed and turning for vehicles/////////
	/**
	 * driveVehicle is for either type of rc vehicle (currently servo and
	 * tracked) driving vehicle based on two values (direction,speed) each
	 * representing ranges between 0-1. If vehicle was specified as tracked
	 * steering, then necessary calculations will be done to properly drive
	 * vehicle.
	 * 
	 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!WARNING!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	 * Actual speed of TRACKED VEHICLE is: sqrt(direction squared + speed
	 * squared) So don't be fooled that only centering the speed will stop
	 * vehicle
	 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	 * 
	 * 
	 * If vehicle uses servo style steering, then direction represents steering
	 * value and speed represents power/speed value.
	 * 
	 * @param direction
	 *            range from 0-1; 0 means turning left; 0.5 means straight; 1
	 *            means turning right;
	 * @param speed
	 *            range from 0-1; 0 means drive backwards; 0.5 means stopped; 1
	 *            means drive forwards;
	 */
	public void driveVehicle(double direction, double speed,
			SteeringType steerType) {
		if (!checkConnection()) {
			// try to connect if not currently m_Connected
			connect();
		}
		// make sure connection is there before trying to drive robot
		if (checkConnection()) {
			switch (steerType) {
			case SERVO:
				driveRC(direction, speed);
				break;
			case TRACKED:
				convertToTrackedDriving(direction, speed);
				break;
			default:
				convertToTrackedDriving(direction, speed);
				break;
			}
		}
	}

	/**
	 * driveRobotNegativeOneToOne is for driving either type of rc vehicle
	 * (currently tracked or servo style)
	 * ***********************************************************************
	 * For tracked vehicles, x and y represent left and right track respectively
	 * -1 means backwards; 0 means stopped; 1 means forwards;
	 * ***********************************************************************
	 * For Servo vehicle x and y represent steering and speed respectively.
	 * 
	 * x = -1 means turning left; x = 0 means straight ahead; x = 1 means
	 * turning right;
	 * 
	 * y = -1 means drive backwards y = 0 means stopped y = 1 means drive
	 * forwards
	 * ************************************************************************
	 * 
	 * @param x
	 *            double value from -1 to 1 in range
	 * @param y
	 *            double value from -1 to 1 in range
	 * @param steerType
	 */
	public void driveRobotNegativeOneToOne(double x, double y,
			SteeringType steerType) {
		if (!checkConnection()) {
			// try to connect if not currently m_Connected
			connect();
		}
		// make sure connection is there before trying to drive robot
		if (checkConnection()) {
			switch (steerType) {
			case SERVO:
				// shrink range down to 0-1 for direction and speed
				driveRC(((x + 1.0) / 2.0), ((y + 1.0) / 2.0));
				break;
			case TRACKED:
				driveTracked(x, y);
				break;
			default:
				driveTracked(x, y);
				break;
			}
		}
	}

	// //////////////////////////////////////////////////////////////////////////////
	/**
	 * 
	 * 
	 * 
	 * 
	 */
	// /////////////start- Private functions not visible to users
	// ///////////////////

	/**
	 * Function to create 6 byte driving data expected by arduino for tracked
	 * robot
	 * 
	 * @param leftTrack
	 *            double value from -1 to 1
	 * @param rightTrack
	 *            double value from -1 to 1
	 */
	private void driveTracked(double leftTrack, double rightTrack) {
		char[] driveBytes = new char[6];
		driveBytes[0] = '#';

		// +-sf_MOTOR_STALL_THRESHOLD was chosen as the threshold for the tracked
		// vehicle turning. x in the range of:
		// -sf_MOTOR_STALL_THRESHOLD < x < sf_MOTOR_STALL_THRESHOLD
		// made motors stall and would not drive vehicle
		if (leftTrack < -sf_MOTOR_STALL_THRESHOLD) {
			// put a floor of -1 on the value
			leftTrack = Math.max(leftTrack, -1);
			driveBytes[1] = sf_BACKWARD;
		} else if (leftTrack > sf_MOTOR_STALL_THRESHOLD) {
			// put a ceiling of 1 on the value
			leftTrack = Math.min(leftTrack, 1);
			driveBytes[1] = sf_FORWARD;
		} else {
			driveBytes[1] = sf_STOP;
		}
		driveBytes[2] = (char) Math.abs(255 * leftTrack);

		if (rightTrack < -sf_MOTOR_STALL_THRESHOLD) {
			// put a floor of -1 on the value
			rightTrack = Math.max(rightTrack, -1);
			driveBytes[3] = sf_BACKWARD;
		} else if (rightTrack > sf_MOTOR_STALL_THRESHOLD) {
			// put a ceiling of -1 on the value
			rightTrack = Math.min(rightTrack, 1);
			driveBytes[3] = sf_FORWARD;
		} else {
			driveBytes[3] = sf_STOP;
		}
		driveBytes[4] = (char) Math.abs(255 * rightTrack);

		driveBytes[5] = ';';

		writeBytes(driveBytes);
	}

	/**
	 * Function to create 6 byte driving data expected by arduino for RC servo
	 * steering robot
	 * 
	 * @param turnDirection
	 *            double value from 0 to 1. low values represent turning to
	 *            left, high values represent turning right, with .5 = straight
	 * @param power
	 *            speed/power double value from 0 to 1. below .5 represents
	 *            backwards, larger than .5 drives forward, .5 = stopped
	 */
	private void driveRC(double turnDirection, double power) {
		char[] driveBytes = new char[6];
		driveBytes[0] = '#';

		driveBytes[1] = 'T';
		// place floor and ceiling values of 1,-1 respectively
		turnDirection = turnDirection > 1.0 ? 1.0 : turnDirection;
		turnDirection = turnDirection < 0 ? 0 : turnDirection;
		driveBytes[2] = (char) Math.abs(180 * turnDirection);

		driveBytes[3] = 'P';
		// place floor and ceiling values of 1,-1 respectively
		power = power > 1.0 ? 1.0 : power;
		power = power < -1.0 ? -1.0 : power;
		driveBytes[4] = (char) Math.abs(180 * power);

		driveBytes[5] = ';';

		writeBytes(driveBytes);
	}

	/**
	 * Converts steering and speed coordinate ranges from 0-1 to a range of -1
	 * and 1 and correctly calculates values for driving a tracked vehicle. The
	 * function then calls driveTracked(left,right);
	 * 
	 * @param direction
	 *            a range of values between 0-1
	 * @param speed
	 *            a range of values between 0-1
	 */
	private void convertToTrackedDriving(double direction, double speed) {
		double leftValue;
		double rightValue;
		double actualSpeed;

		direction = (direction * 2.0) - 1;
		speed = (speed * 2.0) - 1;
		if (direction >= 0) {
			leftValue = 1.0;
			rightValue = 1.0 - (2 * direction);
		} else {
			rightValue = 1.0;
			leftValue = 1.0 + (2 * direction);
		}

		// Actual speed calculated using direction and speed.
		// Originally used with joystick driving where distance the joystick
		// moved from the origin was considered the speed of the vehicle.
		// Therefore moving the joystick completely to right or left
		// without any forward or backward motion would still allow turning

		//actualSpeed = 2 * (Math.sqrt(Math.pow(direction, 2)
		//		+ Math.pow(speed, 2)));

		if (speed < 0) {
			driveTracked(rightValue * speed * -1, leftValue * speed
					* -1);
		} else {
			driveTracked(leftValue * speed, rightValue * speed);
		}
	}

	private void writeBytes(char[] dataVals) {
		if (m_OutStream != null) {
			try {
				for (char c : dataVals) {
					m_OutStream.write(c);
				}
			} catch (IOException e) {
				Log.e(sf_TAG, e.toString());
			}
		}
	}// end of writeBytes

	private void write(char data) {
		if (m_OutStream != null) {
			try {
				m_OutStream.write(data);
			} catch (IOException e) {

				// Log.e(sf_TAG, e.toString());
			}
		}
	}// end of write
	
	

	public boolean isConnected(){
		return m_Connected;
	}

	/**
	 * Funciton used to setup actual connection
	 */
	private void connect() {
		disconnect();
		m_BluetoothThread = new btThread();
		m_BluetoothThread.start();

	}// end connect

	private boolean checkConnection() {
		return m_Connected;
	}

	private class btThread extends Thread {

		btThread() {
			Log.i(sf_TAG, "Instantiated new btThread");
			m_Connected = true;
		}

		// function automatically called during thread.start();
		// it overrides the default run function within thread class
		@Override
		public void run() {
			boolean succeeded = false;
			int timesAttempted = 0;
			do {
				timesAttempted++;
				try {
					m_BluetoothDevice = m_BluetoothAdapter
							.getRemoteDevice(m_Address);
					try {
						m_BlueToothSocket = m_BluetoothDevice
								.createRfcommSocketToServiceRecord(sf_UUID);

						m_BluetoothAdapter.cancelDiscovery();

						try {
							m_BlueToothSocket.connect();
						} catch (IOException e1) {
							Log.e(sf_TAG, "m_BlueToothSocket failed to connect");
							try {
								m_BlueToothSocket.close();
							} catch (IOException e2) {
								Log.e(sf_TAG,
										"m_BlueToothSocket failed to close, in e1(connectFail)");
							}
						}

						try {
							m_OutStream = m_BlueToothSocket.getOutputStream();
							succeeded = true;
						} catch (IOException e2) {
							Log.e(sf_TAG,
									"Failed to open m_BlueToothSocket output stream");
							m_Connected = false;
						}
					} catch (IOException e) {
						Log.e(sf_TAG, "Failed to setup bt socket");
						m_Connected = false;
					}
				} catch (IllegalArgumentException e) {
					Log.e(sf_TAG, "Illegal Arg Exc. on m_BlueToothSocket try/catch");
					m_Connected = false;
				} catch (NullPointerException e) {
					Log.e(sf_TAG, "Null Pointer");
				}
				// attempt 10 times before giving up if no success
			} while (!succeeded && timesAttempted < 10);

		}// end of run

	}// end of btThread
}

/*
 * public void run() {
 * 
 * try { m_BluetoothDevice = m_BluetoothAdapter.getRemoteDevice(m_Address); try {
 * m_BlueToothSocket = m_BluetoothDevice .createRfcommSocketToServiceRecord(sf_UUID);
 * } catch (IOException e) { Log.e(sf_TAG, "Failed to setup bt socket"); m_Connected
 * = false; } } catch (IllegalArgumentException e) { Log.e(sf_TAG,
 * "Illegal Arg Exc. on m_BlueToothSocket try/catch"); m_Connected = false; }
 * catch (NullPointerException e) { Log.e(sf_TAG, "Null Pointer"); }
 * 
 * m_BluetoothAdapter.cancelDiscovery();
 * 
 * try { m_BlueToothSocket.connect(); } catch (IOException e1) { Log.e(sf_TAG,
 * "m_BlueToothSocket failed to connect"); try { m_BlueToothSocket.close(); }
 * catch (IOException e2) { Log.e(sf_TAG,
 * "m_BlueToothSocket failed to close, in e1(connectFail)"); } }
 * 
 * try { m_OutStream = m_BlueToothSocket.getOutputStream(); } catch (IOException
 * e2) { Log.e(sf_TAG, "Failed to open m_BlueToothSocket output stream"); m_Connected
 * = false; } }// end of run
 */
