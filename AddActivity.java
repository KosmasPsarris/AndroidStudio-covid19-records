package com.unipi.suat.covid_19;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;


public class AddActivity extends AppCompatActivity {

    //Declare as global some object, variables,.. so we can use them everywhere :)
    static final int REC_RESULT = 123;
    private FirebaseAuth mAuth;
    FirebaseUser currentUser;
    TextView tv_firstname, tv_lastname, tv_address, tv_date, tv_age, tv_phonenumber;
    EditText firstname, lastname, address, date, age, phonenumber;
    Button add_button, back_button, list_button, statistics_button;
    FirebaseDatabase database;
    DatabaseReference myRef;
    SharedPreferences pref, preferences;

    //Using onCreate when the activity get starts for the first time to initialize and change objects and variables.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        CheckLang();
        setContentView(R.layout.activity_add);
        database = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        firstname = findViewById(R.id.firstname);
        lastname = findViewById(R.id.lastname);
        address = findViewById(R.id.address);
        date = findViewById(R.id.date);
        age = findViewById(R.id.age);
        phonenumber = findViewById(R.id.phonenumber);
        add_button = findViewById(R.id.add_button);
        back_button = findViewById(R.id.back_button);
        list_button = findViewById(R.id.list_button);
        statistics_button = findViewById(R.id.statistics_button);

        tv_firstname = findViewById(R.id.tv_firstname);
        tv_lastname = findViewById(R.id.tv_lastname);
        tv_address = findViewById(R.id.tv_address);
        tv_date = findViewById(R.id.tv_date);
        tv_age = findViewById(R.id.tv_age);
        tv_phonenumber = findViewById(R.id.tv_phonenumber);

    }

    public void addUser(View v) {
        boolean checkFormat = false;
        boolean checkifEmpty = false;
        //Check if given date is of correct format
        if (Pattern.compile("^(?:(?:31(/|-|.)(?:0?[13578]|1[02]))\\1|(?:(?:29|30)(/|-|.)(?:0?[13-9]|1[0-2])\\2))(?:(?:1[6-9]|[2-9]\\d)?\\d{2})$|^(?:29(/|-|.)0?2\\3(?:(?:(?:1[6-9]|[2-9]\\d)?(?:0[48]|[2468][048]|[13579][26])|(?:(?:16|[2468][048]|[3579][26])00))))$|^(?:0?[1-9]|1\\d|2[0-8])(/|-|.)(?:(?:0?[1-9])|(?:1[0-2]))\\4(?:(?:1[6-9]|[2-9]\\d)?\\d{2})$").matcher(date.getText()).matches()) {
            checkFormat = true;
        }
        else{
            Toast.makeText(this, R.string.add_date_toast, Toast.LENGTH_LONG).show();
        }

        //Checks if the text boxes are empty
        if(firstname.getText().toString().isEmpty() ||
                lastname.getText().toString().isEmpty() ||
                address.getText().toString().isEmpty() ||
                date.getText().toString().isEmpty() ||
                age.getText().toString().isEmpty() ||
                phonenumber.getText().toString().isEmpty()) {
            Toast.makeText(this, R.string.add_empty, Toast.LENGTH_LONG).show();
        }else{
            checkifEmpty = true;
        }
        if(checkFormat && checkifEmpty) {

            //Save the character ('-' or '/') based on what the user typed, so we construct the updated date accordingly. (length-5 -> left of yyyy)
            char dateCharacter = date.getText().toString().charAt(date.getText().toString().length()-5);

            //Replace - with '/' on the given date and split it on '/'
            String dateTemp = date.getText().toString().replace("-", "/");

            //separated[0] = Day - separated[1] = Month - separated[2] = Year
            String[] separated = dateTemp.split("/");

            //If Day value is of format single (D) and not (DD), add a zero on the left of the given day
            if(separated[0].length() !=2)
                separated[0] = "0"+separated[0];

            //Similarly with the month value
            if(separated[1].length() !=2)
                separated[1] = "0"+separated[1];

            //We add day, month and year values after the update together to form the updated date (year value is unchanged)
            String resultDate = separated[0] +dateCharacter+ separated[1] +dateCharacter+ separated[2];
            currentUser = mAuth.getCurrentUser();
            myRef = database.getReference().child(currentUser.getEmail().replace(".",",")).child("Patients").child(String.valueOf(pref.getInt("Id", 1)));
            myRef.setValue(String.valueOf(pref.getInt("Id", 1)) + "," +
                    firstname.getText().toString() + "," +
                    lastname.getText().toString() + "," +
                    address.getText().toString() + "," +
                    resultDate + "," +
                    age.getText().toString() + "," +
                    phonenumber.getText().toString());
            SharedPreferences.Editor editor = pref.edit();
            editor.putInt("Id", pref.getInt("Id", 0) + 1);
            editor.apply();
        }
    }

    //Checking the results of Speech Recognition are matches with the given word.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AddActivity.REC_RESULT && resultCode == RESULT_OK) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (matches.get(0).toLowerCase().equals(getString(R.string.speech_add)))
                add_button.performClick();
            else if (matches.get(0).toLowerCase().equals(getString(R.string.speech_statistics)))
                statistics_button.performClick();
            else if (matches.get(0).toLowerCase().equals(getString(R.string.speech_list)))
                list_button.performClick();
            else if (matches.get(0).toLowerCase().equals(getString(R.string.speech_back)))
                back_button.performClick();
            else
                Toast.makeText(this, getString(R.string.speech_toast) + matches.get(0) + "\n\n" +
                        getString(R.string.speech_toast2) + "\n" +
                        getString(R.string.speech_toast_add) + "\n" +
                        getString(R.string.speech_toast_statistics) + "\n" +
                        getString(R.string.speech_toast_list) + "\n" +
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

    public void statistics(View view) {
        Intent intent = new Intent(this, StatisticsActivity.class);
        startActivity(intent); //Move to statistics activity
    }

    public void list(View view){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent); //Move to list activity
    }

    public void back(View view) {
        //It unchecks the checkbox that previously checked
        preferences = getSharedPreferences("checkbox", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("remember", "false");
        editor.apply();
        finish();
    }

    @Override
    public void onBackPressed(){
        //It unchecks the checkbox that previously checked
        preferences = getSharedPreferences("checkbox", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("remember", "false");
        editor.apply();
        finish();
    }
}