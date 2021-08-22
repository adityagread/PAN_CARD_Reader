package com.example.panreader;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;

import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button grabPic;
    ProgressBar progressBar;
    private TextRecognizer recognizer;
    public static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        grabPic = (Button) findViewById(R.id.grabPic);
        progressBar= findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
        grabPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                grabPic();
            }
        });
    }


    private void grabPic(){
        if(checkPermission()){
            takePic();
        }
        else
            requestPermission();
    }

    public static final int PERM_STORAGE_REQ_CODE = 7190;
    private void requestPermission(){
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERM_STORAGE_REQ_CODE);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == PERM_STORAGE_REQ_CODE){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePic();
            } else {
                Toast.makeText(MainActivity.this, "Permission Denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Uri imageUri;
    public static final int PHOTO_REQ_CODE = 7160;

    private void takePic(){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photo = new File(Environment.getExternalStorageDirectory(),"pic.jpg");
        imageUri = Uri.fromFile(photo);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, PHOTO_REQ_CODE);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PHOTO_REQ_CODE && resultCode == RESULT_OK) {
            progressBar.setVisibility(View.VISIBLE);
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            InputImage image = InputImage.fromBitmap(bitmap, 0);
            grabPic.setEnabled(false);
            recognizer.process(image)
                    .addOnSuccessListener(
                            new OnSuccessListener<Text>() {
                                @Override
                                public void onSuccess(Text texts) {
                                    grabPic.setEnabled(true);
                                    ArrayList<String> linetext = new ArrayList<>();
                                    for (Text.TextBlock block : texts.getTextBlocks()) {
                                        for(Text.Line line : block.getLines()){
                                            String lineText = line.getText();
                                            if(lineText.equals(lineText.toUpperCase()))
                                                linetext.add(lineText);
                                        }
                                    }
                                    parseData(linetext);
                                    linetext.clear();
                                }
                            })
                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Task failed with an exception
                                    grabPic.setEnabled(true);
                                    e.printStackTrace();
                                }
                            });
        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private boolean isPAN  = false, isDOB = false, isNAME = false, isFNAME = false;
    private String PAN = "", DOB = "", NAME = "", FNAME = "";

    private void parseData(ArrayList<String> lines) {
        for (String line : lines) {
            if (!isPAN) {
                if (line.matches("((?=.*\\d)(?=.*[A-Z]).{5,})")) {
                    PAN = line;
                    isPAN = true;
                    continue;
                }
            }

            if (!isDOB) {
                if (line.contains("/")) {
                    DOB = line;
                    isDOB = true;
                    continue;
                }
            }
            if (line.contains("GOVT") || line.contains("TAX") || line.contains("INCOME") || line.contains("INDIA")) {
                continue;
            }

            if (line.length() >= 3) {
                if (!isNAME) {
                    NAME = line;
                    isNAME = true;
                    continue;
                }
                if (!isFNAME) {
                    FNAME = line;
                    isFNAME = true;
                    continue;
                }
            }
        }

        if(DOB.equals("") || PAN.equals("") || NAME.equals("") || FNAME.equals("")){
            Toast.makeText(getApplicationContext(), "Not a PAN Card! \nTry Again!", Toast.LENGTH_LONG).show();
            progressBar.setVisibility(View.GONE);
        }
        else {
            Intent intent = new Intent(getApplicationContext(), DataActivity.class);
            intent.putExtra("NAME", NAME);
            intent.putExtra("FNAME", FNAME);
            intent.putExtra("DOB", DOB);
            intent.putExtra("PAN", PAN);
            progressBar.setVisibility(View.GONE);
            startActivity(intent);
        }

        isPAN = false; isDOB = false; isNAME = false; isFNAME = false;
        PAN = ""; DOB = ""; NAME = ""; FNAME = "";
    }

    private boolean checkPermission() {
        if(ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            return true;
        }
        else return false;
    }
}