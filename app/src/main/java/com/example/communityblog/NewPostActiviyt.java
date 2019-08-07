package com.example.communityblog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import id.zelory.compressor.Compressor;

public class NewPostActiviyt extends AppCompatActivity {

    private Toolbar newPosttoolbar;
    private ImageView newPostImage;
    private EditText newPostDesc;
    private Button newPostBtn;
    private Uri PostImageUri=null;
    private ProgressBar newPostPorgress;

    private StorageReference storageReference;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth firebaseAuth;
    private String current_user_id;
    private Bitmap compressedImageFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_post_activiyt);

        storageReference= FirebaseStorage.getInstance().getReference();
        firebaseFirestore=FirebaseFirestore.getInstance();
        firebaseAuth=FirebaseAuth.getInstance();
        current_user_id=firebaseAuth.getCurrentUser().getUid();

        newPosttoolbar=(Toolbar)findViewById(R.id.new_post_toolbar);
        setSupportActionBar(newPosttoolbar);
        getSupportActionBar().setTitle("Add a new Post");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        newPostImage=(ImageView)findViewById(R.id.new_post_image);
        newPostDesc=(EditText)findViewById(R.id.new_post_descp);
        newPostBtn=(Button)findViewById(R.id.post_btn);
        newPostPorgress=(ProgressBar)findViewById(R.id.new_post_progress);

        newPostImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setMinCropWindowSize(512,512)
                        .setAspectRatio(1,1)
                        .start(NewPostActiviyt.this);
            }
        });

        newPostBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String desc=newPostDesc.getText().toString();

                if(!TextUtils.isEmpty(desc)&& PostImageUri != null)
                {
                    newPostPorgress.setVisibility(View.VISIBLE);

                    final String randomName= UUID.randomUUID().toString();
                    StorageReference filePath=storageReference.child("post_images").child(randomName+".jpg");
                    filePath.putFile(PostImageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull final Task<UploadTask.TaskSnapshot> task) {

                            final String downloadUri = task.getResult().getDownloadUrl().toString();
                            if(task.isSuccessful()){
                                File newImageFile= new File(PostImageUri.getPath());
                                try {
                                    compressedImageFile = new Compressor(NewPostActiviyt.this)
                                            .setMaxHeight(100)
                                            .setMaxWidth(100)
                                            .setQuality(2)
                                            .compressToBitmap(newImageFile);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                compressedImageFile.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                                byte[] thumbData = baos.toByteArray();



                                UploadTask uploadTask=storageReference
                                        .child("post_images/thumbs").child(randomName+".jpg").putBytes(thumbData);
                                uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                        String downloadthumbUri=taskSnapshot.getUploadSessionUri().toString();

                                        Map<String, Object>postMap=new HashMap<>();
                                        postMap.put("image_url",downloadUri);
                                        postMap.put("image_thumb",downloadthumbUri);
                                        postMap.put("desc",desc);
                                        postMap.put("user_id",current_user_id);
                                        postMap.put("timestamp",FieldValue.serverTimestamp());

                                        firebaseFirestore.collection("Posts").add(postMap).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                                            @Override
                                            public void onComplete(@NonNull Task<DocumentReference> task) {
                                                if(task.isSuccessful()){
                                                    Toast.makeText(NewPostActiviyt.this,"Post was added successfully",Toast.LENGTH_LONG).show();
                                                    Intent mainIntent=new Intent(NewPostActiviyt.this,MainActivity.class);
                                                    startActivity(mainIntent);
                                                    finish();
                                                }else {
                                                    String error= task.getException().getMessage();
                                                    Toast.makeText(NewPostActiviyt.this,"Eroor: "+error,Toast.LENGTH_LONG).show();
                                                }
                                                newPostPorgress.setVisibility(View.INVISIBLE);
                                            }
                                        });

                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {

                                        String error=task.getException().getMessage();
                                        Toast.makeText(NewPostActiviyt.this,"Eroor: "+error,Toast.LENGTH_LONG).show();

                                    }
                                });
                                /*thumbFilePath.putFile(compressedImageFile)*/


                            }else{
                                newPostPorgress.setVisibility(View.INVISIBLE);
                                String error= task.getException().getMessage();
                                Toast.makeText(NewPostActiviyt.this,"Error: "+error,Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                PostImageUri=result.getUri();
                newPostImage.setImageURI(PostImageUri);

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }

}
