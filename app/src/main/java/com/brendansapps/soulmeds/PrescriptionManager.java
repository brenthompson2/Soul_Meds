package com.brendansapps.soulmeds;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.Context.ALARM_SERVICE;
import static android.content.Context.MODE_PRIVATE;

/**
 *  Created by bt on 03/27/18.
 *
 *  Manages the symptoms and times associated with the user's prescription data
 *
 *  Utilized by AlarmsFragment_Symptoms & AlarmsFragment_Times
 *  Capable of managing dynamic lists of Symptoms & Times but only using 3 of each
 */

public class PrescriptionManager {

    /** =================================================
     * Private Properties
     * ===================================================== */
    private static final String TAG = "PrescriptionManager";
    private static DataManager symptomDataManager;
    private static SharedPreferences prescriptionSP;
    private static SharedPreferences.Editor prescriptionSPEditor;
    private Context mContext;
    private static AlarmManager mAlarmManager;

    // Symptom Data
    private static ArrayList<String> allSymptomsList;
    private static ArrayList<PrescriptionDataObject> userSymptomsList;

    // Alarm Times Data
    private static ArrayList<PrescriptionDataObject> userTimesList;

    // Firebase Database
    DatabaseReference mFirebasePrescriptionDataReference;


    /** =================================================
     * Constructor
     * ===================================================== */
    public PrescriptionManager(Context context){
        mContext = context;

        // Initialize Managers
        mAlarmManager = (AlarmManager) mContext.getSystemService(ALARM_SERVICE);
        symptomDataManager = new DataManager();
        allSymptomsList = symptomDataManager.getSymptomsList();
        prescriptionSP = mContext.getSharedPreferences("Prescriptions", MODE_PRIVATE);
        prescriptionSPEditor = prescriptionSP.edit();

        // Connect to Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        mFirebasePrescriptionDataReference = database.getReference("prescriptionData");

        // Initialize User's Prescriptions
        if (firstTimeUser()){
            Log.d(TAG, "First Time User");
            resetDeafults();
        }
        else {
            loadUserPrescriptions();
//            loadUserPrescriptions_Firebase();
        }

        printAlarms();
    }

    /** =================================================
     * Accessors
     * ===================================================== */
    // Returns the user's symptoms as a ArrayList of Strings
    public ArrayList<String> getUserSymptoms(){
        ArrayList<String> userSymptoms = new ArrayList<>();
        for (int i = 0; i < userSymptomsList.size(); i++){
            userSymptoms.add(userSymptomsList.get(i).name);
        }

        return userSymptoms;
    }

    // Returns the user's times as a ArrayList of Strings
    public ArrayList<String> getUserTimes(){
        ArrayList<String> userTimes = new ArrayList<>();
        for (int i = 0; i < userTimesList.size(); i++){
            userTimes.add(userTimesList.get(i).name);
        }

        return userTimes;
    }

    // Returns all possible symptoms
    public ArrayList<String> getAllSymptoms(){
        return allSymptomsList;
    }

    public String getSymptom(int index){
        return symptomDataManager.getSymptomAtIndex(index);
    }

    // Returns the number of Verses for that symptom
    public int getNumSymptoms(){
        return symptomDataManager.size();
    }

    // Returns the number of Verses for that symptom
    public int getNumVerses(String symptom){
        return symptomDataManager.getNumVerses(symptom);
    }

    // Returns the Verse for that symptom at that index
    public String getVerse(String symptom, int index){
        return symptomDataManager.getVerse(symptom, index);
    }

    // Returns the Reference for the Verse for that symptom at that index
    public String getReference(String symptom, int index){
        return symptomDataManager.getVerseReference(symptom, index);
    }

    public void printTimes(){
        for (int i = 0; i < userTimesList.size(); i++){
            Log.d(TAG, i + ": " + userTimesList.get(i).name + ", " + userTimesList.get(i).isActive + ", " + userTimesList.get(i).alarmID);
        }
    }

    /** =================================================
     * Prescription Data Manipulators
     * ===================================================== */

    public void editSymptom(int index, String newName){
//        Log.d(TAG, "Changing Symptom " + userSymptomsList.get(index).name + " to " + newName);
        userSymptomsList.get(index).name = newName;

//        saveUserSymptoms();
//        saveUserSymptoms_Firebase();
    }

    public void editTime(int index, String newTime){
        // Delete Old Alarm
        deleteAlarm(userTimesList.get(index).alarmID);

        // Edit Time
//        Log.d(TAG, "Changing Time " + userTimesList.get(index).name + " to " + newTime);
        userTimesList.get(index).name = newTime;
        userTimesList.get(index).alarmID = getAlarmID(newTime);
//        saveUserTimes();
//        saveUserSymptoms_Firebase();
//         printTimes();

        // Add New Alarm
        setAlarm(getTimeInMillis(userTimesList.get(index).name), userTimesList.get(index).alarmID);
    }

//    public void addSymptom(String name){
//        Log.d(TAG, "Adding Symptom " + name);
//        PrescriptionDataObject newSymptom = new PrescriptionDataObject();
//        newSymptom.name = name;
//        newSymptom.isActive = true;
//        userSymptomsList.add(newSymptom);
//
//        saveUserSymptoms();
//    }

//    public void addTime(String time){
//        Log.d(TAG, "Adding Time " + time);
//        PrescriptionDataObject newTime = new PrescriptionDataObject();
//        newTime.name = time;
//        newTime.isActive = true;
//        newTime.alarmID = getAlarmID(newTime.name);
//        userTimesList.add(newTime);
//        saveUserTimes();
//        // printTimes();
//
//        // Add Alarm
//        setAlarm(getTimeInMillis(newTime.name), newTime.alarmID);
//    }

//    public void deleteSymptom(int index){
//        Log.d(TAG, "Deleting Symptom " + userSymptomsList.get(index).name);
//        userSymptomsList.remove(index);
//
//        saveUserSymptoms();
//    }

//    public void deleteTime(int index){
//        Log.d(TAG, "Deleting Time " + userTimesList.get(index).name);
//        deleteAlarm(userTimesList.get(index).alarmID);
//        userTimesList.remove(index);
//
//        saveUserTimes();
//        // printTimes();
//    }

    /** =================================================
     * Save and Load User's Prescriptions
     * ===================================================== */

    // Saves the user's Prescriptions to Firebase
    public void saveUserPrescriptions_Firebase(){
        FirebaseAuth mFirebaseAuth = FirebaseAuth.getInstance();
        String userID = mFirebaseAuth.getCurrentUser().getUid();
        mFirebasePrescriptionDataReference.child(userID).child("AlarmOne").setValue(getAsMilitaryTime(userTimesList.get(0).name));
        mFirebasePrescriptionDataReference.child(userID).child("AlarmTwo").setValue(getAsMilitaryTime(userTimesList.get(1).name));
        mFirebasePrescriptionDataReference.child(userID).child("AlarmThree").setValue(getAsMilitaryTime(userTimesList.get(2).name));
        mFirebasePrescriptionDataReference.child(userID).child("SymptomOne").setValue(userSymptomsList.get(0).name);
        mFirebasePrescriptionDataReference.child(userID).child("SymptomTwo").setValue(userSymptomsList.get(1).name);
        mFirebasePrescriptionDataReference.child(userID).child("SymptomThree").setValue(userSymptomsList.get(2).name);
    }

    // Saves the user's Prescriptions to the Shared Preference
    public void saveUserPrescriptions_Local(){
        saveUserSymptoms();
        saveUserTimes();
    }

    // Saves the user's Symptoms to the Shared Preference
    private void saveUserSymptoms(){
        // Compress symptomsList into one string
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < userSymptomsList.size(); i++){
            stringBuilder.append(userSymptomsList.get(i).name).append(",");
        }
        Log.d(TAG, "Saved Symptoms: " + stringBuilder.toString());

        // Save SharedPreference
        prescriptionSPEditor = prescriptionSP.edit();
        prescriptionSPEditor.putString("symptomsList", stringBuilder.toString());
        prescriptionSPEditor.apply();
    }

    // Saves the user's Alarm Times to the Shared Preference
    private void saveUserTimes(){
        // Compress userTimesList into one string
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < userTimesList.size(); i++){
            stringBuilder.append(userTimesList.get(i).name).append(",");
        }
//        Log.d(TAG, "Saved Times: " + stringBuilder.toString());

        // Save SharedPreference
        prescriptionSPEditor = prescriptionSP.edit();
        prescriptionSPEditor.putString("timesList", stringBuilder.toString());
        prescriptionSPEditor.apply();
    }

    private void loadUserPrescriptions_Firebase(){
        FirebaseAuth mFirebaseAuth = FirebaseAuth.getInstance();
        String userID = mFirebaseAuth.getCurrentUser().getUid();
        mFirebasePrescriptionDataReference.child(userID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String alarm1 = dataSnapshot.getValue(String.class);
                Log.d(TAG, alarm1);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "Failed to load data from Firebase: " + databaseError);
            }
        });
    }

    private void loadUserPrescriptions(){
        userSymptomsList = new ArrayList<>();
        userTimesList = new ArrayList<>();

        // Load SharedPreferences
        String compressedSymptomsString = prescriptionSP.getString("symptomsList", "");
//        String compressedSymptomsString = prescriptionSP.getString("symptomsList_Active", "");
        String compressedTimesString = prescriptionSP.getString("timesList", "");
//        String compressedTimesString = prescriptionSP.getString("timesList_Active", "");

        // Parse Loaded data into userSymptomsList
        ArrayList<String> listOfUserSymptoms = new ArrayList<>(Arrays.asList(compressedSymptomsString.split(",")));
        for (int i = 0; i < 3; i++){
            PrescriptionDataObject loadedSymptom = new PrescriptionDataObject();

            if (i < listOfUserSymptoms.size()){
                loadedSymptom.name = listOfUserSymptoms.get(i);
            }
            else {
                loadedSymptom.name = listOfUserSymptoms.get(0);
            }

            if (!(loadedSymptom.name.equals(""))){
                // loadedSymptom.isActive = listOfUserSymptoms_Active;
                userSymptomsList.add(loadedSymptom);
            }
        }

        // Parse Loaded data into userTimesList
        ArrayList<String> listOfUserTimes = new ArrayList<>(Arrays.asList(compressedTimesString.split(",")));
        for (int i = 0; i < 3; i++){
            PrescriptionDataObject loadedTime = new PrescriptionDataObject();

            if (i < listOfUserTimes.size()){
                loadedTime.name = listOfUserTimes.get(i);
            }
            else {
                loadedTime.name = "8:00 AM";
            }

            if (!(loadedTime.name.equals(""))){
                // loadedTime.isActive = listOfUserTimes_Active;
                loadedTime.alarmID = getAlarmID(loadedTime.name);
                userTimesList.add(loadedTime);
            }
        }

//        Log.d(TAG, "Loaded Times:");
//        printTimes();
    }

    // Takes in a string in AMPM format and returns it as military time
    private String getAsMilitaryTime(String time){
        Pattern timePattern = Pattern.compile("^([0-9]|0[0-9]|1[0-9]|2[0-3]):([0-5][0-9]) (AM|PM)$");
        Matcher match1;
        match1 = timePattern.matcher(time);
        StringBuilder stringBuilder = new StringBuilder();
        if (match1.find()){
            int hour1 = Integer.parseInt(match1.group(1));
            if (match1.group(3).equals("PM")){
                hour1 += 12;
            }
            stringBuilder.append(hour1).append(":").append(match1.group(2));
//            Log.d(TAG, "Time " + time + " as military = " + stringBuilder.toString());
        }

        return stringBuilder.toString();
    }

    // Takes in a string in Military time and returns it as AMPM format
    private String getFromMilitaryTime(String time){
        String timeInAMPM = "00:00 AM";
        return timeInAMPM;
    }

    /** =================================================
     * Functions for managing first time user
     * ===================================================== */
    private Boolean firstTimeUser(){
//        Boolean firstTimeVisiting = prescriptionSP.getBoolean("firstTimeVisiting", true);
//        Log.d(TAG, "firstTimeVisiting = " + firstTimeVisiting);
        return prescriptionSP.getBoolean("firstTimeVisiting", true);
    }

    // Initializes Symptom data for first time users
    public void resetDeafults(){
        userSymptomsList = new ArrayList<>();
        userTimesList = new ArrayList<>();

        // Initialize Three Symptoms
        PrescriptionDataObject newSymptom = new PrescriptionDataObject();
        newSymptom.name = allSymptomsList.get(0);
        newSymptom.isActive = false;
        userSymptomsList.add(newSymptom);

        newSymptom = new PrescriptionDataObject();
        newSymptom.name = allSymptomsList.get(1);
        newSymptom.isActive = false;
        userSymptomsList.add(newSymptom);

        newSymptom = new PrescriptionDataObject();
        newSymptom.name = allSymptomsList.get(2);
        newSymptom.isActive = false;
        userSymptomsList.add(newSymptom);

        // Initialize Three Times
        PrescriptionDataObject newTime = new PrescriptionDataObject();
        newTime.name = "8:00 AM";
        newTime.isActive = false;
        userTimesList.add(newTime);

        newTime = new PrescriptionDataObject();
        newTime.name = "9:00 AM";
        newTime.isActive = false;
        userTimesList.add(newTime);

        newTime = new PrescriptionDataObject();
        newTime.name = "10:00 AM";
        newTime.isActive = false;
        userTimesList.add(newTime);

        // Save First Prescription
        saveUserSymptoms();
        saveUserTimes();

        // Mark as having visited
        prescriptionSPEditor.putBoolean("firstTimeVisiting", false);
        prescriptionSPEditor.apply();
    }

    /** =================================================
     * Functions for managing Alarms
     * ===================================================== */
    private long getTimeInMillis(String currentTime){
        // Get Hour & Minute from string
        Matcher m;
        try {
            Pattern timePattern = Pattern.compile("^([0-9]|0[0-9]|1[0-9]|2[0-3]):([0-5][0-9]) (AM|PM)$");
            m = timePattern.matcher(currentTime);
            m.find();
            int currentHour = Integer.parseInt(m.group(1));
            int currentMinute = Integer.parseInt(m.group(2));

            if (m.group(3).equals("PM")){
                currentHour += 12;
            }

            // Prepare Time
            Calendar calendar = Calendar.getInstance();
            calendar.set(
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH),
                    currentHour,
                    currentMinute,
                    0
            );

            long alarmTIM = calendar.getTimeInMillis();
//            Log.d(TAG, "Got Time in Millis as " + alarmTIM);
            return calendar.getTimeInMillis();
        }
        catch (IllegalStateException e){
            Log.d(TAG, "Pattern Matcher Failed: " + e);
            return -1;
        }
    }

    // Returns an AlarmID created by appending the hour, minute, and amOrPm (0 | 1)
    private int getAlarmID(String currentTime){
        Matcher m;
        try {
            Pattern timePattern = Pattern.compile("^([0-9]|0[0-9]|1[0-9]|2[0-3]):([0-5][0-9]) (AM|PM)$");
            m = timePattern.matcher(currentTime);
            m.find();

            int amOrPm;
            if (m.group(3).equals("AM")){
                amOrPm = 0;
            }else {
                amOrPm = 1;
            }
            return Integer.parseInt(m.group(1) + m.group(2) + amOrPm);
        }
        catch (IllegalStateException e){
            Log.d(TAG, "Pattern Matcher Failed: " + e);
            return -1;
        }
    }

    // Sets an alarm for the selected timeInMillis
    private void setAlarm(long timeInMillis, int alarmID){
        Intent intent = new Intent(mContext, AlarmHandler.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, alarmID, intent, 0);
        mAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP, timeInMillis, AlarmManager.INTERVAL_DAY, pendingIntent);
        Log.d(TAG, "Set alarm for " + timeInMillis + " millis");
        printAlarms();
    }

    // Deletes the alarm specified
    private void deleteAlarm(int alarmID){
        Intent intent = new Intent(mContext, AlarmHandler.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, alarmID, intent, 0);
        mAlarmManager.cancel(pendingIntent);
    }

    private void printAlarms(){
//        Log.d(TAG, "Next Alarm: " + String.valueOf(mAlarmManager.getNextAlarmClock()));
    }
}
