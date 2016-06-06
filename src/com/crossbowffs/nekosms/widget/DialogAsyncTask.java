package com.crossbowffs.nekosms.widget;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;

public abstract class DialogAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> implements DialogInterface.OnCancelListener {
    private final Context mContext;
    private final int mProgressMessageId;
    private final boolean mCancelable;
    private ProgressDialog mDialog;

    public DialogAsyncTask(Context context, int progressMessageId) {
        this(context, progressMessageId, false);
    }

    public DialogAsyncTask(Context context, int progressMessageId, boolean cancelable) {
        mContext = context;
        mProgressMessageId = progressMessageId;
        mCancelable = cancelable;
    }

    @Override
    protected void onPreExecute() {
        ProgressDialog dialog = new ProgressDialog(mContext);
        dialog.setMessage(mContext.getString(mProgressMessageId));
        dialog.setIndeterminate(true);
        dialog.setCancelable(mCancelable);
        if (mCancelable) {
            dialog.setOnCancelListener(this);
        }
        dialog.show();
        mDialog = dialog;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        cancel(true);
    }

    @Override
    protected void onPostExecute(Result result) {
        mDialog.dismiss();
    }

    @Override
    protected void onCancelled(Result result) {
        mDialog.dismiss();
    }
}
