package com.crossbowffs.nekosms.app;

import android.annotation.SuppressLint;
import android.app.LoaderManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.InputType;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.consts.BroadcastConsts;
import com.crossbowffs.nekosms.data.SmsMessageData;
import com.crossbowffs.nekosms.loader.BlockedSmsLoader;
import com.crossbowffs.nekosms.loader.DatabaseException;
import com.crossbowffs.nekosms.loader.InboxSmsLoader;
import com.crossbowffs.nekosms.provider.DatabaseContract;
import com.crossbowffs.nekosms.utils.MapUtils;
import com.crossbowffs.nekosms.utils.Xlog;
import com.crossbowffs.nekosms.utils.XposedUtils;
import com.crossbowffs.nekosms.widget.ListRecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class BlockedMessagesFragment extends MainFragment implements LoaderManager.LoaderCallbacks<Cursor> , ActionMode.Callback {
    public static final String ARG_MESSAGE_URI = "message_uri";

    private ListRecyclerView mRecyclerView;
    private View mEmptyView;
    private BlockedMessagesAdapter mAdapter;

    private ActionMode actionMode;
    private SearchView searchView;
    private Set<Long> selectedMsgIds = new HashSet<>();

    public ActionMode getActionMode() {
        return actionMode;
    }

    public Set<Long> getSelectedMsgIds() {
        return selectedMsgIds;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        if(actionMode!=null){
            actionMode.finish();
        }
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_blocked_messages, container, false);
        mRecyclerView = view.findViewById(R.id.blocked_messages_recyclerview);
        mEmptyView = view.findViewById(android.R.id.empty);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAdapter = new BlockedMessagesAdapter(this);
        LoaderManager loaderManager = getLoaderManager();
        loaderManager.initLoader(0, null, this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setEmptyView(mEmptyView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        //registerForContextMenu(mRecyclerView);
        disableFab();
        setTitle(R.string.blocked_messages);
        onNewArguments(getArguments());
        BlockedSmsLoader.get().markAllSeen(getContext());

        mAdapter.setOnItemClickListener(new BlockedMessagesAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, SmsMessageData msg_data) {
                if (actionMode != null) {
                    addOrRemove(msg_data.getId());// 如果处于多选状态，则进入多选状态的逻辑
                } else {
                    showMessageDetailsDialog(msg_data);// 如果不是多选状态，则展示信息详情
                }
            }

            @Override
            public boolean onItemLongClick(View view, SmsMessageData msg_data) {
                if (actionMode == null) {
                    actionMode = getMainActivity().startSupportActionMode(BlockedMessagesFragment.this);
                }
                return false;
            }
        });
    }

    private void addOrRemove(long msgid) {
        // 如果包含，则取消选择；如果不包含，则添加选择
        if (selectedMsgIds.contains(msgid)) {
            selectedMsgIds.remove(msgid);
        } else {
            selectedMsgIds.add(msgid);
        }

        // 如果没有选中任何的item，则退出多选模式；否则更新选中与否的背景
        if (selectedMsgIds.size() == 0) {
            actionMode.finish();
        } else {
            actionMode.setTitle(getString(R.string.message_multi_selected,selectedMsgIds.size()));
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onNewArguments(Bundle args) {
        if (args == null) {
            return;
        }

        Uri messageUri = args.getParcelable(ARG_MESSAGE_URI);
        if (messageUri != null) {
            args.remove(ARG_MESSAGE_URI);
            showMessageDetailsDialog(messageUri);
            BlockedSmsLoader.get().markAllSeen(getContext());
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.options_blocked_messages, menu);
        inflater.inflate(R.menu.options_debug, menu);
        searchView = (SearchView) menu.findItem(R.id.app_bar_search).getActionView();

        searchView.setMaxWidth(500);
        searchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                refreshQueryResultAndUI(query);
                //点击提交后，隐藏输入法
                InputMethodManager imm = (InputMethodManager) getMainActivity().getSystemService((Context.INPUT_METHOD_SERVICE));
                if (imm != null) {
                    imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
                    searchView.clearFocus();
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                refreshQueryResultAndUI(query);
                return true;
            }
        });
    }

    //刷新查询后的列表
    private void refreshQueryResultAndUI(String query) {
        if(getContext()==null)return;
        //查询 发送者 或 短信文本 匹配的条目
        mAdapter.changeCursor(BlockedSmsLoader.get().query(getContext(),
                DatabaseContract.BlockedMessages.BODY + " LIKE ? OR " + DatabaseContract.BlockedMessages.SENDER + " LIKE ?",
                new String[]{"%" + query + "%", "%" + query + "%"},
                DatabaseContract.BlockedMessages.TIME_SENT + " DESC"));
    }

    //刷新列表，在搜索删除后等场景
    private void refreshList() {
        if(getContext()==null)return;
        CharSequence query="";
        if(searchView!=null){
            query=searchView.getQuery();
        }
        //mAdapter = new BlockedMessagesAdapter(this);
        mAdapter.changeCursor(BlockedSmsLoader.get().query(getContext(),
                DatabaseContract.BlockedMessages.BODY + " LIKE ? OR " + DatabaseContract.BlockedMessages.SENDER + " LIKE ?",
                new String[]{"%" + query + "%", "%" + query + "%"},
                DatabaseContract.BlockedMessages.TIME_SENT + " DESC"));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_create_test:
                showCreateTestSmsDialog();
                return true;
            case R.id.menu_item_clear_blocked:
                showConfirmClearDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(
            getContext(),
            DatabaseContract.BlockedMessages.CONTENT_URI,
            DatabaseContract.BlockedMessages.ALL, null, null,
            DatabaseContract.BlockedMessages.TIME_SENT + " DESC"
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.changeCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.changeCursor(null);
    }

    private void showConfirmClearDialog() {
        Context context = getContext();
        if (context == null) return;

        new AlertDialog.Builder(context)
            .setIcon(R.drawable.ic_warning_white_24dp)
            .setTitle(R.string.confirm_clear_messages_title)
            .setMessage(R.string.confirm_clear_messages_message)
            .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //删除当前页面展示的所有信息
                    deleteSms(mAdapter.getAllItemId());
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void showCreateTestSmsDialog() {
        Context context = getContext();
        if (context == null) return;

        //build view
        final EditText et_sender = new EditText(context);
        final EditText et_body = new EditText(context);
        final EditText et_subid = new EditText(context);
        final EditText et_timesend = new EditText(context);
        final EditText et_timereceived = new EditText(context);

        et_sender.setHint("发送者");
        et_body.setHint("内容");
        et_subid.setHint("SIM ID, 默认 1");
        et_timesend.setHint("发送时间");
        et_timereceived.setHint("接收时间");
        et_subid.setInputType(InputType.TYPE_CLASS_NUMBER);

        LinearLayout.LayoutParams llp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.setMargins(0,50,0,0);
        et_sender.setLayoutParams(llp);
        et_body.setLayoutParams(llp);
        et_subid.setLayoutParams(llp);
        et_timesend.setLayoutParams(llp);
        et_timereceived.setLayoutParams(llp);

        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setPadding(50,50,50,50);
        ll.addView(et_sender);
        ll.addView(et_body);
        ll.addView(et_subid);
        ll.addView(et_timesend);
        ll.addView(et_timereceived);

        //build data
        et_sender.setText("106"+(new Random().nextInt(900_000_000)+100_000_000));
        et_body.setText("【测试】您好，验证码为"+(new Random().nextInt(900_000)+100_000));
        et_subid.setText(""+(new Random().nextInt(2)+1));
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        et_timesend.setText(simpleDateFormat.format(new Date()));
        et_timereceived.setText(simpleDateFormat.format(new Date()));

        new AlertDialog.Builder(context)
                .setTitle("创建测试短信")
                .setView(ll)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        createTestSms(et_sender.getText().toString(),et_body.getText().toString(),et_subid.getText().toString(),et_timesend.getText().toString(),et_timereceived.getText().toString());
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showMessageDetailsDialog(Uri uri) {
        Context context = getContext();
        SmsMessageData messageData = BlockedSmsLoader.get().query(context, uri);
        if (messageData != null) {
            showMessageDetailsDialog(messageData);
        } else {
            // This can occur if the user deletes the message, then opens the notification
            showSnackbar(R.string.load_message_failed);
        }
    }

    public void showMessageDetailsDialog(final SmsMessageData messageData) {
        Context context = getContext();
        if (context == null) return;

        // Dismiss notification if present
        NotificationHelper.cancelNotification(context, messageData.getId());

        final long smsId = messageData.getId();
        String sender = messageData.getSender();
        String body = messageData.getBody();
        long timeSent = messageData.getTimeSent();
        String escapedBody = Html.escapeHtml(body).replace("&#10;", "<br>");
        String timeSentString = DateUtils.getRelativeDateTimeString(context, timeSent, 0, DateUtils.WEEK_IN_MILLIS, 0).toString();
        @SuppressLint("StringFormatMatches")
        Spanned html = Html.fromHtml(getString(R.string.format_message_details, sender, timeSentString, escapedBody,messageData.getSubId()));

        AlertDialog dialog = new AlertDialog.Builder(context)
            .setMessage(html)
            .setNeutralButton(R.string.close, null)
            .setPositiveButton(R.string.restore, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    restoreSms(smsId);
                }
            })
            .setNegativeButton(R.string.delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    deleteSms(smsId);
                }
            })
            .show();
        //设置WEB链接自动识别
        TextView textView = dialog.findViewById(android.R.id.message);
        textView.setAutoLinkMask(android.text.util.Linkify.WEB_URLS);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        BlockedSmsLoader.get().setReadStatus(context, messageData.getId(), true);
    }

    private void startXposedActivity(XposedUtils.Section section) {
        Context context = getContext();
        if (context == null) return;

        if (!XposedUtils.startXposedActivity(context, section)) {
            showSnackbar(R.string.xposed_not_installed);
        }
    }

    private void restoreSms(long smsId) {
        Context context = getContext();
        if (context == null) return;

        // We've obviously seen the message, so remove the notification
        NotificationHelper.cancelNotification(context, smsId);

        // Load message content (so we can undo)
        final SmsMessageData messageData = BlockedSmsLoader.get().query(context, smsId);
        if (messageData == null) {
            Xlog.e("Failed to restore message: could not load data");
            showSnackbar(R.string.load_message_failed);
            return;
        }

        // Write message to the inbox
        final Uri inboxSmsUri;
        try {
            inboxSmsUri = InboxSmsLoader.writeMessage(context, messageData);
        } catch (SecurityException e) {
            Xlog.e("Do not have permissions to write SMS");
            showSnackbar(R.string.must_enable_xposed_module, R.string.enable, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startXposedActivity(XposedUtils.Section.MODULES);
                }
            });
            return;
        } catch (DatabaseException e) {
            Xlog.e("Failed to restore message: could not write to SMS inbox");
            showSnackbar(R.string.message_restore_failed);
            return;
        }

        // Delete the message after we successfully write it to the inbox
        BlockedSmsLoader.get().delete(context, smsId);

        showSnackbar(R.string.message_restored, R.string.undo, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context2 = getContext();
                if (context2 == null) return;
                BlockedSmsLoader.get().insert(context2, messageData);
                InboxSmsLoader.deleteMessage(context2, inboxSmsUri);
            }
        });
    }

    private void restoreSms(Set<Long> smsIds) {
        Context context = getContext();
        if (context == null) return;

        int num=0;
        for (long smsId : smsIds) {
            // We've obviously seen the message, so remove the notification
            NotificationHelper.cancelNotification(context, smsId);

            // Load message content (so we can undo)
            final SmsMessageData messageData = BlockedSmsLoader.get().query(context, smsId);
            if (messageData == null) {
                Xlog.e("Failed to restore message: could not load data");
                continue;
            }

            // Write message to the inbox
            final Uri inboxSmsUri;
            try {
                inboxSmsUri = InboxSmsLoader.writeMessage(context, messageData);
                num++;
            } catch (SecurityException e) {
                Xlog.e("Failed to restore message: do not have permissions to write SMS");
                continue;
            } catch (DatabaseException e) {
                Xlog.e("Failed to restore message: could not write to SMS inbox");
                continue;
            }

            // Delete the message after we successfully write it to the inbox
            BlockedSmsLoader.get().delete(context, smsId);
        }
        showSnackbar(getString(R.string.message_multi_restored, num));
    }

    private void deleteSms(long smsId) {
        Context context = getContext();
        if (context == null) return;

        // We've obviously seen the message, so remove the notification
        NotificationHelper.cancelNotification(context, smsId);

        // Load message content (for undo), then delete it
        final SmsMessageData messageData = BlockedSmsLoader.get().queryAndDelete(context, smsId);
        if (messageData == null) {
            Xlog.e("Failed to delete message: could not load data");
            showSnackbar(R.string.load_message_failed);
            return;
        }

        showSnackbar(R.string.message_deleted, R.string.undo, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context2 = getContext();
                if (context2 == null) return;
                BlockedSmsLoader.get().insert(context2, messageData);
            }
        });
    }

    private void deleteSms(Set<Long> smsIds) {
        Context context = getContext();
        if (context == null) return;

        String ids_str="-1";
        for (long smsId : smsIds) {
            NotificationHelper.cancelNotification(context, smsId);
            ids_str=ids_str+","+smsId;
        }
        int num = BlockedSmsLoader.get().delete(context, DatabaseContract.BlockedMessages._ID + " in("+ids_str+")", null);
        refreshList();
        showSnackbar(getString(R.string.message_multi_deleted, num));
    }

    private void setSmsStatusToRead(Set<Long> smsIds) {
        Context context = getContext();
        if (context == null) return;

        String ids_str="-1";
        for (long smsId : smsIds) {
            NotificationHelper.cancelNotification(context, smsId);
            ids_str=ids_str+","+smsId;
        }

        ContentValues values = MapUtils.contentValuesForSize(2);
        values.put(DatabaseContract.BlockedMessages.READ,  1 );
        values.put(DatabaseContract.BlockedMessages.SEEN, 1);
        //批量已读
        int num = BlockedSmsLoader.get().update(context, values,DatabaseContract.BlockedMessages._ID + " in("+ids_str+")", null);
        refreshList();
        //showSnackbar(getString(R.string.xxx, num));
    }

    private void createTestSms(String sender,String body,String str_subid,String sendDate,String receivedDate) {
        Context context = getContext();
        if (context == null) return;
        int subid=1;
        long timeSent=System.currentTimeMillis();
        long timeReceived=timeSent;

        try{
            subid=Integer.parseInt(str_subid);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            timeSent = simpleDateFormat.parse(sendDate).getTime();
            timeReceived = simpleDateFormat.parse(receivedDate).getTime();
        } catch (NumberFormatException e){
            subid=1;
        } catch (ParseException e) {
            timeSent=System.currentTimeMillis();
            timeReceived=timeSent;
        }
        SmsMessageData message = new SmsMessageData();
        message.setSender(sender);
        message.setBody(body);
        message.setTimeSent(timeSent);
        message.setTimeReceived(timeReceived);
        message.setRead(false);
        message.setSeen(false);
        message.setSubId(subid);

        Uri uri = BlockedSmsLoader.get().insert(context, message);
        Intent intent = new Intent(BroadcastConsts.ACTION_RECEIVE_SMS);
        intent.setComponent(new ComponentName(context, BroadcastConsts.RECEIVER_NAME));
        intent.putExtra(BroadcastConsts.EXTRA_MESSAGE, uri);
        context.sendBroadcast(intent);

        //mAdapter.notifyDataSetChanged();
        //Toast.makeText(context,"Test Sms",Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        if (actionMode == null) {
            actionMode = mode;
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.actionmode_blocked_messages, menu);
            actionMode.setTitle(getString(R.string.message_multi_selected,selectedMsgIds.size()));

            mAdapter.notifyDataSetChanged();// 更新列表界面，否则无法显示已选的item
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_action_delete:// 删除选中项
                deleteSms(selectedMsgIds);
                mode.finish();
                return true;
            case R.id.menu_action_restore:// 恢复选中项
                restoreSms(selectedMsgIds);
                mode.finish();
                return true;
            case R.id.menu_action_all_select:// 全选
                selectedMsgIds=mAdapter.getAllItemId();
                actionMode.setTitle(getString(R.string.message_multi_selected,selectedMsgIds.size()));
                mAdapter.notifyDataSetChanged();
                return true;
            case R.id.menu_action_inv_select:// 反选
                Set<Long> allids = mAdapter.getAllItemId();
                if(allids.removeAll(selectedMsgIds)){
                    selectedMsgIds =allids;
                }
                actionMode.setTitle(getString(R.string.message_multi_selected,selectedMsgIds.size()));
                mAdapter.notifyDataSetChanged();
                return true;
            case R.id.menu_action_read:// 选中项设为已读
                setSmsStatusToRead(selectedMsgIds);
                mode.finish();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        actionMode = null;
        selectedMsgIds.clear();
        mAdapter.notifyDataSetChanged();
    }
}
