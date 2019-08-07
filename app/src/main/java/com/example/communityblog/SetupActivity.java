package com.example.communityblog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
/*import android.widget.Toolbar;*/
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.os.Build.VERSION_CODES.M;
import static android.os.Build.VERSION_CODES.O;

public class SetupActivity extends AppCompatActivity {

    private CircleImageView setupImage;
    private Uri mainImageURI=null;
    private EditText setupName;
    private Button setupBtn;
    private String user_id;
    private Boolean isChanged=false;

    private StorageReference storageReference;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;

    private ProgressBar setupProgress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        Toolbar setupToolbar = findViewById(R.id.setupToolbar);
        setSupportActionBar(setupToolbar);
        getSupportActionBar().setTitle("Profile");

        firebaseAuth=FirebaseAuth.getInstance();
        user_id=firebaseAuth.getCurrentUser().getUid();
        storageReference = FirebaseStorage.getInstance().getReference();
        firebaseFirestore=FirebaseFirestore.getInstance();
        setupImage=findViewById(R.id.setup_image);
        setupName=(EditText) findViewById(R.id.setup_name);
        setupBtn=(Button)findViewById(R.id.setup_btn);

        setupProgress=(ProgressBar)findViewById(R.id.setup_progress);
        setupProgress.setVisibility(View.VISIBLE);
        setupBtn.setEnabled(false);

        firebaseFirestore.collection("Users").document(user_id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
            if(task.isSuccessful()){
                if(task.getResult().exists()){
                    String name=task.getResult().getString("name");
                    String image=task.getResult().getString("image");

                    mainImageURI = Uri.parse(image);


                    setupName.setText(name);

                    RequestOptions placeholderRequest=new RequestOptions();
                    placeholderRequest.placeholder(R.drawable.default_image);
                    Glide.with(SetupActivity.this).setDefaultRequestOptions(placeholderRequest).load(image).into(setupImage);


                }
            }else{
                String error=task.getException().getMessage();
                Toast.makeText(SetupActivity.this,"Firestore Retrieve Error"+error,Toast.LENGTH_LONG).show();

            }
            setupProgress.setVisibility(View.INVISIBLE);
            setupBtn.setEnabled(true);
            }
        });

        setupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final String user_name = setupName.getText().toString();
                if (!TextUtils.isEmpty(user_name) && mainImageURI != null) {
                setupProgress.setVisibility(View.VISIBLE);
             if(isChanged) {





                     user_id = firebaseAuth.getCurrentUser().getUid();
                     setupProgress.setVisibility(View.VISIBLE);
                     StorageReference image_path = storageReference.child("Profile_pics").child(user_id + ".jpg");

                     image_path.putFile(mainImageURI).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                         @Override
                         public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                             if (task.isSuccessful()) {
                                 storeFireStore(task, user_name);
                             } else {
                                 String error = task.getException().getMessage();
                                 Toast.makeText(SetupActivity.this, "Image Error: " + error, Toast.LENGTH_LONG).show();
                                 setupProgress.setVisibility(View.INVISIBLE);
                             }


                         }
                     });


                 }else {
                 storeFireStore(null,user_name);
             }
             }
            }
        });

        setupImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
                    if(ContextCompat.checkSelfPermission(SetupActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
                        Toast.makeText(SetupActivity.this,"Permission Denied",Toast.LENGTH_LONG).show();
                        ActivityCompat.requestPermissions(SetupActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);
                    }else{
                        /*Toast.makeText(SetupActivity.this,"You already have permission",Toast.LENGTH_LONG).show();*/

                       BringImagepicker();
                    }
                }else{
                    BringImagepicker();
                }
            }
        });
    }

    private void storeFireStore(@NonNull Task<UploadTask.TaskSnapshot> task,String user_name)
    {
        Uri download_uri;
        if(task!=null){
         download_uri = task.getResult().getDownloadUrl();}
        else{
            download_uri = mainImageURI;
        }
        /*Toast.makeText(SetupActivity.this,"The image is Uploaded",Toast.LENGTH_LONG).show();*/

        Map<String,String> userMap=new HashMap<>();
        userMap.put("name",user_name);
        userMap.put("image",download_uri.toString());
        firebaseFirestore.collection("Users").document(user_id).set(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){

                    Toast.makeText(SetupActivity.this,"The user Setting are updated",Toast.LENGTH_LONG).show();
                    Intent mainIntent= new Intent(SetupActivity.this,MainActivity.class);
                    startActivity(mainIntent);
                    finish();
                }else{
                    String error=task.getException().getMessage();
                    Toast.makeText(SetupActivity.this,"Firestore Error"+error,Toast.LENGTH_LONG).show();
                }

                setupProgress.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void BringImagepicker() {
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(1,1)
                .start(SetupActivity.this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                mainImageURI = result.getUri();
                setupImage.setImageURI(mainImageURI);
                isChanged=true;
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }
}
