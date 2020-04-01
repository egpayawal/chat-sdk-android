package co.chatsdk.ui.threads;

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import co.chatsdk.core.dao.Thread;
import co.chatsdk.core.dao.User;
import co.chatsdk.core.events.EventType;
import co.chatsdk.core.events.NetworkEvent;
import co.chatsdk.core.session.ChatSDK;
import co.chatsdk.ui.R;
import co.chatsdk.ui.main.BaseFragment;
import co.chatsdk.ui.utils.events.EventData;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Predicate;

public abstract class ThreadsFragment extends BaseFragment {

    protected RecyclerView listThreads;
    protected EditText searchField;
    protected ThreadsListAdapter adapter;
    protected String filter;
    protected MenuItem addMenuItem;
    protected LinearLayout containerEmptyState;
    protected TextView textEmptyStateTitle;
    protected TextView textEmptyStateMsg;
    protected TextView btnEmptyState;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        disposableList.add(ChatSDK.events().sourceOnMain()
                .filter(mainEventFilter())
                .subscribe(networkEvent -> {
                    if (tabIsVisible || ChatSDK.config().isMenu) {
                        reloadData();
                    }
                }));

        disposableList.add(ChatSDK.events().sourceOnMain()
                .filter(NetworkEvent.filterType(EventType.TypingStateChanged))
                .subscribe(networkEvent -> {
                    if (tabIsVisible || ChatSDK.config().isMenu) {
                        adapter.setTyping(networkEvent.thread, networkEvent.text);
                        adapter.notifyDataSetChanged();
                    }
                }));

        reloadData();

        mainView = inflater.inflate(activityLayout(), null);

        initViews();

        final Handler handler = new Handler();
        handler.postDelayed( new Runnable() {

            @Override
            public void run() {
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                    handler.postDelayed( this, 60 * 1000 );
                }
            }
        }, 60 * 1000 );

        return mainView;
    }

    protected abstract Predicate<NetworkEvent> mainEventFilter ();

    protected  @LayoutRes int activityLayout () {
        return R.layout.activity_threads;
    }

    public void initViews() {
        searchField = mainView.findViewById(R.id.search_field);
        listThreads = mainView.findViewById(R.id.list_threads);
        containerEmptyState = mainView.findViewById(R.id.container_empty_state);
        textEmptyStateTitle = mainView.findViewById(R.id.text_empty_state_title);
        textEmptyStateMsg = mainView.findViewById(R.id.text_empty_state_msg);
        btnEmptyState = mainView.findViewById(R.id.btn_empty_state);
        CoordinatorLayout coordinatorLayout = mainView.findViewById(R.id.coordinator);
        AppBarLayout appBarLayout = mainView.findViewById(R.id.bar_layout);

        if (getActivity() != null) {
            Typeface typefaceNormal = ResourcesCompat.getFont(getActivity(), R.font.roboto_regular);
            searchField.setTypeface(typefaceNormal);
        }

        if (ChatSDK.config().chatThreadListBackground != 0 && getActivity() != null) {
            coordinatorLayout.setBackgroundColor(ContextCompat.getColor(getActivity(), ChatSDK.config().chatThreadListBackground));
            appBarLayout.setBackgroundColor(ContextCompat.getColor(getActivity(), ChatSDK.config().chatThreadListBackground));
        }

        if (ChatSDK.config().chatThreadEmptyStateIcon != 0 && getActivity() != null) {
            Drawable drawableTop = ContextCompat.getDrawable(getActivity(), ChatSDK.config().chatThreadEmptyStateIcon);
            textEmptyStateTitle.setCompoundDrawablesWithIntrinsicBounds(null, drawableTop, null, null);
        }

        if (!TextUtils.isEmpty(ChatSDK.config().chatThreadEmptyStateTitleText)) {
            textEmptyStateTitle.setText(ChatSDK.config().chatThreadEmptyStateTitleText);
        }

        if (!TextUtils.isEmpty(ChatSDK.config().chatThreadEmptyStateMessageText)) {
            textEmptyStateMsg.setText(ChatSDK.config().chatThreadEmptyStateMessageText);
        }

        if (!TextUtils.isEmpty(ChatSDK.config().chatThreadEmptyStateButtonText)) {
            btnEmptyState.setText(ChatSDK.config().chatThreadEmptyStateButtonText);
        }

        if (ChatSDK.config().chatThreadEmptyStateButtonIcon != 0 && getActivity() != null) {
            Drawable drawableEnd = ContextCompat.getDrawable(getActivity(), ChatSDK.config().chatThreadEmptyStateButtonIcon);
            btnEmptyState.setCompoundDrawablesWithIntrinsicBounds(null, null, drawableEnd, null);
        }

        adapter = new ThreadsListAdapter(getActivity());

        listThreads.setLayoutManager(new LinearLayoutManager(this.getActivity()));
        listThreads.setAdapter(adapter);

        containerEmptyState.setVisibility(View.GONE);

        Disposable d = adapter.onClickObservable().subscribe(thread -> {
            ChatSDK.ui().startChatActivityForID(getContext(), thread.getEntityID());
        });

        if (!TextUtils.isEmpty(ChatSDK.config().chatThreadListSearchText)) {
            searchField.setHint(ChatSDK.config().chatThreadListSearchText);
        }

        if (btnEmptyState != null) {
            btnEmptyState.setOnClickListener(v -> {
                EventBus.getDefault().post(new EventData.CreateButtonClickEvent());
            });
        }
    }

    protected boolean allowThreadCreation () {
        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (allowThreadCreation()) {
            addMenuItem = menu.add(Menu.NONE, R.id.action_add, 10, getString(R.string.thread_fragment_add_item_text));
            addMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

            if (ChatSDK.config().chatThreadListRightIconDrawable != 0 && getActivity() != null) {
                addMenuItem.setIcon(ContextCompat.getDrawable(getActivity(), ChatSDK.config().chatThreadListRightIconDrawable));
            } else {
                addMenuItem.setIcon(R.drawable.ic_plus);
            }
        }
    }

    // Override this in the subclass to handle the plus button
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadData();

        if (searchField != null) {
            searchField.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filter = searchField.getText().toString();
                    reloadData();
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });
        }
    }

    @Override
    public void clearData() {
        if (adapter != null) {
            adapter.clearData();
        }
    }

    public void setTabVisibility (boolean isVisible) {
        super.setTabVisibility(isVisible);
        reloadData();
    }

    @Override
    public void reloadData() {
        if (adapter != null) {
            adapter.clearData();
            List<Thread> threads = filter(getThreads());
            boolean showEmptyState = (threads.size() == 0);
            containerEmptyState.setVisibility(showEmptyState ? View.VISIBLE : View.GONE);
            listThreads.setVisibility(showEmptyState ? View.GONE : View.VISIBLE);
            searchField.setVisibility(showEmptyState ? View.GONE : View.VISIBLE);

            adapter.updateThreads(threads);
        }
    }

    protected abstract List<Thread> getThreads ();

    public List<Thread> filter(List<Thread> threads) {
        if (filter == null || filter.isEmpty()) {
            return threads;
        }

        List<Thread> filteredThreads = new ArrayList<>();
        for (Thread t : threads) {
            if (t.getName() != null && t.getName().toLowerCase().contains(filter.toLowerCase())) {
                filteredThreads.add(t);
            } else {
                for (User u : t.getUsers()) {
                    if (u.getName() != null && u.getName().toLowerCase().contains(filter.toLowerCase())) {
                        filteredThreads.add(t);
                        break;
                    }
                }
            }
        }
        return filteredThreads;
    }

}
