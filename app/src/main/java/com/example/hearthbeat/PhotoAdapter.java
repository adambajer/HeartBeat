package com.example.hearthbeat;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.io.File;
import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.ViewHolder> {
    final List<String> photoPaths;

    public PhotoAdapter(final List<String> photoPaths) {
        this.photoPaths = photoPaths;
    }

    public class ViewHolder extends RecyclerView.ViewHolder { // Changed from static to non-static
        ZoomPanImageView zoomPanImageView;

        public ViewHolder(@NonNull final View itemView) {
            super(itemView);
            this.zoomPanImageView = itemView.findViewById(R.id.photoImageView);
            itemView.setClickable(true); // Added
            itemView.setFocusable(true); // Added
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_photo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        String photoPath = this.photoPaths.get(position);

        // Ensure dimensions of the itemView are available before loading the image
        holder.itemView.post(new Runnable() {
            @Override
            public void run() {
                final int itemWidth = holder.itemView.getWidth();
                Glide.with(holder.itemView.getContext())
                        .load(new File(photoPath))
                        .override(itemWidth)
                        .fitCenter()
                        .into(holder.zoomPanImageView);
            }
        });

        holder.zoomPanImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                openPhoto(photoPath, v.getContext());
            }
        });
    }

    private void openPhoto(final String photoPath, final Context context) {
        Intent intent = new Intent(context, ModalPhotoActivity.class);
        intent.putExtra(ModalPhotoActivity.EXTRA_PHOTO_PATH, photoPath);
        context.startActivity(intent);
    }


    @Override
    public int getItemCount() {
        return this.photoPaths.size();
    }
}
