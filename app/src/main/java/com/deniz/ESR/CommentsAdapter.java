package com.deniz.ESR;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.ViewHolder> {

    private List<Comment> comments;

    public CommentsAdapter(List<Comment> comments) {

        this.comments = comments;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.comment_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Comment comment = comments.get(position);

        holder.userName.setText(comment.getUserName());

        holder.commentText.setText(comment.getText());
        holder.elevatorStatus.setText("Asansör çalışıyor mu?: " + comment.getElevator());
        holder.rampStatus.setText("Rampa var mı?: " + comment.getRamp());
        holder.commentDate.setText(comment.getDate());
        holder.ratingTextView.setText("Puan: " + comment.getRating());

        if (comment.getPhoto1Path() != null) {
            try {
                Bitmap bitmap1 = BitmapFactory.decodeFile(comment.getPhoto1Path());
                if (bitmap1 != null) {
                    holder.photo1.setVisibility(View.VISIBLE);
                    holder.photo1.setImageBitmap(bitmap1);
                } else {
                    Log.e("Fotoğraf Hatası", "Bitmap oluşturulamadı: " + comment.getPhoto1Path());
                    holder.photo1.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                Log.e("Fotoğraf Hatası", "Hata: " + e.getMessage());
                holder.photo1.setVisibility(View.GONE);
            }
        } else {
            holder.photo1.setVisibility(View.GONE);
        }

        if (comment.getPhoto2Path() != null) {
            try {
                Bitmap bitmap2 = BitmapFactory.decodeFile(comment.getPhoto2Path());
                if (bitmap2 != null) {
                    holder.photo2.setVisibility(View.VISIBLE);
                    holder.photo2.setImageBitmap(bitmap2);
                } else {
                    Log.e("Fotoğraf Hatası", "Bitmap oluşturulamadı: " + comment.getPhoto2Path());
                    holder.photo2.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                Log.e("Fotoğraf Hatası", "Hata: " + e.getMessage());
                holder.photo2.setVisibility(View.GONE);
            }
        } else {
            holder.photo2.setVisibility(View.GONE);
        }

        holder.photo1.setOnClickListener(v -> {
            if (comment.getPhoto1Path() != null) {
                showImageDialog(holder.itemView, comment.getPhoto1Path());
            }
        });

        holder.photo2.setOnClickListener(v -> {
            if (comment.getPhoto2Path() != null) {
                showImageDialog(holder.itemView, comment.getPhoto2Path());
            }
        });

    }


    private void showImageDialog(View parentView, String imagePath) {
        Dialog dialog = new Dialog(parentView.getContext());
        dialog.setContentView(R.layout.dialog_image);
        ImageView imageView = dialog.findViewById(R.id.dialog_image_view);

        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        }

        dialog.show();
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }



    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView commentText, commentDate, elevatorStatus, rampStatus, userName, ratingTextView;
        ImageView photo1, photo2;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.comment_user_name);
            commentText = itemView.findViewById(R.id.comment_text);
            elevatorStatus = itemView.findViewById(R.id.elevator_status);
            rampStatus = itemView.findViewById(R.id.ramp_status);
            ratingTextView = itemView.findViewById(R.id.comment_rating);

            photo1 = itemView.findViewById(R.id.photo1);
            photo2 = itemView.findViewById(R.id.photo2);
            commentDate = itemView.findViewById(R.id.comment_date);
        }
    }


}