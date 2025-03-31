package com.example.cryptography;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

public class EncryptVideoActivity extends AppCompatActivity {
    private static final int REQUEST_COVER_VIDEO = 1;
    private static final int REQUEST_HIDDEN_VIDEO = 2;

    private Uri coverVideoUri, hiddenVideoUri;
    private String encryptedKey, hiddenMessage;

    private Button btnSelectCoverVideo, btnSelectHiddenVideo, btnEncrypt, btnShareKey;
    private EditText editTextMessage;
    private TextView textViewEncryptedKey;

    private FirebaseStorage storage;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encrypt_video);

        // Initialize Firebase components
        storage = FirebaseStorage.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("EncryptedVideos");

        btnSelectCoverVideo = findViewById(R.id.btnSelectCoverVideo);
        btnSelectHiddenVideo = findViewById(R.id.btnSelectSecondVideo);
        btnEncrypt = findViewById(R.id.btnEncrypt);
        btnShareKey = findViewById(R.id.btnShare);
        editTextMessage = findViewById(R.id.editTextMessage);
        textViewEncryptedKey = findViewById(R.id.textViewEncryptedKey);

        btnSelectCoverVideo.setOnClickListener(v -> selectVideo(REQUEST_COVER_VIDEO));
        btnSelectHiddenVideo.setOnClickListener(v -> selectVideo(REQUEST_HIDDEN_VIDEO));
        btnEncrypt.setOnClickListener(v -> encryptAndUploadVideo());
        btnShareKey.setOnClickListener(v -> shareEncryptedKey());
    }

    private void selectVideo(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            Uri selectedVideoUri = data.getData();
            if (requestCode == REQUEST_COVER_VIDEO) {
                coverVideoUri = selectedVideoUri;
                Toast.makeText(this, "Cover Video Selected", Toast.LENGTH_SHORT).show();
            } else if (requestCode == REQUEST_HIDDEN_VIDEO) {
                hiddenVideoUri = selectedVideoUri;
                Toast.makeText(this, "Hidden Video Selected", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void encryptAndUploadVideo() {
        if (coverVideoUri == null || hiddenVideoUri == null || editTextMessage.getText().toString().isEmpty()) {
            Toast.makeText(this, "Select both videos and enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        hiddenMessage = editTextMessage.getText().toString();
        encryptedKey = UUID.randomUUID().toString();

        try {
            // Merge Cover Video + Hidden Video
            Uri mergedVideoUri = mergeVideos(coverVideoUri, hiddenVideoUri);
            if (mergedVideoUri == null) {
                Toast.makeText(this, "Failed to merge videos", Toast.LENGTH_SHORT).show();
                return;
            }

            // Upload to Firebase Storage
            StorageReference videoRef = storage.getReference().child("EncryptedVideos/" + encryptedKey + ".mp4");
            UploadTask uploadTask = videoRef.putFile(mergedVideoUri);

            uploadTask.addOnSuccessListener(taskSnapshot ->
                    videoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String videoUrl = uri.toString();

                        // Save metadata to Firebase Database
                        databaseReference.child(encryptedKey).child("message").setValue(hiddenMessage);
                        databaseReference.child(encryptedKey).child("encrypted_key").setValue(encryptedKey);
                        databaseReference.child(encryptedKey).child("video_url").setValue(videoUrl);

                        textViewEncryptedKey.setText("Encrypted Key: " + encryptedKey);
                        textViewEncryptedKey.setVisibility(View.VISIBLE);
                        Toast.makeText(EncryptVideoActivity.this, "Encryption Successful! Video uploaded.", Toast.LENGTH_LONG).show();
                    })).addOnFailureListener(e ->
                    Toast.makeText(EncryptVideoActivity.this, "Upload failed!", Toast.LENGTH_SHORT).show());

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Encryption failed", Toast.LENGTH_LONG).show();
        }
    }

    private Uri mergeVideos(Uri coverUri, Uri hiddenUri) {
        try {
            File mergedFile = new File(getCacheDir(), "merged_video.mp4");
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            InputStream inputStreamCover = getContentResolver().openInputStream(coverUri);
            InputStream inputStreamHidden = getContentResolver().openInputStream(hiddenUri);

            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = inputStreamCover.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            while ((bytesRead = inputStreamHidden.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStreamCover.close();
            inputStreamHidden.close();

            byte[] mergedData = outputStream.toByteArray();
            FileOutputStream fos = new FileOutputStream(mergedFile);
            fos.write(mergedData);
            fos.close();

            return Uri.fromFile(mergedFile);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void shareEncryptedKey() {
        if (encryptedKey != null && !encryptedKey.isEmpty()) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Here is the encrypted key for the video: " + encryptedKey);
            startActivity(Intent.createChooser(shareIntent, "Share Encrypted Key"));
        } else {
            Toast.makeText(this, "No encrypted key available", Toast.LENGTH_SHORT).show();
        }
    }
}
