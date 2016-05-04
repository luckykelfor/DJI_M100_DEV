package com.whu.m100;

import android.app.Application;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import dji.sdk.Products.DJIAircraft;
import dji.sdk.SDKManager.DJISDKManager;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIBaseComponent.DJIComponentListener;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.base.DJIBaseProduct.DJIBaseProductListener;
import dji.sdk.base.DJIBaseProduct.DJIComponentKey;
import dji.sdk.base.DJIError;
import dji.sdk.base.DJISDKError;

public class ControlApplication extends Application {

    public static final String FLAG_CONNECTION_CHANGE = "com_fpv_connection_change";
    private static DJIBaseProduct product;
    private Handler handler;

    public static synchronized DJIBaseProduct getProductInstance() {
        if (null == product) {
            product = DJISDKManager.getInstance().getDJIProduct();
        }
        return product;
    }

    public static boolean isAircraftConnected() {
        return getProductInstance() != null && getProductInstance() instanceof DJIAircraft;
    }

    public static synchronized DJIAircraft getAircraftInstance() {
        if (!isAircraftConnected()) return null;
        return (DJIAircraft) getProductInstance();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        //This is used to start SDK services and initiate SDK.
        DJISDKManager.getInstance().initSDKManager(this, mDJISDKManagerCallback);
    }

    /**
     * When starting SDK services, an instance of interface DJISDKManager.DJISDKManagerCallback will be used to
     * listen to
     * the SDK Registration result and the product changing.
     */
    private DJISDKManager.DJISDKManagerCallback mDJISDKManagerCallback = new DJISDKManager.DJISDKManagerCallback() {

        //Listens to the SDK registration result
        @Override
        public void onGetRegisteredResult(DJIError error) {
            if (error == DJISDKError.REGISTRATION_SUCCESS) {
                DJISDKManager.getInstance().startConnectionToProduct();
            } else {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "register sdk fails, check network is available",
                                Toast.LENGTH_LONG).show();
                    }
                });

            }
            Log.e("TAG", error.toString());
        }

        //Listens to the connected product changing, including two parts, component changing or product connection
        // changing.
        @Override
        public void onProductChanged(DJIBaseProduct oldProduct, DJIBaseProduct newProduct) {
            product = newProduct;
            if (product != null) {
                product.setDJIBaseProductListener(mDJIBaseProductListener);
            }
            notifyStatusChange();
        }
    };

    private DJIBaseProductListener mDJIBaseProductListener = new DJIBaseProductListener() {
        @Override
        public void onComponentChange(DJIComponentKey key, DJIBaseComponent oldComponent, DJIBaseComponent
                newComponent) {
            if (newComponent != null) {
                newComponent.setDJIComponentListener(mDJIComponentListener);
            }
            notifyStatusChange();
        }

        @Override
        public void onProductConnectivityChanged(boolean isConnected) {
            notifyStatusChange();
        }
    };

    private DJIComponentListener mDJIComponentListener = new DJIComponentListener() {
        @Override
        public void onComponentConnectivityChanged(boolean isConnected) {
            notifyStatusChange();
        }
    };

    private void notifyStatusChange() {
        handler.removeCallbacks(updateRunnable);
        handler.postDelayed(updateRunnable, 500);
    }

    private Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
            sendBroadcast(intent);
        }
    };
}
