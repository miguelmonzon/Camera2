package com.example.luisflores.camara2api;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.TextureView;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "Mobil";

    private TextureView mTextureView;

//    Listener to know when TextureView is available
    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(final SurfaceTexture surfaceTexture, final int width, final int height) {
            Log.d(TAG, "onSurfaceTextureAvailable --> width: " + width + " height: " + height);
            setupCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(final SurfaceTexture surfaceTexture, final int width, final int height) {
            Log.d(TAG, "onSurfaceTextureSizeChanged --> width: " + width + " height: " + height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(final SurfaceTexture surfaceTexture) {
            Log.d(TAG, "onSurfaceTextureDestroyed");
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(final SurfaceTexture surfaceTexture) {
            Log.d(TAG, "onSurfaceTextureUpdated");
        }
    };

//    Create Camera Object
    private CameraDevice mCameraDevice;
    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(final CameraDevice camera) {
            mCameraDevice = camera;
        }

        @Override
        public void onDisconnected(final CameraDevice camera) {
//            Clean up resources
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(final CameraDevice camera, final int error) {
            camera.close();
            mCameraDevice = null;
        }
    };

//    To get the Camera Id
    private String mCameraId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextureView = (TextureView) findViewById(R.id.textureView);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        boolean status = mTextureView.isAvailable();
        if (mTextureView.isAvailable()) {
            Log.d(TAG, "TextureView is Available: " + status);
            setupCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            Log.d(TAG, "TextureView is NOT Available: " + status);
//            Set a listener to know when TextureView is available
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    protected void onPause() {
        closeCamera();
        super.onPause();
    }

//    To implement full size app
    @Override
    public void onWindowFocusChanged(final boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        View decorView = getWindow().getDecorView();
        if (hasFocus) {
//            Toast.makeText(MainActivity.this, "It has the focus !!", Toast.LENGTH_SHORT).show();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                                            View.SYSTEM_UI_FLAG_FULLSCREEN |
                                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }

    }

    private void setupCamera(int width, int height) {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }
}
