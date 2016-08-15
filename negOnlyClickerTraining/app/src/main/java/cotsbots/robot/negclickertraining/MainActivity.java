package cotsbots.robot.negclickertraining;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import cotsbots.devicedescovery.BluetoothDiscoveryActivity;
import cotsbots.graduate.robotcontroller.RobotController;
import cotsbots.robot.client.robotClient;
import cotsbots.robot.data.EvolutionConditions;
import cotsbots.robot.data.RobotData;
import cotsbots.robot.robot.Robot;

public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener {
    public String robotAddress;
    public CameraBridgeViewBase mOpenCvCameraView;
    public robotClient client;
    public Robot myRobot;
    public RobotData myRobotData;
    public boolean ready = false;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.e("LCBI", "success");
                    //Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        setContentView(R.layout.activity_main);

        //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mOpenCVCallBack);
        mOpenCvCameraView = (CameraBridgeViewBase) new JavaCameraView(this, -1);
        mOpenCvCameraView.setCvCameraViewListener(this);
        //Intent check = new Intent();
        //check.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        //startActivityForResult(check, Activity.RESULT_OK);

        Intent macIntent = new Intent(this, BluetoothDiscoveryActivity.class);
        startActivityForResult(macIntent, BluetoothDiscoveryActivity.sf_REQUEST_CODE_BLUETOOTH);
    }



    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BluetoothDiscoveryActivity.sf_REQUEST_CODE_BLUETOOTH) {
            setContentView(R.layout.activity_main);
            if (resultCode == RESULT_OK) {

                robotAddress = data.getStringExtra(BluetoothDiscoveryActivity.sf_SELECTED_MAC_ADDRESS);
                //startRobot(new View(this));
            }
        }


    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            //Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            // Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onCameraViewStarted(int width, int height) {
        Log.e("onCamStart", "width " + width + " height " + height);
    }
    public void onCameraViewStopped() {
    }
    public Mat onCameraFrame(Mat inputFrame) {
        //Log.e("onCamFrame", "called");
        if (ready && !myRobot.processLock) {
            return myRobot.frameReceived(inputFrame);
        }
        return inputFrame;

    }


    public void startRobot(View view){
        myRobotData = new RobotData();
        myRobotData.Conditions = new EvolutionConditions(0, false, false,1 ,0);
        myRobot = new Robot(robotAddress, RobotController.SteeringType.TRACKED, myRobotData, true, this);

        client = new robotClient( RobotController.SteeringType.TRACKED, myRobot);
        client.startRemote();

        setContentView(mOpenCvCameraView);
        ready = true;
    }

    public void startRobotBP(View view){
        myRobotData = new RobotData();
        myRobotData.Conditions = new EvolutionConditions(100, false, false,1 ,0);
        myRobot = new Robot(robotAddress, RobotController.SteeringType.TRACKED, myRobotData, true, this);

        client = new robotClient( RobotController.SteeringType.TRACKED, myRobot);
        client.startRemote();

        setContentView(mOpenCvCameraView);
        ready = true;
    }


}
