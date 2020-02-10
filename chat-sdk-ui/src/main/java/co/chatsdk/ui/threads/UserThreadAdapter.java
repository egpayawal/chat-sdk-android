package co.chatsdk.ui.threads;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.drawee.view.SimpleDraweeView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import co.chatsdk.core.interfaces.UserListItem;
import co.chatsdk.core.session.ChatSDK;
import co.chatsdk.ui.R;
import io.reactivex.subjects.PublishSubject;
import timber.log.Timber;

/**
 * Created by Eraño Payawal on 2020-02-06.
 * hunterxer31@gmail.com
 */
public class UserThreadAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Filterable {

    protected static final int TYPE_USER = 0;
    protected static final int TYPE_HEADER = 1;

    protected List<UserListItem> items = new ArrayList<>();
    private List<UserListItem> contactListFiltered;
    protected List<String> headers = new ArrayList<>();

    protected SparseBooleanArray selectedUsersPositions = new SparseBooleanArray();

    protected final PublishSubject<Object> onClickSubject = PublishSubject.create();
    protected final PublishSubject<Object> onLongClickSubject = PublishSubject.create();
    protected final PublishSubject<List<UserListItem>> onToggleSubject = PublishSubject.create();

    private Context mContext;

    protected class HeaderViewHolder extends RecyclerView.ViewHolder {

        TextView textView;

        public HeaderViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.text_header);
        }
    }

    protected class UserViewHolder extends RecyclerView.ViewHolder {

        protected SimpleDraweeView avatarImageView;
        protected TextView nameTextView;
        protected TextView statusTextView;

        public UserViewHolder(View view) {
            super(view);

            nameTextView = view.findViewById(R.id.text_name);
            statusTextView = view.findViewById(R.id.text_status);
            avatarImageView = view.findViewById(R.id.image_avatar);

        }

    }

    public UserThreadAdapter(Context context, List<UserListItem> users) {
        this.mContext = context;
        if (users == null) {
            users = new ArrayList<>();
        }

        setUsers(users);
    }

    @Override
    public int getItemViewType(int position) {
        Object item = items.get(position);
        if (headers.contains(item)) {
            return TYPE_HEADER;
        } else {
            return TYPE_USER;
        }
    }

    @Override
    public int getItemCount() {
        return contactListFiltered.size();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            View row = inflater.inflate(R.layout.view_contact_row_header, null);
            return new HeaderViewHolder(row);
        } else if (viewType == TYPE_USER) {
            View row = inflater.inflate(R.layout.view_contact_row, null);
            return new UserViewHolder(row);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {

        int type = getItemViewType(position);
        final Object item = contactListFiltered.get(position);

        if (type == TYPE_HEADER) {
            HeaderViewHolder hh = (HeaderViewHolder) holder;
            String header = (String) item;
            hh.textView.setText(header);
        }

        if (type == TYPE_USER) {
            UserViewHolder userViewHolder = (UserViewHolder) holder;
            UserListItem userListItem = (UserListItem) item;

            userViewHolder.nameTextView.setText(userListItem.getName());
            userViewHolder.statusTextView.setText(userListItem.getStatus());

            Timber.v("User: " + userListItem.getName() + " Availability: " + userListItem.getAvailability());

            userViewHolder.avatarImageView.setImageURI(userListItem.getAvatarURL());

            // Modify Name and Last Message Text Color
            if (ChatSDK.config().chatThreadNameUnReadColor != 0 &&
                    ChatSDK.config().chatThreadLastMessageReadColor != 0 && mContext != null) {
                userViewHolder.nameTextView.setTextColor(ContextCompat.getColor(mContext, ChatSDK.config().chatThreadNameUnReadColor));
                userViewHolder.statusTextView.setTextColor(ContextCompat.getColor(mContext, ChatSDK.config().chatThreadLastMessageReadColor));
            }

        }

        holder.itemView.setOnClickListener(view -> onClickSubject.onNext(item));
        holder.itemView.setOnLongClickListener(view -> {
            onLongClickSubject.onNext(item);
            return true;
        });

    }

    public Object getItem(int i) {
        return contactListFiltered.get(i);
    }

    public void setUsers(List<UserListItem> users, boolean sort) {
        this.items.clear();

        if (sort) {
            sortList(users);
        }

        for (UserListItem item : users) {
            addUser(item);
        }

        contactListFiltered = users;

        notifyDataSetChanged();
    }

    public void addUser(UserListItem user) {
        addUser(user, false);
    }

    public List<UserListItem> getItems() {
        return items;
    }

    public void addUser(UserListItem user, boolean notify) {
        addUser(user, -1, notify);
    }

    public void addUser(UserListItem user, int atIndex, boolean notify) {
        if (!items.contains(user)) {
            if (atIndex >= 0) {
                items.add(atIndex, user);
            } else {
                items.add(user);
            }
            if (notify) {
                notifyDataSetChanged();
            }
        }
    }

    public List<UserListItem> getSelectedUsers() {
        List<UserListItem> users = new ArrayList<>();
        for (int i = 0; i < getSelectedCount(); i++) {
            int pos = getSelectedUsersPositions().keyAt(i);
            if (items.get(pos) instanceof UserListItem) {
                users.add(((UserListItem) items.get(pos)));
            }
        }
        return users;
    }

    public void setUsers(List<UserListItem> userItems) {
        setUsers(userItems, false);
    }

    /**
     * Clear the list.
     * <p>
     * Calls notifyDataSetChanged.
     * *
     */
    public void clear() {
        items.clear();
        notifyDataSetChanged();
    }

    public UserListItem userAtPosition(int position) {
        Object item = getItem(position);
        if (item instanceof UserListItem) {
            return (UserListItem) item;
        } else {
            return null;
        }
    }

    /**
     * Sorting a given list using the internal comparator.
     * <p>
     * This will be used each time after setting the user item
     * *
     */
    protected void sortList(List<UserListItem> list) {
        Comparator comparator = (Comparator<UserListItem>) (u1, u2) -> {
            String s1 = "";
            if (u1 != null && u1.getName() != null) {
                s1 = u1.getName();
            }
            String s2 = "";
            if (u2 != null && u2.getName() != null) {
                s2 = u2.getName();
            }

            return s1.compareToIgnoreCase(s2);
        };
        Collections.sort(list, comparator);
    }

    public SparseBooleanArray getSelectedUsersPositions() {
        return selectedUsersPositions;
    }

    /**
     * Get the amount of selected users.
     * * *
     */
    public int getSelectedCount() {
        return selectedUsersPositions.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String charString = charSequence.toString();
                if (charString.isEmpty()) {
                    contactListFiltered = items;
                } else {
                    List<UserListItem> filteredList = new ArrayList<>();
                    for (UserListItem row : items) {

                        // name match condition. this might differ depending on your requirement
                        // here we are looking for name or phone number match
                        if (row.getName().toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(row);
                        }
                    }

                    contactListFiltered = filteredList;
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = contactListFiltered;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                contactListFiltered = (ArrayList<UserListItem>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }

}
