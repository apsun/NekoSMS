package com.crossbowffs.nekosms.app;

import android.database.Cursor;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.data.SmsMessageData;
import com.crossbowffs.nekosms.loader.BlockedSmsLoader;
import com.crossbowffs.nekosms.widget.RecyclerCursorAdapter;

/* package */ class BlockedMessagesAdapter extends RecyclerCursorAdapter<BlockedMessagesAdapter.BlockedSmsItemHolder> {
    public class BlockedSmsItemHolder extends RecyclerView.ViewHolder {
        public final TextView mSenderTextView;
        public final TextView mTimeSentTextView;
        public final TextView mBodyTextView;
        public SmsMessageData mMessageData;

        public BlockedSmsItemHolder(View itemView) {
            super(itemView);

            mSenderTextView = (TextView)itemView.findViewById(R.id.blocked_message_sender_textview);
            mTimeSentTextView = (TextView)itemView.findViewById(R.id.blocked_message_time_sent_textview);
            mBodyTextView = (TextView)itemView.findViewById(R.id.blocked_message_body_textview);
        }
    }

    private final BlockedMessagesFragment mFragment;

    public BlockedMessagesAdapter(BlockedMessagesFragment fragment) {
        mFragment = fragment;
    }

    @Override
    public BlockedSmsItemHolder onCreateViewHolder(ViewGroup group, int i) {
        LayoutInflater layoutInflater = LayoutInflater.from(mFragment.getContext());
        View view = layoutInflater.inflate(R.layout.listitem_blocked_messages, group, false);
        return new BlockedSmsItemHolder(view);
    }

    @Override
    protected int[] onBindColumns(Cursor cursor) {
        return BlockedSmsLoader.get().getColumns(cursor);
    }

    @Override
    public void onBindViewHolder(BlockedSmsItemHolder holder, Cursor cursor) {
        final SmsMessageData messageData = BlockedSmsLoader.get().getData(cursor, getColumns(), holder.mMessageData);
        holder.mMessageData = messageData;

        String sender = messageData.getSender();
        long timeSent = messageData.getTimeSent();
        String body = messageData.getBody();
        CharSequence timeSentString = DateUtils.getRelativeTimeSpanString(mFragment.getContext(), timeSent);

        holder.mSenderTextView.setText(sender);
        holder.mTimeSentTextView.setText(timeSentString);
        holder.mBodyTextView.setText(body);
        if (messageData.isRead()) {
            holder.mSenderTextView.setTypeface(null, Typeface.NORMAL);
            holder.mTimeSentTextView.setTypeface(null, Typeface.NORMAL);
            holder.mBodyTextView.setTypeface(null, Typeface.NORMAL);
        } else {
            holder.mSenderTextView.setTypeface(null, Typeface.BOLD);
            holder.mTimeSentTextView.setTypeface(null, Typeface.BOLD);
            holder.mBodyTextView.setTypeface(null, Typeface.BOLD);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFragment.showMessageDetailsDialog(messageData);
            }
        });
    }
}
