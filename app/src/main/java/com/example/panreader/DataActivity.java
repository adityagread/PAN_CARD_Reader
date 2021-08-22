package com.example.panreader;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

public class DataActivity extends AppCompatActivity {

    private TextView name, fname, dob, pan;

    private String name_str, fname_str, dob_str, pan_str;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);

        name = (TextView)findViewById(R.id.name);
        fname = (TextView)findViewById(R.id.fname);
        dob = (TextView) findViewById(R.id.dob);
        pan = (TextView) findViewById(R.id.pan);

        Bundle b = getIntent().getExtras();
        name_str = b.getString("NAME");
        fname_str = b.getString("FNAME");
        dob_str = b.getString("DOB");
        pan_str = b.getString("PAN");

        name.setText(name_str);
        fname.setText(fname_str);
        dob.setText(dob_str);
        pan.setText(pan_str);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}