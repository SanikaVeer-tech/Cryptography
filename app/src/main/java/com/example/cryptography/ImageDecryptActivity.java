package com.example.cryptography;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.InputStream;

public class ImageDecryptActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView imageView;
    private EditText editTextKey;
    private TextView textViewDecryptedMessage;
    private Button btnSelectImage, btnDecrypt;
    private Bitmap decryptedBitmap;
    private DatabaseReference databaseReference;
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_decrypt);

        imageView = findViewById(R.id.imageView);
        editTextKey = findViewById(R.id.editTextKey);
        textViewDecryptedMessage = findViewById(R.id.textViewDecryptedMessage);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnDecrypt = findViewById(R.id.btnDecrypt);

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("EncryptedImages");

        // Handle the select image button click
        btnSelectImage.setOnClickListener(v -> openImageChooser());

        // Handle the decrypt button click
        btnDecrypt.setOnClickListener(v -> decryptImage());
    }

    // Open Image Picker to select an image
    private void openImageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_IMAGE_REQUEST);
    }

    // Handle the result of image selection
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            try {
                selectedImageUri = data.getData();
                InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                Bitmap selectedBitmap = BitmapFactory.decodeStream(inputStream);
                imageView.setImageBitmap(selectedBitmap); // Show selected image
            } catch (Exception e) {
                Toast.makeText(this, "Failed to select image", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    // Decrypt Image from Firebase
    private void decryptImage() {
        String encryptedKey = editTextKey.getText().toString();

        if (encryptedKey.isEmpty()) {
            Toast.makeText(this, "Enter an encrypted key", Toast.LENGTH_SHORT).show();
            return;
        }

        // Fetch the data from Firebase using the encrypted key
        databaseReference.child(encryptedKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Retrieve the encrypted image and hidden message
                    String encryptedImage = dataSnapshot.child("image").getValue(String.class);
                    String hiddenMessage = dataSnapshot.child("message").getValue(String.class);

                    if (encryptedImage != null && hiddenMessage != null) {
                        // Decrypt the image (Base64 to Bitmap)
                        byte[] decodedBytes = Base64.decode(encryptedImage, Base64.DEFAULT);
                        decryptedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                        imageView.setImageBitmap(decryptedBitmap);

                        // Display the hidden message
                        textViewDecryptedMessage.setText("Decrypted Message: " + hiddenMessage);
                    } else {
                        Toast.makeText(ImageDecryptActivity.this, "Decryption failed", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(ImageDecryptActivity.this, "Invalid Key", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ImageDecryptActivity.this, "Error accessing Firebase", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
