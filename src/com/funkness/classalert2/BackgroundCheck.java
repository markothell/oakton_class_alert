package com.funkness.classalert2;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.widget.Spinner;

public class BackgroundCheck extends BroadcastReceiver{
	public static final String CLASSINFO_PREFS = "classAlertPrefs";
	public static final String GLOBAL_PREFS = "classAlertGlobalPrefs";
	
	public static final String inputFormat = "HH:mm";

	private Date date;
	private Date dateCompareOne;
	private Date dateCompareTwo;

	private String compareStringOne = "00:00";
	private String compareStringTwo = "02:30";

	SimpleDateFormat inputParser = new SimpleDateFormat(inputFormat, Locale.US);
	Context gContx;
	Intent gTent;
	@Override
	public void onReceive(Context context, Intent intent) {
		gContx = context.getApplicationContext();
		gTent = intent;
		//check time, if after 12AM restart again at 2:30AM
		Calendar now = Calendar.getInstance();
		
	    int hour = now.get(Calendar.HOUR_OF_DAY);
	    int minute = now.get(Calendar.MINUTE);

	    date = parseDate(hour + ":" + minute);
	    dateCompareOne = parseDate(compareStringOne);
	    dateCompareTwo = parseDate(compareStringTwo);
	    String dateString = inputParser.format(date);
	    
	    //retrieve 'global' prefs
	    SharedPreferences saveStates = context.getSharedPreferences(GLOBAL_PREFS, 0);
  		String prefValue = saveStates.getString("global", null);
  		String[] prefArray = prefValue.split(",");
	    
	    //test if server-down hours at oakton 
	    if ( dateCompareOne.before(date) && dateCompareTwo.after(date)) {
			//Intent intent = new Intent(context, BackgroundCheck.class);
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, 
			                                      intent, PendingIntent.FLAG_UPDATE_CURRENT);
			AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			date = parseDate("2:30");
			now.setTime(date);
			int interval = Integer.valueOf(prefArray[1]) * 60 * 1000;
			alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, now.getTimeInMillis(), interval, pendingIntent);
	
	    } //else, check classes
	    else {
			//retrieve shared preferences for classes.
			saveStates = context.getSharedPreferences(CLASSINFO_PREFS, Context.MODE_PRIVATE);
			Map<String, ?> storedCRNs = saveStates.getAll();
			
			for (Map.Entry<String, ?> entry: storedCRNs.entrySet()) {
				String temp = entry.getValue().toString();
				prefArray = temp.split(",");
				new BackHTTP().execute(prefArray);
			}	
	    }
	}

	private Date parseDate(String date) {
	    try {return inputParser.parse(date);} 
	    catch (java.text.ParseException e) {return new Date(0);}
	}
	
	public class BackHTTP extends AsyncTask<String[], Boolean, Boolean> {
		
		protected Boolean doInBackground(String[]... args) {
			//pref value stored as: crn,classCap,name,id,campus,term,alertType,threshold
			
			String[] courseInfo = args[0];
			//retrieve term as formated for url 
			String dateString = getDateString(courseInfo[5]);
			
			String page = null;
			StringBuffer myString = new StringBuffer();
			
			try {
				String thisLine;
				URL u = new URL("https://ssb.oakton.edu/BPRD/bwckschd.p_disp_detail_sched?term_in=" + dateString +
						"&crn_in=" + courseInfo[0]);
				DataInputStream theHTML = new DataInputStream(u.openStream());
			
				while ((thisLine = theHTML.readLine()) != null) {
					myString.append(thisLine);
				}
				page = myString.toString(); //transfer stringBuffer to string
			}
			
		    catch (StringIndexOutOfBoundsException e) { e.printStackTrace(); return false;}
		    catch (IOException e) { return false; }
			if (page == null) { return false; }
			
			//match tags containing seating numbers
			Pattern p = Pattern.compile("<TD CLASS=\"dddefault\">(\\d+?)</TD>");
	    	Matcher m = p.matcher(page);
		    String[] holder = new String[3];
		   	for (int i = 0; m.find(); i++) {
		   		holder[i] = m.group(1); //loop through all matches and store in holder (capacity,actual,remaining)
		   	}
		   	int student = Integer.valueOf(holder[1]);
		   	int cap = Integer.valueOf(holder[0]);
		   	
		   	if (courseInfo[6].equals("firstOpen") && student < cap) {//notification on open seat
				return true;
			}
			else  if (courseInfo[6].equals("moreThan") && student > Integer.valueOf(courseInfo[7])) {
				return true;
			} 	
			else return false;
		}
		
		protected void onPostExecute(Boolean funk) {
			if(funk == true) alarm();
		}
	}
	
	public void alarm() {
		// Creates an explicit intent for an Activity in your app
		Intent resultIntent = new Intent(gContx, Home.class);

		PendingIntent resultPendingIntent = PendingIntent.getActivity(gContx, 0, resultIntent, 0);

    	NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(gContx)
		.setSmallIcon(R.drawable.oakton_logo2)
        .setContentTitle("Class Alert")
        .setContentText("Your class is ready!")
    	.setContentIntent(resultPendingIntent);
		
		NotificationManager mNotificationManager = 
				(NotificationManager) gContx.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(R.string.app_name, mBuilder.build());
		
		//once notification sent stop checking for updates
		PendingIntent alarmIntent = PendingIntent.getBroadcast(gContx, 0, 
                gTent, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager alarmManager = (AlarmManager) gContx.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(alarmIntent);
	}
	
	public String getDateString(String term) {
		Calendar now = Calendar.getInstance();
		int year = now.get(Calendar.YEAR);
		int month = now.get(Calendar.MONTH);
		String dateString = "";
		
		//set date string to add to url for data query
		if (term.substring(0,2).equals("Fa")) dateString = Integer.toString(year) + "30";  //fall
		else if (term.substring(0,2).equals("Sp")) {										//spring
			if (month > 6) dateString = Integer.toString(year + 1) + "10";
			else if (month < 6) dateString = Integer.toString(year) + "10";
		}
		else if (term.substring(0,2).equals("Su")) dateString = Integer.toString(year) + "20";//summer
		else dateString = "dateString error!!!!";
		//errorAlert("10", dateString);
		return dateString;
	}
	
}


