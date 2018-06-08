package com.crossbowffs.nekosms.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

public final class ContactUtils {
    private ContactUtils() { }

    public static boolean isContact(Context context, String number) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        ContentResolver contentResolver = context.getContentResolver();
        try (Cursor cursor = contentResolver.query(uri, new String[0], null, null, null)) {
            return cursor != null && cursor.getCount() > 0;
        }
    }
}
