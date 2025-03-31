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
import android.widget.VideoView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class DecryptVideoActivity extends AppCompatActivity {
    private Button btnSelectVideo, btnDecrypt, btnExtractMessage, btnPlayHiddenVideo;
    private EditText editTextKey;
    private TextView textViewDecryptedMessage;
    private VideoView videoViewDecrypted;
    private Uri selectedVideoUri;
    private String decryptedVideoBase64, hiddenVideoBase64, hiddenMessage;
    private DatabaseReference databaseReference;
    private File tempFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decrypt_video);

        // Initialize UI elements
        btnSelectVideo = findViewById(R.id.btnSelectEncryptedVideo);
        btnDecrypt = findViewById(R.id.btnDecrypt);
        btnExtractMessage = findViewById(R.id.btnExtractMessage);
        btnPlayHiddenVideo = findViewById(R.id.btnPlayHiddenVideo);
        editTextKey = findViewById(R.id.editTextEncryptedKey);
        textViewDecryptedMessage = findViewById(R.id.textViewExtractedMessage);
        videoViewDecrypted = findViewById(R.id.videoViewDecrypted);

        // Firebase reference
        databaseReference = FirebaseDatabase.getInstance().getReference("EncryptedVideos");

        // Button Click Listeners
        btnSelectVideo.setOnClickListener(v -> openVideoChooser());
        btnDecrypt.setOnClickListener(v -> decryptVideo());
        btnExtractMessage.setOnClickListener(v -> extractMessage());
        btnPlayHiddenVideo.setOnClickListener(v -> playHiddenVideo());

        // Hide buttons initially
        btnExtractMessage.setVisibility(View.GONE);
        btnPlayHiddenVideo.setVisibility(View.GONE);
        textViewDecryptedMessage.setVisibility(View.GONE);
    }

    private void openVideoChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select Encrypted Video"), 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            selectedVideoUri = data.getData();
            if (selectedVideoUri != null) {
                Toast.makeText(this, "Encrypted Video Selected", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to Select Video", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void decryptVideo() {
        String encryptedKey = editTextKey.getText().toString().trim();

        if (encryptedKey.isEmpty()) {
            Toast.makeText(this, "Please enter the encrypted key", Toast.LENGTH_SHORT).show();
            return;
        }

        // Fetch the encrypted video and message from Firebase
        databaseReference.child(encryptedKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    decryptedVideoBase64 = snapshot.child("cover_video").getValue(String.class);
                    hiddenVideoBase64 = snapshot.child("hidden_video").getValue(String.class);
                    hiddenMessage = snapshot.child("message").getValue(String.class);

                    if (decryptedVideoBase64 != null) {
                        Toast.makeText(DecryptVideoActivity.this, "Video Decryption Successful!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(DecryptVideoActivity.this, "Decryption Failed: No cover video found", Toast.LENGTH_SHORT).show();
                    }

                    if (hiddenVideoBase64 != null) {
                        Toast.makeText(DecryptVideoActivity.this, "Hidden Video Found!", Toast.LENGTH_SHORT).show();
                        btnPlayHiddenVideo.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(DecryptVideoActivity.this, "No hidden video found!", Toast.LENGTH_SHORT).show();
                    }

                    if (hiddenMessage != null) {
                        btnExtractMessage.setVisibility(View.VISIBLE);
                    }
                } else {
                    Toast.makeText(DecryptVideoActivity.this, "Invalid Key! No Data Found.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(DecryptVideoActivity.this, "Error fetching data from Firebase", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void extractMessage() {
        if (hiddenMessage != null) {
            textViewDecryptedMessage.setText("Decrypted Message: " + hiddenMessage);
            textViewDecryptedMessage.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Message Extracted!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No message found!", Toast.LENGTH_SHORT).show();
        }
    }

    private void playHiddenVideo() {
        if (hiddenVideoBase64 == null) {
            Toast.makeText(this, "No hidden video available", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            byte[] videoBytes = Base64.decode(hiddenVideoBase64, Base64.DEFAULT);

            // Create a temporary file
            tempFile = File.createTempFile("hidden_video", ".mp4", getCacheDir());
            FileOutputStream fos = new FileOutputStream(tempFile);
            fos.write(videoBytes);
            fos.close();

            // Play the video
            videoViewDecrypted.setVideoURI(Uri.fromFile(tempFile));
            videoViewDecrypted.start();

            Toast.makeText(this, "Playing Hidden Video", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error playing hidden video", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }
}
