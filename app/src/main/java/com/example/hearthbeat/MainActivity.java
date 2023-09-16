package com.example.hearthbeat;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_STORAGE_PERMISSION = 123;
    private static final int REQUEST_CODE = 1;
    private static final int REQUEST_CODE_CAMERA_PERMISSION = 2;
    private static final double MIN_RMS_THRESHOLD = 0.1;
    private static final double MIN_DB_LEVEL = -100.0;
    private static final int MOVING_AVERAGE_WINDOW = 5;
    private static final float ANIMATION_THRESHOLD_DB = 50.0f;
    private int countdown = 5;
    private final Handler handler = new Handler();
    private final double calibrationOffset = 20;
    public float scaleFactor = 10.0f;
    private ProcessCameraProvider cameraProvider;
    private TextView countdownTextView;
    private TextView dbLevelTextView;
    private Runnable countdownRunnable;
    private boolean isCountdownActive = false;

    private ImageView heartImageView;
    private Drawable heartDrawable;
    private Vibrator vibrator;
    private CameraManager cameraManager;
    private ImageCapture imageCapture;
    private ImageView selfieImageView;
    private File photoFile;
    private Thread recordingThread;
    private boolean isRecording;
    private boolean hasTriggeredVibrationAndFlash;
    private boolean hasStartedCamera;
    private boolean isResting;
    public Button startStopButton;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
         startStopButton = findViewById(R.id.button);

        countdownTextView = findViewById(R.id.countdownTextView);
        Button startStopButton = findViewById(R.id.button);
        startStopButton.setOnClickListener(view -> toggleRecording());
        heartImageView = findViewById(R.id.heartImageView);
        heartDrawable = getResources().getDrawable(R.drawable.heart_shape);
        int initialSize = getResources().getDimensionPixelSize(R.dimen.initial_heart_size);
        heartDrawable.setBounds(0, 0, initialSize, initialSize);
        heartImageView.setImageDrawable(heartDrawable);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        selfieImageView = findViewById(R.id.selfieImageView);
        dbLevelTextView = findViewById(R.id.dbLevelTextView);
        if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, new String[] {
                    Manifest.permission.CAMERA
            }, REQUEST_CODE_CAMERA_PERMISSION);
        } else {
            startCamera();
        }
        if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)) {
            ActivityCompat.requestPermissions(this, new String[] {
                    Manifest.permission.RECORD_AUDIO
            }, REQUEST_CODE);
        } else {
            toggleRecording();
        }
        checkPermission();
        Button openGalleryButton = findViewById(R.id.openGalleryButton);
        openGalleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleRecording();
                Intent intent = new Intent(MainActivity.this, PhotoGalleryActivity.class);
                startActivity(intent);
            }
        });
    }private void startCooldownCountdown() {
        countdown = 5;
        countdownTextView.setVisibility(View.VISIBLE);
        isCountdownActive = true;

        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                countdownTextView.setText(String.valueOf(countdown));
                if (countdown > 0) {
                    countdownTextView.setVisibility(View.VISIBLE);
                    countdown--;
                    handler.postDelayed(this, 1000);
                    stopRecording();
                } else {
                    startRecording();
                    countdownTextView.setVisibility(View.INVISIBLE);
                    isCountdownActive = false;
                    // Perform actions to resume after the countdown
                    // For example, start recording or enable certain features
                    // You can add your code here

                    // Handle any other actions you need to take after the countdown
                    // ...

                    // Optionally, remove the callback to prevent multiple executions
                    handler.removeCallbacks(this);
                }
            }
        };

        handler.post(countdownRunnable);
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (MainActivity.REQUEST_CODE == requestCode) {
            if (0 < grantResults.length && PackageManager.PERMISSION_GRANTED == grantResults[0]) {

            } else {}
        }
        if (MainActivity.REQUEST_CODE_CAMERA_PERMISSION == requestCode) {
            if (0 < grantResults.length && PackageManager.PERMISSION_GRANTED == grantResults[0]) {
                startCamera();
            } else {}
        }
        if (MainActivity.REQUEST_STORAGE_PERMISSION == requestCode) {
            if (PackageManager.PERMISSION_GRANTED != grantResults[0]) {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (isRecording) {
            toggleRecording();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (!isRecording) {
            toggleRecording();
        }
    }
    private int interpolateColor(int startColor, int endColor, float factor) {
        int alpha = (int)(Color.alpha(startColor) + factor * (Color.alpha(endColor) - Color.alpha(startColor)));
        int red = (int)(Color.red(startColor) + factor * (Color.red(endColor) - Color.red(startColor)));
        int green = (int)(Color.green(startColor) + factor * (Color.green(endColor) - Color.green(startColor)));
        int blue = (int)(Color.blue(startColor) + factor * (Color.blue(endColor) - Color.blue(startColor)));
        return Color.argb(alpha, red, green, blue);
    }
    private void startRecording() {
        isRecording = true;
        final int sampleRate = 44100;
        int bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)) {
            return;
        }
        AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        audioRecord.startRecording();
        short[] audioBuffer = new short[bufferSize];
        recordingThread = new Thread(() -> {
            while (isRecording) {
                int readSize = audioRecord.read(audioBuffer, 0, bufferSize);
                if (0 < readSize) {
                    double sum = 0;
                    for (int i = 0; i < readSize; i++) {
                        sum += audioBuffer[i] * audioBuffer[i];
                    }

                    double rms = Math.sqrt(sum / readSize);
                    double dbLevel = MIN_DB_LEVEL;
                    if (MainActivity.MIN_RMS_THRESHOLD < rms) {
                        dbLevel = 20 * Math.log10(rms / Math.pow(2, 15));
                    }
                    dbLevel = Math.max(dbLevel, MIN_DB_LEVEL);
                    dbLevel += calibrationOffset;
                    double displayDbLevel = dbLevel + 100.0;
                    double finalDbLevel = dbLevel;
                    runOnUiThread(() -> updateHeartSize((float) finalDbLevel));
                }
            }
        });
        recordingThread.start();
    }
    public void updateHeartSize(float dbLevel) {
        final float MIN_DB_LEVEL = 0;
        final float MAX_DB_LEVEL = 100;
        float factor = (dbLevel - MIN_DB_LEVEL) / (MAX_DB_LEVEL - MIN_DB_LEVEL);
        factor = Math.max(0, Math.min(1, factor));
        final int startColor = Color.BLUE;
        final int endColor = Color.RED;
        int currentColor = interpolateColor(startColor, endColor, factor);
        heartDrawable.setColorFilter(currentColor, PorterDuff.Mode.SRC_IN);
        float displayDbLevel = dbLevel + 100.0f;
        int newSize = (int)(displayDbLevel * scaleFactor);
        if (MainActivity.ANIMATION_THRESHOLD_DB <= dbLevel) {
            Animation scaleAnimation = new ScaleAnimation(1.0f, scaleFactor, 1.0f, scaleFactor, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            scaleAnimation.setDuration(1000);
            heartImageView.startAnimation(scaleAnimation);
        }
        if (displayDbLevel >= 105  ) {
            runOnUiThread(() -> {
                startCooldownCountdown(); // Start the countdown when needed

                startCamera();
                takeSelfieAndStartCamera();
                triggerVibrationAndFlash();
            });
        } else {
            heartDrawable.clearColorFilter();
            resetVibrationAndFlashFlag();
        }
        Button takeSelfieButton = findViewById(R.id.button3);
        takeSelfieButton.setOnClickListener(view -> {
            startCamera();takeSelfieAndStartCamera();
            triggerVibrationAndFlash();
        });
        ViewGroup.LayoutParams params = heartImageView.getLayoutParams();
        params.width = newSize;
        params.height = newSize;
        heartImageView.setLayoutParams(params);
        heartImageView.setImageDrawable(heartDrawable);
        float textScalingFactor = (float) newSize / getResources().getDimensionPixelSize(R.dimen.initial_heart_size);
        float newTextSize = getResources().getDimension(R.dimen.text_size) * textScalingFactor;
        dbLevelTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize);
        dbLevelTextView.setTextColor( Color.WHITE);
         int roundedDisplayDbLevel = Math.round(displayDbLevel);
        dbLevelTextView.setText(String.format("%d dB", roundedDisplayDbLevel));
    }
    private void toggleRecording() {
        if (isRecording) {
            stopRecording();
            startStopButton.setText("START");
        } else {
            startRecording();
            startStopButton.setText("STOP");
        }
    }
    public void triggerVibrationAndFlash() {
        if (!hasTriggeredVibrationAndFlash) {
            vibrator.vibrate(500);
            String cameraId = null;
            try {
                cameraId = cameraManager.getCameraIdList()[0];
                cameraManager.setTorchMode(cameraId, true);
                Thread.sleep(0);
                cameraManager.setTorchMode(cameraId, false);
            } catch (CameraAccessException | InterruptedException e) {
                e.printStackTrace();
            }
            hasTriggeredVibrationAndFlash = true;
        }
    }
    public void resetVibrationAndFlashFlag() {
        hasTriggeredVibrationAndFlash = false;
    }
    public void startCamera() {
        if (!hasStartedCamera) {
            hasStartedCamera = true;
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)) {
                ActivityCompat.requestPermissions(this, new String[] {
                        Manifest.permission.CAMERA
                }, REQUEST_CODE_CAMERA_PERMISSION);
            } else {
                setupCamera();
            }
        }
    }
    private void checkPermission() {
        if (android.os.Build.VERSION_CODES.M <= android.os.Build.VERSION.SDK_INT) {
            if (PackageManager.PERMISSION_GRANTED != this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                requestPermissions(new String[] {
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                }, REQUEST_STORAGE_PERMISSION);
            }
        }
    }
    private void setupCamera() {
        ListenableFuture < ProcessCameraProvider > cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                if (null != this.cameraProvider) {
                    CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build();
                    cameraProvider.unbindAll();
                    imageCapture = new ImageCapture.Builder().build();
                    cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture);
                } else {
                    Log.e("CameraX", "Camera provider is null");
                }
            } catch (Exception e) {
                Log.e("CameraX", "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }
    public void takeSelfieAndStartCamera() {
        if (!hasStartedCamera) {
            hasStartedCamera = true;
            ListenableFuture < ProcessCameraProvider > cameraProviderFuture = ProcessCameraProvider.getInstance(this);
            cameraProviderFuture.addListener(() -> {
                try {
                    cameraProvider = cameraProviderFuture.get();
                    if (null != this.cameraProvider) {
                        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build();
                        cameraProvider.unbindAll();
                        imageCapture = new ImageCapture.Builder().build();
                        cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture);
                        takeSelfie();
                    } else {
                        Log.e("CameraX", "Camera provider is null");
                    }
                } catch (Exception e) {
                    Log.e("CameraX", "Use case binding failed", e);
                }
            }, ContextCompat.getMainExecutor(this));
        } else {
            if (!isResting) {
                takeSelfie();
                isResting = true;
                new Handler().postDelayed(() -> {
                    isResting = false;
                }, 5000);
            }
        }
    }
    private void takeSelfie() {
        if (null != this.cameraProvider && null != this.imageCapture) {
            File file = new File(getExternalFilesDir(null), "selfie.jpg");
            String fileName = "photo_" + System.currentTimeMillis() + ".jpg";
            photoFile = new File(getExternalFilesDir(null), fileName);
            ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();
            imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
                @Override
                public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                    Toast.makeText(MainActivity.this, "LOVE!", Toast.LENGTH_SHORT).show();
                    updateSelfieImageViewAndSavePhoto(BitmapFactory.decodeFile(photoFile.getAbsolutePath()));
                    updateSelfieImageView(photoFile.getAbsolutePath());
                    Toast.makeText(MainActivity.this, photoFile.toString(), Toast.LENGTH_LONG).show();
                }
                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    Log.e("CameraX", "Image capture failed", exception);
                    Toast.makeText(MainActivity.this, "Photo error!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    public void updateSelfieImageView(String filePath) {
        File imageFile = new File(filePath);
        if (!imageFile.exists()) {
            Log.e("UpdateSelfieImageView", "Image file does not exist.");
            return;
        }
        Bitmap myBitmap = BitmapFactory.decodeFile(filePath);
        if (null == myBitmap) {
            Log.e("UpdateSelfieImageView", "Failed to decode the bitmap.");
            return;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(-90);
        Bitmap rotatedBitmap = Bitmap.createBitmap(myBitmap, 0, 0, myBitmap.getWidth(), myBitmap.getHeight(), matrix, true);
        int x = 0;
        int y = 0;
        int length = Math.min(rotatedBitmap.getWidth(), rotatedBitmap.getHeight());
        if (rotatedBitmap.getWidth() > rotatedBitmap.getHeight()) {
            x = (rotatedBitmap.getWidth() - length) / 2;
        } else {
            y = (rotatedBitmap.getHeight() - length) / 2;
        }
        Bitmap squareBitmap = Bitmap.createBitmap(rotatedBitmap, x, y, length, length);
        selfieImageView.setImageBitmap(squareBitmap);
    }
    private void stopRecording() {
        isRecording = false;
        if (null != this.recordingThread) {
            recordingThread.interrupt();
            Toast.makeText(this, "lOVE dB stopped!", Toast.LENGTH_SHORT).show();
        }
    }
    private void updateSelfieImageViewAndSavePhoto(Bitmap bitmap) {
        startCooldownCountdown();
        File thumbnailDirectory = new File(getExternalFilesDir(null), "thumbnails");
        if (!thumbnailDirectory.exists()) {
            thumbnailDirectory.mkdirs();
        }
        File downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File heartbeatFolder = new File(downloadsFolder, "hearthbeat");
        if (!heartbeatFolder.exists()) {
            heartbeatFolder.mkdirs();
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String currentDateAndTime = sdf.format(new Date());
        String fileName = currentDateAndTime + "_love.jpg";
        File photoFile = new File(heartbeatFolder, fileName);
        Matrix matrix = new Matrix();
        matrix.postRotate(-90);
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        int heartSize = Math.min(rotatedBitmap.getWidth(), rotatedBitmap.getHeight());
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(rotatedBitmap, heartSize, heartSize, true);
        Drawable heartOverlay = getResources().getDrawable(R.drawable.heart_cut);
        Bitmap overlayBitmap = Bitmap.createBitmap(scaledBitmap.getWidth(), scaledBitmap.getHeight(), scaledBitmap.getConfig());
        Canvas canvas = new Canvas(overlayBitmap);
        canvas.drawBitmap(scaledBitmap, new Matrix(), null);
        heartOverlay.setBounds(0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight());
        heartOverlay.draw(canvas);


        try {
            FileOutputStream outputStream = new FileOutputStream(photoFile);
            overlayBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.close();
            MediaScannerConnection.scanFile(this, new String[] {
                    photoFile.getAbsolutePath()
            }, null, null);
            Toast.makeText(this, "Photo saved to Downloads/hearthbeat/" + fileName, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
        final int thumbnailSize = 500;
        Bitmap thumbnailBitmap = Bitmap.createScaledBitmap(overlayBitmap, thumbnailSize, thumbnailSize, true);
        try {
            File thumbnailFile = new File(thumbnailDirectory, "thumbnail_" + fileName);
            FileOutputStream thumbnailOutputStream = new FileOutputStream(thumbnailFile);
            thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 80, thumbnailOutputStream);
            thumbnailOutputStream.close();
            MediaScannerConnection.scanFile(this, new String[] {
                    thumbnailFile.getAbsolutePath()
            }, null, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}