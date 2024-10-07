package com.shaihi.imagetotextexample;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 100;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.READ_MEDIA_IMAGES,
    };

    private ImageView imageView;
    private TextView textView;

    // Register the activity result launcher
    private final ActivityResultLauncher<Intent> getContent =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(), imageUri);
                        try {
                            Bitmap bitmap = ImageDecoder.decodeBitmap(source);
                            imageView.setImageBitmap(bitmap);
                            recognizeTextFromImage(bitmap);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.textView);

        checkPermissions();
    }

    private void checkPermissions() {
        if (!hasPermissions(this, REQUIRED_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        } else {
            // Permissions granted, proceed with the image selection
            openImage();
        }
    }

    private boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void openImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        getContent.launch(intent); // Launch the image picker using the Activity Result API
    }

    private void recognizeTextFromImage(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizerOptions options = new TextRecognizerOptions.Builder()
                //.setConfidenceThreshold(0.5f) // Set confidence threshold for recognition
                .build();

        TextRecognition.getClient(options).process(image)
                .addOnSuccessListener(this::displayRecognizedText)
                .addOnFailureListener(e -> Log.e("TextRecognition", "Error recognizing text", e));
    }

    private void displayRecognizedText(Text result) {
        StringBuilder recognizedText = new StringBuilder();
        for (Text.TextBlock block : result.getTextBlocks()) {
            recognizedText.append(block.getText()).append("\n");
        }
        textView.setText(recognizedText.toString());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.length > 0) {
                boolean allPermissionsGranted = true;
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        allPermissionsGranted = false;
                        break;
                    }
                }
                if (allPermissionsGranted) {
                    // Permissions granted, proceed with the image selection
                    openImage();
                } else {
                    Toast.makeText(this, "Permissions denied. Cannot proceed.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Permissions request was cancelled.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
