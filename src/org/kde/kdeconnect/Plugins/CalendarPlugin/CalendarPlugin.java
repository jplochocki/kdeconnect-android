/*
 * SPDX-FileCopyrightText: 2022 Jacek Płochocki <jplochocki@op.pl>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.Plugins.CalendarPlugin;
import java.util.Calendar;

import android.Manifest;
import android.util.Log;
import android.provider.CalendarContract;
import android.content.ContentResolver;
import android.content.Context;
import androidx.core.content.ContextCompat;
import android.database.Cursor;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.content.ContentUris;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.kde.kdeconnect.Helpers.NotificationHelper;
import org.kde.kdeconnect.NetworkPacket;
import org.kde.kdeconnect.Plugins.Plugin;
import org.kde.kdeconnect.Plugins.PluginFactory;
import org.kde.kdeconnect_tp.R;

@PluginFactory.LoadablePlugin
public class CalendarPlugin extends Plugin {

    private final static String PACKET_TYPE_REQUEST_ALL_CALENDARS = "kdeconnect.calendar.request_all_calendars";
    private final static String PACKET_TYPE_RESPONSE_CALENDARS = "kdeconnect.calendar.response_calendars";

    private final static String PACKET_TYPE_REQUEST_ALL_EVENTS = "kdeconnect.calendar.request_all_events";
    private final static String PACKET_TYPE_RESPONSE_EVENTS = "kdeconnect.calendar.response_events";

    @Override
    public String getDisplayName() {
        return context.getResources().getString(R.string.pref_plugin_calendar);
    }

    @Override
    public String getDescription() {
        return context.getResources().getString(R.string.pref_plugin_calendar_desc);
    }

    @Override
    public boolean onPacketReceived(NetworkPacket np) {
        if (np.getType().equals(PACKET_TYPE_REQUEST_ALL_CALENDARS)) {
            this.getCalendars();
        }
        else if (np.getType().equals(PACKET_TYPE_REQUEST_ALL_EVENTS)) {
            NetworkPacket response = new NetworkPacket(PACKET_TYPE_RESPONSE_EVENTS);
            response.set("responseMessage", "aaaaaa");
            device.sendPacket(response);
        }
        else {
            Log.e("CalendarPlugin", "...");
            return false;
        }

        return true;

    }

    public void getCalendars() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            Log.e("CalendarPlugin", "Permission denied");
            return;
        }

        final String[] EVENT_PROJECTION = new String[] {
                CalendarContract.Calendars._ID,                           // 0
                CalendarContract.Calendars.ACCOUNT_NAME,                  // 1
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,         // 2
                CalendarContract.Calendars.OWNER_ACCOUNT                  // 3
        };

        final int PROJECTION_ID_INDEX = 0;
        final int PROJECTION_ACCOUNT_NAME_INDEX = 1;
        final int PROJECTION_DISPLAY_NAME_INDEX = 2;
        final int PROJECTION_OWNER_ACCOUNT_INDEX = 3;


        ContentResolver contentResolver = context.getContentResolver();
        String selection = CalendarContract.Calendars.VISIBLE + " = 1 AND "  + CalendarContract.Calendars.IS_PRIMARY + "=1";
        Cursor cur = contentResolver.query(CalendarContract.Calendars.CONTENT_URI, EVENT_PROJECTION, selection, null, null);

        JSONArray calendars = new JSONArray();
        while (cur.moveToNext()) {
            JSONObject calendar = new JSONObject();
            try {
                calendar.put("calendarId", cur.getLong(PROJECTION_ID_INDEX));
                calendar.put("displayName", cur.getString(PROJECTION_DISPLAY_NAME_INDEX));
                calendar.put("accountName", cur.getString(PROJECTION_ACCOUNT_NAME_INDEX));
                calendar.put("ownerName", cur.getString(PROJECTION_OWNER_ACCOUNT_INDEX));

                calendars.put(calendar);
            } catch (JSONException ignored) {
            }
        }
        NetworkPacket response = new NetworkPacket(PACKET_TYPE_RESPONSE_CALENDARS);
        response.set("calendars1", calendars);


        // wybieranie kalendarza po nazwie
        selection = "((" + CalendarContract.Calendars.ACCOUNT_NAME + " = ?) AND ("
            + CalendarContract.Calendars.ACCOUNT_TYPE + " = ?) AND ("
            + CalendarContract.Calendars.OWNER_ACCOUNT + " = ?))";
        String[] selectionArgs = new String[] {"konradn698@gmail.com", "com.google", "konradn698@gmail.com"};
        cur = contentResolver.query(CalendarContract.Calendars.CONTENT_URI, EVENT_PROJECTION, selection, selectionArgs, null);

        JSONArray calendars2 = new JSONArray();
        while (cur.moveToNext()) {
            JSONObject calendar = new JSONObject();
            try {
                calendar.put("calendarId", cur.getLong(PROJECTION_ID_INDEX));
                calendar.put("displayName", cur.getString(PROJECTION_DISPLAY_NAME_INDEX));
                calendar.put("accountName", cur.getString(PROJECTION_ACCOUNT_NAME_INDEX));
                calendar.put("ownerName", cur.getString(PROJECTION_OWNER_ACCOUNT_INDEX));

                calendars2.put(calendar);
            } catch (JSONException ignored) {
            }
        }
        response.set("calendars2", calendars2);

        // Wyszukiwanie zdarzeń w przedziale czasowym:
        final String[] INSTANCE_PROJECTION = new String[] {
            CalendarContract.Instances.EVENT_ID,      // 0
            CalendarContract.Instances.BEGIN,         // 1
            CalendarContract.Instances.END,           // 2
            CalendarContract.Instances.TITLE          // 3
        };

        final int PROJECTION_EVENT_ID_INDEX = 0;
        final int PROJECTION_EVENT_BEGIN_INDEX = 1;
        final int PROJECTION_EVENT_END_INDEX = 2;
        final int PROJECTION_EVENT_TITLE_INDEX = 3;

        long startMillis = 0;
        long endMillis = 0;
        Calendar beginTime = Calendar.getInstance();
        beginTime.set(2017, 1, 1, 0, 0);
        startMillis = beginTime.getTimeInMillis();
        Calendar endTime = Calendar.getInstance();
        //endTime.set(2017, 11, 15, 8, 00);
        endMillis = endTime.getTimeInMillis();

    // The ID of the recurring event whose instances you are searching for in the Instances table
    // String selection = CalendarContract.Instances.TITLE + " = ?";
    // String[] selectionArgs = new String[] {eventTitle};

    // Construct the query with the desired date range.
        Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, startMillis);
        ContentUris.appendId(builder, endMillis);

        cur =  contentResolver.query(builder.build(), INSTANCE_PROJECTION, null, null, null);

        JSONArray events = new JSONArray();
        while (cur.moveToNext()) {
            JSONObject event = new JSONObject();
            try {
                event.put("eventId", cur.getLong(PROJECTION_EVENT_ID_INDEX));
                event.put("beginTime", cur.getLong(PROJECTION_EVENT_BEGIN_INDEX));
                event.put("endTime", cur.getLong(PROJECTION_EVENT_END_INDEX));
                event.put("title", cur.getString(PROJECTION_EVENT_TITLE_INDEX));

                events.put(event);
            } catch (JSONException ignored) {
                Log.e("CalendarPlugin", "błąd przy events");
            }
        }
        response.set("events", events);

        device.sendPacket(response);
    }

    @Override
    public String[] getSupportedPacketTypes() {
        return new String[]{
            PACKET_TYPE_REQUEST_ALL_CALENDARS,
            PACKET_TYPE_REQUEST_ALL_EVENTS
        };
    }

    @Override
    public String[] getOutgoingPacketTypes() {
        return new String[]{
            PACKET_TYPE_RESPONSE_CALENDARS,
            PACKET_TYPE_RESPONSE_EVENTS
        };
    }

    @Override
    public String[] getRequiredPermissions() {
        return new String[]{
                Manifest.permission.READ_CALENDAR,
                Manifest.permission.WRITE_CALENDAR,
        };
    }
}
