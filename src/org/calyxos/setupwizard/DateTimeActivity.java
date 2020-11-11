/*
 * Copyright (C) 2016 The CyanogenMod Project
 * Copyright (C) 2017 The LineageOS Project
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

package org.calyxos.setupwizard;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import org.calyxos.setupwizard.util.SetupWizardUtils;

import org.xmlpull.v1.XmlPullParserException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class DateTimeActivity extends BaseSetupWizardActivity implements
        TimePickerDialog.OnTimeSetListener, DatePickerDialog.OnDateSetListener {

    public static final String TAG = DateTimeActivity.class.getSimpleName();

    private static final String KEY_ID = "id";  // value: String
    private static final String KEY_DISPLAYNAME = "name";  // value: String
    private static final String KEY_GMT = "gmt";  // value: String
    private static final String KEY_OFFSET = "offset";  // value: int (Integer)
    private static final String XMLTAG_TIMEZONE = "timezone";

    private static final int HOURS_1 = 60 * 60000;

    private TimeZone mCurrentTimeZone;
    private TextView mDateTextView;
    private TextView mTimeTextView;


    private final Handler mHandler = new Handler();

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
                updateTimeAndDateDisplay();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setNextText(R.string.next);

        final Spinner spinner = (Spinner) findViewById(R.id.timezone_list);
        final SimpleAdapter adapter = constructTimezoneAdapter(this, false);
        mCurrentTimeZone = TimeZone.getDefault();
        View dateView = findViewById(R.id.date_item);
        dateView.setOnClickListener((view) -> showDatePicker());
        View timeView = findViewById(R.id.time_item);
        timeView.setOnClickListener((view) -> showTimePicker());
        mDateTextView = (TextView)findViewById(R.id.date_text);
        mTimeTextView = (TextView)findViewById(R.id.time_text);
        // Pre-select current/default timezone
        mHandler.post(() -> {
            int tzIndex = getTimeZoneIndex(adapter, mCurrentTimeZone);
            spinner.setAdapter(adapter);
            if (tzIndex != -1) {
                spinner.setSelection(tzIndex);
            }
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position,
                                           long id) {
                    final Map<?, ?> map = (Map<?, ?>) adapterView.getItemAtPosition(position);
                    final String tzId = (String) map.get(KEY_ID);
                    if (mCurrentTimeZone != null && !mCurrentTimeZone.getID().equals(tzId)) {
                        // Update the system timezone value
                        final AlarmManager alarm =
                                (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                        alarm.setTimeZone(tzId);
                        mCurrentTimeZone = TimeZone.getTimeZone(tzId);
                    }

                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });
        });
        // Pre-select current/default date if epoch
        mHandler.post(() -> {
            final Calendar calendar = Calendar.getInstance();
            final boolean isEpoch = calendar.get(Calendar.YEAR) == 1970;
            if (isEpoch) {
                // If epoch, set date to build date
                long timestamp = SetupWizardUtils.getBuildDateTimestamp();
                if (timestamp > 0) {
                    calendar.setTimeInMillis(timestamp * 1000);
                    setDate(DateTimeActivity.this, calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
                } else {
                    // no build date available, use a sane default
                    setDate(DateTimeActivity.this, 2017, Calendar.JANUARY, 1);
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register for time ticks and other reasons for time change
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver(mIntentReceiver, filter, null, null);

        updateTimeAndDateDisplay();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mIntentReceiver);
    }

    @Override
    protected int getTransition() {
        return TRANSITION_ID_SLIDE;
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.setup_datetime_page;
    }

    @Override
    protected int getTitleResId() {
        return R.string.setup_datetime;
    }

    @Override
    protected int getIconResId() {
        return R.drawable.ic_datetime;
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {
            setDate(this, year, month, day);
            updateTimeAndDateDisplay();
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            setTime(this, hourOfDay, minute);
            updateTimeAndDateDisplay();
    }

    private void showDatePicker() {
        DatePickerFragment datePickerFragment = DatePickerFragment.newInstance();
        datePickerFragment.show(getFragmentManager(), DatePickerFragment.TAG);
    }

    private void showTimePicker() {
        TimePickerFragment timePickerFragment = TimePickerFragment.newInstance();
        timePickerFragment.show(getFragmentManager(), TimePickerFragment.TAG);
    }

    private void updateTimeAndDateDisplay() {
        java.text.DateFormat shortDateFormat = DateFormat.getDateFormat(this);
        final Calendar now = Calendar.getInstance();
        mTimeTextView.setText(DateFormat.getTimeFormat(this).format(now.getTime()));
        mDateTextView.setText(shortDateFormat.format(now.getTime()));
    }

    private static SimpleAdapter constructTimezoneAdapter(Context context,
            boolean sortedByName) {
        final String[] from = new String[] {KEY_DISPLAYNAME, KEY_GMT};
        final int[] to = new int[] {android.R.id.text1, android.R.id.text2};

        final String sortKey = (sortedByName ? KEY_DISPLAYNAME : KEY_OFFSET);
        final TimeZoneComparator comparator = new TimeZoneComparator(sortKey);
        final List<HashMap<String, Object>> sortedList = getZones(context);
        Collections.sort(sortedList, comparator);
        final SimpleAdapter adapter = new SimpleAdapter(context,
                sortedList,
                R.layout.date_time_setup_custom_list_item_2,
                from,
                to);

        return adapter;
    }

    private static List<HashMap<String, Object>> getZones(Context context) {
        final List<HashMap<String, Object>> myData = new ArrayList();
        final long date = Calendar.getInstance().getTimeInMillis();
        try {
            XmlResourceParser xrp = context.getResources().getXml(R.xml.timezones);
            while (xrp.next() != XmlResourceParser.START_TAG)
                continue;
            xrp.next();
            while (xrp.getEventType() != XmlResourceParser.END_TAG) {
                while (xrp.getEventType() != XmlResourceParser.START_TAG) {
                    if (xrp.getEventType() == XmlResourceParser.END_DOCUMENT) {
                        return myData;
                    }
                    xrp.next();
                }
                if (xrp.getName().equals(XMLTAG_TIMEZONE)) {
                    String id = xrp.getAttributeValue(0);
                    String displayName = xrp.nextText();
                    addItem(myData, id, displayName, date);
                }
                while (xrp.getEventType() != XmlResourceParser.END_TAG) {
                    xrp.next();
                }
                xrp.next();
            }
            xrp.close();
        } catch (XmlPullParserException xppe) {
            Log.e(TAG, "Ill-formatted timezones.xml file");
        } catch (java.io.IOException ioe) {
            Log.e(TAG, "Unable to read timezones.xml file");
        }

        return myData;
    }

    private static void addItem(
            List<HashMap<String, Object>> myData, String id, String displayName, long date) {
        final HashMap<String, Object> map = new HashMap();
        map.put(KEY_ID, id);
        map.put(KEY_DISPLAYNAME, displayName);
        final TimeZone tz = TimeZone.getTimeZone(id);
        final int offset = tz.getOffset(date);
        final int p = Math.abs(offset);
        final StringBuilder name = new StringBuilder();
        name.append("GMT");

        if (offset < 0) {
            name.append('-');
        } else {
            name.append('+');
        }

        name.append(p / (HOURS_1));
        name.append(':');

        int min = p / 60000;
        min %= 60;

        if (min < 10) {
            name.append('0');
        }
        name.append(min);

        map.put(KEY_GMT, name.toString());
        map.put(KEY_OFFSET, offset);

        myData.add(map);
    }

    private static int getTimeZoneIndex(SimpleAdapter adapter, TimeZone tz) {
        final String defaultId = tz.getID();
        final int listSize = adapter.getCount();
        for (int i = 0; i < listSize; i++) {
            // Using HashMap<String, Object> induces unnecessary warning.
            final HashMap<?,?> map = (HashMap<?,?>)adapter.getItem(i);
            final String id = (String)map.get(KEY_ID);
            if (defaultId.equals(id)) {
                // If current timezone is in this list, move focus to it
                return i;
            }
        }
        return -1;
    }

    private static void setDate(Context context, int year, int month, int day) {
        Calendar c = Calendar.getInstance();

        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        long when = c.getTimeInMillis();

        if (when / 1000 < Integer.MAX_VALUE) {
            ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).setTime(when);
        }
    }

    private static void setTime(Context context, int hourOfDay, int minute) {
        Calendar c = Calendar.getInstance();

        c.set(Calendar.HOUR_OF_DAY, hourOfDay);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long when = c.getTimeInMillis();

        if (when / 1000 < Integer.MAX_VALUE) {
            ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).setTime(when);
        }
    }

    private static class TimeZoneComparator implements Comparator<HashMap<?, ?>> {
        private String mSortingKey;

        public TimeZoneComparator(String sortingKey) {
            mSortingKey = sortingKey;
        }

        public void setSortingKey(String sortingKey) {
            mSortingKey = sortingKey;
        }

        public int compare(HashMap<?, ?> map1, HashMap<?, ?> map2) {
            Object value1 = map1.get(mSortingKey);
            Object value2 = map2.get(mSortingKey);

            /*
             * This should never happen, but just in-case, put non-comparable
             * items at the end.
             */
            if (!isComparable(value1)) {
                return isComparable(value2) ? 1 : 0;
            } else if (!isComparable(value2)) {
                return -1;
            }

            return ((Comparable) value1).compareTo(value2);
        }

        private boolean isComparable(Object value) {
            return (value != null) && (value instanceof Comparable);
        }
    }

    public static class TimePickerFragment extends DialogFragment
            implements TimePickerDialog.OnTimeSetListener {

        private static String TAG = TimePickerFragment.class.getSimpleName();

        public static TimePickerFragment newInstance() {
            TimePickerFragment frag = new TimePickerFragment();
            return frag;
        }

        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            ((DateTimeActivity)getActivity()).onTimeSet(view, hourOfDay, minute);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Calendar calendar = Calendar.getInstance();
            return new TimePickerDialog(
                    getActivity(),
                    this,
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    DateFormat.is24HourFormat(getActivity()));
        }

    }

    public static class DatePickerFragment extends DialogFragment
            implements DatePickerDialog.OnDateSetListener {

        private static String TAG = DatePickerFragment.class.getSimpleName();

        public static DatePickerFragment newInstance() {
            DatePickerFragment frag = new DatePickerFragment();
            return frag;
        }

        @Override
        public void onDateSet(DatePicker view, int year, int month, int day) {
            ((DateTimeActivity)getActivity()).onDateSet(view, year, month, day);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Calendar calendar = Calendar.getInstance();
            return new DatePickerDialog(
                    getActivity(),
                    this,
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
        }
    }
}
