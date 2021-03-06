package com.brendansapps.soulmeds;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import static android.content.Context.ALARM_SERVICE;
import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by bt on 2/12/18.
 *
 * Handles what happens when an alarm goes off
 * Notification Documentation: https://developer.android.com/guide/topics/ui/notifiers/notifications
 * NotificationCompat.Builder Documentation: https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder
 */

public class AlarmHandler extends BroadcastReceiver {

    private static final String TAG = "AlarmHandler";
    private static final String notificationTitle = "Soul Meds";
    private static final String notificationMessage = "An alarm has gone off";
    private static final int NOTIFICATION_ID = 6578;

    private Context mContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Receiving an Alarm");
        mContext = context;

        showNotification();
    }

    private void showNotification(){
        // Setup Intent for when Notification clicked
        Intent intent = new Intent(mContext, MedsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // See https://developer.android.com/training/notify-user/navigation for better navigation
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, 0);

        // Setup Ringtone & Vibrate
        Uri alarmSound = Settings.System.DEFAULT_RINGTONE_URI;
        long[] vibratePattern = { 0, 100, 200, 300 };

        // Setup Notification
        String channelID = mContext.getResources().getString(R.string.channel_id_alarms);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mContext, channelID)
                .setContentText(notificationMessage)
                .setContentTitle(notificationTitle)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pendingIntent)
                .setSound(alarmSound, AudioManager.STREAM_ALARM)
                .setVibrate(vibratePattern)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true);

        // Send Notification
        NotificationManager manager = (NotificationManager) mContext.getSystemService(NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, mBuilder.build());
    }
}
