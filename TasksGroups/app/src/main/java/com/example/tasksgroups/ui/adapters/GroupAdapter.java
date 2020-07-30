package com.example.tasksgroups.ui.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.tasksgroups.R;
import com.example.tasksgroups.data.Group;

import java.util.List;

import androidx.recyclerview.widget.RecyclerView;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupAdapterViewHolder> {

    private List<Group> mGroupData;
    private Context mContext;



    public int getPositionGroup(String id){
        if(mGroupData == null || mGroupData.size()==0){
            return -1;
        }
        int retorno = -1;
        for(int i = 0; i<mGroupData.size(); i++){
            if(mGroupData.get(i).getId().equals(id)){
                retorno = i;
            }
        }

        return retorno;
    }

    public void clear(){
        mGroupData.clear();
        notifyDataSetChanged();
    }



    public boolean remove(Group group){
        if(group == null || getItemCount()==0){
            return false;
        }

        int length = mGroupData.size();
        for(int i = 0; i< length; i++){
            if(group.getId().equals(mGroupData.get(i).getId())){
                mGroupData.remove(mGroupData.get(i));
                notifyDataSetChanged();
                return true;
            }
        }
        return false;
    }

    private final GroupAdapterOnClickHandler mClickHandler;

    /**
     * The interface that receives onClick messages.
     */
    public interface GroupAdapterOnClickHandler {
        void onClick(Group taskClicked, View view);
    }

    /**
     * Creates a GroupAdapter.
     *
     * @param clickHandler The on-click handler for this adapter. This single handler is called
     *                     when an item is clicked.
     */
    public GroupAdapter(GroupAdapterOnClickHandler clickHandler, Context context) {
        mClickHandler = clickHandler;
        mContext = context;
    }
    /**
     * Cache of the children views for a forecast list item.
     */
    public class GroupAdapterViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public final TextView mTvGroupName;
        public final TextView mTvGroupAvailableTasks;




        public GroupAdapterViewHolder(View view) {
            super(view);
            mTvGroupName = (TextView) view.findViewById(R.id.tv_group_item_name);
            mTvGroupAvailableTasks = (TextView) view.findViewById(R.id.tv_group_item_available_task);
            view.setOnClickListener(this);
        }
        /**
         * This gets called by the child views during a click.
         *
         * @param v The View that was clicked
         */
        @Override
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();
            Group groupClicked = mGroupData.get(adapterPosition) ;
            mClickHandler.onClick(groupClicked, v);
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
    public GroupAdapterViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        Context context = viewGroup.getContext();

        int layoutIdForListItem = R.layout.item_group;
        LayoutInflater inflater = LayoutInflater.from(context);
        boolean shouldAttachToParentImmediately = false;

        @SuppressLint("ResourceType")
        View view = inflater.inflate(layoutIdForListItem, viewGroup, shouldAttachToParentImmediately);
        return new GroupAdapterViewHolder(view);
    }

    /**
     * OnBindViewHolder is called by the RecyclerView to display the data at the specified
     * position. In this method, we update the contents of the ViewHolder to display the weather
     * details for this particular position, using the "position" argument that is conveniently
     * passed into us.
     *
     * @param groupAdapterViewHolder The ViewHolder which should be updated to represent the
     *                                  contents of the item at the given position in the data set.
     * @param position                  The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(GroupAdapterViewHolder groupAdapterViewHolder, int position) {
        Group group = mGroupData.get(position);
        groupAdapterViewHolder.mTvGroupName.setText(group.getName());
        //groupAdapterViewHolder.mTvGroupAvailableTasks.setText(group.getAvailableNumberOfTask());
    }

    /**
     * This method simply returns the number of items to display. It is used behind the scenes
     * to help layout our Views and for animations.
     *
     * @return The number of items available in our forecast
     */
    @Override
    public int getItemCount() {
        if (null == mGroupData || mGroupData.size()==0)
            return 0;

        return mGroupData.size();
    }

    /**
     * This method is used to set the weather forecast on a MovieAdapter if we've already
     * created one. This is handy when we get new data from the web but don't want to create a
     * new MovieAdapter to display it.
     *
     * @param groupData The new weather data to be displayed.
     */
    public void setGroupsData(List<Group> groupData) {
        mGroupData = groupData;
        if(mGroupData!=null)
            notifyDataSetChanged();
    }
}