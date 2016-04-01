package com.crossbowffs.nekosms.app;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

public abstract class DialogAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {
    private final Context mContext;
    private final int mProgressMessageId;
    private ProgressDialog mDialog;

    public DialogAsyncTask(Context context, int progressMessageId) {
        mContext = context;
        mProgressMessageId = progressMessageId;
    }

    @Override
    protected void onPreExecute() {
        ProgressDialog dialog = new ProgressDialog(mContext);
        dialog.setMessage(mContext.getString(mProgressMessageId));
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.show();
        mDialog = dialog;
    }

    @Override
    protected void onPostExecute(Result result) {
        mDialog.dismiss();
    }
}
