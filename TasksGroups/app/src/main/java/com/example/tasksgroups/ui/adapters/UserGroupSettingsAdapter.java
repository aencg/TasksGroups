package com.example.tasksgroups.ui.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.tasksgroups.R;
import com.example.tasksgroups.data.User;

import java.util.List;

import androidx.recyclerview.widget.RecyclerView;

public class UserGroupSettingsAdapter extends RecyclerView.Adapter<UserGroupSettingsAdapter.UserAdapterViewHolder> {

    private List<User> mUserData;
    private Context mContext;

    private String mAdminId;

    public void setAdminId(String adminId){
        mAdminId = adminId;
        notifyDataSetChanged();
    }

    public static final int MODE_REQUESTS = 1;
    public static final int MODE_ACTUAL = 2;

    public int getmMode() {
        return mMode;
    }

    public void setmMode(int mMode) {
        this.mMode = mMode;
    }

    private int mMode;

    public int getPositionUser(int id){
        if(mUserData == null || mUserData.size()==0){
            return -1;
        }
        int retorno = -1;
        for(int i = 0; i<mUserData.size(); i++){
            if(mUserData.get(i).getId().equals(id)){
                retorno = i;
            }
        }

        return retorno;
    }


    public void clear(){
        mUserData.clear();
        notifyDataSetChanged();
    }


    private final UserListRecyclerviewClickInterface mClickListener;

    /**
     * Creates a UserAdapter.
     *
     * @param clickHandler The on-click handler for this adapter. This single handler is called
     *                     when an item is clicked.
     */
    public UserGroupSettingsAdapter(UserListRecyclerviewClickInterface  clickHandler, Context context, int mode) {
        mClickListener = clickHandler;
        mContext = context;
        this.mMode = mode;
    }

    public interface UserListRecyclerviewClickInterface {
        void onItemClicked(int position, String tag);
    }

    /**
     * Cache of the children views for a forecast list item.
     */
    public class UserAdapterViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public final TextView mTvUserName;
        public final ImageView mIvAdmin;
        public final ImageView mIvDelete;
        public final ImageView mIvConfirm;




        public UserAdapterViewHolder(View view) {
            super(view);
            view.setTag("holder");
            mTvUserName = view.findViewById(R.id.tv_user_name);
            mIvAdmin = view.findViewById(R.id.iv_admin);
            mIvDelete = view.findViewById(R.id.iv_delete);
            mIvConfirm = view.findViewById(R.id.iv_confirm);
            mTvUserName.setTag("userName");
            mIvAdmin.setTag("admin");
            mIvDelete.setTag("delete");
            mIvConfirm.setTag("confirm");
            view.setOnClickListener(this);
            mTvUserName.setOnClickListener(this);
            mIvAdmin.setOnClickListener(this);
            mIvDelete.setOnClickListener(this);
            mIvConfirm.setOnClickListener(this);
        }
        /**
         * This gets called by the child views during a click.
         *
         * @param v The View that was clicked
         */
        @Override
        public void onClick(View v) {
            if (mClickListener != null) {
                mClickListener.onItemClicked(getAdapterPosition(), v.getTag().toString());
            }
        }
    }

    /**
     * This gets called when each new ViewHolder is created. This happens when the RecyclerView
     * is laid out. Enough ViewHolders will be created to fill the screen and allow for scrolling.
     *
     * @param viewGroup The ViewGroup that these ViewHolders are contained within.
     * @param viewType  If your RecyclerView has more than one type of item (which ours doesn't) you
     *                  can use this viewType integer to provide a different layout. See
     *            ///      link android.support.v7.widget.RecyclerView.Adapter#getItemViewType(int)}
     *                  for more details.
     * @return A new MovieAdapterViewHolder that holds the View for each list item
     */
    @Override
    public UserAdapterViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        Context context = viewGroup.getContext();

        int layoutIdForListItem = R.layout.item_user_group_settings;
        LayoutInflater inflater = LayoutInflater.from(context);
        boolean shouldAttachToParentImmediately = false;

        @SuppressLint("ResourceType")
        View view = inflater.inflate(layoutIdForListItem, viewGroup, shouldAttachToParentImmediately);
        return new UserAdapterViewHolder(view);
    }

    /**
     * OnBindViewHolder is called by the RecyclerView to display the data at the specified
     * position. In this method, we update the contents of the ViewHolder to display the weather
     * details for this particular position, using the "position" argument that is conveniently
     * passed into us.
     *
     * @param userAdapterViewHolder The ViewHolder which should be updated to represent the
     *                                  contents of the item at the given position in the data set.
     * @param position                  The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(UserAdapterViewHolder userAdapterViewHolder, int position) {
        User user = mUserData.get(position);
        userAdapterViewHolder.mTvUserName.setText(user.getName());
        if(mMode == MODE_ACTUAL){
            if(mAdminId!=null && mAdminId.equals(user.getId())){
                userAdapterViewHolder.mIvAdmin.setVisibility(View.VISIBLE);
                userAdapterViewHolder.mIvConfirm.setVisibility(View.GONE);
                userAdapterViewHolder.mIvDelete.setVisibility(View.GONE);
            }   else{
                userAdapterViewHolder.mIvAdmin.setVisibility(View.GONE);
                userAdapterViewHolder.mIvConfirm.setVisibility(View.GONE);
                userAdapterViewHolder.mIvDelete.setVisibility(View.VISIBLE);
            }
        }   else if(mMode == MODE_REQUESTS){
            userAdapterViewHolder.mIvAdmin.setVisibility(View.GONE);
            userAdapterViewHolder.mIvConfirm.setVisibility(View.VISIBLE);
            userAdapterViewHolder.mIvDelete.setVisibility(View.VISIBLE);
        }   else{
            userAdapterViewHolder.mIvAdmin.setVisibility(View.GONE);
            userAdapterViewHolder.mIvConfirm.setVisibility(View.GONE);
            userAdapterViewHolder.mIvDelete.setVisibility(View.GONE);
        }

        userAdapterViewHolder.mTvUserName.setText(user.getName());
    }

    /**
     * This method simply returns the number of items to display. It is used behind the scenes
     * to help layout our Views and for animations.
     *
     * @return The number of items available in our forecast
     */
    @Override
    public int getItemCount() {
        if (null == mUserData) return 0;
        return mUserData.size();
    }

    /**
     * This method is used to set the weather forecast on a MovieAdapter if we've already
     * created one. This is handy when we get new data from the web but don't want to create a
     * new MovieAdapter to display it.
     *
     * @param userData The new weather data to be displayed.
     */
    public void setUsersData(List<User> userData) {
        mUserData = userData;
        if(mUserData!=null)
            notifyDataSetChanged();
    }
}
