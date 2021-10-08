package com.unipi.suat.covid_19;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Locale;

public class UpdateActivity extends AppCompatActivity {

    //Declare as global some object, variables,.. so we can use them everywhere :)
    TextView tv_firstname, tv_lastname, tv_address, tv_date, tv_age, tv_phonenumber;
    EditText firstname, lastname, address, date, age, phonenumber;
    Button update_button, back_button;
    FirebaseDatabase database;
    DatabaseReference myRef;
    SharedPreferences pref;
    private FirebaseAuth mAuth;
    FirebaseUser currentUser;
    String sepId;


    //Using onCreate when the activity get starts for the first time to initialize and change objects and variables.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        CheckLang();
        setContentView(R.layout.activity_update);
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        database = FirebaseDatabase.getInstance();
        firstname = findViewById(R.id.firstname);
        lastname = findViewById(R.id.lastname);
        address = findViewById(R.id.address);
        date = findViewById(R.id.date);
        age = findViewById(R.id.age);
        phonenumber = findViewById(R.id.phonenumber);
        update_button = findViewById(R.id.update_button);
        back_button = findViewById(R.id.back_button);


        tv_firstname = findViewById(R.id.tv_firstname);
        tv_lastname = findViewById(R.id.tv_lastname);
        tv_address = findViewById(R.id.tv_address);
        tv_date = findViewById(R.id.tv_date);
        tv_age = findViewById(R.id.tv_age);
        tv_phonenumber = findViewById(R.id.tv_phonenumber);

        //Get data of selected patient
        String currentString = pref.getString("Value", "QQ");
        String[] separated = currentString.split(",");

        sepId = separated[0];
        firstname.setText(separated[1]);
        lastname.setText(separated[2]);
        address.setText(separated[3]);
        date.setText(separated[4]);
        age.setText(separated[5]);
        phonenumber.setText(separated[6]);

        //Set actionbar title after getAndSetIntentData method.
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle(firstname.getText() + " " + lastname.getText());
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

    public void updateUser(View view) { //Make necessary input validations
        if(firstname.getText().toString().isEmpty() ||
                lastname.getText().toString().isEmpty() ||
                address.getText().toString().isEmpty() ||
                date.getText().toString().isEmpty() ||
                age.getText().toString().isEmpty() ||
                phonenumber.getText().toString().isEmpty()) { //Like checking if the text boxes are empty
            Toast.makeText(this, R.string.add_empty, Toast.LENGTH_LONG).show();
            return;
        }else {
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
            //Replace old entry with updated entry
            myRef = database.getReference().child(currentUser.getEmail().replace(".",",")).child("Patients").child(sepId);
            myRef.setValue(sepId + "," +
                    firstname.getText().toString() + "," +
                    lastname.getText().toString() + "," +
                    address.getText().toString() + "," +
                    resultDate + "," +
                    age.getText().toString() + "," +
                    phonenumber.getText().toString());
            finish();
        }
    }

    public void back(View view) {
        finish();
    }

    @Override
    public void onBackPressed(){
        finish();
    }

}