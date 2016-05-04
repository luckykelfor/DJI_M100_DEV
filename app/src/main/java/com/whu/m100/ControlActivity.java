package com.whu.m100;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.TextureView.SurfaceTextureListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;

import dji.sdk.AirLink.DJILBAirLink.DJIOnReceivedVideoCallback;
import dji.sdk.Battery.DJIBattery;
import dji.sdk.Battery.DJIBattery.DJIBatteryStateUpdateCallback;
import dji.sdk.Camera.DJICamera;
import dji.sdk.Camera.DJICamera.CameraReceivedVideoDataCallback;
import dji.sdk.Codec.DJICodecManager;
import dji.sdk.FlightController.DJIFlightController;
import dji.sdk.FlightController.DJIFlightControllerDataType.DJIFlightControllerCurrentState;
import dji.sdk.FlightController.DJIFlightControllerDelegate.FlightControllerReceivedDataFromExternalDeviceCallback;
import dji.sdk.base.DJIBaseComponent.DJICompletionCallback;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.base.DJIBaseProduct.Model;
import dji.sdk.base.DJIError;

public class ControlActivity extends Activity implements SurfaceTextureListener, OnClickListener {

    private double scale;
    private final int DELAY_TIME = 200;
    private final String START = "q";
    private final String END = "d";
    private final String TAG = ControlActivity.class.getName();

    private DJICamera camera = null;
    private DJIBattery battery = null;
    private DJIBaseProduct product = null;
    private DJICodecManager codecManager = null;
    private DJIFlightController flightController = null;
    private DJIOnReceivedVideoCallback onReceiveVideoCallback = null;
    private CameraReceivedVideoDataCallback receivedVideoDataCallBack = null;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // updateTitleBar();
            initPreviewer();
        }
    };
    private DJICompletionCallback djiCompletionCallback = new DJICompletionCallback() {
        @Override
        public void onResult(DJIError djiError) {
            if (djiError != null) {
                showToast(djiError.getDescription());
            }
        }
    };
    private FlightControllerReceivedDataFromExternalDeviceCallback recvCallback = new
            FlightControllerReceivedDataFromExternalDeviceCallback() {
                @Override
                public void onResult(byte[] bytes) {
                    try {
                        String slux = new String(bytes, 0, 3, "ascii");
                        String sluy = new String(bytes, 3, 3, "ascii");
                        String srux = new String(bytes, 6, 3, "ascii");
                        String sruy = new String(bytes, 9, 3, "ascii");
                        String sldx = new String(bytes, 12, 3, "ascii");
                        String sldy = new String(bytes, 15, 3, "ascii");
                        String srdx = new String(bytes, 18, 3, "ascii");
                        String srdy = new String(bytes, 21, 3, "ascii");
                        String ssid = new String(bytes, 24, 3, "ascii");
                        String longitude = new String(bytes, 27, 8, "ascii");
                        String latitude = new String(bytes, 35, 7, "ascii");
                        GPSInfo.setText(longitude + ',' + latitude);
                        int lux = (int) (Integer.valueOf(slux) * scale);
                        int luy = (int) (Integer.valueOf(sluy) * scale);
                        int rux = (int) (Integer.valueOf(srux) * scale);
                        int ruy = (int) (Integer.valueOf(sruy) * scale);
                        int ldx = (int) (Integer.valueOf(sldx) * scale);
                        int ldy = (int) (Integer.valueOf(sldy) * scale);
                        int rdx = (int) (Integer.valueOf(srdx) * scale);
                        int rdy = (int) (Integer.valueOf(srdy) * scale);
                        int id = (int) (Integer.valueOf(ssid));
                        drawView.setXY(lux, luy, rux, ruy, ldx, ldy, rdx, rdy, id);
                        drawView.invalidate();
                    } catch (UnsupportedEncodingException e) {
                        Log.d(TAG, "Not ASCII string !");
                    }
                }
            };
    private DJIBatteryStateUpdateCallback batteryCallback = new DJIBatteryStateUpdateCallback() {
        @Override
        public void onResult(DJIBattery.DJIBatteryState djiBatteryState) {
            if (djiBatteryState != null) {
                int percent = djiBatteryState.getBatteryEnergyRemainingPercent();
                if (percent >= 30) {
                    batteryText.setTextColor(getResources().getColor(R.color.lime));
                } else if (percent >= 15) {
                    batteryText.setTextColor(getResources().getColor(R.color.yellow));
                } else if (percent > 0) {
                    batteryText.setTextColor(getResources().getColor(R.color.red));
                }
                batteryText.setText(String.valueOf(percent) + '%');
            } else {
                batteryText.setText(getString(R.string.NA));
            }
        }
    };

    private DrawView drawView;
    private TextView textView;
    private EditText sendText;
    private TextView GPSInfo;
    private TextView heightText;
    private TextView batteryText;
    private TextureView videoSurface;

    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (flightController != null) {
                heightText.setText(String.valueOf(flightController.getCurrentState().getUltrasonicHeight()));
            } else {
                heightText.setText(getString(R.string.not_available));
            }
            handler.postDelayed(this, DELAY_TIME);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        // final int WIDTH = 1280;
        // final int HEIGHT = 720;

        // get all widget
        drawView = (DrawView) findViewById(R.id.drawView);
        textView = (TextView) findViewById(R.id.stateView);
        GPSInfo = (TextView) findViewById(R.id.GPSInfo);
        heightText = (TextView) findViewById(R.id.heightView);
        batteryText = (TextView) findViewById(R.id.batteryView);
        videoSurface = (TextureView) findViewById(R.id.textureView);
        Button initButton = (Button) findViewById(R.id.button1);
        Button startButton = (Button) findViewById(R.id.button2);
        Button landButton = (Button) findViewById(R.id.button3);
        Button endButton = (Button) findViewById(R.id.button4);
        initButton.setOnClickListener(this);
        startButton.setOnClickListener(this);
        landButton.setOnClickListener(this);
        endButton.setOnClickListener(this);

        // set video surface
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        ViewGroup.LayoutParams params = videoSurface.getLayoutParams();
        params.width = displayMetrics.widthPixels;
        params.height = displayMetrics.heightPixels;
        scale = (double) params.width / 640;
        videoSurface.setLayoutParams(params);
        videoSurface.setSurfaceTextureListener(this);

        // receive video callback
        onReceiveVideoCallback = new DJIOnReceivedVideoCallback() {
            @Override
            public void onResult(byte[] videoBuffer, int size) {
                codecManager.sendDataToDecoder(videoBuffer, size);
            }
        };
        receivedVideoDataCallBack = new CameraReceivedVideoDataCallback() {
            @Override
            public void onResult(byte[] videoBuffer, int size) {
                codecManager.sendDataToDecoder(videoBuffer, size);
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(ControlApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(broadcastReceiver, filter);

        // start refresh height
        handler.postDelayed(runnable, DELAY_TIME);

    }

    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(ControlActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initPreviewer() {
        try {
            product = ControlApplication.getProductInstance();
        } catch (Exception e) {
            product = null;
        }

        if (product != null && product.isConnected()) {
            textView.setText(product.getModel() + " " + getString(R.string.connected));
            if (!product.getModel().equals(Model.UnknownAircraft)) {
                camera = product.getCamera();
                if (camera != null) {
                    camera.setDJICameraReceivedVideoDataCallback(receivedVideoDataCallBack);
                }
                battery = product.getBattery();
                if (battery != null) {
                    battery.setBatteryStateUpdateCallback(batteryCallback);
                } else {
                    batteryText.setText(getString(R.string.NA));
                }
            } else if (product.getAirLink() != null && product.getAirLink().getLBAirLink() != null) {
                product.getAirLink().getLBAirLink().setDJIOnReceivedVideoCallback(onReceiveVideoCallback);
            }
        } else {
            textView.setText(getString(R.string.disconnected));
        }
    }

    private void uninitPreviewer() {
        if (product != null && product.isConnected()) {
            if (!product.getModel().equals(Model.UnknownAircraft)) {
                if (camera != null) {
                    camera.setDJICameraReceivedVideoDataCallback(null);
                }
                if (battery != null) {
                    battery.setBatteryStateUpdateCallback(null);
                }
            } else if (product.getAirLink() != null && product.getAirLink().getLBAirLink() != null) {
                product.getAirLink().getLBAirLink().setDJIOnReceivedVideoCallback(null);
            }
        }
    }

    private void getFc() {
        try {
            flightController = ControlApplication.getAircraftInstance().getFlightController();
            flightController.setReceiveExternalDeviceDataCallback(recvCallback);
            showToast(getString(R.string.get_fc_success));
        } catch (NullPointerException e) {
            showToast(getString(R.string.fc_null));
        }
    }

    private void turnOnMotor() {
        if (flightController != null) {
            flightController.turnOnMotors(djiCompletionCallback);
        } else {
            showToast(getString(R.string.fc_null));
        }
    }

    private void takeOff() {
        if (flightController != null) {
            flightController.takeOff(djiCompletionCallback);
        } else {
            showToast(getString(R.string.fc_null));
        }
    }

    private void land() {
        if (flightController != null) {
            DJIFlightControllerCurrentState currentState = flightController.getCurrentState();
            if (currentState.isFlying()) {
                flightController.autoLanding(djiCompletionCallback);
            } else {
                showToast(getString(R.string.not_flying));
            }
        } else {
            showToast(getString(R.string.fc_null));
        }
    }

    private void startMission() {
        if (flightController != null) {
            flightController.turnOnMotors(djiCompletionCallback);
            try {
                Thread.currentThread().sleep(2000);
            } catch (InterruptedException e) {
                showToast("sleep failed...");
            }
            if (flightController.isOnboardSDKDeviceAvailable()) {
                flightController.sendDataToOnboardSDKDevice(START.getBytes(), djiCompletionCallback);
                showToast(getString(R.string.start_mission));
            } else {
                showToast(getString(R.string.onboard_device_unavailable));
            }
        } else {
            showToast(getString(R.string.fc_null));
        }
    }

    private void endMission() {
        if (flightController != null) {
            if (flightController.isOnboardSDKDeviceAvailable()) {
                flightController.sendDataToOnboardSDKDevice(END.getBytes(), djiCompletionCallback);
                showToast(getString(R.string.end_mission));
            } else {
                showToast(getString(R.string.onboard_device_unavailable));
            }
        } else {
            showToast(getString(R.string.fc_null));
        }
    }

    @Override
    public void onClick(View v) {
        try {
            product = ControlApplication.getProductInstance();
        } catch (Exception e) {
            product = null;
        }

        if (product == null || !product.isConnected()) {
            camera = null;
            showToast(getString(R.string.disconnected));
            return;
        }

        switch (v.getId()) {
            case R.id.button1:
                getFc();
                break;
            case R.id.button2:
                startMission();
                break;
            case R.id.button3:
                land();
                break;
            case R.id.button4:
                endMission();
                break;
            default:
                break;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (codecManager == null) {
            Log.d(TAG, "CodecManager is NULL");
            codecManager = new DJICodecManager(this, surface, width, height);
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (codecManager != null) {
            codecManager.cleanSurface();
            codecManager = null;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        // updateTitleBar();
        initPreviewer();
    }

    @Override
    public void onPause() {
        uninitPreviewer();
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    public void onReturn(View view) {
        this.finish();
    }

    @Override
    protected void onDestroy() {
        uninitPreviewer();
        unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }
}

