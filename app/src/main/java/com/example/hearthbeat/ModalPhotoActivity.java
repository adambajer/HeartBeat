package com.example.hearthbeat;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import java.io.File;

public class ModalPhotoActivity extends AppCompatActivity {
    public static final String EXTRA_PHOTO_PATH = "EXTRA_PHOTO_PATH";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setContentView(R.layout.activity_modal_photo);
        ZoomPanImageView imageView = findViewById(R.id.modalPhotoImageView);
        String photoPath = getIntent().getStringExtra(EXTRA_PHOTO_PATH);

        Glide.with(this)
                .load(new File(photoPath))
                .fitCenter()
                .into(imageView);

        imageView.setOnClickListener(v -> finish()); // Close the modal when the image is clicked
    }
}
