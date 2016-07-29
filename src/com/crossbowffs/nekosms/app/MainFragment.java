package com.crossbowffs.nekosms.app;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.Toast;
import com.crossbowffs.nekosms.utils.PermissionUtils;

public class MainFragment extends Fragment {
    public MainActivity getMainActivity() {
        return (MainActivity)getActivity();
    }

    @Override
    public Context getContext() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return super.getContext();
        } else {
            return getActivity();
        }
    }

    private Snackbar makeSnackbar(int textId) {
        return getMainActivity().makeSnackbar(textId);
    }

    private Snackbar makeSnackbar(int textId, int actionTextId, View.OnClickListener listener) {
        return makeSnackbar(textId).setAction(actionTextId, listener);
    }

    public void showSnackbar(int textId, int actionTextId, View.OnClickListener listener) {
        makeSnackbar(textId, actionTextId, listener).show();
    }

    private Toast makeToast(int textId) {
        return Toast.makeText(getContext(), textId, Toast.LENGTH_SHORT);
    }

    public void showToast(int textId) {
        makeToast(textId).show();
    }

    public Intent getIntent() {
        return getMainActivity().getIntent();
    }

    public void setTitle(int titleId) {
        getMainActivity().setTitle(titleId);
    }

    public void setFabVisible(boolean visible) {
        getMainActivity().setFabVisible(visible);
    }

    public void setFabIcon(int iconId) {
        getMainActivity().setFabIcon(iconId);
    }

    public void setFabCallback(View.OnClickListener listener) {
        getMainActivity().setFabCallback(listener);
    }

    public void requestPermissionsCompat(String permission, int requestCode) {
        requestPermissionsCompat(new String[] {permission}, requestCode);
    }

    public void requestPermissionsCompat(String[] permissions, int requestCode) {
        // Unlike requestPermissions(), this will not display the request dialog
        // on Android 23+ if the permissions have already been granted.
        int[] grantResults = new int[permissions.length];
        boolean hasPermissions = PermissionUtils.checkPermissions(getContext(), permissions, grantResults);
        if (!hasPermissions && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, requestCode);
        } else {
            onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        for (int permissionStatus : grantResults) {
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                onRequestPermissionsResult(requestCode, false);
                return;
            }
        }

        // Empty permissions means that the request was cancelled
        // by the user, so the permissions were not granted
        boolean requestCancelled = (grantResults.length == 0);
        onRequestPermissionsResult(requestCode, !requestCancelled);
    }

    protected void onRequestPermissionsResult(int requestCode, boolean granted) {

    }

    protected void onNewIntent(Intent intent) {

    }
}
