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

public class BaseFragment extends Fragment {
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

    public Snackbar makeSnackbar(int textId) {
        return getMainActivity().makeSnackbar(textId);
    }

    public Snackbar makeSnackbar(int textId, int actionTextId, View.OnClickListener listener) {
        return makeSnackbar(textId).setAction(actionTextId, listener);
    }

    public void showSnackbar(int textId, int actionTextId, View.OnClickListener listener) {
        makeSnackbar(textId, actionTextId, listener).show();
    }

    public Toast makeToast(int textId) {
        Context context = getContext();
        if (context == null) return null;
        return Toast.makeText(context, textId, Toast.LENGTH_SHORT);
    }

    public void showToast(int textId) {
        Toast toast = makeToast(textId);
        if (toast != null) {
            makeToast(textId).show();
        }
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

    public void setFabCallback(View.OnClickListener listener) {
        getMainActivity().setFabCallback(listener);
    }

    public void scrollFabIn() {
        getMainActivity().scrollFabIn();
    }

    public void scrollFabOut() {
        getMainActivity().scrollFabOut();
    }

    public void finishTryTransition() {
        getMainActivity().finishTryTransition();
    }

    public void requestPermissions(int requestCode, String... permissions) {
        getMainActivity().requestPermissions(requestCode, permissions);
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
