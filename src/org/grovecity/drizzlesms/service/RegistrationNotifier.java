package org.grovecity.drizzlesms.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import org.grovecity.drizzlesms.ConversationListActivity;
import org.grovecity.drizzlesms.R;

public class RegistrationNotifier extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
    builder.setSmallIcon(R.drawable.notify_drizzlesms);
    builder.setContentTitle(intent.getStringExtra(RegistrationService.NOTIFICATION_TITLE));
    builder.setContentText(intent.getStringExtra(RegistrationService.NOTIFICATION_TEXT));
    builder.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, ConversationListActivity.class), 0));
    builder.setWhen(System.currentTimeMillis());
    builder.setDefaults(Notification.DEFAULT_VIBRATE);
    builder.setAutoCancel(true);

    Notification notification = builder.build();
    ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(31337, notification);
  }
}
