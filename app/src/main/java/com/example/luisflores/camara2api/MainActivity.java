package com.example.luisflores.camara2api;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.TextureView;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "Mobil";

    private TextureView mTextureView;

    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(final SurfaceTexture surfaceTexture, final int width, final int height) {
            Log.d(TAG, "onSurfaceTextureAvailable --> width: " + width + " height: " + height);
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
        } else {
            Log.d(TAG, "TextureView is NOT Available: " + status);
//            Set a listener to know when TextureView is available
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
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
}
