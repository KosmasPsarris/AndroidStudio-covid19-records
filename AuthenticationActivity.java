package com.unipi.suat.covid_19;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class AuthenticationActivity extends AppCompatActivity {

    //Declare as global some object, variables,.. so we can use them everywhere :)
    private FirebaseAuth mAuth;
    FirebaseUser currentUser;
    LocationManager locationManager;
    Location gps_loc, final_loc;
    double longitude, latitude;
    EditText editText, editText2;
    CheckBox remember;
    SharedPreferences pref, preferences;
    String email, password;
    FirebaseDatabase database;
    DatabaseReference myRef;
    Button signUp, logIn;
    boolean choosemenu;
    boolean menucheck = false;

    //Using onCreate when the activity get starts for the first time to initialize and change objects and variables.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        CheckLang(); //get SharedPreferences and call CheckLang method before we set ContentView, so the selected language gets applied to our application
        setContentView(R.layout.activity_authentication);
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        editText = findViewById(R.id.editTextTextPersonName);
        editText2 = findViewById(R.id.editTextTextPassword);
        remember = findViewById(R.id.rememberMe);
        signUp = findViewById(R.id.signUp);
        logIn = findViewById(R.id.logIn);

        database = FirebaseDatabase.getInstance();
        preferences = getSharedPreferences("checkbox", MODE_PRIVATE);
        email = pref.getString("emailKey","");
        password = pref.getString("passKey","");
        editText.setText(email);      //Initial value is blank which replaces the text box with it.
        editText2.setText(password);

        //Check if user saved his credentials, if yes then bring them to AddActivity instead of LogIn Form
        String checkbox = preferences.getString("remember", "");
        if(checkbox.equals("true")){
            Intent intent = new Intent(this, AddActivity.class);
            intent.putExtra("emailKey", email);               //When the application starts, it checks to see if we last checked the checkbox.
            startActivity(intent);                                  //If yes, then forward the data in the 2nd window without showing the 1st window,
        }else if(checkbox.equals("false")){                         //otherwise enter we enter data to continue.
            Toast.makeText(this, getString(R.string.checkbox_toast), Toast.LENGTH_LONG).show();
        }

        //If the checkbox is checked, then saves it locally to the mobile, so the next time when we run the application, it will be already checked. And vice versa.
        remember.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (compoundButton.isChecked()) {
                    preferences = getSharedPreferences("checkbox", MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("remember", "true");
                    editor.apply();
                    Toast.makeText(AuthenticationActivity.this, R.string.checkbox_save, Toast.LENGTH_SHORT).show();
                } else if(!compoundButton.isChecked()) {
                    preferences= getSharedPreferences("checkbox", MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("remember", "false");
                    editor.apply();
                    Toast.makeText(AuthenticationActivity.this, R.string.checkbox_no_save, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void signup(View view) {
        if(editText.getText().toString().isEmpty() || editText2.getText().toString().isEmpty()){    //Checks if the text boxes are empty
            Toast.makeText(AuthenticationActivity.this, R.string.sign_up_login_toast, Toast.LENGTH_LONG).show();
            return;
        }else {
            mAuth.createUserWithEmailAndPassword(editText.getText().toString(), editText2.getText().toString())
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Sign up success, update UI with the signed-in user's information as it sings in user automatically
                                Toast.makeText(getApplicationContext(), R.string.sign_up_complete, Toast.LENGTH_LONG).show();
                            } else {
                                // If sign in fails, display a message to the user.
                                Toast.makeText(getApplicationContext(), task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }
    }

    public void login(View view) {
        if(editText.getText().toString().isEmpty() || editText2.getText().toString().isEmpty()){    //Checks if the text boxes are empty
            Toast.makeText(AuthenticationActivity.this, R.string.sign_up_login_toast, Toast.LENGTH_LONG).show();
            return;
        }else {
            mAuth.signInWithEmailAndPassword(editText.getText().toString(),editText2.getText().toString()).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {

                        // Sign in success, update UI with the signed-in user's information
                        currentUser = mAuth.getCurrentUser();
                        //Save email and password to preferences
                        SharedPreferences.Editor editor = pref.edit();
                        email = editText.getText().toString();
                        password = editText2.getText().toString();                      //When the user presses the button, it saves / modifies the items that he put in the
                        editor.putString("emailKey", email);                            //text boxes with the initial value.
                        editor.putString("passKey", password);
                        //GpsId preference starts with 1 and with each log in, we add 1 so the next ID is previous ID + 1.
                        editor.putInt("GpsId", pref.getInt("GpsId", 0) + 1);
                        //WE replace '.' with ',' to Email as we get error in firebase otherwise
                        myRef = database.getReference().child(currentUser.getEmail().replace(".",",")).child("Location Info").child(String.valueOf(pref.getInt("GpsId", 0)));
                        editor.apply();
                        Toast.makeText(getApplicationContext(), R.string.login_complete, Toast.LENGTH_LONG).show();

                        //By using this function we check/ask user for permissions and then we initialize LocationManager object.
                        if (ActivityCompat.checkSelfPermission(AuthenticationActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(AuthenticationActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(AuthenticationActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},234);
                            return;
                        }
                        try{
                            gps_loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);   //Getting last known location
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (gps_loc != null) { //if we get back a successful Gps update
                            final_loc = gps_loc;
                            //Get the latitude and the longitude
                            latitude = Double.parseDouble(String.format(Locale.ENGLISH,"%.6f",final_loc.getLatitude()));
                            longitude = Double.parseDouble(String.format(Locale.ENGLISH,"%.6f",final_loc.getLongitude()));

                            //Get the current timestamp and change the format.
                            DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            Date date = new Date(final_loc.getTime());
                            String timestamp = format.format(date);
                            //Put the above info into one string
                            String gpsInfo = latitude+" "+longitude + " | " + timestamp;
                            Toast.makeText(AuthenticationActivity.this, gpsInfo, Toast.LENGTH_LONG).show();
                            //Send the GPS location and the timestamp to the firebase database.
                            myRef.setValue(gpsInfo);
                        } else { //IF gps update failed
                            latitude = 0.0;
                            longitude = 0.0;
                            //Put the above info into one string
                            String gpsInfo = "-";
                            //Send the GPS location and the timestamp to the firebase database.
                            myRef.setValue(gpsInfo);
                        }
                        Intent intent = new Intent(getApplicationContext(), AddActivity.class);
                        intent.putExtra("emailKey", email);

                        startActivity(intent);
                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(getApplicationContext(),task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    //Checking the results of Speech Recognition are matches with the given word.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AddActivity.REC_RESULT && resultCode == RESULT_OK) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (matches.get(0).toLowerCase().equals(getString(R.string.speech_sign_up)))
                signUp.performClick();
            else if (matches.get(0).toLowerCase().equals(getString(R.string.speech_login)))
                logIn.performClick();
            else if (matches.get(0).toLowerCase().equals(getString(R.string.speech_exit)))
                finish();
            else
                Toast.makeText(this, getString(R.string.speech_toast) + matches.get(0) + "\n\n" +
                        getString(R.string.speech_toast2) + "\n" +
                        getString(R.string.speech_toast_sign_up) + "\n" +
                        getString(R.string.speech_toast_login) + "\n" +
                        getString(R.string.speech_toast_exit), Toast.LENGTH_LONG).show();
        }
    }

    //This method gets called on each Activity's OnCreate, so we check what language is selected
    //and change language strings.xml accordingly.
    //All strings derive from the strings.xml so, when we change locale language, all strings get updated
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
        inflater.inflate(R.menu.flag, menu);
        choosemenu = pref.getString("CurrentLang", "En").equals("En");
        //This method runs similarly to OnCreate, so it gets called automatically when we run the application.
        //It also gets called when we press the flag on top right of the application to change language.
        //If current language is English, change current language to Greek so change the application's language, and vise versa
        //Boolean menucheck is a variable flag that prevents the change of language when the user is brought to this activity
        //the first time.
        if(menucheck) {
            if (choosemenu) {
                menu.findItem(R.id.us).setVisible(false);
                menu.findItem(R.id.gr).setVisible(true);
                String language = "el";                                 //We are setting a string with a desired language and then we modify it to the locate default language(that's the device's language),
                Locale locale = new Locale(language);                   //and save that language to SharedPreferences.
                Locale.setDefault(locale);                              //if we change the language with a running Activity, we will need to restart it for the changes to take effect.
                Configuration configuration  = new Configuration();
                configuration.locale = locale;
                getBaseContext().getResources().updateConfiguration(configuration,getBaseContext().getResources().getDisplayMetrics());
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("CurrentLang", "Gr");
                editor.apply();
                recreate(); //recreate is mandatory for the language to change
            } else {
                menu.findItem(R.id.us).setVisible(true);
                menu.findItem(R.id.gr).setVisible(false);
                String language = "en";                                 //We are setting a string with a desired language and then we modify it to the locate default language(that's the device's language),
                Locale locale = new Locale(language);                   //and save that language to SharedPreferences.
                Locale.setDefault(locale);                              //if we change the language with a running Activity, we will need to restart it for the changes to take effect.
                Configuration configuration  = new Configuration();
                configuration.locale = locale;
                getBaseContext().getResources().updateConfiguration(configuration,getBaseContext().getResources().getDisplayMetrics());
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("CurrentLang", "En");
                editor.apply();
                recreate();
            }
        }else{//this else runs only once, when user is first brought to this activity
            menucheck = true; //becomes true so it won't occur again
            if (choosemenu) { // we still wish to update the visible flag on top right of the application
                menu.findItem(R.id.us).setVisible(true);
                menu.findItem(R.id.gr).setVisible(false);
            } else {
                menu.findItem(R.id.us).setVisible(false);
                menu.findItem(R.id.gr).setVisible(true);
            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    //This is the menu on top right of the application
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.Voice: //If user presses the microphone icon
                //Starts an activity that will prompt the user for speech and send it through a speech recognizer.
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, R.string.speech_language); //Change spoken language based on selected language
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, R.string.speech);
                startActivityForResult(intent, AddActivity.REC_RESULT);
                return true;
            case R.id.us: ; //If user presses the flag icon and the visible flag is the Greek one
                invalidateOptionsMenu();
                return true;
            case R.id.gr: //If user presses the flag icon and the visible flag is the American one
                invalidateOptionsMenu();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}