/*
 * Copyright (C) 2014 OSE Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.ose;

import android.app.Activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.provider.Settings;
import android.os.UserHandle;

import com.android.internal.util.ose.DeviceUtils;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.ose.quicksettings.QuickSettingsUtil;
import com.android.settings.R;
import com.android.settings.quicklaunch.BookmarkPicker;
import com.android.settings.widget.SeekBarPreference;

import java.net.URISyntaxException;

import com.android.settings.ose.AppSelectListPreference;

public class NotificationDrawerQsSettings extends SettingsPreferenceFragment
            implements OnPreferenceChangeListener  {

    public static final String TAG = "NotificationDrawerSettings";

    private static final String PREF_NOTIFICATION_HIDE_LABELS =
            "notification_hide_labels";
    private static final String PREF_NOTIFICATION_ALPHA =
            "notification_alpha";
    private static final String PREF_NOTI_REMINDER_SOUND =
            "noti_reminder_sound";
    private static final String PREF_NOTI_REMINDER_ENABLED =
            "noti_reminder_enabled";
    private static final String PREF_NOTI_REMINDER_INTERVAL =
            "noti_reminder_interval";
    private static final String PREF_NOTI_REMINDER_RINGTONE =
            "noti_reminder_ringtone";
    private static final String PRE_QUICK_PULLDOWN =
            "quick_pulldown";
    private static final String PRE_SMART_PULLDOWN =
            "smart_pulldown";
    private static final String PRE_COLLAPSE_PANEL =
            "collapse_panel";
    private static final String PREF_TILES_STYLE =
            "quicksettings_tiles_style";
    private static final String PREF_TILE_PICKER =
            "tile_picker";
    private static final String PREF_QUICK_SETTINGS_TILES_FLIP =
            "quick_settings_tiles_flip";
    private static final String STATUS_BAR_CUSTOM_HEADER =
            "custom_status_bar_header";
    private static final String CLOCK_SHORTCUT =
            "clock_shortcut";
    private static final String CALENDAR_SHORTCUT =
            "calendar_shortcut";

    ListPreference mHideLabels;
    SeekBarPreference mNotificationAlpha;
    CheckBoxPreference mReminder;
    ListPreference mReminderInterval;
    ListPreference mReminderMode;
    RingtonePreference mReminderRingtone;
    ListPreference mQuickPulldown;
    ListPreference mSmartPulldown;
    CheckBoxPreference mCollapsePanel;
    CheckBoxPreference mTileFlip;
    private AppSelectListPreference mClockShortcut;
    private AppSelectListPreference mCalendarShortcut;

    private CheckBoxPreference mStatusBarCustomHeader;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.notification_drawer_qs_settings);

        PreferenceScreen prefs = getPreferenceScreen();

        mHideLabels = (ListPreference) findPreference(PREF_NOTIFICATION_HIDE_LABELS);
        int hideCarrier = Settings.System.getInt(getContentResolver(),
                Settings.System.NOTIFICATION_HIDE_LABELS, 0);
        mHideLabels.setValue(String.valueOf(hideCarrier));
        mHideLabels.setOnPreferenceChangeListener(this);
        updateHideNotificationLabelsSummary(hideCarrier);

        mStatusBarCustomHeader = (CheckBoxPreference) findPreference(STATUS_BAR_CUSTOM_HEADER);
        mStatusBarCustomHeader.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.STATUS_BAR_CUSTOM_HEADER, 0) == 1);
        mStatusBarCustomHeader.setOnPreferenceChangeListener(this);

        mClockShortcut = (AppSelectListPreference)prefs.findPreference(CLOCK_SHORTCUT);
        mClockShortcut.setOnPreferenceChangeListener(this);

        mCalendarShortcut = (AppSelectListPreference)prefs.findPreference(CALENDAR_SHORTCUT);
        mCalendarShortcut.setOnPreferenceChangeListener(this);

        updateClockCalendarSummary();

        PackageManager pm = getPackageManager();
        boolean isMobileData = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);

        if (!DeviceUtils.isPhone(getActivity())) {
            // Nothing for tablets and large screen devices which doesn't show
            // information in notification drawer.....remove option
            prefs.removePreference(mHideLabels);
        }

        float transparency;
        try{
            transparency = Settings.System.getFloat(getContentResolver(),
                    Settings.System.NOTIFICATION_ALPHA);
        } catch (Exception e) {
            transparency = 0;
            Settings.System.putFloat(getContentResolver(),
                    Settings.System.NOTIFICATION_ALPHA, 0.0f);
        }
        mNotificationAlpha = (SeekBarPreference) findPreference(PREF_NOTIFICATION_ALPHA);
        mNotificationAlpha.setInitValue((int) (transparency * 100));
        mNotificationAlpha.setOnPreferenceChangeListener(this);

        mReminder = (CheckBoxPreference) findPreference(PREF_NOTI_REMINDER_ENABLED);
        mReminder.setChecked(Settings.System.getIntForUser(getContentResolver(),
                Settings.System.REMINDER_ALERT_ENABLED, 0, UserHandle.USER_CURRENT) == 1);
        mReminder.setOnPreferenceChangeListener(this);

        mReminderInterval = (ListPreference) findPreference(PREF_NOTI_REMINDER_INTERVAL);
        int interval = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.REMINDER_ALERT_INTERVAL, 0, UserHandle.USER_CURRENT);
        mReminderInterval.setOnPreferenceChangeListener(this);
        updateReminderIntervalSummary(interval);

        mReminderMode = (ListPreference) findPreference(PREF_NOTI_REMINDER_SOUND);
        int mode = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.REMINDER_ALERT_NOTIFY, 0, UserHandle.USER_CURRENT);
        mReminderMode.setValue(String.valueOf(mode));
        mReminderMode.setOnPreferenceChangeListener(this);
        updateReminderModeSummary(mode);

        mReminderRingtone =
                (RingtonePreference) findPreference(PREF_NOTI_REMINDER_RINGTONE);
        Uri ringtone = null;
        String ringtoneString = Settings.System.getStringForUser(getContentResolver(),
                Settings.System.REMINDER_ALERT_RINGER, UserHandle.USER_CURRENT);
        if (ringtoneString == null) {
            // Value not set, defaults to Default Ringtone
            ringtone = RingtoneManager.getDefaultUri(
                    RingtoneManager.TYPE_RINGTONE);
        } else {
            ringtone = Uri.parse(ringtoneString);
        }
        Ringtone alert = RingtoneManager.getRingtone(getActivity(), ringtone);
        mReminderRingtone.setSummary(alert.getTitle(getActivity()));
        mReminderRingtone.setOnPreferenceChangeListener(this);
        mReminderRingtone.setEnabled(mode != 0);

        mQuickPulldown = (ListPreference) findPreference(PRE_QUICK_PULLDOWN);
        mSmartPulldown = (ListPreference) findPreference(PRE_SMART_PULLDOWN);
        if (!DeviceUtils.isPhone(getActivity())) {
            prefs.removePreference(mQuickPulldown);
            prefs.removePreference(mSmartPulldown);
        } else {
            // Quick Pulldown
            mQuickPulldown.setOnPreferenceChangeListener(this);
            int statusQuickPulldown = Settings.System.getInt(getContentResolver(),
                    Settings.System.QS_QUICK_PULLDOWN, 0);
            mQuickPulldown.setValue(String.valueOf(statusQuickPulldown));
            updateQuickPulldownSummary(statusQuickPulldown);

            // Smart Pulldown
            mSmartPulldown.setOnPreferenceChangeListener(this);
            int smartPulldown = Settings.System.getIntForUser(getContentResolver(),
                    Settings.System.QS_SMART_PULLDOWN, 0, UserHandle.USER_CURRENT);
            mSmartPulldown.setValue(String.valueOf(smartPulldown));
            updateSmartPulldownSummary(smartPulldown);
        }

        mCollapsePanel = (CheckBoxPreference) findPreference(PRE_COLLAPSE_PANEL);
        mCollapsePanel.setChecked(Settings.System.getIntForUser(getContentResolver(),
                Settings.System.QS_COLLAPSE_PANEL, 0, UserHandle.USER_CURRENT) == 1);
        mCollapsePanel.setOnPreferenceChangeListener(this);

        mTileFlip = (CheckBoxPreference) findPreference(PREF_QUICK_SETTINGS_TILES_FLIP);
        mTileFlip.setChecked(Settings.System.getIntForUser(getContentResolver(),
                Settings.System.QUICK_SETTINGS_TILES_FLIP, 0, UserHandle.USER_CURRENT) == 1);
        mTileFlip.setOnPreferenceChangeListener(this);

        updateQuickSettingsOptions();
    }

    private void updateQuickSettingsOptions() {
        Preference tilesStyle = (Preference) findPreference(PREF_TILES_STYLE);
        Preference tilesPicker = (Preference) findPreference(PREF_TILE_PICKER);
        String qsConfig = Settings.System.getStringForUser(getContentResolver(),
                Settings.System.QUICK_SETTINGS_TILES, UserHandle.USER_CURRENT);
        boolean hideSettingsPanel = qsConfig != null && qsConfig.isEmpty();
        mQuickPulldown.setEnabled(!hideSettingsPanel);
        mSmartPulldown.setEnabled(!hideSettingsPanel);
        tilesStyle.setEnabled(!hideSettingsPanel);
        if (hideSettingsPanel) {
            tilesPicker.setSummary(getResources().getString(R.string.disable_qs));
        } else {
            tilesPicker.setSummary(getResources().getString(R.string.tile_picker_summary));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        QuickSettingsUtil.updateAvailableTiles(getActivity());
        updateQuickSettingsOptions();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mHideLabels) {
            int hideLabels = Integer.valueOf((String) newValue);
            Settings.System.putInt(getContentResolver(), Settings.System.NOTIFICATION_HIDE_LABELS,
                    hideLabels);
            updateHideNotificationLabelsSummary(hideLabels);
            return true;
        } else if (preference == mNotificationAlpha) {
            float valNav = Float.parseFloat((String) newValue);
            Settings.System.putFloat(getContentResolver(),
                    Settings.System.NOTIFICATION_ALPHA, valNav / 100);
            return true;
        } else if (preference == mQuickPulldown) {
            int statusQuickPulldown = Integer.valueOf((String) newValue);
            Settings.System.putInt(getContentResolver(), Settings.System.QS_QUICK_PULLDOWN,
                    statusQuickPulldown);
            updateQuickPulldownSummary(statusQuickPulldown);
            return true;
        } else if (preference == mSmartPulldown) {
            int smartPulldown = Integer.valueOf((String) newValue);
            Settings.System.putInt(getContentResolver(), Settings.System.QS_SMART_PULLDOWN,
                    smartPulldown);
            updateSmartPulldownSummary(smartPulldown);
            return true;
        } else if (preference == mCollapsePanel) {
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.QS_COLLAPSE_PANEL,
                    (Boolean) newValue ? 1 : 0, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mReminder) {
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.REMINDER_ALERT_ENABLED,
                    (Boolean) newValue ? 1 : 0, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mReminderInterval) {
            int interval = Integer.valueOf((String) newValue);
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.REMINDER_ALERT_INTERVAL,
                    interval, UserHandle.USER_CURRENT);
            updateReminderIntervalSummary(interval);
        } else if (preference == mReminderMode) {
            int mode = Integer.valueOf((String) newValue);
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.REMINDER_ALERT_NOTIFY,
                    mode, UserHandle.USER_CURRENT);
            updateReminderModeSummary(mode);
            mReminderRingtone.setEnabled(mode != 0);
            return true;
        } else if (preference == mReminderRingtone) {
            Uri val = Uri.parse((String) newValue);
            Ringtone ringtone = RingtoneManager.getRingtone(getActivity(), val);
            mReminderRingtone.setSummary(ringtone.getTitle(getActivity()));
            Settings.System.putStringForUser(getContentResolver(),
                    Settings.System.REMINDER_ALERT_RINGER,
                    val.toString(), UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mTileFlip) {
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.QUICK_SETTINGS_TILES_FLIP,
                    (Boolean) newValue ? 1 : 0, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mStatusBarCustomHeader) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(getContentResolver(),
                Settings.System.STATUS_BAR_CUSTOM_HEADER, value ? 1 : 0);
            return true;
        } else if (preference == mClockShortcut) {
            String value = (String) newValue;
            // a value of null means to use the default
            Settings.System.putString(getContentResolver(),
                Settings.System.CLOCK_SHORTCUT, value);
            updateClockCalendarSummary();
        } else if (preference == mCalendarShortcut) {
            String value = (String) newValue;
            // a value of null means to use the default
            Settings.System.putString(getContentResolver(),
                Settings.System.CALENDAR_SHORTCUT, value);
            updateClockCalendarSummary();
        }
        return false;
    }

    private void updateQuickPulldownSummary(int value) {
        Resources res = getResources();

        if (value == 0) {
            // quick pulldown deactivated
            mQuickPulldown.setSummary(res.getString(R.string.quick_pulldown_off));
        } else {
            String direction = res.getString(value == 2
                    ? R.string.quick_pulldown_left
                    : R.string.quick_pulldown_right);
            mQuickPulldown.setSummary(res.getString(R.string.summary_quick_pulldown, direction));
        }
    }

    private void updateSmartPulldownSummary(int value) {
        Resources res = getResources();

        if (value == 0) {
            // Smart pulldown deactivated
            mSmartPulldown.setSummary(res.getString(R.string.smart_pulldown_off));
        } else {
            String type = res.getString(value == 2
                    ? R.string.smart_pulldown_persistent
                    : R.string.smart_pulldown_dismissable);
            mSmartPulldown.setSummary(res.getString(R.string.smart_pulldown_summary, type));
        }
    }

    private void updateHideNotificationLabelsSummary(int value) {
        Resources res = getResources();

        StringBuilder text = new StringBuilder();

        switch (value) {
        case 1  : text.append(res.getString(R.string.notification_hide_labels_carrier));
                break;
        case 2  : text.append(res.getString(R.string.notification_hide_labels_wifi));
                break;
        case 3  : text.append(res.getString(R.string.notification_hide_labels_all));
                break;
        default : text.append(res.getString(R.string.notification_hide_labels_disable));
                break;
        }

        text.append(" " + res.getString(R.string.notification_hide_labels_text));

        mHideLabels.setSummary(text.toString());
    }

    private void updateReminderIntervalSummary(int value) {
        int resId;
        switch (value) {
            case 1000:
                resId = R.string.noti_reminder_interval_1s;
                break;
            case 2000:
                resId = R.string.noti_reminder_interval_2s;
                break;
            case 2500:
                resId = R.string.noti_reminder_interval_2dot5s;
                break;
            case 3000:
                resId = R.string.noti_reminder_interval_3s;
                break;
            case 3500:
                resId = R.string.noti_reminder_interval_3dot5s;
                break;
            case 4000:
                resId = R.string.noti_reminder_interval_4s;
                break;
            default:
                resId = R.string.noti_reminder_interval_1dot5s;
                break;
        }
        mReminderInterval.setValue(Integer.toString(value));
        mReminderInterval.setSummary(getResources().getString(resId));
    }

    private void updateReminderModeSummary(int value) {
        int resId;
        switch (value) {
            case 1:
                resId = R.string.enabled;
                break;
            case 2:
                resId = R.string.noti_reminder_sound_looping;
                break;
            default:
                resId = R.string.disabled;
                break;
        }
        mReminderMode.setSummary(getResources().getString(resId));
    }

    private void updateClockCalendarSummary() {
        final PackageManager packageManager = getPackageManager();

        mClockShortcut.setSummary(getResources().getString(R.string.default_shortcut));
        mCalendarShortcut.setSummary(getResources().getString(R.string.default_shortcut));

        String clockShortcutIntentUri = Settings.System.getString(getContentResolver(), Settings.System.CLOCK_SHORTCUT);
        if (clockShortcutIntentUri != null) {
            Intent clockShortcutIntent = null;
            try {
                clockShortcutIntent = Intent.parseUri(clockShortcutIntentUri, 0);
            } catch (URISyntaxException e) {
                clockShortcutIntent = null;
            }

            if(clockShortcutIntent != null) {
                ResolveInfo info = packageManager.resolveActivity(clockShortcutIntent, 0);
                if (info != null) {
                    mClockShortcut.setSummary(info.loadLabel(packageManager));
                }
            }
        }

        String calendarShortcutIntentUri = Settings.System.getString(getContentResolver(), Settings.System.CALENDAR_SHORTCUT);
        if (calendarShortcutIntentUri != null) {
            Intent calendarShortcutIntent = null;
            try {
                calendarShortcutIntent = Intent.parseUri(calendarShortcutIntentUri, 0);
            } catch (URISyntaxException e) {
                calendarShortcutIntent = null;
            }

            if(calendarShortcutIntent != null) {
                ResolveInfo info = packageManager.resolveActivity(calendarShortcutIntent, 0);
                if (info != null) {
                    mCalendarShortcut.setSummary(info.loadLabel(packageManager));
                }
            }
        }
    }
}
