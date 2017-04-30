package com.example.mayank.freeloc;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class my_name extends AppCompatActivity {


    int ENTRY_REQUEST = 888;
    EditText namefield;
    Button cont;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_name);

        namefield = (EditText)findViewById(R.id.name);
        cont = (Button) findViewById(R.id.cont);

        cont.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String name =  namefield.getText().toString();
                Intent intent = new Intent(getApplicationContext(), my_location.class);
                Bundle b = new Bundle();
                b.putString("NAME", name); //Your id
                intent.putExtras(b); //Put your id to your next Intent
                startActivity(intent);
            }
        });
    }
}
