package com.example.cryptography;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import android.widget.MediaController;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class VideoDecryptActivity extends AppCompatActivity {

    private static final int PICK_VIDEO_REQUEST = 1;
    private VideoView videoView;
    private EditText editTextKey;
    private TextView textViewDecryptedMessage;
    private Button btnSelectVideo, btnDecrypt;
    private Uri selectedVideoUri;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_decrypt);

        videoView = findViewById(R.id.videoView);
        editTextKey = findViewById(R.id.editTextKey);
        textViewDecryptedMessage = findViewById(R.id.textViewDecryptedMessage);
        btnSelectVideo = findViewById(R.id.btnSelectVideo);
        btnDecrypt = findViewById(R.id.btnDecrypt);

        // Initialize Firebase Database and Storage
        databaseReference = FirebaseDatabase.getInstance().getReference("EncryptedVideos");
        storageReference = FirebaseStorage.getInstance().getReference("Videos");

        btnSelectVideo.setOnClickListener(v -> openVideoChooser());
        btnDecrypt.setOnClickListener(v -> decryptVideo());
    }

    private void openVideoChooser() {
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Video"), PICK_VIDEO_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_VIDEO_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedVideoUri = data.getData();
            videoView.setVideoURI(selectedVideoUri);

            MediaController mediaController = new MediaController(this);
            mediaController.setAnchorView(videoView);
            videoView.setMediaController(mediaController);
            videoView.start();
        }
    }

    private void decryptVideo() {
        String encryptedKey = editTextKey.getText().toString().trim();

        if (encryptedKey.isEmpty()) {
            Toast.makeText(this, "Enter an encrypted key", Toast.LENGTH_SHORT).show();
            return;
        }

        databaseReference.child(encryptedKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String videoDownloadUrl = dataSnapshot.child("videoUrl").getValue(String.class);
                    String hiddenMessage = dataSnapshot.child("message").getValue(String.class);

                    if (videoDownloadUrl != null) {
                        // Retrieve video from Firebase Storage
                        playDecryptedVideo(videoDownloadUrl);
                    } else {
                        Toast.makeText(VideoDecryptActivity.this, "decrypt video", Toast.LENGTH_SHORT).show();
                    }

                    if (hiddenMessage != null) {
                        textViewDecryptedMessage.setText("Decrypted Message: " + hiddenMessage);
                    } else {
                        textViewDecryptedMessage.setText("No hidden message found.");
                    }
                } else {
                    Toast.makeText(VideoDecryptActivity.this, "Invalid Key", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(VideoDecryptActivity.this, "Error accessing Firebase", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void playDecryptedVideo(String videoUrl) {
        storageReference.child(videoUrl).getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    videoView.setVideoURI(uri);
                    videoView.start();
                })
                .addOnFailureListener(e -> Toast.makeText(VideoDecryptActivity.this, "Failed to load video", Toast.LENGTH_SHORT).show());
    }
}
