package com.whu.m100;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.TextureView.SurfaceTextureListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ford.DJIL;
import com.ford.DJILListener;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final String TEAM_NAME = "SHIJIU TEAM";

    /* DJI variable */
    private DJIL djil = null;
    private DJICamera camera = null;
    private DJIBattery battery = null;
    private DJIBaseProduct product = null;
    private DJICodecManager codecManager = null;
    private DJIFlightController flightController = null;
    private DJIOnReceivedVideoCallback onReceiveVideoCallback = null;
    private CameraReceivedVideoDataCallback receivedVideoDataCallBack = null;

    /* callback */
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

                        int lux = (int) (Integer.valueOf(slux) * scale);
                        int luy = (int) (Integer.valueOf(sluy) * scale);
                        int rux = (int) (Integer.valueOf(srux) * scale);
                        int ruy = (int) (Integer.valueOf(sruy) * scale);
                        int ldx = (int) (Integer.valueOf(sldx) * scale);
                        int ldy = (int) (Integer.valueOf(sldy) * scale);
                        int rdx = (int) (Integer.valueOf(srdx) * scale);
                        int rdy = (int) (Integer.valueOf(srdy) * scale);
                        int id = Integer.valueOf(ssid);

                        if (!map.containsKey(id)) {
                            historyAdapter.add(new AprilTag(ssid, latitude, longitude));
                            historyAdapter.setSelectItem(historyAdapter.getCount() - 1);
                            listView.setSelection(historyAdapter.getCount() - 1);
                            map.put(id, 1);

                            new Thread() {
                                @Override
                                public void run() {
                                    Message msg = new Message();
                                    msg.what = 0;
                                    handler.sendMessage(msg);
                                }
                            }.start();
                        }

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

    /* widget */
    private EditText server;
    private ListView listView;
    private DrawView drawView;
    private TextView textView;
    private TextView velInfo;
    private TextView heightText;
    private TextView batteryText;
    private ImageView preImageView;
    private TextureView videoSurface;

    /* data for listview */
    private Map<Integer, Integer> map;
    private List<Bitmap> detectedImagesList;
    private HistoryAdapter historyAdapter;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    historyAdapter.notifyDataSetChanged();
                    Bitmap tag = videoSurface.getBitmap();
                    preImageView.setImageBitmap(tag);
                    detectedImagesList.add(tag);
                    break;
                case 1:
                    int position = (int) msg.obj;
                    historyAdapter.setSelectItem(position);
                    historyAdapter.notifyDataSetChanged();
                    preImageView.setImageBitmap(detectedImagesList.get(position));
                    break;
            }
        }
    };
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (flightController != null) {
                DJIFlightControllerCurrentState state = flightController.getCurrentState();
                heightText.setText(String.valueOf(state.getAircraftLocation().getAltitude()));
                double x = state.getVelocityX();
                double y = state.getVelocityY();
                double velocity = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
                BigDecimal bigDecimal = new BigDecimal(velocity);
                velocity = bigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                velInfo.setText(String.valueOf(velocity));
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

        initAll();

        IntentFilter filter = new IntentFilter();
        filter.addAction(ControlApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(broadcastReceiver, filter);

        // start refresh height
        handler.postDelayed(runnable, DELAY_TIME);
    }

    private void initAll() {
        // get all widget
        server = (EditText) findViewById(R.id.server_ip);
        listView = (ListView) findViewById(R.id.historyList);
        drawView = (DrawView) findViewById(R.id.drawView);
        textView = (TextView) findViewById(R.id.stateView);
        velInfo = (TextView) findViewById(R.id.velInfo);
        heightText = (TextView) findViewById(R.id.heightView);
        batteryText = (TextView) findViewById(R.id.batteryView);
        preImageView = (ImageView) findViewById(R.id.pre_tag);
        videoSurface = (TextureView) findViewById(R.id.textureView);

        Button initButton = (Button) findViewById(R.id.button1);
        Button startButton = (Button) findViewById(R.id.button2);
        Button landButton = (Button) findViewById(R.id.button3);
        Button endButton = (Button) findViewById(R.id.button4);
        initButton.setOnClickListener(this);
        startButton.setOnClickListener(this);
        landButton.setOnClickListener(this);
        endButton.setOnClickListener(this);

        /* set resolution */
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        ViewGroup.LayoutParams params = videoSurface.getLayoutParams();
        params.width = displayMetrics.widthPixels / 2;
        params.height = displayMetrics.heightPixels / 2;
        scale = (double) params.width / 640;
        videoSurface.setLayoutParams(params);

        /* set video surface */
        videoSurface.setSurfaceTextureListener(this);

        /* receive video callback */
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

        // init listview
        map = new HashMap<>();
        detectedImagesList = new ArrayList<>();
        historyAdapter = new HistoryAdapter(this);
        listView.setAdapter(historyAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Message msg = new Message();
                msg.what = 1;
                msg.obj = position;
                handler.sendMessage(msg);
            }
        });

        /* init ford api */
        djil = new DJIL(new DJILListener() {
            @Override
            public void startButton() {
                startMission();
            }

            @Override
            public void abortLanding() {
                stopMission();
            }

            @Override
            public void abortMission() {
            }
        });
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

    private void initialize() {
        /* get flight controller */
        try {
            flightController = ControlApplication.getAircraftInstance().getFlightController();
            flightController.setReceiveExternalDeviceDataCallback(recvCallback);
            showToast(getString(R.string.get_fc_success));
        } catch (NullPointerException e) {
            showToast(getString(R.string.fc_null));
        }

        /* connect to ford server */
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String ip = server.getText().toString().trim();
                    djil.connect(ip, TEAM_NAME);
                } catch (Exception e) {
                }
            }
        }).start();
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

    private void stopMission() {
        if (flightController != null) {
            if (flightController.isOnboardSDKDeviceAvailable()) {
                flightController.sendDataToOnboardSDKDevice(END.getBytes(), djiCompletionCallback);
                showToast(getString(R.string.stop_mission));
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
                initialize();
                break;
            case R.id.button2:
                startMission();
                break;
            case R.id.button3:
                land();
                break;
            case R.id.button4:
                stopMission();
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
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // updateTitleBar();
        initPreviewer();
    }

    @Override
    protected void onPause() {
        uninitPreviewer();
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    protected void onReturn(View view) {
        this.finish();
    }

    @Override
    protected void onDestroy() {
        uninitPreviewer();
        unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }
}

