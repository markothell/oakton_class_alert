package com.funkness.classalert2;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URL;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;


public class Home extends Activity {
	
	//private static final String TAG = "ClassAlert Activity Home";
	private LinearLayout activityHomeLinearLayout;
	public static final String CLASSINFO_PREFS = "classAlertPrefs";
	public static final String GLOBAL_PREFS = "classAlertGlobalPrefs";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		
		//retrieve shared preferences for application. Contains:
		SharedPreferences saveStates = getSharedPreferences(CLASSINFO_PREFS, MODE_PRIVATE);
		Map<String, ?> storedCRNs = saveStates.getAll();
		
		//"get references to gui component"
		ImageButton searchButton = (ImageButton) findViewById(R.id.searchButtonId);
		EditText searchInput = (EditText) findViewById(R.id.searchEditTextId);
		activityHomeLinearLayout = (LinearLayout) findViewById(R.id.activityHomeLinearLayout);	
		
		//create listener for textEdit entry button
		searchButton.setOnClickListener(searchButtonListener);
		searchInput.setOnEditorActionListener(goSubmitSearch);
		
		//if there are stored alerts load data and update seat count
		if (storedCRNs.size() != 0) {
			for (Map.Entry<String, ?> entry: storedCRNs.entrySet()) {
				String temp = entry.getKey().toString();
				displayOldAlert(temp);
			}
		}
		
		//check global prefs. If empty set defaults to create pref file
		SharedPreferences globalState = getSharedPreferences(GLOBAL_PREFS, MODE_PRIVATE);
		if (globalState.getString("global", null) == null) {
			SharedPreferences.Editor prefEditor = globalState.edit();
			prefEditor.putString("global", "interval,60,on");
			prefEditor.commit();
		}
		
		//setup custom layout for term select drop-down
		Spinner spinner = (Spinner) findViewById(R.id.spinnerId);
		 ArrayList<String> term = new ArrayList<String>();
		    term.add("Fall");
		    term.add("Spring");
		    term.add("Summer");
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(Home.this, R.layout.spinner_style, term);
		adapter.setDropDownViewResource(R.layout.spinner_style);		
		spinner.setAdapter(adapter);
		spinner.setPrompt("Select Term");
		
		//setup dummy alert for when server is down
		//String[] funk = getResources().getStringArray(R.array.crn20469);
		//displayAlert(funk);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_home, menu);
		return true;
	}
	
	public void onPause() {
		super.onPause();
		
		//create intents and alarmManager (pointed to class:BackgroundCheck)
		Context context = Home.this;
		Intent intent = new Intent(context, BackgroundCheck.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, 
		                                      intent, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
		
		//check preference for alert type
		String funk = extractGlobal("update");
		if (funk.equals("manual")) {  //if set to manual, cancel background updates (just in case)
			alarmManager.cancel(pendingIntent);
		} else if (funk.equals("interval")) { //else update alert settings and initiate
			//retrieve interval length
			funk = extractGlobal("interval");
			//create integer with value in milliseconds
			int interval = (Integer.valueOf(funk) * 60 * 1000);
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.MINUTE, Integer.valueOf(funk));
			alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), interval, pendingIntent);
		} 
	}
	
	private void displayAlert(String[] courseInfo){
		//get reference to the LayoutInflator service
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				
		//inflate a bar of the linearView in activity_home for each of the courses
		LinearLayout newLinLay = (LinearLayout) inflater.inflate(R.layout.class_alert_bar, null);
		newLinLay.setId(Integer.valueOf(courseInfo[4]));
		
		FrameLayout statusFrame = (FrameLayout) newLinLay.findViewById(R.id.statusFrameLayout);
		//determine status and set background of FrameLayout (id=statusFrameLayout)
		if (Integer.valueOf(courseInfo[0]) < Integer.valueOf(courseInfo[1])) {
			statusFrame.setBackgroundResource(R.drawable.status_green);
		}
		else if (Integer.valueOf(courseInfo[0]) >= Integer.valueOf(courseInfo[1])) {
			statusFrame.setBackgroundResource(R.drawable.status_red);
		}
				
		//set values for alert bar
				
		//set current reg student number id="@+id/studentNumTextView"
		TextView studentNumTextView = (TextView) newLinLay.findViewById(R.id.studentNumTextView);
		studentNumTextView.setText(courseInfo[0]);
				
		//set class cap id="@+id/classCapTextView"
		TextView classCapTextView = (TextView) newLinLay.findViewById(R.id.classCapTextView);
		classCapTextView.setText(courseInfo[1]);
					
		String holderString; //container for alternate string if course name is too long
		//test length of course name and assign appropriate string to holder
		if (courseInfo[2].length() > 30) {
			holderString = courseInfo[2].substring(0, 30) + "...";
		}	
		else holderString = courseInfo[2];
					
		TextView courseNameTextView = (TextView) newLinLay.findViewById(R.id.courseNameTextView);
		courseNameTextView.setText(holderString);
			
		TextView courseIdTextView = (TextView) newLinLay.findViewById(R.id.courseIdTextView);
		courseIdTextView.setText(courseInfo[3]);
				
		TextView crnTextView = (TextView) newLinLay.findViewById(R.id.crnTextView);
		crnTextView.setText(courseInfo[4]);
				
		TextView campusTextView = (TextView) newLinLay.findViewById(R.id.campusTextView);
		campusTextView.setText(courseInfo[5]);	
		
		TextView termTextView = (TextView) newLinLay.findViewById(R.id.termTextView);
		termTextView.setText(courseInfo[6]);	
		
		activityHomeLinearLayout.addView(newLinLay);	
		
		//store current alert in user preferences
		//pref value stored as: crn,classCap,name,id,campus,alertType,threshold	
		String[] preferences = new String[8];
		preferences[0] = courseInfo[4];
		preferences[1] = courseInfo[1];
		preferences[2] = courseInfo[2];
		preferences[3] = courseInfo[3];
		preferences[4] = courseInfo[5];
		preferences[5] = courseInfo[6];
		preferences[6] = "firstOpen"; //default value for alert type		
		preferences[7] = "1"; //default value on seekerBar	
		savePref(preferences);
		
		
		//create event listener for each bar that calls slide down, slide up animation	
		newLinLay.setOnClickListener(alertBarListener);	
	}
	
	private void displayOldAlert(String crn) {
		//retrieve preference for given crn
		SharedPreferences settings = getSharedPreferences(CLASSINFO_PREFS, 0);
		
		String prefValue = settings.getString(crn, null);
		String[] courseInfo = prefValue.split(",");
		
		//get reference to the LayoutInflator service
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
						
		//inflate a bar of the linearView in activity_home for each of the courses
		LinearLayout newLinLay = (LinearLayout) inflater.inflate(R.layout.class_alert_bar, null);
		newLinLay.setId(Integer.valueOf(courseInfo[0]));						
		//set status frame and student number
		new UpdateHTTP().execute(crn);
						
		//***set values for alert bar		
		//set class cap id="@+id/classCapTextView"
		TextView classCapTextView = (TextView) newLinLay.findViewById(R.id.classCapTextView);
		classCapTextView.setText(courseInfo[1]);
							
		String holderString; //container for alternate string if course name is too long
		//test length of course name and assign appropriate string to holder
		if (courseInfo[2].length() > 30) {
			holderString = courseInfo[2].substring(0, 30) + "...";
		}	
		else holderString = courseInfo[2];
							
		TextView courseNameTextView = (TextView) newLinLay.findViewById(R.id.courseNameTextView);
		courseNameTextView.setText(holderString);
					
		TextView courseIdTextView = (TextView) newLinLay.findViewById(R.id.courseIdTextView);
		courseIdTextView.setText(courseInfo[3]);
						
		TextView crnTextView = (TextView) newLinLay.findViewById(R.id.crnTextView);
		crnTextView.setText(courseInfo[0]);
						
		TextView campusTextView = (TextView) newLinLay.findViewById(R.id.campusTextView);
		campusTextView.setText(courseInfo[4]);	
		
		TextView termTextView = (TextView) newLinLay.findViewById(R.id.termTextView);
		termTextView.setText(courseInfo[5]);	
				
		
		activityHomeLinearLayout.addView(newLinLay);					
		
	}
	
	private void updateAlert(String[] info) {   //***called in postExecute() of UpdateHTTP()***
		
		//reference the id set in dispalyAlert()
		LinearLayout linLay = (LinearLayout) findViewById(Integer.valueOf(info[2]));
		FrameLayout statusFrame = (FrameLayout) linLay.findViewById(R.id.statusFrameLayout);
		//reference student number textView and update
		TextView studentNumTextView = (TextView) linLay.findViewById(R.id.studentNumTextView);
		studentNumTextView.setText(info[0]);
		
		String cray = extractPref(info[2],"alertType");
		if (cray.equals("firstOpen")) {
			//determine status and set background of FrameLayout (id=statusFrameLayout)
			if (Integer.valueOf(info[0]) < Integer.valueOf(info[1])) {
				statusFrame.setBackgroundResource(R.drawable.status_green);
			}
			else if (Integer.valueOf(info[0]) >= Integer.valueOf(info[1])) {
				statusFrame.setBackgroundResource(R.drawable.status_red);
			}
		} else if (cray.equals("moreThan")) {
			cray = extractPref(info[2], "threshold");
			//determine status and set background of FrameLayout (id=statusFrameLayout)
			if (Integer.valueOf(info[0]) >= Integer.valueOf(cray)) {
				statusFrame.setBackgroundResource(R.drawable.status_green);
			}
			else if (Integer.valueOf(info[0]) < Integer.valueOf(cray)) {
				statusFrame.setBackgroundResource(R.drawable.status_red);
			}
		}
		
		//create event listener for each bar that calls slide down, slide up animation	
		linLay.setOnClickListener(alertBarListener);	
	}
	
	private void verifyAndSearch() {
		EditText searchEditText = (EditText) findViewById(R.id.searchEditTextId);
		 
    	Pattern p = Pattern.compile("\\d{5}");
    	String searchString = searchEditText.getText().toString();
    	Matcher m = p.matcher(searchString);
    	if (searchString.length() == 5) {
    		if (m.matches()) {
    			new GetHTTP().execute(searchString);
    		} else {
        		errorAlert("1","");
        	}
    	}else if (searchString.length() < 5) {
    		errorAlert("1","");
    	}else if (searchString.length() > 5) {
    		p = Pattern.compile("\\d{5}(,\\d{5})+");
    		searchString = searchEditText.getText().toString();
    		m = p.matcher(searchString);

    		if (m.matches()) {
    			String[] crnArray = searchString.split(",");
    			for (int i = 0; i < crnArray.length; i++) {
    				new GetHTTP().execute(crnArray[i]);
    			}
    		}
    		else {
    			errorAlert("5","");
    		} 
    	}
	}
	
	
	//*****Event Listeners******//
	// called when a guess Button is touched
	private OnClickListener alertBarListener = new OnClickListener() 
	{
	    public void onClick(View v) 
	    {
	    	alertOptionDialog(v);  //display option dialog for selected alert
	    } // end method onClick
	}; // end answerButtonListener
	
	private OnEditorActionListener goSubmitSearch = new OnEditorActionListener()
	{
		public boolean onEditorAction(TextView v, int actionID, KeyEvent event) {
			if (event != null && event.getAction() != KeyEvent.ACTION_DOWN) {
				return false;
			} else if (event == null || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
				verifyAndSearch();
			}
			v.setText("");
			//hide soft keyboard after entry accepted
			InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
			return true;
		}
	};
	
	// called when the search Button is touched
	private OnClickListener searchButtonListener = new OnClickListener() 
	{
	    public void onClick(View v) {	
	    	verifyAndSearch();
	    	//EditText editText = (EditText) v.findViewById(R.id.searchEditTextId);
	    	EditText et = (EditText) findViewById(R.id.searchEditTextId);
	    	et.setText("");
	    	InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
	    	}
	     
	}; // end searchButtonListener
	//----End Listeners----//
	
	
	
	//******* inner classes ********//
	private class GetHTTP extends AsyncTask<String, String, String[]> {
		
		ProgressDialog dialog;
		
		@Override
		protected void onPreExecute() {
			dialog = new ProgressDialog(Home.this);
			dialog.setMessage("Loading class...");
			dialog.setCancelable(false);
			dialog.show();
		}
		
		@Override
		protected String[] doInBackground(String... arg) {
			String[] courseInfo = new String[7];
			String page = null;
			StringBuffer myString = new StringBuffer();
			
			String dateString = makeDateString();
			String year = dateString.substring(2, 4);
			String term = dateString.substring(4, 6);
			
			if (term.equals("30")) courseInfo[6] = "Fall'" + year;
			else if (term.equals("20")) courseInfo[6] = "Summer'" + year;
			else if (term.equals("10")) courseInfo[6] = "Spring'" + year;
			else courseInfo[6] = term + ":" + year + ":" + dateString;
				
			
			try {
				String thisLine;
				URL u = new URL("https://ssb.oakton.edu/BPRD/bwckschd.p_disp_detail_sched?term_in=" +
											dateString + "&crn_in=" + arg[0]);
				DataInputStream theHTML = new DataInputStream(u.openStream());
			
				while ((thisLine = theHTML.readLine()) != null) {
					myString.append(thisLine);
				}
				page = myString.toString(); //transfer stringBuffer to string
			}
			
		    catch (StringIndexOutOfBoundsException e)
		    { e.printStackTrace(); }
		    catch (IOException e)
		    { 	//currently used when server was down. must implement more robust tests
		    	courseInfo[0] = "error";
				courseInfo[1] = "4";
				courseInfo[2] = arg[0];
				return courseInfo;
		    }
		
			if (page == null) {
				courseInfo[0] = "error";
				courseInfo[1] = "3";
				courseInfo[2] = arg[0];
				return courseInfo;
			}
			//search for "no class found" response
			Pattern p = Pattern.compile(">No detailed class");
			Matcher m = p.matcher(page);
			if (m.find()) {
				courseInfo[0] = "error";
				courseInfo[1] = "2";
				courseInfo[2] = arg[0];
				return courseInfo;
			}
			
			//Retrieve relevant class information
			//match tag containing title, crn, course and section number
			p = Pattern.compile("<TH CLASS=\"ddlabel\" scope=\"row\" >(.+?)<BR>");
		    m = p.matcher(page);
			if (m.find()) {	
				String found = m.group(1);
				String[] separated = found.split(" - ");
				courseInfo[2] = separated[0];
				courseInfo[4] = separated[1];
				courseInfo[3] = separated[2] + "-" + separated[3];
			} 
				
			//match tags containing seating numbers
			p = Pattern.compile("<TD CLASS=\"dddefault\">(\\d+?)</TD>");
	    	m = p.matcher(page);
		    String[] holder = new String[3];
		   	for (int i = 0; m.find(); i++) {
		   		holder[i] = m.group(1); //loop through all matches and store in holder
		   	}
		   	courseInfo[1] = holder[0];
	   		courseInfo[0] = holder[1];
	   		
	   		//match tags containing campus location
			p = Pattern.compile("<BR><BR>(.{5,15}) Campus<BR>");
	    	m = p.matcher(page);
	    	if(m.find()) {
	    		courseInfo[5] = m.group(1);
	    	}
	    	
			return courseInfo;
			
		}
		protected void onPostExecute(String[] datum) {
			//dismiss progressDialog
			dialog.dismiss();
			//check for error
			if (datum[0].equals("error")) {
				errorAlert(datum[1], datum[2]);
			} else{ 	//otherwise display class data
				displayAlert(datum);
			}
		}
	}
	
	
	private class UpdateHTTP extends AsyncTask<String, String, String[]> {
		ProgressDialog dialog;
		
		@Override
		protected void onPreExecute() {
			dialog = new ProgressDialog(Home.this);
			dialog.setMessage("Updating classes...");
			dialog.setCancelable(false);
			dialog.show();
		}
		
		@Override
		protected String[] doInBackground(String... arg) {
			String[] courseInfo = new String[3];
			String page = null;
			StringBuffer myString = new StringBuffer();
			
			//retrieve term value for course
			String term = extractPref(arg[0],"term");
			//format term for url query
			String dateString = getDateString(term);
			
			
			try {
				String thisLine;
				URL u = new URL("https://ssb.oakton.edu/BPRD/bwckschd.p_disp_detail_sched?term_in=" +
											dateString + "&crn_in=" + arg[0]);
				DataInputStream theHTML = new DataInputStream(u.openStream());
			
				while ((thisLine = theHTML.readLine()) != null) {
					myString.append(thisLine);
				}
				page = myString.toString(); //transfer stringBuffer to string
			}
		    catch (StringIndexOutOfBoundsException e)
		    { e.printStackTrace(); }
		    catch (IOException e)
		    { 	//currently used when server was down. must implement more robust tests
		    	//e.printStackTrace();
		    	courseInfo[0] = "error";
				courseInfo[1] = "4";
				courseInfo[2] = arg[0];
				return courseInfo;
		    }
		
			if (page == null) {
				courseInfo[0] = "error";
				courseInfo[1] = "3";
				courseInfo[2] = arg[0];
				return courseInfo;
			}
			//search for "no class found" response
			Pattern p = Pattern.compile(">No detailed class");
			Matcher m = p.matcher(page);
			if (m.find()) {
				courseInfo[0] = "error";
				courseInfo[1] = "2";
				courseInfo[2] = arg[0];
				return courseInfo;
			}
				
			//match tags containing seating numbers
			p = Pattern.compile("<TD CLASS=\"dddefault\">(\\d+?)</TD>");
	    	m = p.matcher(page);
		    String holder[] = new String[3];
		   	for (int i = 0; m.find(); i++) {
		   		holder[i] = m.group(1); //loop through all matches and store in holder
		   	}
		   	courseInfo[1] = holder[0];
	   		courseInfo[0] = holder[1];
	   		courseInfo[2] = arg[0];
 	    	
			return courseInfo;
			
		}
		protected void onPostExecute(String[] datum) {
			//dismiss progressDialog
			dialog.dismiss();
			//check for error
			if (datum[0].equals("error")) {
				errorAlert(datum[1], datum[2]);
			} else{ 	//otherwise display class data
				updateAlert(datum); 
			}
		}

	}

	//--------End Inner Classes--------//
	
	
	public String makeDateString() {
		Calendar now = Calendar.getInstance();
		int year = now.get(Calendar.YEAR);
		int month = now.get(Calendar.MONTH);
		String dateString = "";
		
		Spinner spinner = (Spinner) findViewById(R.id.spinnerId);
		int pos = spinner.getSelectedItemPosition();
		
		//set date string to add to url for data query
		if (pos == 0) dateString = Integer.toString(year) + "30";  //fall
		else if (pos == 1) {										//spring
			if (month > 6) dateString = Integer.toString(year + 1) + "10";
			else if (month < 6) dateString = Integer.toString(year) + "10";
		}
		else if (pos == 2) dateString = Integer.toString(year) + "20";//summer
		else dateString = "dateString error!!!!";
		//errorAlert("10", dateString);
		return dateString;
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
	
	public void update(MenuItem item) {
		//retrieve shared preferences for application. Contains:
		SharedPreferences saveStates = getSharedPreferences(CLASSINFO_PREFS, MODE_PRIVATE);
		Map<String, ?> storedCRNs = saveStates.getAll();
		
		
		if (storedCRNs.size() != 0) {
			//errorAlert("10", Integer.toString(storedCRNs.size()));
			for (Map.Entry<String, ?> entry: storedCRNs.entrySet()) {
				String key = entry.getKey().toString();
				new UpdateHTTP().execute(key); 
			}
		}
	}
	
	
	
	public void updateDialog(MenuItem item) {
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    
	    // Get the layout inflater
	    LayoutInflater inflater = this.getLayoutInflater();
	    final LinearLayout newOptionBox = (LinearLayout) inflater.inflate(R.layout.update_option_box, null);
	    //reference option dialog elements
		final RadioButton but1 = (RadioButton) newOptionBox.findViewById(R.id.updateRadio1);
		final RadioButton but2 = (RadioButton) newOptionBox.findViewById(R.id.updateRadio2);
		final TextView seekBarValue = (TextView) newOptionBox.findViewById(R.id.updateSBTVId);
		final SeekBar sb = (SeekBar) newOptionBox.findViewById(R.id.updateSBId);
	    
	    builder.setTitle("Update Class Status...");
	    builder.setView(newOptionBox)
	    	.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
	    		public void onClick(DialogInterface dialog, int id) {
	    			if (but1.isChecked()) {
	    				saveGlobal("update","manual");
	    			}
	    			else  if (but2.isChecked()) {
	    				saveGlobal("update","interval");
	    				int prog = sb.getProgress();
	    				if (prog <= 15) saveGlobal("interval", "15");	
	    				else saveGlobal("interval", Integer.toString(prog));
	    			} else {
	    				errorAlert("10","UpdateDialog:no check registered");
	    			}
	    		}
	    	  })
	          .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
	               public void onClick(DialogInterface dialog, int id) {
	            	   dialog.dismiss();
	               }
	           });   
	    //build and show dialog
	    AlertDialog alert = builder.create();
	    alert.show();
	    
	    //set seekerBar max & increment
	    sb.setMax(1440); //24 hours is max interval
	    sb.incrementProgressBy(1);	
	    
	    //restore settings from preferences
	    String update = extractGlobal("update");
	    int interval = Integer.valueOf(extractGlobal("interval"));
		if (update.equals("manual")) {
			but1.setChecked(true); 
			but2.setChecked(false);
			sb.setProgress(interval);
		    seekBarValue.setText(hrMin(interval));
		}else if (update.equals("interval")){
			but1.setChecked(false); 
			but2.setChecked(true);
			sb.setProgress(interval);
		    seekBarValue.setText(hrMin(interval));
		}
	    
	    //set change listeners (updates textView to match seekerBar move)
	    sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar seekBar, int interval, boolean fromUser) {
				String intS;
				if (interval <= 15) { intS = "15mins"; }
				else if (interval >= 15 && interval < 60) { intS = Integer.toString(Math.round(interval/5)*5) + "mins"; }
				else if (interval > 60 && interval < 120) { intS = "1hr"; }
				else { intS = Integer.toString(interval/60) + "hrs"; }
				seekBarValue.setText(intS);
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}
	    });	    
	}
	
	public String hrMin(int interval) {
		String intS;
		if (interval <= 15) { intS = "15mins"; }
		else if (interval >= 15 && interval < 60) { intS = Integer.toString((Math.round(interval/5))*5) + "mins"; }
		else if (interval > 60) { intS = Integer.toString(interval/60) + "hrs"; }
		else {intS = "100000";}
		return intS;
	}
	
	
	public void notifyDialog(MenuItem item) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    
	    // Get the layout inflater
	    LayoutInflater inflater = this.getLayoutInflater();
	    final LinearLayout newOptionBox = (LinearLayout) inflater.inflate(R.layout.notify_option_box, null);
	    //reference option dialog elements
		final CheckBox but1 = (CheckBox) newOptionBox.findViewById(R.id.checkBox1);
	    
	    builder.setTitle("Notification");
	    builder.setView(newOptionBox)
	    	.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
	    		public void onClick(DialogInterface dialog, int id) {
	    			if (but1.isChecked()) {
	    				saveGlobal("notify","on");
	    			} else if (!but1.isChecked()) {
	    				saveGlobal("notify", "off");
	    			} else {
	    				errorAlert("10","UpdateDialog:no check registered");
	    			}
	    		}
	    	  });   
	    //build and show dialog
	    AlertDialog alert = builder.create();
	    alert.show();
	    
	    //restore settings from preferences
	    String updateType = extractGlobal("notify");
		if (updateType.equals("on")) {
			but1.setChecked(true); 
		}else if (updateType.equals("off")) {
			but1.setChecked(false); 
		}else {errorAlert("10", "updateDialogFromPref");}
	}
	
	
	public void alertOptionDialog(View v) {
		final View viewPass = v;
	    AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    
	    // Get the layout inflater
	    LayoutInflater inflater = this.getLayoutInflater();
	    final LinearLayout newOptionBox = (LinearLayout) inflater.inflate(R.layout.alert_option_box, null);
	    final SeekBar sb = (SeekBar) newOptionBox.findViewById(R.id.seekBarId);
	    builder.setTitle("Alert Me When...");
	    //View optionBox = (View) findViewById(R.layout.option_box);
	    
	    // Inflate and set the layout for the dialog
	    // Pass null as the parent view because its going in the dialog layout
	    builder.setView(newOptionBox)
	    	.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
	    		public void onClick(DialogInterface dialog, int id) {
	    			//reference option dialog elements
	    			RadioButton but1 = (RadioButton) newOptionBox.findViewById(R.id.optionRadio1);
	    			RadioButton but2 = (RadioButton) newOptionBox.findViewById(R.id.optionRadio2);
	    			//reference alert bar elements
	    			FrameLayout statusFrame = (FrameLayout) viewPass.findViewById(R.id.statusFrameLayout);
	    			TextView studentNumTextView = (TextView) viewPass.findViewById(R.id.studentNumTextView);
	    			int student = Integer.valueOf(studentNumTextView.getText().toString());
	    			TextView classCapTextView = (TextView) viewPass.findViewById(R.id.classCapTextView);
	    			int cap = Integer.valueOf(classCapTextView.getText().toString());
	    			
	    			if (but1.isChecked()) {
	    				savePref(viewPass,"firstOpen", "");
	    				 
	    				//update view based on changes to settings
	    				//determine status and set background of FrameLayout (id=statusFrameLayout)
	    				if (student < cap) {
	    					statusFrame.setBackgroundResource(R.drawable.status_green);
	    				}
	    				else if (student >= cap) {
	    					statusFrame.setBackgroundResource(R.drawable.status_red);
	    				}
	    			}
	    			
	    			else  if (but2.isChecked()) {
	    				String thresh = Integer.toString(sb.getProgress());
	 	    		  	savePref(viewPass,"moreThan", thresh);
	 	    		
	 	    		  	if (student >= Integer.valueOf(thresh)) {
	 	   				statusFrame.setBackgroundResource(R.drawable.status_green);
	 	    		  	} else if (student < Integer.valueOf(thresh)) {
	 	   				statusFrame.setBackgroundResource(R.drawable.status_red);
	 	    		  	} else { errorAlert("10" , "Please select alert option"); }
	    			} 	
	    		}
	    	 })
	         .setNegativeButton(R.string.remove, new DialogInterface.OnClickListener() {
	        	 public void onClick(DialogInterface dialog, int id) {
	        		 dialog.dismiss();
	            	 clearSavedAlert(viewPass);
	            	 //clearAllAlerts();
	            }
	         });   
	    
	    //build and show dialog
	    AlertDialog alert = builder.create();
	    alert.show();
	    
	    //set initial states for options
	    //...class capacity
	    
	    TextView classCap = (TextView) v.findViewById(R.id.classCapTextView);
	    final TextView seekBarValue = (TextView) newOptionBox.findViewById(R.id.seekBarValue);
	    //retrieve alert type and set radio button
	    RadioButton but1 = (RadioButton) newOptionBox.findViewById(R.id.optionRadio1);
		RadioButton but2 = (RadioButton) newOptionBox.findViewById(R.id.optionRadio2);
		String alertType = extractPref(v, "alertType");
		if (alertType.equals("firstOpen"))
			{but1.setChecked(true); 
			but2.setChecked(false);}
		else if (alertType.equals("moreThan")) 
			{but1.setChecked(false);
			but2.setChecked(true);}
		else {errorAlert("10", alertType);}
		//set seekerBar max & increment
	    sb.setMax(Integer.valueOf(classCap.getText().toString()));
	    sb.incrementProgressBy(1);	    
	    //set seekerBar from saved value
	    sb.setProgress(Integer.valueOf(extractPref(v, "threshold")));
	    seekBarValue.setText(Integer.toString(sb.getProgress()));
	    
	    //set change listeners (updates textView to match seekerBar move)
	    sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				seekBarValue.setText(String.valueOf(progress));
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}
	    });	    
	}
	
	
	public void savePref(String[] prefArray) {
		SharedPreferences settings = getSharedPreferences(CLASSINFO_PREFS, 0);
		SharedPreferences.Editor prefEditor = settings.edit();
		//pref value stored as: crn,classCap,name,id,campus,alertType,threshold	
		String prefKey = prefArray[0];
		String prefValue = prefArray[0] + "," + prefArray[1] + "," + prefArray[2] + "," + prefArray[3] + "," + 
				prefArray[4] + "," + prefArray[5] + "," + prefArray[6] + "," + prefArray[7];
		prefEditor.putString(prefKey, prefValue);
		prefEditor.commit();
	}
	
	public void savePref(View view, String key, String value) {
		SharedPreferences settings = getSharedPreferences(CLASSINFO_PREFS, 0);
		SharedPreferences.Editor prefEditor = settings.edit();
		
		TextView crnTV = (TextView) view.findViewById(R.id.crnTextView);
		String prefKey = crnTV.getText().toString();
		String prefValue = settings.getString(prefKey, null);
		String[] prefArray = prefValue.split(",");
		//pref value stored as: crn,classCap,name,id,campus,alertType,threshold		
				
		if (key.equals("firstOpen")) {
			prefArray[6] = "firstOpen";
		} else if (key.equals("moreThan")) {
			prefArray[6] = "moreThan";
			prefArray[7] = value;
		} else {errorAlert("10", "something funky passed to savePref(view,string,string");}
		prefValue = prefArray[0] + "," + prefArray[1] + "," + prefArray[2] + "," + prefArray[3] + "," + 
				prefArray[4] + "," + prefArray[5] + "," + prefArray[6] + "," + prefArray[7];
		prefEditor.putString(prefKey, prefValue);
		prefEditor.commit();
	}
	
	public void saveGlobal(String key, String value) {
		SharedPreferences settings = getSharedPreferences(GLOBAL_PREFS, 0);
		SharedPreferences.Editor prefEditor = settings.edit();
		
		String prefValue = settings.getString("global", null); //real pref key is global
		String[] prefArray = prefValue.split(",");
		//global stored as updateMethod(manual/interval),syncFreq(mins),notifyType(on/off)
		if (key.equals("update")) {
			prefValue = value + "," + prefArray[1] + "," + prefArray[2];
			prefEditor.putString("global", prefValue);
			prefEditor.commit();
		} else if (key.equals("interval")) {
			prefValue = prefArray[0] + "," + value + "," + prefArray[2];
			prefEditor.putString("global", prefValue);
			prefEditor.commit();
		} else if (key.equals("notify")) {
			prefValue = prefArray[0] + "," + prefArray[1] + "," + value;
			prefEditor.putString("global", prefValue);
			prefEditor.commit();
		}
		
	}
	
	//clear a single alert from home screen and user prefs
	private void clearSavedAlert(View bar) {
		//retrieve crn of referenced view
		TextView crnTV = (TextView) bar.findViewById(R.id.crnTextView);
		String crn = crnTV.getText().toString();
		//clear preferences
		SharedPreferences settings = getSharedPreferences(CLASSINFO_PREFS, 0);
        SharedPreferences.Editor prefEditor = settings.edit();
	    if(settings.contains(crn)) {
	   	   prefEditor.remove(crn);
	    }
        else{errorAlert("10", "couldn't locate pref#" + crn);}
	    	prefEditor.commit();
			//remove view
			activityHomeLinearLayout.removeView(bar);		
		}
			
	//clear all alerts from home screen and user prefs
	public void clearAllAlerts(MenuItem item) {
		//remove views
		activityHomeLinearLayout.removeAllViews();
		//clear preferences
		SharedPreferences settings = getSharedPreferences(CLASSINFO_PREFS, 0);
	    SharedPreferences.Editor prefEditor = settings.edit();
	    prefEditor.clear();
	    prefEditor.commit();
	}
	
	public String extractPref(String crn, String key) {
		SharedPreferences settings = getSharedPreferences(CLASSINFO_PREFS, 0);
		
		String prefValue = settings.getString(crn, null);
		String[] prefArray = prefValue.split(",");
		
		//pref value stored as: crn,classCap,name,id,campus,term,alertType,threshold
		//global stored as syncFreq,notifyType
		if (key.equals("threshold")) {prefValue = prefArray[7];}
		else if (key.equals("alertType")) {prefValue = prefArray[6];}
		else if (key.equals("term")) {prefValue = prefArray[5];}
		else {errorAlert("10", "error in extraction. PrefValue:"+prefValue);}
			
		return prefValue;
	}

	public String extractGlobal(String key) {
		SharedPreferences settings = getSharedPreferences(GLOBAL_PREFS, 0);
		
		String prefValue = settings.getString("global", null);
		String[] prefArray = prefValue.split(",");
		
		//global stored as updateMethod(manual/interval),syncFreq(mins),notifyType(on/off)
		if (key.equals("update")) {prefValue = prefArray[0];}
		else if (key.equals("interval")) {prefValue = prefArray[1];}
		else if (key.equals("notify")) {prefValue = prefArray[2];}
		else {errorAlert("10", "error in extractGlobalPrefVal:" +  prefValue);}
			
		return prefValue;
	}
	
	public String extractPref(View view, String key) {
		SharedPreferences settings = getSharedPreferences(CLASSINFO_PREFS, 0);
		
		TextView crnTV = (TextView) view.findViewById(R.id.crnTextView);
		String prefKey = crnTV.getText().toString();
		String prefValue = settings.getString(prefKey, null);
		String[] prefArray = prefValue.split(",");
		//pref value stored as: crn,classCap,name,id,campus,alertType,threshold
		if (key.equals("threshold")) {prefValue = prefArray[7];}
		else if (key.equals("alertType")) {prefValue = prefArray[6];}
		else {errorAlert("10", prefValue);}
			
		return prefValue;
	}
	
	
	//*****alerts for various errors. 10 is passed error description for debug
	private void errorAlert(String errorType, String failedCrn) {
		AlertDialog.Builder builder = new AlertDialog.Builder(Home.this);
		builder.setTitle("Error");
		
		if (errorType.equals("1")) {
			builder.setMessage(getResources().getString(R.string.crnFormat)); //wrong format
		} else if (errorType.equals("2")) {
			builder.setMessage("CRN#" + failedCrn + " not found. Try again."); //server says no class by than number
		} else if (errorType.equals("3")) {
			builder.setMessage("null passed to page, not sure why"); 
		} else if (errorType.equals("4")) {
			builder.setMessage("Server unavailable. Try again later.");
		} else if (errorType.equals("5")) {
			builder.setMessage(getResources().getString(R.string.crnMultiFormat));
		} else if (errorType.equals("10")) {
			builder.setMessage(failedCrn);
		}
		else {
			builder.setMessage("Mystery error. What's going on?!?!?!?");
		}
		
		builder.setCancelable(true);
		builder.setPositiveButton("ok", 
			new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int id) {
					dialog.dismiss();
					//Bring keyboard back up after dialog dismissed
					InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					inputManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_NOT_ALWAYS);
				}
			}
		);
		
		AlertDialog alert = builder.create();
		alert.show();
	}

	private void showResults(String[] datum) {
		AlertDialog.Builder builder = new AlertDialog.Builder(Home.this);
		builder.setTitle("Success");
		builder.setMessage(datum[0] + ":" + datum[1] + ":" + datum[2] + ":" + datum[3] + ":" + datum[4] + ":" + datum[5]);
		builder.setCancelable(true);
		
		builder.setPositiveButton("OK", 
			new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int id) {
					dialog.dismiss();
				}
			}
		);
		AlertDialog errorAlert = builder.create();
		errorAlert.show();
	}	
} //***end of Home Activity
