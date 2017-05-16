package com.example.luisflores.camara2api;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "Mobil";

    private static final int REQUEST_CAMERA_PERMISSION_RESULT = 0;

    private TextureView mTextureView;

//    Listener to know when TextureView is available
    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(final SurfaceTexture surfaceTexture, final int width, final int height) {
            Log.d(TAG, "onSurfaceTextureAvailable --> width: " + width + " height: " + height);
            setupCamera(width, height);
            connectCamera();
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
            startPreview();
            Toast.makeText(MainActivity.this, "Camera connection made !!", Toast.LENGTH_SHORT).show();
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

//    Create a Background thread
    private HandlerThread mBackgroundHandlerThread;
    private Handler mBackgroundHandler;

//    To get the Camera Id
    private String mCameraId;

//    To get preview size
    private Size mPreviewSize;

//    To start preview display
    private CaptureRequest.Builder mCaptureRequestBuilder;

//    To check if app is recording or not
    private ImageButton mRecordImageButton;
    private boolean mIsRecording = false;

//    Setup Orientations
    private static SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    private static class CompareSizeByArea implements Comparator<Size> {

        @Override
        public int compare(final Size lhs, final Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() / (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextureView = (TextureView) findViewById(R.id.textureView);
        mRecordImageButton = (ImageButton) findViewById(R.id.videoOnLineImageButton);

        mRecordImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                if (mIsRecording) {
                    mIsRecording = false;
                    mRecordImageButton.setImageResource(R.mipmap.btn_video_online);
                } else {
                    mIsRecording = true;
                    mRecordImageButton.setImageResource(R.mipmap.btn_video_busy);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        startBackgroundThread();
        boolean status = mTextureView.isAvailable();
        if (mTextureView.isAvailable()) {
            Log.d(TAG, "TextureView is Available: " + status);
            setupCamera(mTextureView.getWidth(), mTextureView.getHeight());
            connectCamera();
        } else {
            Log.d(TAG, "TextureView is NOT Available: " + status);
//            Set a listener to know when TextureView is available
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION_RESULT) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Application will not run without camera services", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onPause() {
        closeCamera();
        stopBackgroundThread();
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

//                Preview Size
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

//                Finding out if we are in Portrait Mode. If so, we switch width for height and the other way around
                int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
                Log.d(TAG, "DEVICE ORIENTATION: " + deviceOrientation);
                int totalRotation = sensorToDeviceRotation(cameraCharacteristics, deviceOrientation);
                boolean swapRotation = totalRotation == 90 || totalRotation == 270;
                int rotatedWidth = width;
                int rotatedHeight = height;
                if (swapRotation) {
                    rotatedWidth = height;
                    rotatedHeight = width;
                }

                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedWidth, rotatedHeight);

                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void connectCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
                } else {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                        Toast.makeText(this, "Video app required access to the camera", Toast.LENGTH_SHORT).show();
                    }
                    requestPermissions(new String[] {Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_RESULT);
                }
            } else {
                cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startPreview() {
        SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);

        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(previewSurface);

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface),
                new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(final CameraCaptureSession session) {
                        try {
                            session.setRepeatingRequest(mCaptureRequestBuilder.build(), null, mBackgroundHandler);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onConfigureFailed(final CameraCaptureSession session) {
                        Toast.makeText(MainActivity.this, "Unable to setup camera preview", Toast.LENGTH_SHORT).show();
                    }
                }, null);
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

//    Start background thread
    private void startBackgroundThread() {
//        Thread created
        mBackgroundHandlerThread = new HandlerThread("Camara2Api");
//        Thread started
        mBackgroundHandlerThread.start();
//        Once is created and started we can setup a handler pointing to that thread
        mBackgroundHandler = new Handler(mBackgroundHandlerThread.getLooper());
    }

//    Stop background thread
    private void stopBackgroundThread() {
        mBackgroundHandlerThread.quitSafely();
        try {
            mBackgroundHandlerThread.join();
            mBackgroundHandlerThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static int sensorToDeviceRotation(CameraCharacteristics cameraCharacteristics, int deviceOrientation) {
        int sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);
        return (sensorOrientation + deviceOrientation + 360) % 360;
    }

    private static Size chooseOptimalSize(Size[] choices, int width, int height) {
        List<Size> bigEnough = new ArrayList<Size>();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * height / width && option.getWidth() == width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizeByArea());
        } else {
            return choices[0];
        }
    }
}
