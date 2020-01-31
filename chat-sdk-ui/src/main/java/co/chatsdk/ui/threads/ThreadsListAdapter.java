/*
 * Created by Itzik Braun on 12/3/2015.
 * Copyright (c) 2015 deluge. All rights reserved.
 *
 * Last Modification at: 3/12/15 4:27 PM
 */

package co.chatsdk.ui.threads;

import android.content.Context;
import android.graphics.Typeface;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import co.chatsdk.core.dao.Message;
import co.chatsdk.core.dao.Thread;
import co.chatsdk.core.interfaces.ThreadType;
import co.chatsdk.core.session.ChatSDK;
import co.chatsdk.core.utils.Strings;
import co.chatsdk.ui.R;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public class ThreadsListAdapter extends RecyclerView.Adapter<ThreadViewHolder> {

    public static int ThreadCellType = 0;

    protected WeakReference<Context> context;

    protected List<Thread> threads = new ArrayList<>();

    protected HashMap<Thread, String> typing = new HashMap<>();
    protected PublishSubject<Thread> onClickSubject = PublishSubject.create();
    protected PublishSubject<Thread> onLongClickSubject = PublishSubject.create();

    private Typeface mTypefaceNormal;
    private Typeface mTypefaceBold;

    public ThreadsListAdapter(Context context) {
        this.context = new WeakReference(context);

        if (this.context != null) {
            mTypefaceBold = ResourcesCompat.getFont(this.context.get(), R.font.roboto_bold);
            mTypefaceNormal = ResourcesCompat.getFont(this.context.get(), R.font.roboto_regular);
        }
    }

    @Override
    public ThreadViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View row = inflater.inflate(R.layout.view_thread_row, null);
        return new ThreadViewHolder(row);
    }

    @Override
    public void onBindViewHolder(final ThreadViewHolder holder, int position) {

        final Thread thread = threads.get(position);

        holder.nameTextView.setText(Strings.nameForThread(thread));

        holder.itemView.setOnClickListener(view -> onClickSubject.onNext(thread));

        holder.itemView.setOnLongClickListener(view -> {
            onLongClickSubject.onNext(thread);
            return true;
        });

        Message lastMessage = thread.lastMessage();
        if (lastMessage != null) {
            String dateTime = getTimeFormat(getLastMessageDateAsString(lastMessage.getDate().toDate())/*"10:28 1/01/20"*/);
            holder.dateTextView.setText(dateTime);

            String message = "";
            if (lastMessage.getSender().isMe()) {

                String lastMsg = getLastMessageText(thread.lastMessage());

                if (lastMsg.equalsIgnoreCase("Image")) {
                    message = message + "You sent a photo";
                } else {
                    message = message + "You: " + getLastMessageText(thread.lastMessage());
                }

            } else {

                String lastMsg = getLastMessageText(thread.lastMessage());

                if (lastMsg.equalsIgnoreCase("Image")) {
                    message = message + "Sent a photo";
                } else {
                    message = getLastMessageText(thread.lastMessage());
                }
            }
            holder.lastMessageTextView.setText(message);

        } else {
            holder.dateTextView.setText("");
            holder.lastMessageTextView.setText("");
        }

        if (typing.get(thread) != null) {
            holder.lastMessageTextView.setText(String.format(context.get().getString(R.string.__typing), typing.get(thread)));
        }

        int unreadMessageCount = thread.getUnreadMessagesCount();

        if (unreadMessageCount != 0 && (thread.typeIs(ThreadType.Private) || ChatSDK.config().unreadMessagesCountForPublicChatRoomsEnabled)) {

            holder.unreadMessageCountTextView.setText(String.valueOf(unreadMessageCount));
            //holder.unreadMessageCountTextView.setVisibility(View.VISIBLE);

            // Modify Name and Last Message Text Color
            if (ChatSDK.config().chatThreadNameUnReadColor != 0 && context != null) {
                holder.nameTextView.setTextColor(ContextCompat.getColor(context.get(), ChatSDK.config().chatThreadNameUnReadColor));
                holder.lastMessageTextView.setTextColor(ContextCompat.getColor(context.get(), ChatSDK.config().chatThreadNameUnReadColor));
            }

            if (mTypefaceBold != null) {
                holder.nameTextView.setTypeface(mTypefaceBold);
                holder.lastMessageTextView.setTypeface(mTypefaceBold);
            }

            // holder.showUnreadIndicator();
        } else {
            // holder.hideUnreadIndicator();
            // holder.unreadMessageCountTextView.setVisibility(View.INVISIBLE);

            // Modify Name and Last Message Text Color
            if (ChatSDK.config().chatThreadNameUnReadColor != 0 &&
                    ChatSDK.config().chatThreadLastMessageReadColor != 0 && context != null) {
                holder.nameTextView.setTextColor(ContextCompat.getColor(context.get(), ChatSDK.config().chatThreadNameUnReadColor));
                holder.lastMessageTextView.setTextColor(ContextCompat.getColor(context.get(), ChatSDK.config().chatThreadLastMessageReadColor));
            }

            if (mTypefaceNormal != null) {
                holder.nameTextView.setTypeface(mTypefaceNormal);
                holder.lastMessageTextView.setTypeface(mTypefaceNormal);
            }
        }

        if (ChatSDK.config().chatThreadListTimeColor != 0 && context != null) {
            holder.dateTextView.setTextColor(ContextCompat.getColor(context.get(), ChatSDK.config().chatThreadListTimeColor));
        }

        ThreadImageBuilder.load(holder.imageView, thread);
    }

    public String getLastMessageDateAsString (Date date) {
        if (date != null) {
            return Strings.dateTime(date);
        }
        return null;
    }

    public String getLastMessageText(Message lastMessage) {
        String messageText = "";//Strings.t(R.string.no_messages);
        if (lastMessage != null) {
            messageText = Strings.payloadAsString(lastMessage);
        }
        return messageText;
    }

    @Override
    public int getItemViewType(int position) {
        return ThreadCellType;
    }

    @Override
    public int getItemCount() {
        return threads.size();
    }

    public boolean addRow (Thread thread, boolean notify) {
        for (Thread t : threads) {
            if (t.equalsEntity(thread)) {
                return false;
            }
        }

        threads.add(thread);
        if (notify) {
            notifyDataSetChanged();
        }
        return true;
    }

    public void addRow(Thread thread) {
        addRow(thread, true);
    }

    public void setTyping (Thread thread, String message) {
        if (message != null) {
            typing.put(thread, message);
        }
        else {
            typing.remove(thread);
        }
    }

    protected void sort() {
        Collections.sort(threads, new ThreadSorter());
    }

    public void clearData () {
        clearData(true);
    }

    public void clearData (boolean notify) {
        threads.clear();
        if (notify) {
            notifyDataSetChanged();
        }
    }

    public Observable<Thread> onClickObservable () {
        return onClickSubject;
    }

    public Observable<Thread> onLongClickObservable () {
        return onLongClickSubject;
    }


    public void updateThreads (List<Thread> threads) {
        boolean added = false;
        for (Thread t : threads) {
            added = addRow(t, false) || added;
        }
        // Maybe the last text has changed. I think this can lead to a race condition
        // Which causes the thread not to update when a new text comes in
//        if (added) {
            sort();
            notifyDataSetChanged();
//        }
    }

    public void setThreads(List<Thread> threads) {
        clearData(false);
        updateThreads(threads);
    }

    private String getTimeFormat(String dateTime) {
        String convTime = "";

        try {
            SimpleDateFormat format = new SimpleDateFormat("HH:mm dd/MM/yy");
            Date past = format.parse(dateTime);
            Date now = new Date();

            long seconds = TimeUnit.MILLISECONDS.toSeconds(now.getTime() - past.getTime());
            long minutes = TimeUnit.MILLISECONDS.toMinutes(now.getTime() - past.getTime());
            long hours = TimeUnit.MILLISECONDS.toHours(now.getTime() - past.getTime());
            long days = TimeUnit.MILLISECONDS.toDays(now.getTime() - past.getTime());

            String suffix = "ago";

            if (seconds < 60) {
                convTime = seconds + "s " + suffix;
            } else if (minutes < 60) {
                convTime = minutes + "m " + suffix;
            } else if (hours < 24) {
                convTime = hours + "h " + suffix;
            } else if (hours > 24 && hours < 48) {
                convTime = "Yesterday";
            } else if (days >= 7) {
                /*if (days > 360) {
                    convTime = (days / 30) + " Years " + suffix;
                    // Log.e("DEBUG", "----> YEARS " + getFormattedDate(past.getTime()));
                    convTime = getFormattedDate(past.getTime());
                } else if (days > 30) {
                    convTime = (days / 360) + " Months " + suffix;
                    // Log.e("DEBUG", "----> MONTHS " + getFormattedDate(past.getTime()));
                    convTime = getFormattedDate(past.getTime());
                } else {
//                    convTime = (days / 7) + " Week " + suffix;
                    convTime = getFormattedDate(past.getTime());
                }*/
                convTime = getFormattedDate(past.getTime());
            } else if (days < 7) {
                convTime = days + "d " + suffix;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return convTime;
    }

    protected String getFormattedDate(long milliSeconds) {
        Calendar smsTime = Calendar.getInstance();
        smsTime.setTimeInMillis(milliSeconds);

        Calendar now = Calendar.getInstance();

        if (now.get(Calendar.YEAR) == smsTime.get(Calendar.YEAR)) {
            return DateFormat.format("MMM dd", smsTime).toString();
        } else {
            return DateFormat.format("MMM dd, yyyy", smsTime).toString();
        }
    }

}
