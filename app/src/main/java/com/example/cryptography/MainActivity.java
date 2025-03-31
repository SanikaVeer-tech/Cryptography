package com.example.cryptography;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    Button btnImageEncrypt, btnImageDecrypt, btnAudioEncrypt, btnAudioDecrypt, btnVideoEncrypt, btnVideoDecrypt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnImageEncrypt = findViewById(R.id.btnImageEncrypt);
        btnImageDecrypt = findViewById(R.id.btnImageDecrypt);
        btnAudioEncrypt = findViewById(R.id.btnAudioEncrypt);
        btnAudioDecrypt = findViewById(R.id.btnAudioDecrypt);
        btnVideoEncrypt = findViewById(R.id.btnVideoEncrypt);
        btnVideoDecrypt = findViewById(R.id.btnVideoDecrypt);

        btnImageEncrypt.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ImageEncryptActivity.class)));
        btnImageDecrypt.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ImageDecryptActivity.class)));
        btnAudioEncrypt.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AudioEncryptActivity.class)));
        btnAudioDecrypt.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AudioDecryptActivity.class)));
        btnVideoEncrypt.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, VideoEncryptActivity.class)));
        btnVideoDecrypt.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, VideoDecryptActivity.class)));
    }
}
