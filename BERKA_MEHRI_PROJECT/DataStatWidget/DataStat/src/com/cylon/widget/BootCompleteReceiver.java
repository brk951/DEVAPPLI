package com.cylon.widget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * This class will receive the ACTION_BOOT_COMPLETED broadcast. After receiving
 * this, puts a value to preferences which indicates that the boot has completed
 */
public class BootCompleteReceiver extends BroadcastReceiver {
	public static String BOOT_PREF_NAME = "bootpref";
	public static String KEY_BOOT_COMPLETED = "keyBootCompleted";

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
			Log.d("TAG", "BootCompleteReceiver - boot success");
			SharedPreferences sharedPreferences = context.getSharedPreferences(BOOT_PREF_NAME, Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = sharedPreferences.edit();
			editor.putBoolean(KEY_BOOT_COMPLETED, true);
			editor.commit();
		}
	}

}
