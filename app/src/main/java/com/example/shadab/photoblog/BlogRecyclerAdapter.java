package com.example.shadab.photoblog;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import de.hdodenhof.circleimageview.CircleImageView;

public class BlogRecyclerAdapter extends RecyclerView.Adapter<BlogRecyclerAdapter.ViewHolder> {

    public List<BlogPost> blog_list;
    public Context context;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth firebaseAuth;

    public BlogRecyclerAdapter(List<BlogPost> blog_list){
        this.blog_list = blog_list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.blog_list_item,parent,false);
        context = parent.getContext();
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseAuth      = FirebaseAuth.getInstance();

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {

        holder.setIsRecyclable(false);
        final String blogPostId =  blog_list.get(position).BlogPostId;
        final String currentUserId = Objects.requireNonNull(firebaseAuth.getCurrentUser()).getUid();


        String desc_data = blog_list.get(position).getDesc();
        holder.setDescText(desc_data);

        String image_url = blog_list.get(position).getImage_url();
        String thumbUri  = blog_list.get(position).getImage_thumb();

        holder.setBlogImage(image_url,thumbUri);

        String user_id = blog_list.get(position).getUser_id();

        firebaseFirestore.collection("Users").document(user_id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    String userName = task.getResult().getString("name");
                    String userImage = task.getResult().getString("image");
                    holder.setUserData(userName,userImage);

                }
                else{
                    Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show();
                }
            }
        });

        long millisecond = blog_list.get(position).getTimestamp().getTime();
        String dateString = new SimpleDateFormat("MM/dd/yyyy").format(new Date(millisecond));
        holder.setTime(dateString);


        firebaseFirestore.collection("Posts/"+ blogPostId + "/Likes").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                    if( queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()){

                        int count = queryDocumentSnapshots.size();
                        holder.updateLikesCount(count);

                    }else{
                        holder.updateLikesCount(0);
                    }
            }
        });

        firebaseFirestore.collection("Posts/"+ blogPostId + "/Likes").document(currentUserId).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if(documentSnapshot!= null &&  documentSnapshot.exists()){

                    holder.blogLikeBtn.setImageDrawable(context.getDrawable(R.mipmap.action_like_accent));

                }
                else{

                    holder.blogLikeBtn.setImageDrawable(context.getDrawable(R.mipmap.action_like_gray));
                }
            }
        });

        holder.blogLikeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                firebaseFirestore.collection("Posts/"+ blogPostId + "/Likes").document(currentUserId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                        if(!task.getResult().exists()){

                            Map<String,Object> likesMap = new HashMap<>();
                            likesMap.put("timestamp",FieldValue.serverTimestamp());

                            firebaseFirestore.collection("Posts/"+ blogPostId + "/Likes").document(currentUserId).set(likesMap);
                        }
                        else {
                            firebaseFirestore.collection("Posts/"+ blogPostId + "/Likes").document(currentUserId).delete();

                        }
                    }
                });
            }
        });

    }

    @Override
    public int getItemCount() {
        return blog_list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

         private View mView;
         private TextView descView;
         private ImageView blogImageView;
         private TextView blogDate;
         private TextView blogUserName;
         private CircleImageView blogUserImage;
         private ImageView blogLikeBtn;
         private TextView blogLikeCount;


         public ViewHolder(View itemView) {
            super(itemView);
            mView = itemView;
            blogLikeBtn = mView.findViewById(R.id.blog_like_btn);

        }

        public void setDescText(String descText){
             descView = mView.findViewById(R.id.blog_desc);
             descView.setText(descText);
        }
        public void setBlogImage(String downloadUri,String thumbUri){
             blogImageView = mView.findViewById(R.id.blog_image);
             RequestOptions requestOptions = new RequestOptions();
             requestOptions.placeholder(R.drawable.rectangle);
            Glide.with(context).applyDefaultRequestOptions(requestOptions).load(downloadUri)
                    .thumbnail(Glide.with(context).load(thumbUri))
                    .into(blogImageView);
        }

        public void setTime(String date){
            blogDate = mView.findViewById(R.id.blog_date);
            blogDate.setText(date);
        }

        public void setUserData(String name,String image){
             blogUserImage = mView.findViewById(R.id.blog_user_image);
             blogUserName = mView.findViewById(R.id.blog_user_name);
             blogUserName.setText(name);

             RequestOptions placeholderOptiopn  = new RequestOptions();
             placeholderOptiopn.placeholder(R.drawable.circle);
             Glide.with(context).applyDefaultRequestOptions(placeholderOptiopn).load(image).into(blogUserImage);

        }

        public void updateLikesCount(int count){
             blogLikeCount = mView.findViewById(R.id.blog_like_count);
             blogLikeCount.setText(count + "Likes");
        }

    }
}
