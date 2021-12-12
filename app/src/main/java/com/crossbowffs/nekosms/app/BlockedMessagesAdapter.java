package com.crossbowffs.nekosms.app;

import android.database.Cursor;
import android.graphics.Typeface;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.data.SmsMessageData;
import com.crossbowffs.nekosms.loader.BlockedSmsLoader;
import com.crossbowffs.nekosms.widget.RecyclerCursorAdapter;

import java.util.Set;

/* package */ class BlockedMessagesAdapter extends RecyclerCursorAdapter<BlockedMessagesAdapter.BlockedSmsItemHolder> {

    private final BlockedMessagesFragment mFragment;
    private OnItemClickListener onItemClickListener;

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
    public void onBindViewHolder(BlockedSmsItemHolder holder,Cursor cursor) {
        Set<Long> selectedMsgIds = mFragment.getSelectedMsgIds();
        final SmsMessageData messageData = BlockedSmsLoader.get().getData(cursor, getColumns(), holder.mMessageData);
        holder.mMessageData = messageData;

        int subid = messageData.getSubId();
        String sender = messageData.getSender();
        long timeSent = messageData.getTimeSent();
        String body = messageData.getBody();
        CharSequence timeSentString = DateUtils.getRelativeTimeSpanString(mFragment.getContext(), timeSent);

        holder.mSenderTextView.setText(sender+"  (SIM "+subid+")");
        holder.mTimeSentTextView.setText(timeSentString);
        holder.mBodyTextView.setText(body);
        if (messageData.isRead()) {
            holder.mSenderTextView.setTypeface(null, Typeface.NORMAL);
            holder.mTimeSentTextView.setTypeface(null, Typeface.NORMAL);
            holder.mBodyTextView.setTypeface(null, Typeface.NORMAL);
        } else {
            holder.mSenderTextView.setTypeface(null, Typeface.BOLD_ITALIC);
            holder.mTimeSentTextView.setTypeface(null, Typeface.BOLD_ITALIC);
            holder.mBodyTextView.setTypeface(null, Typeface.BOLD_ITALIC);
        }

        if (selectedMsgIds.contains(messageData.getId())) {
            holder.itemView.setAlpha(0.2f);
        } else {
            holder.itemView.setAlpha(1.0f);
        }

        if (onItemClickListener != null) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickListener.onItemClick(v, messageData);
                }
            });
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    //return： true:不继续传递事件，告诉系统此事件已被处理；false:继续传递事件，上级可继续处理
                    return  onItemClickListener.onItemLongClick(v, messageData);
                }
            });
        }
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(View view, SmsMessageData messageData);
        boolean onItemLongClick(View view, SmsMessageData messageData);
    }

    public class BlockedSmsItemHolder extends RecyclerView.ViewHolder {
        public final TextView mSenderTextView;
        public final TextView mTimeSentTextView;
        public final TextView mBodyTextView;
        public SmsMessageData mMessageData;

        public BlockedSmsItemHolder(View itemView) {
            super(itemView);
            mSenderTextView = (TextView) itemView.findViewById(R.id.blocked_message_sender_textview);
            mTimeSentTextView = (TextView) itemView.findViewById(R.id.blocked_message_time_sent_textview);
            mBodyTextView = (TextView) itemView.findViewById(R.id.blocked_message_body_textview);
        }
    }

}
