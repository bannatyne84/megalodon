package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.lists.CreateList;
import org.joinmastodon.android.api.requests.lists.UpdateList;
import org.joinmastodon.android.api.requests.timelines.GetListTimeline;
import org.joinmastodon.android.model.ListTimeline;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.ListTimelineEditor;

import java.util.List;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.utils.V;


public class ListTimelineFragment extends StatusListFragment {
    private String listID;
    private String listTitle;
    private ListTimeline.RepliesPolicy repliesPolicy;
    private ImageButton fab;

    public ListTimelineFragment() {
        setListLayoutId(R.layout.recycler_fragment_with_fab);
    }

    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);
        Bundle args = getArguments();
        listID = args.getString("listID");
        listTitle = args.getString("listTitle");
        repliesPolicy = ListTimeline.RepliesPolicy.values()[args.getInt("repliesPolicy", 0)];

        setTitle(listTitle);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.list, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Bundle args = new Bundle();
        args.putString("listID", listID);
        if (item.getItemId() == R.id.edit) {
            ListTimelineEditor editor = new ListTimelineEditor(getContext());
            editor.applyList(listTitle, repliesPolicy);
            new M3AlertDialogBuilder(getActivity())
                    .setTitle(R.string.sk_edit_list_title)
                    .setIcon(R.drawable.ic_fluent_people_list_28_regular)
                    .setView(editor)
                    .setPositiveButton(R.string.save, (d, which) -> {
                        new UpdateList(listID, editor.getTitle(), editor.getRepliesPolicy()).setCallback(new Callback<>() {
                            @Override
                            public void onSuccess(ListTimeline list) {
                                setTitle(list.title);
                                listTitle = list.title;
                                repliesPolicy = list.repliesPolicy;
                                args.putString("listTitle", listTitle);
                                args.putInt("repliesPolicy", repliesPolicy.ordinal());
                                setResult(true, args);
                            }

                            @Override
                            public void onError(ErrorResponse error) {
                                error.showToast(getContext());
                            }
                        }).exec(accountID);
                    })
                    .setNegativeButton(R.string.cancel, (d, which) -> {})
                    .show();
        } else if (item.getItemId() == R.id.delete) {
            UiUtils.confirmDeleteList(getActivity(), accountID, listID, () -> {
                args.putBoolean("deleted", true);
                setResult(true, args);
                Nav.finish(this);
            });
        }
        return true;
    }

    @Override
    protected void doLoadData(int offset, int count) {
        currentRequest=new GetListTimeline(listID, offset==0 ? null : getMaxID(), null, count, null)
                .setCallback(new SimpleCallback<>(this) {
                    @Override
                    public void onSuccess(List<Status> result) {
                        onDataLoaded(result, !result.isEmpty());
                    }
                })
                .exec(accountID);
    }

    @Override
    protected void onShown() {
        super.onShown();
        if(!getArguments().getBoolean("noAutoLoad") && !loaded && !dataLoading)
            loadData();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fab=view.findViewById(R.id.fab);
        fab.setOnClickListener(this::onFabClick);
        fab.setOnLongClickListener(v -> UiUtils.pickAccountForCompose(getActivity(), accountID));
    }

    private void onFabClick(View v){
        Bundle args=new Bundle();
        args.putString("account", accountID);
        Nav.go(getActivity(), ComposeFragment.class, args);
    }

    @Override
    protected void onSetFabBottomInset(int inset) {
        ((ViewGroup.MarginLayoutParams) fab.getLayoutParams()).bottomMargin=V.dp(24)+inset;
    }
}
