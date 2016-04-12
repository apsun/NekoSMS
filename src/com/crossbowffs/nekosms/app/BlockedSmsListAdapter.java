package com.crossbowffs.nekosms.app;

import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.data.SmsMessageData;
import com.crossbowffs.nekosms.database.BlockedSmsDbLoader;

/* package */ class BlockedSmsListAdapter extends RecyclerCursorAdapter<BlockedSmsListAdapter.BlockedSmsItemHolder> {
    public class BlockedSmsItemHolder extends RecyclerView.ViewHolder {
        public final TextView mSenderTextView;
        public final TextView mTimeSentTextView;
        public final TextView mBodyTextView;
        public SmsMessageData mMessageData;

        public BlockedSmsItemHolder(View itemView) {
            super(itemView);

            mSenderTextView = (TextView)itemView.findViewById(R.id.listitem_blockedsms_list_sender_textview);
            mTimeSentTextView = (TextView)itemView.findViewById(R.id.listitem_blockedsms_list_timesent_textview);
            mBodyTextView = (TextView)itemView.findViewById(R.id.listitem_blockedsms_list_body_textview);
        }
    }

    private static final String TAG = BlockedSmsListAdapter.class.getSimpleName();
    private final BlockedSmsListFragment mFragment;

    public BlockedSmsListAdapter(BlockedSmsListFragment fragment) {
        mFragment = fragment;
    }

    @Override
    public BlockedSmsItemHolder onCreateViewHolder(ViewGroup group, int i) {
        LayoutInflater layoutInflater = LayoutInflater.from(mFragment.getContext());
        View view = layoutInflater.inflate(R.layout.listitem_blockedsms_list, group, false);
        return new BlockedSmsItemHolder(view);
    }

    @Override
    protected int[] onBindColumns(Cursor cursor) {
        return BlockedSmsDbLoader.getColumns(cursor);
    }

    @Override
    public void onBindViewHolder(BlockedSmsItemHolder holder, Cursor cursor) {
        final SmsMessageData messageData = BlockedSmsDbLoader.getMessageData(cursor, getColumns(), holder.mMessageData);
        holder.mMessageData = messageData;

        String sender = messageData.getSender();
        long timeSent = messageData.getTimeSent();
        String body = messageData.getBody();
        CharSequence timeSentString = DateUtils.getRelativeTimeSpanString(mFragment.getContext(), timeSent);

        holder.mSenderTextView.setText(sender);
        holder.mTimeSentTextView.setText(timeSentString);
        holder.mBodyTextView.setText(body);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFragment.showMessageDetailsDialog(messageData);
            }
        });
    }
}
