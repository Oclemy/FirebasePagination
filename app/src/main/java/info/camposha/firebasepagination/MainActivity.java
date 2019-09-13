package info.camposha.firebasepagination;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static info.camposha.firebasepagination.Utils.DataCache;
import static info.camposha.firebasepagination.Utils.show;
import static java.util.Objects.requireNonNull;

public class MainActivity extends Activity {

    private RecyclerView mRV;
    private MyAdapter mAdapter;
    private ProgressBar pb;
    private int mTotalItemCount = 0;
    private int mLastVisibleItemPosition;
    private boolean mIsLoading = false;
    private int mPostsPerPage = 3;
    private Boolean isScrolling = false;
    private int currentScientists, totalScientists, scrolledOutScientists;
    public ArrayList<Scientist> allPagesScientists = new ArrayList();
    private List<Scientist> currentPageScientists = new ArrayList<>();
    private Boolean reachedEnd = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scientists);

        pb=findViewById(R.id.mProgressBarLoad);
        mRV = findViewById(R.id.mRecyclerView);
        final LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRV.setLayoutManager(mLayoutManager);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mRV.getContext(),
                mLayoutManager.getOrientation());
        mRV.addItemDecoration(dividerItemDecoration);

        mAdapter = new MyAdapter();
        mRV.setAdapter(mAdapter);

        if(DataCache.size() > 0){
            mAdapter.addAll(DataCache);
        }else{
            getUsers(null);
        }

        mRV.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                //check for scroll state
                if (newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                    isScrolling = true;
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                currentScientists = mLayoutManager.getChildCount();
                totalScientists = mLayoutManager.getItemCount();
                scrolledOutScientists = ((LinearLayoutManager) recyclerView.getLayoutManager()).
                        findFirstVisibleItemPosition();

                if (isScrolling && (currentScientists + scrolledOutScientists ==
                        totalScientists)) {
                    isScrolling = false;

                    if (dy > 0) {
                        // Scrolling up
                        if(!reachedEnd){
                            getUsers(mAdapter.getLastItemId());
                            pb.setVisibility(View.VISIBLE);
                        }else{
                            show(MainActivity.this,"No More Item Found");
                        }


                    } else {
                        // Scrolling down
                    }
                }





            }
        });
    }

    private void getUsers(String nodeId) {
        mIsLoading=true;
        pb.setVisibility(View.VISIBLE);

        Query query;

        if (nodeId == null)
            query = FirebaseDatabase.getInstance().getReference()
                    .child("Scientists")
                    .orderByKey()
                    .limitToFirst(mPostsPerPage);
        else
            query = FirebaseDatabase.getInstance().getReference()
                    .child("Scientists")
                    .orderByKey()
                    .startAt(nodeId)
                    .limitToFirst(mPostsPerPage);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Scientist> userModels = new ArrayList<>();
                if(dataSnapshot != null && dataSnapshot.exists()){
                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        if(ds.getChildrenCount() > 0){
                            Scientist scientist=ds.getValue(Scientist.class);
                            requireNonNull(scientist).setKey(ds.getKey());
                            if(Utils.scientistExists(ds.getKey())){
                                reachedEnd = true;
                            }else{
                                reachedEnd=false;
                                DataCache.add(scientist);
                                userModels.add(scientist);
                                currentPageScientists = userModels;
                                currentScientists=userModels.size();
                            }
                        }else{
                            Toast.makeText(MainActivity.this, "Ds count 0", Toast.LENGTH_SHORT).show();
                        }
                    }


                }else{
                   show(MainActivity.this, "Data Doesn't Exists or is Null");
                }
                if(!reachedEnd){
                    mAdapter.addAll(userModels);
                }
                mIsLoading = false;
                pb.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                mIsLoading = false;
                pb.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

