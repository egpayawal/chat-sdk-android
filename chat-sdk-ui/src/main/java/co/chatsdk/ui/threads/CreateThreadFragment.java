package co.chatsdk.ui.threads;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;

import java.util.ArrayList;
import java.util.List;

import co.chatsdk.core.dao.Keys;
import co.chatsdk.core.dao.Thread;
import co.chatsdk.core.dao.User;
import co.chatsdk.core.events.EventType;
import co.chatsdk.core.events.NetworkEvent;
import co.chatsdk.core.interfaces.UserListItem;
import co.chatsdk.core.session.ChatSDK;
import co.chatsdk.core.utils.DisposableList;
import co.chatsdk.core.utils.UserListItemConverter;
import co.chatsdk.ui.R;
import co.chatsdk.ui.utils.RecyclerItemClickListener;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;

/**
 * Created by Eraño Payawal on 2020-02-07.
 * hunterxer31@gmail.com
 */
public class CreateThreadFragment extends Fragment {

    protected DisposableList disposableList = new DisposableList();

    protected String threadEntityID = "";
    protected Thread thread;

    private RecyclerView mRecyclerView;
    protected EditText searchField;
    private UserThreadAdapter mAdapter;
    /** Set true if you want slide down animation for this context exit. */
    protected boolean animateExit = false;
    protected String filter;

    public static CreateThreadFragment newInstance() {
        return new CreateThreadFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            getDataFromBundle(savedInstanceState);
        } else {
            if (getActivity() != null && getActivity().getIntent().getExtras() != null) {
                getDataFromBundle(getActivity().getIntent().getExtras());
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disposableList.dispose();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(Keys.IntentKeyAnimateExit, animateExit);
        outState.putString(Keys.IntentKeyThreadEntityID, threadEntityID);
    }

    protected void getDataFromBundle(Bundle bundle){
        animateExit = bundle.getBoolean(Keys.IntentKeyAnimateExit, animateExit);
        threadEntityID = bundle.getString(Keys.IntentKeyThreadEntityID, threadEntityID);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_user_create_thread, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mRecyclerView = view.findViewById(R.id.recycler_view);
        CoordinatorLayout coordinatorLayout = view.findViewById(R.id.coordinator);
        AppBarLayout appBarLayout = view.findViewById(R.id.bar_layout);
        searchField = view.findViewById(R.id.search_field);
        TextView header = view.findViewById(R.id.text_view_header);

        if (ChatSDK.config().chatThreadListBackground != 0) {
            coordinatorLayout.setBackgroundColor(ContextCompat.getColor(getActivity(), ChatSDK.config().chatThreadListBackground));
            appBarLayout.setBackgroundColor(ContextCompat.getColor(getActivity(), ChatSDK.config().chatThreadListBackground));
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
            header.setTextColor(ContextCompat.getColor(getActivity(), ChatSDK.config().chatCreateThreadHeaderTextColor));
        }

        if (!TextUtils.isEmpty(ChatSDK.config().chatThreadListSearchText)) {
            searchField.setHint(ChatSDK.config().chatThreadListSearchText);
        }

        initList();
    }

    protected void initList() {
        if (getActivity() == null) return;

        mAdapter = new UserThreadAdapter(getActivity(), null);

        DividerItemDecoration itemDecorator = new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL);
        itemDecorator.setDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.divider));

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.addItemDecoration(itemDecorator);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), (view, position) -> {

            if (position != -1) {
                UserListItem userListItem = mAdapter.userAtPosition(position);
                Log.e("DEBUG", ">>>>>>>>>>>>>>>>>>> " + userListItem);
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
                ChatSDK.ui().startThreadEditDetailsActivity(getActivity(), null, userEntityIDs);
            } else {
                Log.e("DEBUG", "createAndOpenThread");
                createAndOpenThread("", users);
            }
        }
    }

    protected void loadData () {
        mAdapter.setUsers(new ArrayList<>(ChatSDK.contact().contacts()), true);
    }

    protected void createAndOpenThread (String name, List<User> users) {
        if (getActivity() == null) return;

        Log.e("DEBUG", "=======>");

        disposableList.add(ChatSDK.thread()
                .createThread(name, users)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnEvent((thread, throwable) -> Log.e("ERROR", "throwable " + throwable.getMessage()))
                .subscribe(thread -> {
                    Log.e("DEBUG", "thread>>>>>>>>");
                    ChatSDK.ui().startChatActivityForID(getActivity(), thread.getEntityID());
                    getActivity().finish();
                }, toastOnErrorConsumer()));
    }

    protected Consumer<? super Throwable> toastOnErrorConsumer () {
        return (Consumer<Throwable>) throwable -> throwable.getLocalizedMessage();
    }
}
