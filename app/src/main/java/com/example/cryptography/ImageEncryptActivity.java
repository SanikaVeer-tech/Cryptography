package com.example.cryptography;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.UUID;

public class ImageEncryptActivity extends AppCompatActivity {

    private ImageView imageView;
    private EditText editTextMessage;
    private TextView textViewEncryptedKey;
    private Button btnSelectImage, btnEncrypt, btnShare, btnSaveGallery, btnSendImage;
    private Bitmap selectedBitmap;
    private DatabaseReference databaseReference;
    private Uri savedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_encrypt);

        imageView = findViewById(R.id.imageView);
        editTextMessage = findViewById(R.id.editTextMessage);
        textViewEncryptedKey = findViewById(R.id.textViewEncryptedKey);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnEncrypt = findViewById(R.id.btnEncrypt);
        btnShare = findViewById(R.id.btnShare);
        btnSaveGallery = findViewById(R.id.btnSaveGallery);
        btnSendImage = findViewById(R.id.btnSendImage);

        databaseReference = FirebaseDatabase.getInstance().getReference("EncryptedImages");

        btnSelectImage.setOnClickListener(v -> openImageChooser());
        btnEncrypt.setOnClickListener(v -> encryptAndSaveImage());
        btnShare.setOnClickListener(v -> shareEncryptedKey());
        btnSaveGallery.setOnClickListener(v -> saveImageToGallery());
        btnSendImage.setOnClickListener(v -> shareSavedImage());
    }

    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                selectedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                imageView.setImageBitmap(selectedBitmap);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void encryptAndSaveImage() {
        if (selectedBitmap == null || editTextMessage.getText().toString().isEmpty()) {
            Toast.makeText(this, "Select an image and enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        String hiddenMessage = editTextMessage.getText().toString();
        String encryptedKey = UUID.randomUUID().toString().trim();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        selectedBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        String encryptedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);

        databaseReference.child(encryptedKey).child("message").setValue(hiddenMessage);
        databaseReference.child(encryptedKey).child("image").setValue(encryptedImage);

        textViewEncryptedKey.setText("Encrypted Key: " + encryptedKey);
        textViewEncryptedKey.setVisibility(View.VISIBLE);

        Toast.makeText(this, "Image encrypted! Key: " + encryptedKey, Toast.LENGTH_LONG).show();
    }

    private void shareEncryptedKey() {
        String encryptedKey = textViewEncryptedKey.getText().toString();

        if (encryptedKey.isEmpty()) {
            Toast.makeText(this, "No encrypted key available to share", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, encryptedKey);

        startActivity(Intent.createChooser(shareIntent, "Share Key via"));
    }

    private void saveImageToGallery() {
        if (selectedBitmap == null) {
            Toast.makeText(this, "No image to save", Toast.LENGTH_SHORT).show();
            return;
        }

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "Encrypted_Image_" + System.currentTimeMillis() + ".png");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/EncryptedImages");

        savedImageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        try {
            OutputStream outputStream = getContentResolver().openOutputStream(savedImageUri);
            selectedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.close();

            Toast.makeText(this, "Image saved to gallery!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareSavedImage() {
        if (savedImageUri == null) {
            Toast.makeText(this, "No saved image to share", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/png");
        shareIntent.putExtra(Intent.EXTRA_STREAM, savedImageUri);
        startActivity(Intent.createChooser(shareIntent, "Share Saved Image"));
    }
}
