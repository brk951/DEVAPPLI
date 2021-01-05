package com.cylon.widget;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.TrafficStats;
import android.net.wifi.WifiManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

/**
 * Managing network statistics widgets based on TrafficStats class
 */
public class DataStatWidget extends AppWidgetProvider {

	public static String PREFERENCE_UPDATED_FIRST_TIME_ACTION = "com.cylon.widget.ACTION_CONF_UPDATED_FIRST";
	public static String CLOCK_WIDGET_UPDATE_ACTION = "com.cylon.widget.ACTION_ALARM";
	public static String AFTER_REBOOT_ACTION = "com.cylon.widget.ACTION_AFTER_REBOOT";
	public static String PREFERENCE_UPDATED_ACTION = "com.cylon.widget.ACTION_CONF_UPDATED";

	private static final String KEY_NEWEST_UP_WIFI = "newestUpWifi";
	private static final String KEY_NEWEST_DOWN_WIFI = "newestDownWifi";
	private static final String KEY_NEWEST_DOWN_MOBILE = "newestDownMobile";
	private static final String KEY_NEWEST_UP_MOBILE = "newestUpMobile";

	private static final String PREF = "initpref";
	private static final String KEY_INIT_DOWNLOAD_TOTAL = "init_download_total";
	private static final String KEY_INIT_UPLOAD_TOTAL = "init_upload_total";
	private static final String KEY_INIT_DOWNLOAD_MOBILE = "init_download_mobile";
	private static final String KEY_INIT_UPLOAD_MOBILE = "init_upload_mobile";
	public static final String KEY_UPDATE_FREQ = "update_freq";
	public static final String DEFAULT_UPDATE_FREQ = "60";
	public static final String KEY_MOBILE_LIMIT = "mobile_limit";
	public static final String KEY_WIFI_LIMIT = "wifi_limit";
	public static final String KEY_ALLOW_NOTIFICATION = "allow_notification";
	public static final String KEY_DISABLE_INTERNET = "disable_internet";
	private static final String ERROR_MESSAGE1 = "Can't measure wifi data usage (unsupported device)!";
	private static final String ERROR_MESSAGE2 = "Can't measure mobile data usage (unsupported device)!";
	private static final String UP_ARROW = "\u25B2 ";
	private static final String DOWN_ARROW = "\u25BC ";
	private static final int NOTIFICATION_AT_PERCENT = 90;

	/**
	 * Receives an intent with a specific action, these actions could be:
	 * CLOCK_WIDGET_UPDATE_ACTION, PREFERENCE_UPDATED_FIRST_TIME_ACTION,
	 * AFTER_REBOOT_ACTION, PREFERENCE_UPDATED_ACTION
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		String action = intent.getAction();

		// if the alarm has fired
		if (CLOCK_WIDGET_UPDATE_ACTION.equals(action)) {

			// get the widgetId (which widget has fired)
			// the user can create multiple widgets
			Log.d("TAG", "onReceive() CLOCK UPDATE");
			int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
			Log.d("TAG", "From: " + widgetId);

			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

			// update the widget
			updateAppWidget(context, appWidgetManager, widgetId);

			// after the first configuration
		} else if (PREFERENCE_UPDATED_FIRST_TIME_ACTION.equals(action)) {
			Log.d("TAG", "onReceive() PREFERENCE UPDATE");
			int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
			widgetInitialization(context, widgetId);

		} else if (PREFERENCE_UPDATED_ACTION.equals(action)) {
			int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
			// Cancel the old alarm

			// we need to build up a same pendingintent to cancel the old one
			Intent oldIntent = new Intent(CLOCK_WIDGET_UPDATE_ACTION);
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);

			PendingIntent oldPendingIntent = PendingIntent.getBroadcast(context, widgetId, intent,
					PendingIntent.FLAG_UPDATE_CURRENT);
			AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

			alarmManager.cancel(oldPendingIntent);

			// update the widget
			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
			updateAppWidget(context, appWidgetManager, widgetId);

		} else if (AFTER_REBOOT_ACTION.equals(action)) {
			int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
			widgetInitializationAfterReboot(context, widgetId);

		}
	}

	/**
	 * Initialize the widget with the total usage since boot, this will be our
	 * reference data. Also saves this to preferences.
	 * 
	 * @param context
	 * @param widgetId
	 *            The widget's id.
	 */
	private void widgetInitialization(Context context, int widgetId) {
		long initReceivedBytesTotal;
		long initSentBytesTotal;
		long initReceivedBytesMobile;
		long initSentBytesMobile;

		// get the total usage since boot
		initReceivedBytesTotal = TrafficStats.getTotalRxBytes();
		initSentBytesTotal = TrafficStats.getTotalTxBytes();

		// get the mobile usage since boot
		initReceivedBytesMobile = TrafficStats.getMobileRxBytes();
		initSentBytesMobile = TrafficStats.getMobileTxBytes();

		// check if the device supports TrafficStats
		if (initReceivedBytesTotal == TrafficStats.UNSUPPORTED || initSentBytesTotal == TrafficStats.UNSUPPORTED) {
			Toast.makeText(context, ERROR_MESSAGE1, Toast.LENGTH_SHORT).show();
		}

		if (initReceivedBytesMobile == TrafficStats.UNSUPPORTED || initSentBytesMobile == TrafficStats.UNSUPPORTED) {
			Toast.makeText(context, ERROR_MESSAGE2, Toast.LENGTH_SHORT).show();
		}

		Log.d("TAG", "widgetInitialization() " + initReceivedBytesTotal + "");
		Log.d("TAG", "widgetInitialization() " + initSentBytesTotal + "");
		Log.d("TAG", "widgetInitialization() " + initReceivedBytesMobile + "");
		Log.d("TAG", "widgetInitialization() " + initSentBytesMobile + "");

		// store in SharedPreferences

		putInSharedPreferences(PREF + widgetId, context, KEY_INIT_DOWNLOAD_TOTAL, initReceivedBytesTotal);
		putInSharedPreferences(PREF + widgetId, context, KEY_INIT_UPLOAD_TOTAL, initSentBytesTotal);
		putInSharedPreferences(PREF + widgetId, context, KEY_INIT_DOWNLOAD_MOBILE, initReceivedBytesMobile);
		putInSharedPreferences(PREF + widgetId, context, KEY_INIT_UPLOAD_MOBILE, initSentBytesMobile);

		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

		updateAppWidget(context, appWidgetManager, widgetId);
	}

	/**
	 * This method will be called from
	 * {@link #onUpgrade(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)}
	 * after reboot. Refresh the reference data usage with the new one. ( new
	 * data usage since boot - latest saved )
	 * 
	 * @param context
	 * @param widgetId
	 *            The widget's id.
	 */
	private void widgetInitializationAfterReboot(Context context, int widgetId) {
		long savedDownWifi = getFromSharedPreferences(PREF + widgetId, context, KEY_NEWEST_DOWN_WIFI);
		long savedUpWifi = getFromSharedPreferences(PREF + widgetId, context, KEY_NEWEST_UP_WIFI);
		long savedDownMobile = getFromSharedPreferences(PREF + widgetId, context, KEY_NEWEST_DOWN_MOBILE);
		long savedUpMobile = getFromSharedPreferences(PREF + widgetId, context, KEY_NEWEST_UP_MOBILE);

		Log.d("Saved down wifi: ", savedDownWifi + "");
		Log.d("Saved up wifi: ", savedUpWifi + "");
		Log.d("Saved down mobile: ", savedDownMobile + "");
		Log.d("Saved up mobile: ", savedUpMobile + "");

		// change the old reference
		putInSharedPreferences(PREF + widgetId, context, KEY_INIT_DOWNLOAD_TOTAL, TrafficStats.getTotalRxBytes()
				- (savedDownWifi + savedDownMobile));
		putInSharedPreferences(PREF + widgetId, context, KEY_INIT_UPLOAD_TOTAL, TrafficStats.getTotalTxBytes()
				- (savedUpWifi + savedUpMobile));
		putInSharedPreferences(PREF + widgetId, context, KEY_INIT_DOWNLOAD_MOBILE, TrafficStats.getMobileRxBytes()
				- savedDownMobile);
		putInSharedPreferences(PREF + widgetId, context, KEY_INIT_UPLOAD_MOBILE, TrafficStats.getMobileTxBytes()
				- savedUpMobile);

		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

		updateAppWidget(context, appWidgetManager, widgetId);
	}

	/**
	 * This method is called once when the widget is created (also after every
	 * reboot), since this application does not use updatePeriodMillis based
	 * updates. Check if a boot was completed, based on a boolean value in
	 * preferences. If this value is true, then sends a broadcast with
	 * AFTER_REBOOT_ACTION action. (
	 * {@link #onReceive(Context context, Intent intent)} will receive this)
	 */
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		Log.d("TAG", "onUpdate()");
		SharedPreferences sharedPreferences = context.getSharedPreferences(BootCompleteReceiver.BOOT_PREF_NAME,
				Context.MODE_PRIVATE);

		if (sharedPreferences.getBoolean(BootCompleteReceiver.KEY_BOOT_COMPLETED, false) == true) {
			SharedPreferences.Editor editor = sharedPreferences.edit();
			editor.putBoolean(BootCompleteReceiver.KEY_BOOT_COMPLETED, false);
			editor.commit();
			for (int i = 0; i < appWidgetIds.length; i++) {
				Log.d("TAG", "onUpdate() after boot, widget id: " + appWidgetIds[i]);

				// send a broadcast with AFTER_REBOOT_ACTION
				// this will be restart our "loop"
				Intent intent = new Intent(AFTER_REBOOT_ACTION);
				intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
				context.sendBroadcast(intent);

			}
		}
	}

	/**
	 * This method updates a widget. Updates the "setOnClickPendingIntent()",
	 * with this ensures that the settings activity will be displayed after
	 * tapping on the widget. Loads the widget's settings from preferences.
	 * Updates the pending intent for the alarm with the right widgetId (this is
	 * important if we have multiple instances of the widget). Sets an alarm
	 * again, calculates the current usage. If it needed calls
	 * {@link #sendNotification(Context context, double currentTotalDataUsage, int limit) }
	 * , {@link #disableWifiData(Context context)} ,
	 * {@link #disableMobileData(Context context)} methods.
	 * 
	 * @param context
	 * @param appWidgetManager
	 * @param appWidgetId
	 */
	public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {

		// Create an intent, this ensures that if the user taps on a widget, the
		// configuration activity will be displayed

		Intent intentClick = new Intent(context, DataStatWidgetConfigure.class);
		intentClick.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

		// this extra marks that this is not the first configuration
		intentClick.putExtra(DataStatWidgetConfigure.KEY_CONFIG, 1);
		PendingIntent pendingIntentClick = PendingIntent.getActivity(context, 0, intentClick,
				PendingIntent.FLAG_UPDATE_CURRENT);

		RemoteViews view = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
		view.setOnClickPendingIntent(R.id.widget_linear_layout, pendingIntentClick);

		// Loads the widget's settings

		SharedPreferences prefs = context.getSharedPreferences(Integer.toString(appWidgetId), Context.MODE_PRIVATE);

		int updateFrequencyMin = Integer.parseInt(prefs.getString(KEY_UPDATE_FREQ, DEFAULT_UPDATE_FREQ));
		int wifiLimit = Integer.parseInt(prefs.getString(KEY_WIFI_LIMIT, "-1"));
		int mobileLimit = Integer.parseInt(prefs.getString(KEY_MOBILE_LIMIT, "-1"));
		boolean allowNotification = prefs.getBoolean(KEY_ALLOW_NOTIFICATION, false);
		boolean disableInternet = prefs.getBoolean(KEY_DISABLE_INTERNET, false);

		Log.d("TAG", "Notification: " + allowNotification);
		Log.d("TAG", "Disable internet: " + disableInternet);
		Log.d("TAG", "Limit wifi: " + wifiLimit);
		Log.d("TAG", "Limit mobile: " + mobileLimit);

		// Starts an alarm

		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		Intent intent = new Intent(CLOCK_WIDGET_UPDATE_ACTION);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		Log.d("TAG", "Update freq: " + updateFrequencyMin);
		long updateFrequencyMillis = updateFrequencyMin * 60 * 1000;

		alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + updateFrequencyMillis,
				updateFrequencyMillis, pendingIntent);

		// Get the reference (data usage since boot)
		long downTotal = getFromSharedPreferences(PREF + appWidgetId, context, KEY_INIT_DOWNLOAD_TOTAL);
		long downMobile = getFromSharedPreferences(PREF + appWidgetId, context, KEY_INIT_DOWNLOAD_MOBILE);
		long upTotal = getFromSharedPreferences(PREF + appWidgetId, context, KEY_INIT_UPLOAD_TOTAL);
		long upMobile = getFromSharedPreferences(PREF + appWidgetId, context, KEY_INIT_UPLOAD_MOBILE);

		// Calculate the data usage
		long currentDownWifi = TrafficStats.getTotalRxBytes() - TrafficStats.getMobileRxBytes() - downTotal
				+ downMobile;
		long currentUpWifi = TrafficStats.getTotalTxBytes() - TrafficStats.getMobileTxBytes() - upTotal + upMobile;
		long currentDownMobile = TrafficStats.getMobileRxBytes() - downMobile;
		long currentUpMobile = TrafficStats.getMobileTxBytes() - upMobile;

		// Save these newest informations to shared preferences ( we need to
		// retrieve these in case reboot )

		putInSharedPreferences(PREF + appWidgetId, context, KEY_NEWEST_DOWN_WIFI, currentDownWifi);
		putInSharedPreferences(PREF + appWidgetId, context, KEY_NEWEST_UP_WIFI, currentUpWifi);
		putInSharedPreferences(PREF + appWidgetId, context, KEY_NEWEST_DOWN_MOBILE, currentDownMobile);
		putInSharedPreferences(PREF + appWidgetId, context, KEY_NEWEST_UP_MOBILE, currentUpMobile);

		view.setTextViewText(R.id.tv_download_wifi, DOWN_ARROW + formatBytes(currentDownWifi));
		view.setTextViewText(R.id.tv_upload_wifi, UP_ARROW + formatBytes(currentUpWifi));
		view.setTextViewText(R.id.tv_download_mobile, DOWN_ARROW + formatBytes(currentDownMobile));
		view.setTextViewText(R.id.tv_upload_mobile, UP_ARROW + formatBytes(currentUpMobile));

		double currentTotalWifi = convertToMegabyte(currentDownWifi + currentUpWifi);
		double currentTotalMobile = convertToMegabyte(currentDownMobile + currentUpMobile);

		// Check if the limits are set
		if (wifiLimit != -1) {

			// notification at 90%
			if (currentTotalWifi >= (wifiLimit * NOTIFICATION_AT_PERCENT) / 100) {
				if (allowNotification == true) {
					Log.d("TAG", "You have reached the limit!");
					sendNotification(context, currentTotalWifi, wifiLimit);
				}
				if (disableInternet == true) {
					disableWifiData(context);
				}

			}

		}

		if (mobileLimit != -1) {

			// notification at 90%
			if (currentTotalMobile >= ((double) (mobileLimit * NOTIFICATION_AT_PERCENT)) / 100) {
				Log.d("TAG", "" + currentTotalMobile);

				if (allowNotification == true) {
					Log.d("TAG", "You have reached the limit!");
					sendNotification(context, currentTotalMobile, mobileLimit);
				}
				if (disableInternet == true) {
					disableMobileData(context);
				}

			}
		}

		appWidgetManager.updateAppWidget(appWidgetId, view);

	}

	/**
	 * This method is called when the user removes a widget. Deletes the
	 * widget's settings and reference data from shared preferences
	 */
	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		// TODO Auto-generated method stub

		super.onDeleted(context, appWidgetIds);
		for (int i = 0; i < appWidgetIds.length; i++) {
			Log.d("TAG", "Cancel: " + appWidgetIds[i]);
			Intent intent = new Intent(CLOCK_WIDGET_UPDATE_ACTION);
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
			AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			alarmManager.cancel(PendingIntent.getBroadcast(context, appWidgetIds[i], intent,
					PendingIntent.FLAG_UPDATE_CURRENT));

			// delete the settings
			deleteFromSharedPreferences(Integer.toString(appWidgetIds[i]), context);

			// delete the reference data usage
			deleteFromSharedPreferences(PREF + appWidgetIds[i], context);

		}
	}

	/**
	 * This method is called when the user removes all the widgets.
	 */
	@Override
	public void onDisabled(Context context) {
		// TODO Auto-generated method stub
		super.onDisabled(context);
		Log.d("TAG", "onDisabled()");
	}

	/**
	 * Saves a value to preferences with a given name, key, value.
	 * 
	 * @param name
	 *            Shared preference's name
	 * @param context
	 * @param key
	 * @param value
	 */
	private static void putInSharedPreferences(String name, Context context, String key, long value) {
		SharedPreferences sharedPreferences = context.getSharedPreferences(name, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putLong(key, value);
		editor.commit();
	}

	/**
	 * Gets a value by key.
	 * 
	 * @param name
	 *            Shared preference's name
	 * @param context
	 * @param key
	 * @return
	 */
	private static long getFromSharedPreferences(String name, Context context, String key) {
		SharedPreferences sharedPreferences = context.getSharedPreferences(name, Context.MODE_PRIVATE);
		return sharedPreferences.getLong(key, -1);
	}

	/**
	 * Removes all values from a preferences.
	 * 
	 * @param name
	 *            Shared preference's name
	 * @param context
	 */
	private static void deleteFromSharedPreferences(String name, Context context) {
		SharedPreferences sharedPreferences = context.getSharedPreferences(name, Context.MODE_PRIVATE);
		sharedPreferences.edit().clear().commit();
	}

	/**
	 * Gives back KB, MB, or GB based on the incoming byte amount in "0.0"
	 * format.
	 * 
	 * @param bytes
	 * @return
	 */
	private static String formatBytes(long bytes) {
		DecimalFormat decimalFormat = new DecimalFormat("0.0");
		if (bytes < 1024000) {

			return decimalFormat.format((double) bytes / 1024) + " KB"; // kilobyte
		}

		if (bytes < 1024000000) {
			return decimalFormat.format((double) bytes / (1024 * 1024)) + " MB"; // megabyte
		}

		return decimalFormat.format((double) bytes / (1024 * 1024 * 1024)) + " GB"; // gigabyte

	}

	/**
	 * Converts the incoming byte amount to megabyte.
	 * 
	 * @param bytes
	 * @return
	 */
	private static double convertToMegabyte(long bytes) {

		return ((double) bytes / (1024 * 1024));
	}

	/**
	 * Sends a notification.
	 * 
	 * @param context
	 * @param currentTotalDataUsage
	 *            Data usage since the user has created the widget.
	 * @param limit
	 *            Predefined limit in MB by the user.
	 */
	private static void sendNotification(Context context, double currentTotalDataUsage, int limit) {
		DecimalFormat dformat = new DecimalFormat("0.0");
		String currentTotalDataUsageString = dformat.format(currentTotalDataUsage);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

		builder.setAutoCancel(true);
		builder.setContentTitle("Network usage");
		builder.setContentText("You almost reached the limit! (" + currentTotalDataUsageString + " MB / " + limit
				+ " MB)");
		builder.setSmallIcon(R.drawable.ic_launcher);

		Notification notification = builder.build();
		NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		manager.notify(1, notification);
	}

	/**
	 * Disable wifi network
	 * 
	 * @param context
	 */
	private static void disableWifiData(Context context) {
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		wifiManager.setWifiEnabled(false);
	}

	/**
	 * Disable mobile network
	 * 
	 * @param context
	 */
	private static void disableMobileData(Context context) {
		final ConnectivityManager conman = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		Class conmanClass = null;
		try {
			conmanClass = Class.forName(conman.getClass().getName());
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Field connectivityManagerField = null;
		try {
			connectivityManagerField = conmanClass.getDeclaredField("mService");
		} catch (NoSuchFieldException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		connectivityManagerField.setAccessible(true);
		Object connectivityManager = null;
		try {
			connectivityManager = connectivityManagerField.get(conman);
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Class connectivityManagerClass = null;
		try {
			connectivityManagerClass = Class.forName(connectivityManager.getClass().getName());
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Method setMobileDataEnabledMethod = null;
		try {
			setMobileDataEnabledMethod = connectivityManagerClass.getDeclaredMethod("setMobileDataEnabled",
					Boolean.TYPE);
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		setMobileDataEnabledMethod.setAccessible(true);

		try {
			setMobileDataEnabledMethod.invoke(connectivityManager, false);
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
