package com.deniz.ESR;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.navigation.NavigationView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PlaceDetailActivity extends DrawerActivity {

    private TextView placeNameTextView;
    private EditText commentEditText;
    private RadioGroup elevatorRadioGroup;
    private RadioGroup rampRadioGroup;
    private Button submitCommentButton;
    private RecyclerView commentsRecylerView;

    private String photo1Path = null;
    private String photo2Path = null;
    private Button writeCommentButton;
    private LinearLayout commentForm;
    private Button cancelCommentButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_detail);
        setupDrawer();
        checkPermission();
        Button addPhoto1Button = findViewById(R.id.add_photo_1_button);
        Button addPhoto2Button = findViewById(R.id.add_photo_2_button);
        addPhoto1Button.setOnClickListener(v -> openImagePickerForPhoto1());
        addPhoto2Button.setOnClickListener(v -> openImagePickerForPhoto2());

        placeNameTextView = findViewById(R.id.place_name);
        commentEditText = findViewById(R.id.comment);
        elevatorRadioGroup = findViewById(R.id.elevator_radio_group);
        submitCommentButton = findViewById(R.id.comment_button);
        commentsRecylerView = findViewById(R.id.comments);
        rampRadioGroup = findViewById(R.id.ramp_radio_group);

        TextView placeAddressTextView = findViewById(R.id.place_address);
        ImageView placePhotoImageView = findViewById(R.id.place_photo);

        writeCommentButton = findViewById(R.id.write_comment_button);
        commentForm = findViewById(R.id.comment_form);
        cancelCommentButton = findViewById(R.id.cancel_comment_button);
        NavigationView navigationView = findViewById(R.id.navigation_view);
        Menu menu = navigationView.getMenu();
        menu.findItem(R.id.nav_logout).setOnMenuItemClickListener(item -> {
            SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove("currentUser");
            editor.apply();
            recreate();
            return true;
        });

        String placeName = getIntent().getStringExtra("PLACE_NAME");
        String placeAddress = getIntent().getStringExtra("PLACE_ADDRESS");
        byte[] photoByteArray = getIntent().getByteArrayExtra("PLACE_PHOTO");
        writeCommentButton.setVisibility(View.VISIBLE);
        commentForm.setVisibility(View.GONE);


        writeCommentButton.setOnClickListener(v -> {
            commentForm.setVisibility(View.VISIBLE);
            writeCommentButton.setVisibility(View.GONE);
            if (cancelCommentButton != null) {
                cancelCommentButton.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
                cancelCommentButton.setTextColor(Color.WHITE);
            }

            if (addPhoto1Button != null) {

                addPhoto1Button.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
                addPhoto1Button.setTextColor(Color.BLACK);
            }

            if (addPhoto2Button != null) {

                addPhoto2Button.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
                addPhoto2Button.setTextColor(Color.BLACK);
            }

        });


        cancelCommentButton.setOnClickListener(v -> {
            commentForm.setVisibility(View.GONE);
            writeCommentButton.setVisibility(View.VISIBLE);
        });


        placeNameTextView.setText(placeName);
        placeAddressTextView.setText(placeAddress);

        if (photoByteArray != null) {
            Bitmap placePhoto = BitmapFactory.decodeByteArray(photoByteArray, 0, photoByteArray.length);
            placePhotoImageView.setImageBitmap(placePhoto);
        }

        String photoFileName = getIntent().getStringExtra("PLACE_PHOTO_FILE");
        if (photoFileName != null) {
            File file = new File(getCacheDir(), photoFileName);
            Bitmap placePhoto = BitmapFactory.decodeFile(file.getAbsolutePath());
            placePhotoImageView.setImageBitmap(placePhoto);
        }
        RatingBar ratingBar = findViewById(R.id.rating_bar);
        CommentsDatabase commentsDatabase = new CommentsDatabase(this);
        commentsRecylerView.setLayoutManager(new LinearLayoutManager(this));

        submitCommentButton.setOnClickListener(v -> {
            String comment = commentEditText.getText().toString();

            String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

            int selectedElevatorId = elevatorRadioGroup.getCheckedRadioButtonId();
            int selectedRampId = rampRadioGroup.getCheckedRadioButtonId();

            String elevator = (selectedElevatorId == R.id.yes) ? "Evet" :
                    (selectedElevatorId == R.id.no) ? "Hayır" : "Belirtilmedi";

            String ramp = (selectedRampId == R.id.ramp_yes) ? "Var" :
                    (selectedRampId == R.id.ramp_no) ? "Yok" : "Belirtilmedi";

            Log.d("RadioButtonValues", "Elevator: " + elevator + ", Ramp: " + ramp);
            int rating = (int) ratingBar.getRating();


            SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String username = sharedPreferences.getString("currentUser", "Anonim");
            Log.d("Database", "Kullanıcı adı: " + username);

            if (!comment.isEmpty()) {
                Log.d("DatabaseInsert", "Elevator: " + elevator + ", Ramp: " + ramp);

                long result = commentsDatabase.addComment(
                        placeName, comment, rating, date, elevator, ramp, username, photo1Path, photo2Path
                );
                if (result != -1) {
                    Toast.makeText(this, "Yorum başarıyla eklendi!", Toast.LENGTH_SHORT).show();
                    commentEditText.setText("");
                    photo1Path = null;
                    photo2Path = null;
                    loadComments(commentsDatabase, placeName);
                } else {
                    Toast.makeText(this, "Yorum eklenirken hata oluştu.", Toast.LENGTH_SHORT).show();
                }
                if (result != -1) {
                    Log.d("Database", "Veri başarıyla eklendi, ID: " + result);
                } else {
                    Log.e("Database", "Veri ekleme başarısız oldu.");
                }

            }

        });

        loadComments(commentsDatabase, placeName);
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED ||
                        checkSelfPermission(android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) != PackageManager.PERMISSION_GRANTED) {

                    requestPermissions(new String[]{
                            android.Manifest.permission.READ_MEDIA_IMAGES,
                            android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                    }, 101);
                }
            } else {
                if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{android.Manifest.permission.READ_MEDIA_IMAGES}, 101);
                }
            }
        } else {

            Toast.makeText(this, "Bu özellik yalnızca Android 6.0 ve üstü cihazlarda desteklenir.", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "İzin verildi!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "İzin reddedildi.", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String currentUser = sharedPreferences.getString("currentUser", null);

        NavigationView navigationView = findViewById(R.id.navigation_view);
        Menu menu = navigationView.getMenu();

        if (currentUser != null) {
            menu.findItem(R.id.nav_login).setVisible(false);
            menu.findItem(R.id.nav_register).setVisible(false);
            menu.findItem(R.id.nav_logout).setVisible(true);
        } else {
            menu.findItem(R.id.nav_login).setVisible(true);
            menu.findItem(R.id.nav_register).setVisible(true);
            menu.findItem(R.id.nav_logout).setVisible(false);
        }
    }


    private void loadComments(CommentsDatabase commentsDatabase, String placeName) {
        SQLiteDatabase db = commentsDatabase.getReadableDatabase();
        commentsRecylerView.setLayoutManager(new LinearLayoutManager(this));

        Cursor cursor = db.query(
                CommentsDatabase.TABLE_COMMENTS,
                null,
                CommentsDatabase.COLUMN_PLACE_NAME + "=?",
                new String[]{placeName},
                null, null, CommentsDatabase.COLUMN_DATE + " DESC"
        );
        float totalRating = 0;
        int ratingCount = 0;
        String lastElevatorStatus = null;
        String lastRampStatus = null;


        if (cursor != null) {
            int commentIndex = cursor.getColumnIndex(CommentsDatabase.COLUMN_COMMENT);
            int dateIndex = cursor.getColumnIndex(CommentsDatabase.COLUMN_DATE);
            int elevatorIndex = cursor.getColumnIndex(CommentsDatabase.COLUMN_ELEVATOR);
            int rampIndex = cursor.getColumnIndex(CommentsDatabase.COLUMN_RAMP);
            int photo1Index = cursor.getColumnIndex(CommentsDatabase.COLUMN_PHOTO_1);
            int photo2Index = cursor.getColumnIndex(CommentsDatabase.COLUMN_PHOTO_2);
            int userNameIndex = cursor.getColumnIndex(CommentsDatabase.COLUMN_USER);

            int ratingIndex = cursor.getColumnIndex(CommentsDatabase.COLUMN_RATING);

            if (commentIndex == -1 || dateIndex == -1 || elevatorIndex == -1 || rampIndex == -1 || photo1Index == -1 || photo2Index == -1 || userNameIndex == -1 || ratingIndex == -1) {
                Log.e("Database Error", "Sütun bulunamadı.");
                cursor.close();
                return;
            }
            Log.d("Database", "Sorgu sonucu satır sayısı: " + cursor.getCount());

            List<Comment> commentsList = new ArrayList<>();
            if (cursor.moveToFirst()) {
                lastElevatorStatus = cursor.getString(elevatorIndex);
                lastRampStatus = cursor.getString(rampIndex);
                float averageRating = cursor.getFloat(ratingIndex);

                Log.d("LastValues", "Last Elevator Status: " + lastElevatorStatus);
                Log.d("LastValues", "Last Ramp Status: " + lastRampStatus);

                updatePlaceDetailsUI(averageRating, lastElevatorStatus, lastRampStatus);
            }
            do {
                String comment = cursor.getString(commentIndex);
                int rating = cursor.getInt(ratingIndex);
                String elevator = cursor.getString(elevatorIndex);
                String ramp = cursor.getString(rampIndex);
                String photo1 = cursor.getString(photo1Index);
                String photo2 = cursor.getString(photo2Index);
                String date = cursor.getString(dateIndex);
                String userName = cursor.getString(userNameIndex);

                commentsList.add(new Comment(comment, date, elevator, ramp, photo1, photo2, userName, rating));
                totalRating += rating;
                ratingCount++;

                Log.d("DatabaseCheck", "Tarih: " + date + ", Asansör: " + elevator + ", Rampa: " + ramp);
            } while (cursor.moveToNext());

            cursor.close();
            Log.d("Database", "cursor close sonrası Sorgu sonucu satır sayısı: " + cursor.getCount());

            CommentsAdapter adapter = new CommentsAdapter(commentsList);
            commentsRecylerView.setAdapter(adapter);

            Log.d("RecyclerView", "Adapter başarıyla bağlandı.");
            int selectedRampId = rampRadioGroup.getCheckedRadioButtonId();

            Log.d("Database", "photo1Path: " + photo1Path);
            Log.d("Database", "photo2Path: " + photo2Path);

            Log.d("RampaDurumu", "Seçilen id: " + selectedRampId);
            Log.d("LoadComments", "Yorumlar yükleniyor...");
            Log.d("LoadComments", "Yorum sayısı: " + commentsList.size());
            Log.d("DB_Load", "Rows Count: " + cursor.getCount());
            Log.d("Database", "Sütun indexleri: " +
                    cursor.getColumnIndex(CommentsDatabase.COLUMN_COMMENT) + ", " +
                    cursor.getColumnIndex(CommentsDatabase.COLUMN_DATE) + ", " +
                    cursor.getColumnIndex(CommentsDatabase.COLUMN_RATING));
            Log.d("LoadComments", "Yorumlar: " + commentsList.toString());

        } else {
            Log.e("Database Error", "Cursor null döndü.");
        }
    }


    private void openImagePickerForPhoto1() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickPhoto1Launcher.launch(intent);
    }

    private void openImagePickerForPhoto2() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickPhoto2Launcher.launch(intent);
    }

    private ActivityResultLauncher<Intent> pickPhoto1Launcher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    ImageView photo1Preview = findViewById(R.id.photo_1_preview);

                    try {

                        photo1Path = getRealPathFromURI(selectedImageUri);
                        if (photo1Path == null) {

                            File tempFile = createTempFileFromUri(selectedImageUri);
                            photo1Path = tempFile.getAbsolutePath();
                        }
                        photo1Preview.setImageURI(Uri.parse(photo1Path));
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Fotoğraf yüklenemedi", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );


    private ActivityResultLauncher<Intent> pickPhoto2Launcher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    ImageView photo2Preview = findViewById(R.id.photo_2_preview);

                    try {

                        photo2Path = getRealPathFromURI(selectedImageUri);
                        if (photo2Path == null) {
                            File tempFile = createTempFileFromUri(selectedImageUri);
                            photo2Path = tempFile.getAbsolutePath();
                        }
                        photo2Preview.setImageURI(Uri.parse(photo2Path));
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Fotoğraf yüklenemedi", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );


    private String getRealPathFromURI(Uri contentUri) {
        Cursor cursor = getContentResolver().query(contentUri, null, null, null, null);
        if (cursor == null) {
            return contentUri.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            String path = cursor.getString(idx);
            cursor.close();
            return path;
        }
    }

    private File createTempFileFromUri(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        File tempFile = File.createTempFile("temp_image", ".jpg", getCacheDir());
        OutputStream outputStream = new FileOutputStream(tempFile);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }

        inputStream.close();
        outputStream.close();

        return tempFile;
    }

    private void updatePlaceDetailsUI(float averageRating, String lastElevatorStatus, String lastRampStatus) {
        TextView averageRatingTextView = findViewById(R.id.average_rating_text);
        TextView lastElevatorStatusTextView = findViewById(R.id.last_elevator_status);
        TextView lastRampStatusTextView = findViewById(R.id.last_ramp_status);
        RatingBar averageRatingBar = findViewById(R.id.average_rating_bar);

        averageRatingBar.setRating(averageRating);
        averageRatingTextView.setText(String.format(Locale.getDefault(), " Puan: %.1f", averageRating));
        lastElevatorStatusTextView.setText("Asansör çalışıyor mu?: " + (lastElevatorStatus != null ? lastElevatorStatus : "Belirtilmemiş"));

        lastRampStatusTextView.setText("Rampa var mı?: " + (lastRampStatus != null ? lastRampStatus : "Belirtilmemiş"));

        Log.d("UpdateUI", "Updating UI with Elevator: " + lastElevatorStatus + ", Ramp: " + lastRampStatus);
    }

}
