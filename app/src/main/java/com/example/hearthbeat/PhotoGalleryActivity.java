package com.example.hearthbeat;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryActivity extends AppCompatActivity {
    private RecyclerView photoRecyclerView;
     private PhotoAdapter photoAdapter;
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_photo_gallery);
        this.photoRecyclerView = this.findViewById(R.id.photoRecyclerView);
        setGrid();
    }
    @Override
    protected void onResume() {
        super.onResume();
        refreshGrid();
    }

    private void setGrid() {
        final List<String> photoPaths = this.loadPhotoPaths();
        this.photoAdapter = new PhotoAdapter(photoPaths);
        this.photoRecyclerView.setAdapter(photoAdapter);
        updateLayoutManager(photoPaths.size());
    }
    private void refreshGrid() {
        final List<String> photoPaths = this.loadPhotoPaths();
        photoAdapter.photoPaths.clear(); // Clear the existing photo paths
        photoAdapter.photoPaths.addAll(photoPaths); // Add the new photo paths
        photoAdapter.notifyDataSetChanged(); // Notify the adapter of the changes
        updateLayoutManager(photoPaths.size());
    }

    private void updateLayoutManager(int numPhotos) {
        final int[] dimensions = this.calculateLayoutDimensions(numPhotos);
        final int numColumns = dimensions[1];
        final GridLayoutManager layoutManager = new GridLayoutManager(this, numColumns);
        this.photoRecyclerView.setLayoutManager(layoutManager);
    }
    private int getScreenWidth() {
        return this.getResources().getDisplayMetrics().widthPixels;
    }

    private int getScreenHeight() {
        return this.getResources().getDisplayMetrics().heightPixels;
    }private int[] calculateLayoutDimensions(final int numPhotos) {
        final int[] dimensions = new int[4];  // [numberOfRows, numberOfColumns, gridItemWidth, gridItemHeight]

        final int screenWidth = this.getScreenWidth();
        final int screenHeight = this.getScreenHeight();
        final int totalScreenArea = screenWidth * screenHeight;
        Log.d("LayoutDimensions", "Total Screen Area: " + totalScreenArea);

        // Calculate the area of a single grid item
        final int itemArea = totalScreenArea / (numPhotos+10);

        // Calculate the dimensions of a square grid item
        final int itemSize = (int) Math.sqrt(itemArea);
        Log.d("LayoutDimensions", "Item Size (Width x Height): " + itemSize);

        // Calculate the number of rows and columns
        final int numColumns = screenWidth / itemSize;
        final int numRows = screenHeight / itemSize;

        dimensions[0] = numRows;
        dimensions[1] = numColumns;
        dimensions[2] = itemSize;
        dimensions[3] = itemSize;

        // Log the calculated dimensions
        Log.d("LayoutDimensions", "Number of Rows: " + dimensions[0]);
        Log.d("LayoutDimensions", "Number of Columns: " + dimensions[1]);
        Log.d("LayoutDimensions", "Grid Item Width: " + dimensions[2]);
        Log.d("LayoutDimensions", "Grid Item Height: " + dimensions[3]);

        return dimensions;
    }


    public List<String> loadPhotoPaths() {
        final List<String> photoPaths = new ArrayList<>();

        // Use the same directory structure as the save method
        final File downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        final File heartbeatFolder = new File(downloadsFolder, "hearthbeat");

        // Check if the directory exists
        if (!heartbeatFolder.exists()) {
            return photoPaths; // Return the empty list
        }

        if (heartbeatFolder.isDirectory()) {
            final File[] photoFiles = heartbeatFolder.listFiles();
            if (null != photoFiles) {
                for (final File photoFile : photoFiles) {
                    if (photoFile.isFile()) {
                        final String photoPath = photoFile.getAbsolutePath();
                        photoPaths.add(photoPath);
                        Log.d("PhotoPathDebug", "Photo Path: " + photoPath);
                    }
                }
            }
        }

        return photoPaths;
    }




}