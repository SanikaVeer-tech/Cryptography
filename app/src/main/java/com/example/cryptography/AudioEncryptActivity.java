package com.example.cryptography;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class AudioEncryptActivity extends AppCompatActivity {
    private static final int PICK_FIRST_AUDIO = 100;
    private static final int PICK_SECOND_AUDIO = 200;
    private static final int STORAGE_PERMISSION_CODE = 101;

    private Button btnSelectAudio, btnSelectAnotherAudio, btnEncrypt, btnShare, btnSave;
    private EditText editTextMessage;
    private TextView textViewEncryptedKey;

    private Uri firstAudioUri, secondAudioUri;
    private DatabaseReference databaseReference;
    private String encryptedKey, firstAudioBase64, secondAudioBase64, hiddenMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_encrypt);

        // Initialize UI components
        btnSelectAudio = findViewById(R.id.btnSelectAudio);
        btnSelectAnotherAudio = findViewById(R.id.btnSelectAnotherAudio);
        btnEncrypt = findViewById(R.id.btnEncrypt);
        btnShare = findViewById(R.id.btnShare);
        btnSave = findViewById(R.id.btnSave);
        editTextMessage = findViewById(R.id.editTextMessage);
        textViewEncryptedKey = findViewById(R.id.textViewEncryptedKey);

        // Initialize Firebase database reference
        databaseReference = FirebaseDatabase.getInstance().getReference("EncryptedAudio");

        // Select audios
        btnSelectAudio.setOnClickListener(v -> openAudioChooser(PICK_FIRST_AUDIO));
        btnSelectAnotherAudio.setOnClickListener(v -> openAudioChooser(PICK_SECOND_AUDIO));

        // Encrypt and Save to Firebase
        btnEncrypt.setOnClickListener(v -> encryptAndSaveAudio());

        // Share encrypted key
        btnShare.setOnClickListener(v -> shareEncryptedKey());

        // Save to local storage
        btnSave.setOnClickListener(v -> checkAndRequestStoragePermission());

        // Hide the Save button initially
        btnSave.setVisibility(View.GONE);
    }

    private void openAudioChooser(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        startActivityForResult(Intent.createChooser(intent, "Select an Audio File"), requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            Uri selectedAudioUri = data.getData();
            if (requestCode == PICK_FIRST_AUDIO) {
                firstAudioUri = selectedAudioUri;
                Toast.makeText(this, "First Audio Selected", Toast.LENGTH_SHORT).show();
            } else if (requestCode == PICK_SECOND_AUDIO) {
                secondAudioUri = selectedAudioUri;
                Toast.makeText(this, "Second Audio Selected", Toast.LENGTH_SHORT).show();
            }
        }
    }



    private void encryptAndSaveAudio() {
        if (firstAudioUri == null || secondAudioUri == null || editTextMessage.getText().toString().isEmpty()) {
            Toast.makeText(this, "Select both audios and enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        hiddenMessage = editTextMessage.getText().toString();
        encryptedKey = UUID.randomUUID().toString();  // Generate a unique key

        // Convert audios to Base64
        firstAudioBase64 = encodeAudioToBase64(firstAudioUri);
        secondAudioBase64 = encodeAudioToBase64(secondAudioUri);

        if (firstAudioBase64 == null || secondAudioBase64 == null) {
            Toast.makeText(this, "Error encoding audio", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save to Firebase
        databaseReference.child(encryptedKey).child("message").setValue(hiddenMessage);
        databaseReference.child(encryptedKey).child("first_audio").setValue(firstAudioBase64);
        databaseReference.child(encryptedKey).child("second_audio").setValue(secondAudioBase64);

        // Show the encrypted key
        textViewEncryptedKey.setText("Encrypted Key: " + encryptedKey);
        textViewEncryptedKey.setVisibility(View.VISIBLE);
        btnSave.setVisibility(View.VISIBLE); // Show Save button

        Toast.makeText(this, "Audio encrypted!", Toast.LENGTH_LONG).show();
    }

    private String encodeAudioToBase64(Uri audioUri) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;

            InputStream inputStream = getContentResolver().openInputStream(audioUri);
            if (inputStream == null) return null;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            byte[] audioBytes = byteArrayOutputStream.toByteArray();
            return Base64.encodeToString(audioBytes, Base64.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void checkAndRequestStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
            } else {
                saveEncryptedAudioLocally();
            }
        } else {
            saveEncryptedAudioLocally();
        }
    }
    private void saveEncryptedAudioLocally() {
        if (firstAudioBase64 == null || encryptedKey == null) {
            Toast.makeText(this, "Encrypt the audio first!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Save in Downloads folder
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Save first audio as .mp3
            String audioFileName = "encrypted_audio_" + System.currentTimeMillis() + ".mp3";
            File audioFile = new File(dir, audioFileName);

            // Decode Base64 back to bytes and save as MP3
            byte[] audioBytes = Base64.decode(firstAudioBase64, Base64.DEFAULT);
            FileOutputStream fos = new FileOutputStream(audioFile);
            fos.write(audioBytes);
            fos.close();

            // Make file visible in file manager
            Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            scanIntent.setData(Uri.fromFile(audioFile));
            sendBroadcast(scanIntent);

            Toast.makeText(this, "Saved as MP3: " + audioFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
            Log.d("DEBUG", "MP3 file saved: " + audioFile.getAbsolutePath());

        } catch (IOException e) {
            e.printStackTrace();
            Log.e("ERROR", "Error saving file: " + e.getMessage());
            Toast.makeText(this, "Error saving MP3 file!", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareEncryptedKey() {
        if (encryptedKey == null) {
            Toast.makeText(this, "No encrypted key available to share", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Encrypted Key: " + encryptedKey);
        startActivity(Intent.createChooser(shareIntent, "Share Key via"));
    }
}
