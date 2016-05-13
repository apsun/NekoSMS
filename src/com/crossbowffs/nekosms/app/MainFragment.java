package com.crossbowffs.nekosms.app;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.Toast;

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

    public void scrollFabIn() {
        getMainActivity().scrollFabIn();
    }

    public void scrollFabOut() {
        getMainActivity().scrollFabOut();
    }

    protected void onNewIntent(Intent intent) {

    }
}
