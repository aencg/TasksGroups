package com.example.tasksgroups.ui.adapters;

import android.annotation.SuppressLint;
        import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
        import android.view.View;
        import android.view.ViewGroup;
        import android.widget.TextView;

import com.example.tasksgroups.R;
import com.example.tasksgroups.data.Group;
import com.example.tasksgroups.data.Task;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskAdapterViewHolder> {
    public static final int COMPARE_ID = 1;
    public static final int COMPARE_PRIORITY = 2;
    public static final int COMPARE_STATE = 3;

    public static final int SHOW_ALL = 3;
    public static final int SHOW_ONLY_AVILABLE = 4;
    public static final int SHOW_ONLY_ASSIGNED = 5;
    public static final int SHOW_ONLY_COMPLETED = 6;

    private List<Task> mTaskData;
    private Context mContext;
    private int mOrderBy;


    Comparator<Task> comparatorByPriority = new Comparator<Task>() {
        @Override
        public int compare(Task lhs, Task rhs) {
            return lhs.getPriority().compareTo(rhs.getPriority());
        }
    };

    Comparator<Task> comparatorByState = new Comparator<Task>() {
        @Override
        public int compare(Task lhs, Task rhs) {
            return lhs.getState().compareTo(rhs.getState());
        }
    };

    Comparator<Task> comparatorById = new Comparator<Task>() {
        @Override
        public int compare(Task lhs, Task rhs) {
            return lhs.getId().compareTo(rhs.getId());
        }
    };

    public void setComparator(int mode){
        mOrderBy = mode;
        applyComparator();
    }

    public void  setTask(int position, Task task){
        if(mTaskData == null || mTaskData.size()==0 || position >= mTaskData.size()){
            return;
        }
        mTaskData.set(position, task);
        applyComparator();
    }


    public void  removeTask(int position){
        if(mTaskData == null || mTaskData.size()==0 || position >= mTaskData.size()){
            return;
        }
        mTaskData.remove(position);
        notifyDataSetChanged();
    }


    public int getPositionTask(String id){
        if(mTaskData == null || mTaskData.size()==0){
            return -1;
        }
        int retorno = -1;
        for(int i = 0; i<mTaskData.size(); i++){
            if(mTaskData.get(i).getId().equals(id)){
                retorno = i;
            }
        }

        return retorno;
    }

    public void clear(){
        mTaskData.clear();
        notifyDataSetChanged();
    }


    public boolean remove(Task task){
        if(task == null || getItemCount()==0){
            return false;
        }

        int length = mTaskData.size();
        for(int i = 0; i< length; i++){
            if(task.getId().equals(mTaskData.get(i).getId())){
                mTaskData.remove(mTaskData.get(i));

                notifyDataSetChanged();
                return true;
            }
        }
        return false;
    }


    private void applyComparator(){
        switch(mOrderBy){
            case COMPARE_PRIORITY:
                Collections.sort(mTaskData, comparatorByPriority);
                break;
            case COMPARE_STATE:
                Collections.sort(mTaskData, comparatorByState);
                break;
            default:
                Collections.sort(mTaskData, comparatorById);
        }
        notifyDataSetChanged();

    }


    private final TaskAdapterOnClickHandler mClickHandler;

    /**
     * The interface that receives onClick messages.
     */
    public interface TaskAdapterOnClickHandler {
        void onClick(Task taskClicked, View view);
    }

    /**
     * Creates a TaskAdapter.
     *
     * @param clickHandler The on-click handler for this adapter. This single handler is called
     *                     when an item is clicked.
     */
    public TaskAdapter(TaskAdapterOnClickHandler clickHandler, Context context, int orderBy) {
        mClickHandler = clickHandler;
        mContext = context;
        mOrderBy = orderBy;
    }
    /**
     * Cache of the children views for a forecast list item.
     */
    public class TaskAdapterViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public final TextView mTvTaskName;
        public final TextView mTvTaskState;
        public final TextView mTvTaskPriority;




        public TaskAdapterViewHolder(View view) {
            super(view);
            mTvTaskName = (TextView) view.findViewById(R.id.tv_task_item_title);
            mTvTaskState = (TextView) view.findViewById(R.id.tv_task_item_state);
            mTvTaskPriority = (TextView) view.findViewById(R.id.tv_task_item_priority);
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
            Task taskClicked = mTaskData.get(adapterPosition) ;
            mClickHandler.onClick(taskClicked, v);
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
    @NonNull
    @Override
    public TaskAdapterViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        Context context = viewGroup.getContext();

        int layoutIdForListItem = R.layout.item_task;
        LayoutInflater inflater = LayoutInflater.from(context);
        boolean shouldAttachToParentImmediately = false;

        @SuppressLint("ResourceType")
        View view = inflater.inflate(layoutIdForListItem, viewGroup, shouldAttachToParentImmediately);
        return new TaskAdapterViewHolder(view);
    }

    /**
     * OnBindViewHolder is called by the RecyclerView to display the data at the specified
     * position. In this method, we update the contents of the ViewHolder to display the weather
     * details for this particular position, using the "position" argument that is conveniently
     * passed into us.
     *
     * @param taskAdapterViewHolder The ViewHolder which should be updated to represent the
     *                                  contents of the item at the given position in the data set.
     * @param position                  The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(TaskAdapterViewHolder taskAdapterViewHolder, int position) {
        Task task = mTaskData.get(position);
        taskAdapterViewHolder.mTvTaskName.setText(task.getName());
        taskAdapterViewHolder.mTvTaskPriority.setText(task.getPriority());
        taskAdapterViewHolder.mTvTaskState.setText(task.getState());
    }

    /**
     * This method simply returns the number of items to display. It is used behind the scenes
     * to help layout our Views and for animations.
     *
     * @return The number of items available in our forecast
     */
    @Override
    public int getItemCount() {
        if (null == mTaskData || mTaskData.size()==0)
            return 0;

        return mTaskData.size();
    }

    /**
     * This method is used to set the weather forecast on a MovieAdapter if we've already
     * created one. This is handy when we get new data from the web but don't want to create a
     * new MovieAdapter to display it.
     *
     * @param taskData The new weather data to be displayed.
     */
    public void setTasksData(List<Task> taskData) {
        mTaskData = taskData;
        if(mTaskData!=null)
            applyComparator();
    }

    public void addTask(Task task) {
        if (task != null) {
            mTaskData.add(task);

            applyComparator();
        }
    }
}
