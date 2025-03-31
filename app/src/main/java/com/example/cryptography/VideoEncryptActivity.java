package com.example.cryptography;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VideoEncryptActivity extends AppCompatActivity {
    private static final int REQUEST_COVER_VIDEO = 1;
    private Uri coverVideoUri;
    private String encryptedKey;
    private String hiddenMessage;

    private Button btnSelectCoverVideo, btnEncrypt, btnSave, btnShareKey;
    private EditText editTextMessage;
    private TextView textViewEncryptedKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_encrypt);

        btnSelectCoverVideo = findViewById(R.id.btnSelectCoverVideo);
        btnEncrypt = findViewById(R.id.btnEncrypt);
        btnSave = findViewById(R.id.btnSave);
        btnShareKey = findViewById(R.id.btnShare);
        editTextMessage = findViewById(R.id.editTextMessage);
        textViewEncryptedKey = findViewById(R.id.textViewEncryptedKey);

        // Select cover video button
        btnSelectCoverVideo.setOnClickListener(v -> selectVideo(REQUEST_COVER_VIDEO));

        // Encryption button
        btnEncrypt.setOnClickListener(v -> embedMessageInVideo());

        // Save encrypted video button
        btnSave.setOnClickListener(v -> saveVideoToGallery());

        // Share key button
        btnShareKey.setOnClickListener(v -> shareEncryptedKey());
    }

    private void selectVideo(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
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
            }
        }
    }

    private void embedMessageInVideo() {
        if (coverVideoUri == null || editTextMessage.getText().toString().isEmpty()) {
            Toast.makeText(this, "Select cover video and enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        hiddenMessage = editTextMessage.getText().toString();
        encryptedKey = UUID.randomUUID().toString();

        try {
            // Convert video to byte array
            byte[] coverVideoData = encodeVideoToByteArray(coverVideoUri);

            if (coverVideoData == null) {
                Toast.makeText(this, "Error encoding cover video", Toast.LENGTH_SHORT).show();
                return;
            }

            // Embed message in video (using LSB or other technique)
            byte[] videoWithMessage = embedMessageInCoverVideo(coverVideoData, hiddenMessage);

            // Convert video to Base64 string for saving in Firebase
            String encryptedVideo = Base64.encodeToString(videoWithMessage, Base64.DEFAULT);

            // Save data to Firebase
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference databaseReference = database.getReference("EncryptedVideos");

            // Save encrypted data in Firebase
            databaseReference.child(encryptedKey).child("message").setValue(hiddenMessage);  // Store the original message (optional)
            databaseReference.child(encryptedKey).child("encrypted_key").setValue(encryptedKey);  // Save the key
            databaseReference.child(encryptedKey).child("encrypted_video").setValue(encryptedVideo);  // Save the encrypted video

            // Show the encrypted key in the TextView
            textViewEncryptedKey.setText("Encrypted Key: " + encryptedKey);
            textViewEncryptedKey.setVisibility(View.VISIBLE);
            btnSave.setVisibility(View.VISIBLE); // Show Save button

            Toast.makeText(this, "Message embedded and video saved!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to embed message in video", Toast.LENGTH_LONG).show();
        }
    }


    private byte[] embedMessageInCoverVideo(byte[] coverVideoData, String message) {
        // A simple method to embed the message into the video data
        // You could use Least Significant Bit (LSB) or another steganographic approach here
        byte[] messageBytes = message.getBytes();
        int messageIndex = 0;

        // Loop through the video data and embed the message
        for (int i = 0; i < coverVideoData.length; i++) {
            if (messageIndex < messageBytes.length) {
                // Replace the least significant bit of each byte in the video
                coverVideoData[i] = (byte) ((coverVideoData[i] & 0xFE) | (messageBytes[messageIndex] >> 7));
                messageBytes[messageIndex] = (byte) (messageBytes[messageIndex] << 1);
                messageIndex++;
            } else {
                break;
            }
        }
        return coverVideoData;
    }

    private byte[] encodeVideoToByteArray(Uri videoUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(videoUri);
            byte[] videoBytes = new byte[inputStream.available()];
            inputStream.read(videoBytes);
            inputStream.close();
            return videoBytes;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void saveVideoToGallery() {
        if (coverVideoUri == null) {
            Toast.makeText(this, "No video to save", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a ContentValues object to store metadata about the video
        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.DISPLAY_NAME, "Encrypted_Video_" + System.currentTimeMillis() + ".mp4");
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/EncryptedVideos"); // Save to "Movies" folder (accessible in gallery)

        // Insert video metadata into MediaStore
        Uri videoUri = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);

        try {
            // Open an OutputStream to the MediaStore Uri to write the video data
            OutputStream outputStream = getContentResolver().openOutputStream(videoUri);

            // Get the video data as byte array (Assuming you have video data in byte array from encoding/processing)
            byte[] videoData = encodeVideoToByteArray(coverVideoUri);  // Get video data (you may need to adjust this based on how you're handling video)

            if (videoData != null) {
                // Write the video data to the OutputStream
                outputStream.write(videoData);
                outputStream.close();

                // Show feedback to the user
                Toast.makeText(this, "Video saved to gallery!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Failed to process video data", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save video", Toast.LENGTH_SHORT).show();
        }
    }


    private void shareEncryptedKey() {
        if (encryptedKey != null && !encryptedKey.isEmpty()) {
            // Create the share intent
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Here is the encrypted key for the video: " + encryptedKey);

            // Open a chooser to select how the user wants to share the key
            startActivity(Intent.createChooser(shareIntent, "Share Encrypted Key"));
        } else {
            Toast.makeText(this, "No encrypted key available", Toast.LENGTH_SHORT).show();
        }
    }
}