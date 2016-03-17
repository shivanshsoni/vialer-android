package com.voipgrid.vialer;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;

/**
 * Class for Android 6.0+ related microphone permissions.
 */
public class MicrophonePermission {

    public static final int GRANTED = 0;
    public static final int DENIED = 1;
    public static final int BLOCKED = 2;
    private static final int REQUEST_PERMISSION_SETTING = 125;

    public static final String mPermissionToCheck = Manifest.permission.RECORD_AUDIO;
    public static final String[] mPermissions = new String[] {Manifest.permission.RECORD_AUDIO};

    /**
     * Function to check if the we have the microphone permission.
     * @param context Context needed for the check.
     * @return Whether or not we have permission.
     */
    public static boolean hasPermission(Context context) {
        return getPermissionStatus((Activity) context, mPermissionToCheck) == GRANTED;
    }

    private static int getPermissionStatus(Activity activity, String androidPermissionName) {
        if(ContextCompat.checkSelfPermission(activity, androidPermissionName) != PackageManager.PERMISSION_GRANTED) {
            if(!ActivityCompat.shouldShowRequestPermissionRationale(activity, androidPermissionName)){
                return BLOCKED;
            }
            return DENIED;
        }
        return GRANTED;
    }

    private static void showPermissionBlockedDialog(final Activity activity){
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(activity.getString(R.string.permission_microphone_dialog_title));
        builder.setMessage(activity.getString(R.string.permission_microphone_missing_message));
        builder.setPositiveButton(activity.getString(R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        builder.setNegativeButton(activity.getString(R.string.permission_settings),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();

                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                        intent.setData(uri);
                        activity.startActivityForResult(intent, REQUEST_PERMISSION_SETTING);
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Function to ask the user for the microphone permissions.
     * @param activity The activity where to show the permissions dialogs.
     */
    public static void askForPermission(final Activity activity) {
        int permissionStatus = getPermissionStatus(activity, mPermissionToCheck);

        // Request code for the callback verifying in the Activity.
        final int requestCode = activity.getResources().getInteger(
                R.integer.microphone_permission_request_code);
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                MicrophonePermission.mPermissionToCheck)) {
            // Function to show a dialog that explains the permissions.
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(activity.getString(R.string.permission_microphone_dialog_title));
            builder.setMessage(activity.getString(R.string.permission_microphone_dialog_message));
            builder.setPositiveButton(activity.getString(R.string.ok),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            ActivityCompat.requestPermissions(activity, mPermissions, requestCode);
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            if (permissionStatus == DENIED) {
                ActivityCompat.requestPermissions(activity, mPermissions, requestCode);
            }
            else if(permissionStatus == BLOCKED) {
                showPermissionBlockedDialog(activity);
            }
        }
    }
}