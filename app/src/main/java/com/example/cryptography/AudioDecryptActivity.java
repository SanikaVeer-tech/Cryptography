package com.example.cryptography;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class AudioDecryptActivity extends AppCompatActivity {

    private Button btnSelectAudio, btnDecrypt, btnExtractMessage, btnPlayDecrypted;
    private EditText editTextKey;
    private TextView textViewDecryptedMessage;
    private Uri selectedAudioUri;
    private String decryptedAudioBase64, secondAudioBase64, hiddenMessage;
    private MediaPlayer mediaPlayer;
    private DatabaseReference databaseReference;
    private File tempFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_decrypt);

        // Initialize UI elements
        btnSelectAudio = findViewById(R.id.btnSelectAudio);
        btnDecrypt = findViewById(R.id.btnDecrypt);
        btnExtractMessage = findViewById(R.id.btnExtractMessage);
        btnPlayDecrypted = findViewById(R.id.btnPlayDecrypted);
        editTextKey = findViewById(R.id.editTextKey);
        textViewDecryptedMessage = findViewById(R.id.textViewDecryptedMessage);

        // Firebase reference
        databaseReference = FirebaseDatabase.getInstance().getReference("EncryptedAudio");

        // Button Click Listeners
        btnSelectAudio.setOnClickListener(v -> openAudioChooser());
        btnDecrypt.setOnClickListener(v -> decryptAudio());
        btnExtractMessage.setOnClickListener(v -> extractMessage());
        btnPlayDecrypted.setOnClickListener(v -> playHiddenAudio());

        // Hide buttons initially
        btnExtractMessage.setVisibility(View.GONE);
        btnPlayDecrypted.setVisibility(View.GONE);
        textViewDecryptedMessage.setVisibility(View.GONE);

        // Request storage permissions for Android 6+
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            requestStoragePermission();
        }
    }

    private void openAudioChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select Encrypted Audio"), 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            selectedAudioUri = data.getData();
            if (selectedAudioUri != null) {
                Toast.makeText(this, "Encrypted Audio Selected", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to Select Audio", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void decryptAudio() {
        String encryptedKey = editTextKey.getText().toString().trim();

        if (encryptedKey.isEmpty()) {
            Toast.makeText(this, "Please enter the encrypted key", Toast.LENGTH_SHORT).show();
            return;
        }

        // Fetch the encrypted audio and message from Firebase
        databaseReference.child(encryptedKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    decryptedAudioBase64 = snapshot.child("first_audio").getValue(String.class);
                    secondAudioBase64 = snapshot.child("second_audio").getValue(String.class);
                    hiddenMessage = snapshot.child("message").getValue(String.class);

                    if (decryptedAudioBase64 != null) {
                        Toast.makeText(AudioDecryptActivity.this, "Audio Decryption Successful!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(AudioDecryptActivity.this, "Decryption Failed: No main audio found", Toast.LENGTH_SHORT).show();
                    }

                    if (secondAudioBase64 != null) {
                        Toast.makeText(AudioDecryptActivity.this, "Hidden Audio Found!", Toast.LENGTH_SHORT).show();
                        btnPlayDecrypted.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(AudioDecryptActivity.this, "No hidden audio found!", Toast.LENGTH_SHORT).show();
                    }

                    if (hiddenMessage != null) {
                        btnExtractMessage.setVisibility(View.VISIBLE);
                    }

                } else {
                    Toast.makeText(AudioDecryptActivity.this, "Invalid Key! No Data Found.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(AudioDecryptActivity.this, "Error fetching data from Firebase", Toast.LENGTH_SHORT).show();
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

    private void playHiddenAudio() {
        if (secondAudioBase64 == null) {
            Toast.makeText(this, "No hidden audio available", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            byte[] audioBytes = Base64.decode(secondAudioBase64, Base64.DEFAULT);

            // Create a temporary file
            tempFile = File.createTempFile("hidden_audio", ".mp3", getCacheDir());
            FileOutputStream fos = new FileOutputStream(tempFile);
            fos.write(audioBytes);
            fos.close();

            // Release any existing MediaPlayer
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }

            // Prepare MediaPlayer
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(tempFile.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();

            Toast.makeText(this, "Playing Hidden Audio", Toast.LENGTH_SHORT).show();

            // Release MediaPlayer when finished playing
            mediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
                mediaPlayer = null;
            });

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error playing hidden audio", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }
}
