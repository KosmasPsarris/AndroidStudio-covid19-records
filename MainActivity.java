package com.unipi.suat.covid_19;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import androidx.preference.PreferenceManager;

import android.speech.RecognizerIntent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {

    //Declare as global some object, variables,.. so we can use them everywhere :)
    private ListView listView;
    SharedPreferences pref, preferences;
    FloatingActionButton add_button, back_button;
    private FirebaseAuth mAuth;
    FirebaseUser currentUser;
    FirebaseDatabase database;

    //Using onCreate when the activity get starts for the first time to initialize and change objects and variables.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        CheckLang();
        setContentView(R.layout.activity_main);
        listView = findViewById(R.id.listView);
        add_button = findViewById(R.id.add_button);
        back_button = findViewById(R.id.back_button);
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        database = FirebaseDatabase.getInstance();

        final ArrayList<String> list = new ArrayList<>();
        final ArrayAdapter adapter = new ArrayAdapter<String>(this, R.layout.list_item, list);
        listView.setAdapter(adapter);

        //Path to current user's patients node
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child(currentUser.getEmail().replace(".",",")).child("Patients");
        reference.addValueEventListener(new ValueEventListener(){
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                list.clear(); //reset list
                //for each entry add it to list
                for(DataSnapshot snapshot :dataSnapshot.getChildren()){
                    list.add(snapshot.getValue().toString());
                }
                adapter.notifyDataSetChanged(); //So adapter list gets updated
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            //Gets called whenever user clicks on an entry in displayed list
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position,
                                    long arg3)
            {
                String value = (String)parent.getItemAtPosition(position); // Get data of selected patient
                SharedPreferences.Editor editor = pref.edit(); //save it to preferences
                editor.putString("Value", value);
                editor.apply();
                Intent intent = new Intent(MainActivity.this, UpdateActivity.class);
                startActivity(intent); //Move to UpdateActivity
            }
        });

        //When the Plus button is pressed, move to AddActivity
        add_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AddActivity.class);
                startActivity(intent);
                finish();
            }
        });

        //If back arrow is pressed, finish activity
        back_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    //Checking the results of Speech Recognition are matches with the given word.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AddActivity.REC_RESULT && resultCode == RESULT_OK) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (matches.get(0).toLowerCase().equals(getString(R.string.speech_add)))
                add_button.performClick();
            else if (matches.get(0).toLowerCase().equals(getString(R.string.speech_back)))
                finish();
            else
                Toast.makeText(this, getString(R.string.speech_toast) + matches.get(0) + "\n\n" +
                        getString(R.string.speech_toast2) + "\n" +
                        getString(R.string.speech_toast_add) + "\n" +
                        getString(R.string.speech_toast_back) + "\n", Toast.LENGTH_LONG).show();
        }
    }

    private void CheckLang(){
        if(pref.getString("CurrentLang", "En").equals("En")){
            String language = "en";
            Locale locale = new Locale(language);
            Locale.setDefault(locale);
            Configuration configuration  = new Configuration();
            configuration.locale = locale;
            getBaseContext().getResources().updateConfiguration(configuration,getBaseContext().getResources().getDisplayMetrics());
        } else {
            String language = "el";
            Locale locale = new Locale(language);
            Locale.setDefault(locale);
            Configuration configuration  = new Configuration();
            configuration.locale = locale;
            getBaseContext().getResources().updateConfiguration(configuration,getBaseContext().getResources().getDisplayMetrics());
        }
    }

    //Creating menu method
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case R.id.Voice:
                //Starts an activity that will prompt the user for speech and send it through a speech recognizer.
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, R.string.speech_language);
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, R.string.speech);
                startActivityForResult(intent, AddActivity.REC_RESULT);
                return true;
            case R.id.Delete:
                //Alert Dialog, asking user Yes or No to clear whole firebase database.
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.delete_builder_title);
                builder.setMessage(R.string.delete_builder_message);
                builder.setPositiveButton(R.string.delete_yes, new DialogInterface.OnClickListener() {
                    //Deletes ALL data from the current user's Node (NOT ALL data from database)
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        database.getReference().child(currentUser.getEmail().replace(".", ",")).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                database.getReference().removeValue();
                                SharedPreferences.Editor editor = pref.edit();
                                editor.putInt("GpsId", 1);
                                editor.putInt("Id", 1);
                                editor.apply();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                            }
                        });
                        //It unchecks the checkbox that previously checked and returns to Authentication Activity.
                        preferences = getSharedPreferences("checkbox", MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("remember", "false");
                        editor.apply();
                        finish();
                        Intent intent = new Intent(MainActivity.this, AuthenticationActivity.class);
                        startActivity(intent);
                    }
                });
                builder.setNegativeButton(R.string.delete_no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                builder.create().show();

                return true;
        }

            return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed(){
        finish();
    }
}