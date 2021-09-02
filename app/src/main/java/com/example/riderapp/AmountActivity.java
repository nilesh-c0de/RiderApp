package com.example.riderapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.riderapp.Model.Rider;
import com.google.android.material.chip.Chip;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

public class AmountActivity extends AppCompatActivity {

    String driverUid, url, name;
    Button submit;
    Chip chip1, chip2, chip3, chip4, chip5;
    EditText amount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_amount);

        if(getIntent()!=null)
        {
            url = getIntent().getStringExtra("url");
            name = getIntent().getStringExtra("name");
            driverUid = getIntent().getStringExtra("token");
        }

        chip1 = findViewById(R.id.chip1);
        chip2 = findViewById(R.id.chip2);
        chip3 = findViewById(R.id.chip3);
        chip4 = findViewById(R.id.chip4);
        chip5 = findViewById(R.id.chip5);

        chip1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                amount.setText("10");
            }
        });

        chip2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                amount.setText("20");
            }
        });

        chip3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                amount.setText("30");
            }
        });

        chip4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                amount.setText("40");
            }
        });

        chip5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                amount.setText("50");
            }
        });

        submit = findViewById(R.id.btn_submit);
        amount = findViewById(R.id.edit_amount);

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int amt = Integer.parseInt(amount.getText().toString());

                Intent fbintent = new Intent(getBaseContext(), FeedbackActivity.class); // changed
                fbintent.putExtra("name", name);
                fbintent.putExtra("url", url);
                fbintent.putExtra("token", driverUid);
                fbintent.putExtra("amt", amt);
                startActivity(fbintent);
            }
        });
    }
}
