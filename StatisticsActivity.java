package com.unipi.suat.covid_19;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class StatisticsActivity extends AppCompatActivity {

    FirebaseDatabase database;
    SharedPreferences pref;
    private FirebaseAuth mAuth;
    FirebaseUser currentUser;
    Button all_button, week_button, by_age_button, back_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        CheckLang();
        setContentView(R.layout.activity_statistics);
        database = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        all_button = findViewById(R.id.all_button);
        week_button = findViewById(R.id.week_button);
        by_age_button = findViewById(R.id.by_age_button);
        back_button = findViewById(R.id.back_button);


        //Set actionbar title
        ActionBar ab = getSupportActionBar();
        if(ab !=null)
        {
            ab.setTitle(R.string.actionbar);
        }
    }

    //All Gps data of current user
    public void all(View view) {
        //Get firebase database path to Gps information Node
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child(currentUser.getEmail().replace(".",",")).child("Location Info");
        //Use addListenerForSingleValueEvent so this won't keep running indefinitely whenever Data is changed
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) { //If there is at least once entry
                    StringBuilder builder = new StringBuilder();
                    for (DataSnapshot entry : dataSnapshot.getChildren()) { //For each entry
                        builder.append(getString(R.string.builder_entry)+" ").append(entry).append("\n");
                        builder.append("----------------------------------------------------\n");
                    }
                    showMessage(getString(R.string.builder_results), builder.toString());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    //Shows records of patients that occurred the last week
    public void week(View view) {
        //Path to user's patients node
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child(currentUser.getEmail().replace(".",",")).child("Patients");
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get current week number
                LocalDate date1 = LocalDate.now();
                WeekFields weekFields1 = WeekFields.of(Locale.getDefault());
                int currentWeek = date1.get(weekFields1.weekOfWeekBasedYear());

                if (dataSnapshot.exists()) {
                    StringBuilder builder = new StringBuilder();
                    //GET all records of the patients node
                    for (DataSnapshot entry : dataSnapshot.getChildren()) {
                        //Split each record and save the date
                        String[] separated = entry.toString().split(",");
                        String date = separated[5];
                        //Manipulate the date so we can check if it belongs to current week
                        date = date.replace("-", "/");
                        DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
                        try {
                            Date calendar = formatter.parse(date);
                            // Get week number from timestamp
                            // Convert long timestamp to LoCalDateTime
                            LocalDateTime triggerTime =
                                    LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(String.valueOf(calendar.getTime()))),
                                            TimeZone.getDefault().toZoneId());

                            // Convert LocalDateTime to LocalDate and find week number
                            LocalDate date2 = triggerTime.toLocalDate();
                            WeekFields weekFields2 = WeekFields.of(Locale.getDefault());
                            int weekFromTimestamp = date2.get(weekFields2.weekOfWeekBasedYear());

                            // if given week is in this week, show it
                            if (weekFromTimestamp == currentWeek) {
                                builder.append(getString(R.string.builder_entry)+" ").append(entry).append("\n");
                                builder.append("----------------------------------------------------\n");
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                    showMessage(getString(R.string.builder_results), builder.toString());
                }
            }


            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    //Shows all patients that are older than 30 years old
    public void by_age(View view) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child(currentUser.getEmail().replace(".",",")).child("Patients");
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    StringBuilder builder = new StringBuilder();
                    //For all records of patients Node split and get the age
                    for (DataSnapshot entry : dataSnapshot.getChildren()) {
                        String[] separated = entry.toString().split(",");
                        String age = separated[6];
                        if(Integer.parseInt(age) > 30) { //check if older than 30 years old
                            builder.append(getString(R.string.builder_entry)+" ").append(entry).append("\n");
                            builder.append("----------------------------------------------------\n");
                        }
                    }
                    showMessage(getString(R.string.builder_results), builder.toString());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    //Selected and got values are printed here.
    public void showMessage(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true)
                .setTitle(title)
                .setMessage(message)
                .show();
    }

    //Checking the results of Speech Recognition are matches with the given word.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AddActivity.REC_RESULT && resultCode == RESULT_OK) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (matches.get(0).toLowerCase().equals(getString(R.string.speech_all)))
                all_button.performClick();
            else if (matches.get(0).toLowerCase().equals(getString(R.string.speech_week)))
                week_button.performClick();
            else if (matches.get(0).toLowerCase().equals(getString(R.string.speech_by_age)))
                by_age_button.performClick();
            else if (matches.get(0).toLowerCase().equals(getString(R.string.speech_back)))
                back_button.performClick();
            else
                Toast.makeText(this, getString(R.string.speech_toast) + matches.get(0) + "\n\n" +
                        getString(R.string.speech_toast2) + "\n" +
                        getString(R.string.speech_toast_all) + "\n" +
                        getString(R.string.speech_toast_week) + "\n" +
                        getString(R.string.speech_toast_by_age) + "\n" +
                        getString(R.string.speech_toast_back), Toast.LENGTH_LONG).show();
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
        inflater.inflate(R.menu.speech, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //Starts an activity that will prompt the user for speech and send it through a speech recognizer.
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, R.string.speech_language);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, R.string.speech);
        startActivityForResult(intent, AddActivity.REC_RESULT);
        return super.onOptionsItemSelected(item);
    }

    public void back(View view) {
        finish();
    }

    //Override the phone's back arrow functionality so it finishes the activity
    @Override
    public void onBackPressed(){
        finish();
    }
}