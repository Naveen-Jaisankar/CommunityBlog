package com.example.communityblog;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class BlogRecyclerAdapter extends RecyclerView.Adapter<BlogRecyclerAdapter.ViewHolder> {

    public List<BlogPost> blog_list;
    public Context context;
    private FirebaseFirestore firebaseFirestore;
    private TextView blogUserName;
    private CircleImageView blogUserImage;

    public BlogRecyclerAdapter(List<BlogPost> blog_list) {
        this.blog_list=blog_list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.blog_list_item,parent,false);
        context=parent.getContext();
        firebaseFirestore=FirebaseFirestore.getInstance();
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {

        String desc_data=blog_list.get(position).getDesc();
        holder.setDescText(desc_data);

        String image_url=blog_list.get(position).getImage_url();
        holder.setBlogImage(image_url);

        String user_id=blog_list.get(position).getUser_id();
        firebaseFirestore.collection("Users").document(user_id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    String userName=task.getResult().getString("name");
                    String userImage=task.getResult().getString("image");

                    holder.setUserData(userName,userImage);
                }else{

                }
            }
        });

        long milliseconds=blog_list.get(position).getTimestamp().getTime();
        String dateString= DateFormat.format("MM/dd/yyyy",new Date(milliseconds)).toString();
        holder.setTime(dateString);


    }

    @Override
    public int getItemCount() {
        return blog_list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        private View mview;

        private TextView descView;
        private ImageView blogImageView;
        private TextView blogDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            mview=itemView;
        }

        public void setDescText(String descText){
            descView=mview.findViewById(R.id.blog_desc);
            descView.setText(descText);

        }

        public  void setBlogImage(String downloadUri){

            blogImageView=mview.findViewById(R.id.blog_image);
            RequestOptions requestOptions=new RequestOptions();
            requestOptions.placeholder(R.drawable.image_placeholder);
            Glide.with(context).applyDefaultRequestOptions(requestOptions).load(downloadUri).into(blogImageView);

        }

        public void setTime(String date){
             blogDate= mview.findViewById(R.id.blog_date);
             blogDate.setText(date);
        }

        public void setUserData(String name,String image){
            blogUserImage=mview.findViewById(R.id.blog_user_image);
            blogUserName=mview.findViewById(R.id.blog_user_name);

            blogUserName.setText(name);
            RequestOptions placeHolderOption = new RequestOptions();
            placeHolderOption.placeholder(R.drawable.profile_placeholder);
            Glide.with(context).applyDefaultRequestOptions(placeHolderOption).load(image).into(blogUserImage);

        }
    }
}
