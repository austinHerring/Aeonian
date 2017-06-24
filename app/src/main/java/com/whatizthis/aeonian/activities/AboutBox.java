package com.whatizthis.aeonian.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;
import android.view.InflateException;
import android.view.View;

import com.whatizthis.aeonian.R;
import com.whatizthis.aeonian.activities.AeonianActivity;

/**
 * Creates and displays an "about" box.
 */
public class AboutBox {
    private static final String TAG = AeonianActivity.TAG;

    /**
     * Retrieves the application's version string.
     */
    private static String getVersionString(Context context) {
        PackageManager pman = context.getPackageManager();
        String packageName = context.getPackageName();
        try {
            PackageInfo pinfo = pman.getPackageInfo(packageName, 0);
            Log.d(TAG, "Found version " + pinfo.versionName + " for " + packageName);
            return pinfo.versionName;
        } catch (NameNotFoundException nnfe) {
            Log.w(TAG, "Unable to retrieve package info for " + packageName);
            return "(unknown)";
        }
    }

    /**
     * Displays the About box.  An AlertDialog is created in the calling activity's context.
     * <p>
     * The box will disappear if the "OK" button is touched, if an area outside the box is
     * touched, if the screen is rotated ... doing just about anything makes it disappear.
     */
    public static void display(Activity caller) {
        String versionStr = getVersionString(caller);
        String aboutHeader = caller.getString(R.string.app_name) + " v" + versionStr;

        // Manually inflate the view that will form the body of the dialog.
        View aboutView;
        try {
            aboutView = caller.getLayoutInflater().inflate(R.layout.about, null);
        } catch (InflateException ie) {
            Log.e(TAG, "Exception while inflating about box: " + ie.getMessage());
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(caller);
        builder.setTitle(aboutHeader);
        builder.setIcon(R.mipmap.ic_launcher);
        builder.setCancelable(true);        // implies setCanceledOnTouchOutside
        builder.setPositiveButton(R.string.ok, null);
        builder.setView(aboutView);
        builder.show();
    }
}
