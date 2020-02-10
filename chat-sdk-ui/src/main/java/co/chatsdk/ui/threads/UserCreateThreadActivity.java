package co.chatsdk.ui.threads;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import co.chatsdk.core.dao.Keys;
import co.chatsdk.core.dao.Thread;
import co.chatsdk.core.dao.User;
import co.chatsdk.core.events.EventType;
import co.chatsdk.core.events.NetworkEvent;
import co.chatsdk.core.interfaces.UserListItem;
import co.chatsdk.core.session.ChatSDK;
import co.chatsdk.core.utils.UserListItemConverter;
import co.chatsdk.ui.R;
import co.chatsdk.ui.main.BaseActivity;
import co.chatsdk.ui.utils.RecyclerItemClickListener;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Predicate;

public class UserCreateThreadActivity extends BaseActivity {

    protected String threadEntityID = "";
    protected Thread thread;

    private RecyclerView mRecyclerView;
    protected EditText searchField;
    private UserThreadAdapter mAdapter;
    /** Set true if you want slide down animation for this context exit. */
    protected boolean animateExit = false;
    protected String filter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setActionBarTitle(R.string.new_chat);

        if (savedInstanceState != null) {
            getDataFromBundle(savedInstanceState);
        } else {
            if (getIntent().getExtras() != null) {
                getDataFromBundle(getIntent().getExtras());
            }
        }

        Predicate<NetworkEvent> contactChanged = ne -> {
            // Make a filter for user update events
            return NetworkEvent.filterContactsChanged().test(ne) || NetworkEvent.filterType(EventType.UserMetaUpdated).test(ne);
        };

        // Refresh the list when the contacts change
        disposableList.add(ChatSDK.events().sourceOnMain()
                .filter(contactChanged)
                .subscribe(networkEvent -> loadData()));

        setContentView(activityLayout());

        initActionBar();
        initViews();
        initList();
    }

    protected @LayoutRes
    int activityLayout() {
        return R.layout.activity_user_create_thread;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(Keys.IntentKeyAnimateExit, animateExit);
        outState.putString(Keys.IntentKeyThreadEntityID, threadEntityID);
    }

    protected void getDataFromBundle(Bundle bundle){
        animateExit = bundle.getBoolean(Keys.IntentKeyAnimateExit, animateExit);
        threadEntityID = bundle.getString(Keys.IntentKeyThreadEntityID, threadEntityID);
    }

    private void initActionBar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayShowCustomEnabled(true);
        }

        if (toolbar != null) {
            ImageView btnBack = toolbar.findViewById(R.id.btn_back);
            TextView textView = toolbar.findViewById(R.id.text_name);
            if (btnBack != null) {
                btnBack.setOnClickListener(v -> {
                    finish();
                });

                if (ChatSDK.config().navigationBackIcon != 0) {
                    btnBack.setImageDrawable(getResources().getDrawable(ChatSDK.config().navigationBackIcon));
                } else {
                    btnBack.setImageDrawable(getResources().getDrawable(R.drawable.ic_back_default));
                }
            }

            if (textView != null) {
                if (!TextUtils.isEmpty(ChatSDK.config().toolbarText)) {
                    textView.setText(ChatSDK.config().toolbarText);
                } else {
                    textView.setText("");
                }

                if (ChatSDK.config().toolbarTextColor !=0) {
                    int color = ContextCompat.getColor(this, ChatSDK.config().toolbarTextColor);
                    textView.setTextColor(color);
                }
            }

        }
    }

    protected void initViews() {
        mRecyclerView = findViewById(R.id.recycler_view);
        CoordinatorLayout coordinatorLayout = findViewById(R.id.coordinator);
        searchField = findViewById(R.id.search_field);
        TextView header = findViewById(R.id.text_view_header);

        if (ChatSDK.config().chatThreadListBackground != 0) {
            coordinatorLayout.setBackgroundColor(ContextCompat.getColor(this, ChatSDK.config().chatThreadListBackground));
        }

        if (searchField != null) {
            searchField.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filter = searchField.getText().toString();
                    mAdapter.getFilter().filter(filter);
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });
        }

        if (ChatSDK.config().chatCreateThreadHeaderTextColor != 0) {
            header.setTextColor(ContextCompat.getColor(this, ChatSDK.config().chatCreateThreadHeaderTextColor));
        }

        if (!TextUtils.isEmpty(ChatSDK.config().chatThreadListSearchText)) {
            searchField.setHint(ChatSDK.config().chatThreadListSearchText);
        }
    }

    protected void initList() {
        mAdapter = new UserThreadAdapter(this, null);

        DividerItemDecoration itemDecorator = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        itemDecorator.setDrawable(ContextCompat.getDrawable(this, R.drawable.divider));

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.addItemDecoration(itemDecorator);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(this, (view, position) -> {

            if (position != -1) {
                UserListItem userListItem = mAdapter.userAtPosition(position);
                if (userListItem != null) {
                    List<UserListItem> users = new ArrayList<>();
                    users.add(userListItem);
                    userList(UserListItemConverter.toUserList(users));
                }

            }
        }));

        loadData();
    }

    protected void userList(List<User> users) {
        if (users != null) {
            if (users.size() > 1) {
                ArrayList<String> userEntityIDs = new ArrayList<>();
                for (User u : users) {
                    userEntityIDs.add(u.getEntityID());
                }
                ChatSDK.ui().startThreadEditDetailsActivity(this, null, userEntityIDs);
            } else {
                createAndOpenThread("", users);
            }
        }
    }

    protected List<User> getUserList () {
        return UserListItemConverter.toUserList(mAdapter.getSelectedUsers());
    }

    protected void loadData () {
        mAdapter.setUsers(new ArrayList<>(ChatSDK.contact().contacts()), true);
    }

    protected void createAndOpenThread (String name, List<User> users) {
        disposableList.add(ChatSDK.thread()
                .createThread(name, users)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnEvent((thread, throwable) -> dismissProgressDialog())
                .subscribe(thread -> {
                    ChatSDK.ui().startChatActivityForID(this, thread.getEntityID());
                    finish();
                }, toastOnErrorConsumer()));
    }

}
