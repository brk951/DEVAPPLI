package com.cylon.widget;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;

/**
 * Managing settings of a widget
 */
public class DataStatWidgetConfigure extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	private int widgetId;
	private static String FIRST_CONFIGURATION_KEY = "firstconf";
	public static String KEY_CONFIG = "keyConfiguration";

	/**
	 * Change the default shared preference name to a unique name based on the
	 * widget id which is located in extras. Check if this is the first
	 * configuration or not by reading out a value from shared preferences.
	 */
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle extras = getIntent().getExtras();

		if (extras != null) {

			widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
			Log.d("TAG", "Preference activity for id: " + widgetId);
			getPreferenceManager().setSharedPreferencesName(Integer.toString(widgetId));

			addPreferencesFromResource(R.xml.prefs);

			int res = extras.getInt(KEY_CONFIG, -1);
			// the user has just tapped the widget to configure again
			if (res == 1) {

				// we need to set the summaries to the right values
				ListPreference listTemp = (ListPreference) getPreferenceScreen().findPreference(
						getString(R.string.update_freq_key));
				if (listTemp.getEntry() != null) {
					listTemp.setSummary(listTemp.getEntry());
				}
				EditTextPreference editTemp1 = (EditTextPreference) getPreferenceManager().findPreference(
						getString(R.string.wifi_limit_key));

				if (editTemp1.getText() != null) {

					editTemp1.setSummary(editTemp1.getText() + " MB");
				}

				EditTextPreference editTemp2 = (EditTextPreference) getPreferenceManager().findPreference(
						getString(R.string.mobile_limit_key));
				if (editTemp2.getText() != null) {

					editTemp2.setSummary(editTemp2.getText() + " MB");
				}

				// if one of the limit is set, then enable the two check
				// boxes. otherwise disable them
				if (editTemp1.getText() == null && editTemp2.getText() == null) {
					getPreferenceScreen().findPreference(getString(R.string.allow_notification_key)).setEnabled(false);
					getPreferenceScreen().findPreference(getString(R.string.disable_internet_key)).setEnabled(false);

				}

			} else {
				// If this is the first configuration we must disable the two
				// check boxes at the beginning (until the user choose a limit)
				// save FIRST_CONFIGURATION_COMPLETED_KEY as true
				SharedPreferences e = getSharedPreferences(Integer.toString(widgetId), MODE_PRIVATE);
				SharedPreferences.Editor ed = e.edit();
				ed.putBoolean(FIRST_CONFIGURATION_KEY, true);
				ed.commit();

				getPreferenceScreen().findPreference(getString(R.string.allow_notification_key)).setEnabled(false);
				getPreferenceScreen().findPreference(getString(R.string.disable_internet_key)).setEnabled(false);
			}
		}

	}

	@Override
	public void onBackPressed() {
		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
		setResult(RESULT_OK, resultValue);

		SharedPreferences e = getSharedPreferences(Integer.toString(widgetId), MODE_PRIVATE);
		boolean firstConfCompleted = e.getBoolean(FIRST_CONFIGURATION_KEY, false);

		Intent update = new Intent(this, DataStatWidget.class);
		update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
		if (firstConfCompleted) {
			update.setAction(DataStatWidget.PREFERENCE_UPDATED_FIRST_TIME_ACTION);
			SharedPreferences.Editor ed = e.edit();
			ed.putBoolean(FIRST_CONFIGURATION_KEY, false);
			ed.commit();

		} else {
			update.setAction(DataStatWidget.PREFERENCE_UPDATED_ACTION);
		}
		sendBroadcast(update);
		finish();
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onResume() {
		super.onResume();
		// Set up a listener whenever a key changes
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onPause() {
		super.onPause();
		// Unregister the listener whenever a key changes
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		updatePrefSummary(findPreference(key));
	}

	@SuppressWarnings("deprecation")
	private void updatePrefSummary(Preference p) {
		if (p instanceof ListPreference) {
			ListPreference listPref = (ListPreference) p;
			p.setSummary(listPref.getEntry());
		}
		if (p instanceof EditTextPreference) {
			EditTextPreference editTextPref = (EditTextPreference) p;

			// if the Mobile/Wifi data usage limit preference was cleared
			// then reset the summary from @string xml and put a "-1" with
			// a right key witch indicates no limit

			// if one of the editboxes is not empty, then enable the checkboxes

			if (p.getTitle().toString().equals(getString(R.string.mobile_data_usage_limit_title))
					&& editTextPref.getText().length() == 0) {
				p.setSummary(getString(R.string.mobile_internet_limit_summary));

				SharedPreferences e = getSharedPreferences(Integer.toString(widgetId), MODE_PRIVATE);
				SharedPreferences.Editor ed = e.edit();
				ed.remove(getString(R.string.mobile_limit_key));
				ed.putString(getString(R.string.mobile_limit_key), "-1");
				ed.commit();

				// if the other edittext's length equal to 0, then disable the
				// checkboxes

				CheckBoxPreference temp_check1 = (CheckBoxPreference) findPreference(getString(R.string.allow_notification_key));
				CheckBoxPreference temp_check2 = (CheckBoxPreference) findPreference(getString(R.string.disable_internet_key));

				EditTextPreference temp1 = (EditTextPreference) getPreferenceScreen().findPreference(
						getString(R.string.wifi_limit_key));
				if (temp1.getText() == null) {
					temp_check1.setChecked(false);
					temp_check2.setChecked(false);
					getPreferenceScreen().findPreference(getString(R.string.allow_notification_key)).setEnabled(false);
					getPreferenceScreen().findPreference(getString(R.string.disable_internet_key)).setEnabled(false);

				} else if (temp1.getText().length() == 0) {
					temp_check1.setChecked(false);
					temp_check2.setChecked(false);
					getPreferenceScreen().findPreference(getString(R.string.allow_notification_key)).setEnabled(false);
					getPreferenceScreen().findPreference(getString(R.string.disable_internet_key)).setEnabled(false);
				}

			} else if (p.getTitle().toString().equals(getString(R.string.wifi_data_usage_limit_title))
					&& editTextPref.getText().length() == 0) {
				p.setSummary(getString(R.string.wifi_internet_limit_summary));

				SharedPreferences e = getSharedPreferences(Integer.toString(widgetId), MODE_PRIVATE);
				SharedPreferences.Editor ed = e.edit();
				ed.remove(getString(R.string.wifi_limit_key));
				ed.putString(getString(R.string.wifi_limit_key), "-1");
				ed.commit();

				// if the other edittext's length equal to 0, then disable the
				// checkboxes
				CheckBoxPreference temp_check1 = (CheckBoxPreference) findPreference(getString(R.string.allow_notification_key));
				CheckBoxPreference temp_check2 = (CheckBoxPreference) findPreference(getString(R.string.disable_internet_key));

				EditTextPreference temp1 = (EditTextPreference) getPreferenceScreen().findPreference(
						getString(R.string.mobile_limit_key));

				if (temp1.getText() == null) {
					temp_check1.setChecked(false);
					temp_check2.setChecked(false);
					getPreferenceScreen().findPreference(getString(R.string.allow_notification_key)).setEnabled(false);
					getPreferenceScreen().findPreference(getString(R.string.disable_internet_key)).setEnabled(false);

				} else if (temp1.getText().length() == 0) {
					temp_check1.setChecked(false);
					temp_check2.setChecked(false);
					getPreferenceScreen().findPreference(getString(R.string.allow_notification_key)).setEnabled(false);
					getPreferenceScreen().findPreference(getString(R.string.disable_internet_key)).setEnabled(false);

				}

			} else {
				getPreferenceScreen().findPreference(getString(R.string.allow_notification_key)).setEnabled(true);
				getPreferenceScreen().findPreference(getString(R.string.disable_internet_key)).setEnabled(true);
				p.setSummary(editTextPref.getText() + " MB");
			}
		}
		if (p instanceof MultiSelectListPreference) {
			EditTextPreference editTextPref = (EditTextPreference) p;
			p.setSummary(editTextPref.getText());
		}
	}
}
