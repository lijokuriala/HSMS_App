package com.ahdproject.homesecuritymonitoring;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "HSMS_MainActivity";
    private static final String CHANNEL_ID = "com.ahdproject.homesecuritymonitoring.ANDROID";

    // Default string for status displayed on screen
    public static String currentStatus = "Current status not available! Try later!!";
    public static String sensor, sensor_state, sensor_time,state_formatted;
    // Global text view field for status text
    public static TextView showStateText;


    public static boolean isAppForground(Context context) {

        ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> l = mActivityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo info : l) {
            if (info.uid == context.getApplicationInfo().uid && info.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return true;
            }
        }
        return false;
    }

    // Method to access the Firebase Database
    public void accessFirebaseDB() {
        FirebaseDatabase database = FirebaseDatabase.getInstance(getString(R.string.getInstanceUrl));
        DatabaseReference myRef = database.getReference(getString(R.string.getReferencePath));

        /*
        Below is the format of the HashMap in table_SensorStateData
        -MNxpY8Bnb8SE57Fcqtt                    // Key of the record
        Alert_Notify: "N"                       // Flag to determine if notification should be created
        Sensor: "Test Garage Door"              // Sensor name
        State: "Open"                           // Sensor state
        Time: "2020-Dec-07 09:33:36 AM CST"     // Time the sensor state occurred
        */


        // Read from the database
        // Get last set of data
        Query lastEntry = myRef.limitToLast(1);
        lastEntry.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot snapshot_record : snapshot.getChildren()) {
                    // Get values
                    sensor = snapshot_record.child("Sensor").getValue(String.class);
                    sensor_time = snapshot_record.child("Time").getValue(String.class);
                    sensor_state = snapshot_record.child("State").getValue(String.class);

                    // Show android notification if ALERT
                    if(sensor_state.equalsIgnoreCase("ALERT")) {
                        state_formatted = "LEFT OPENED since ";

                        // Generate notification if app not open and alert_notify flag is Y
                        if(!isAppForground(MainActivity.this)
                                && snapshot_record.child("Alert_Notify").getValue(String.class).equals("Y"))
                            addNotification();
                    }
                    if(sensor_state.toUpperCase().startsWith("OPEN"))
                        state_formatted = "opened at ";
                    else if(sensor_state.toUpperCase().startsWith("CLOSE"))
                        state_formatted = "closed at ";
                    currentStatus = sensor + " was " + state_formatted + sensor_time;
                }
                Log.d(TAG, "******Current status is********* "+ currentStatus);
                // Update text displayed
                showStateText.setText(currentStatus);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create the android notification channel
        createNotificationChannel();

        // Call the Firebase database access method
        accessFirebaseDB();
        // Locate and update the status text
        showStateText = findViewById(R.id.textView_status);
        showStateText.setText(currentStatus);


        findViewById(R.id.status_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast myToast = Toast.makeText(MainActivity.this, currentStatus, Toast.LENGTH_SHORT);
                myToast.show();
                accessFirebaseDB();
                showStateText.setText(currentStatus);
            }
        });
    }


    // Creates and displays a notification
    private void addNotification() {
        // Builds your notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("HSMS Alert")
                .setAutoCancel(true)
                .setContentText(sensor + " " + state_formatted + sensor_time);

        // Creates the intent needed to show the notification
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(contentIntent);

        // Add as notification
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(6372, notificationBuilder.build());
    }

    // Create the NotificationChannel
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            //Set notification light color to red
            channel.setLightColor(Color.RED);
            // Set notification visibility to lock screen
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            // Register the channel with the system
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

}