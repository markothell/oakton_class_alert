package com.funkness.classalert2;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

public class TestClass extends BroadcastReceiver{
	
	@Override
	public void onReceive(Context context, Intent intent) {
		//retrieve shared preferences for application. Contains:
		/*SharedPreferences saveStates = getSharedPreferences(CLASSINFO_PREFS, MODE_PRIVATE);
		Map<String, ?> storedCRNs = saveStates.getAll();
		
		for (Map.Entry<String, ?> entry: storedCRNs.entrySet()) {
			String temp = entry.getKey().toString();
			
		}*/
		//launch alert to test al
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
		        .setSmallIcon(R.drawable.oakton_logo2)
		        .setContentTitle("Class Alert")
		        .setContentText("One of your classes is ready.");
		// Creates an explicit intent for an Activity in your app
		/*Intent resultIntent = new Intent(context, Home.class);

		// The stack builder object will contain an artificial back stack for the started Activity.
		// This ensures that navigating backward from the Activity leads out of your application to the Home screen.
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(Home.this);
		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(Home.class);
		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent =
		        stackBuilder.getPendingIntent(
		            0,
		            PendingIntent.FLAG_UPDATE_CURRENT
		        );
		mBuilder.setContentIntent(resultPendingIntent);*/
		NotificationManager mNotificationManager = 
				(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		// mId allows you to update the notification later on.
		mNotificationManager.notify(R.string.app_name, mBuilder.build());
	}
	
	
}
