package com.example.tasksgroups.ui.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.tasksgroups.R;
import com.example.tasksgroups.data.AddedFile;

import java.util.List;

import android.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

public class AddedFileAdapter extends RecyclerView.Adapter<AddedFileAdapter.AddedFileAdapterViewHolder> {

    private List<AddedFile> mAddedFileData;
    private Context mContext;

    public int getPositionById(String id){
        if(mAddedFileData == null || mAddedFileData.size()==0){
            return -1;
        }
        int retorno = -1;
        for(int i = 0; i<mAddedFileData.size(); i++){
            if(mAddedFileData.get(i).getId().equals(id)){
                retorno = i;
                break;
            }
        }

        return retorno;
    }


    public int getPositionByName(String name){
        if(mAddedFileData == null || mAddedFileData.size()==0){
            return -1;
        }
        int retorno = -1;
        for(int i = 0; i<mAddedFileData.size(); i++){
            if(mAddedFileData.get(i).getName().equals(name)){
               return i;
            }
        }

        return retorno;
    }



    public void addFile(AddedFile file){
        if(file!=null){
            mAddedFileData.add(file);
            notifyDataSetChanged();
        }

    }

    public void setFilePosition(int position, AddedFile file){

        if(file!=null && position < mAddedFileData.size() && position >= 0){
            mAddedFileData.set(position, file);
            notifyDataSetChanged();
        }

    }

    public void removeFilePosition(int position){

        if( position < mAddedFileData.size() && position >= 0){
            mAddedFileData.remove(position);
            notifyDataSetChanged();
        }
    }

    public AddedFile getAddedFileByPosition(int position){

        if( position < mAddedFileData.size() && position >= 0){
            return mAddedFileData.get(position);
        }

        return null;
    }


    public void clear(){
        mAddedFileData.clear();
        notifyDataSetChanged();
    }

    private final AddedFileAdapterOnClickHandler mClickHandler;

    /**
     * The interface that receives onClick messages.
     */
    public interface AddedFileAdapterOnClickHandler {
        void onOptionClick(AddedFile addedFileClicked, View view);
    }

    private final ClickMenuOptionHandler mClickMenuOptionListener;

    /**
     * The interface that receives onClick messages.
     */
    public interface ClickMenuOptionHandler {
        void onOptionClick(AddedFile addedFileClicked, String option);
    }

    /**
     * Creates a AddedFileAdapter.
     *
     * @param clickHandler The on-click handler for this adapter. This single handler is called
     *                     when an item is clicked.
     */
    public AddedFileAdapter(AddedFileAdapterOnClickHandler clickHandler, ClickMenuOptionHandler clickMenuOptionListener,   Context context) {
        mClickHandler = clickHandler;
        mClickMenuOptionListener = clickMenuOptionListener;
        mContext = context;
    }
    /**
     * Cache of the children views for a forecast list item.
     */
    public class AddedFileAdapterViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener{

        public final TextView mTvAddedFileName;
        public final ImageView mIvAddedFileImage;





        public AddedFileAdapterViewHolder(View view) {
            super(view);


            mTvAddedFileName = (TextView) view.findViewById(R.id.tv_file_name);
            mIvAddedFileImage = (ImageView) view.findViewById(R.id.iv_file);
            view.setOnClickListener(this);
            view.setOnLongClickListener(this);
        }
        /**
         * This gets called by the child views during a click.
         *
         * @param v The View that was clicked
         */
        @Override
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();
            AddedFile addedFileClicked = mAddedFileData.get(adapterPosition) ;
            mClickHandler.onOptionClick(addedFileClicked, v);
        }

        @Override
        public boolean onLongClick(View v) {
            int adapterPosition = getAdapterPosition();
            AddedFile addedFileClicked = mAddedFileData.get(adapterPosition) ;
            PopupMenu popup = new PopupMenu(v.getContext(), v);

            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.download_option:
                            mClickMenuOptionListener.onOptionClick(addedFileClicked, "download");
                            return true;
                        case R.id.delete_option:
                            mClickMenuOptionListener.onOptionClick(addedFileClicked, "delete");
                            return true;
                        default:
                            return false;
                    }
                }
            });
            // here you can inflate your menu
            popup.inflate(R.menu.file_context_menu);
            //popup.setGravity(Gravity.RIGHT);
            popup.show();

            return true;
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
    public AddedFileAdapterViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        Context context = viewGroup.getContext();

        int layoutIdForListItem = R.layout.item_file;
        LayoutInflater inflater = LayoutInflater.from(context);
        boolean shouldAttachToParentImmediately = false;

        @SuppressLint("ResourceType")
        View view = inflater.inflate(layoutIdForListItem, viewGroup, shouldAttachToParentImmediately);
        return new AddedFileAdapterViewHolder(view);
    }

    /**
     * OnBindViewHolder is called by the RecyclerView to display the data at the specified
     * position. In this method, we update the contents of the ViewHolder to display the weather
     * details for this particular position, using the "position" argument that is conveniently
     * passed into us.
     *
     * @param addedFileAdapterViewHolder The ViewHolder which should be updated to represent the
     *                                  contents of the item at the given position in the data set.
     * @param position                  The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(AddedFileAdapterViewHolder addedFileAdapterViewHolder, int position) {
        AddedFile addedFile = mAddedFileData.get(position);
        String fileName = addedFile.getName();

        addedFileAdapterViewHolder.mTvAddedFileName.setText(fileName);
        addedFileAdapterViewHolder.mIvAddedFileImage.setContentDescription(fileName);

        String extension="";
        int i = fileName.lastIndexOf(".");
        if(i>0){
            extension = fileName.substring(i+1);
        }


        if(extension.equals("jpg") || extension.equals("jpeg") || extension.equals("png") || extension.equals("bmp")){
            Glide.with(mContext)
                    .load(addedFile.getUrl())
                    .circleCrop()
                    .into(addedFileAdapterViewHolder.mIvAddedFileImage);
        }   else{
            addedFileAdapterViewHolder.mIvAddedFileImage.setImageDrawable( mContext.getResources().getDrawable(R.drawable.ic_files));
        }

       // addedFileAdapterViewHolder.mIvAddedFileImage.set;);
    }

    /**
     * This method simply returns the number of items to display. It is used behind the scenes
     * to help layout our Views and for animations.
     *
     * @return The number of items available in our forecast
     */
    @Override
    public int getItemCount() {
        if (null == mAddedFileData) return 0;
        return mAddedFileData.size();
    }

    /**
     * This method is used to set the weather forecast on a MovieAdapter if we've already
     * created one. This is handy when we get new data from the web but don't want to create a
     * new MovieAdapter to display it.
     *
     * @param addedFileData The new weather data to be displayed.
     */
    public void setAddedFilesData(List<AddedFile> addedFileData) {
        mAddedFileData = addedFileData;
        if(mAddedFileData!=null)
            notifyDataSetChanged();
    }
}
