package com.rangetech.eventnotify.Activities;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.crowdfire.cfalertdialog.CFAlertDialog;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.rangetech.eventnotify.Helpers.CheckExpiry;
import com.rangetech.eventnotify.R;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

public class EventDisplayActivity extends AppCompatActivity {

    private static final String EVENT_DISPLAY = "Event Display";
    private String eventName = "";
    private String eventDetails = "";
    private String eventLocation = "";
    private String eventDate = "";
    private String eventCover = "";


    private TextView eventDetailsTxt;
    private Button eventLocationBtn;
    private Button eventDateBtn;
    private ImageView eventCoverImageView;
    private FloatingActionButton eventFab,eventFabRemove,eventQRFab;
    private Toolbar toolbar;
    private FirebaseAuth firebaseAuth;
    private String currentUser;
    private String album_id;
    private FirebaseFirestore rootRef;
    private DocumentReference docIdRef;
    private String userID;
    private boolean expired=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_display);

        eventDetailsTxt = findViewById(R.id.event_details);
        eventDateBtn = findViewById(R.id.dated);
        eventLocationBtn = findViewById(R.id.located);
        eventCoverImageView = findViewById(R.id.event_cover);
        eventFab = (FloatingActionButton) findViewById(R.id.event_fab);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        firebaseAuth = FirebaseAuth.getInstance();
        eventFabRemove=findViewById(R.id.event_fab_remove);
        eventQRFab=findViewById(R.id.event_fab_qr);
        eventName = getIntent().getStringExtra("EventName");
        eventLocation = getIntent().getStringExtra("EventLocation");
        eventDetails = getIntent().getStringExtra("EventDetails");
        eventDate = getIntent().getStringExtra("EventDate");
        eventCover = getIntent().getStringExtra("EventCover");
        currentUser = firebaseAuth.getCurrentUser().getUid();
        album_id=getIntent().getStringExtra("AlbumId");
        rootRef = FirebaseFirestore.getInstance();
        userID=getIntent().getStringExtra("UserID");

        CheckExpiry checkExpiry=new CheckExpiry(eventDate,0);
        try {
            if(checkExpiry.isExpired()==true){
                expired=true;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        Glide.with(getApplicationContext())
                .load(eventCover)
                .into(eventCoverImageView);
        eventDetailsTxt.setText(eventDetails);
        if(expired) {
            eventDateBtn.setText(eventDate+" (expired!)");
            eventDateBtn.setBackgroundColor(getColor(R.color.red));
        }
        eventLocationBtn.setText(eventLocation);
        toolbar.setTitle("" + eventName);
        setSupportActionBar(toolbar);

        docIdRef = rootRef.collection("Users")
                .document(currentUser)
                .collection("MyEvents")
                .document(album_id);
        docIdRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull final Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    final DocumentSnapshot document = task.getResult();
                    if (document.exists()) {

                        Log.i(EVENT_DISPLAY, "Document exists!");

                        if(task.getResult().getString("participated").contentEquals("no")&&!expired) {

                            eventFab.setVisibility(View.INVISIBLE);
                            eventFabRemove.setVisibility(View.VISIBLE);
                            eventQRFab.setVisibility(View.VISIBLE);

                            eventQRFab.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (userID.contentEquals(currentUser)) {
                                        Intent qrCodeReader = new Intent(EventDisplayActivity.this, QRCodeActivityReader.class);
                                        qrCodeReader.putExtra("data", album_id);
                                        startActivity(qrCodeReader);
                                    } else {
                                        Intent qrCodeReader = new Intent(EventDisplayActivity.this, QRcode.class);
                                        qrCodeReader.putExtra("data", currentUser);
                                        startActivity(qrCodeReader);

                                    }
                                }
                            });


                            eventFabRemove.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    CFAlertDialog.Builder builder = new CFAlertDialog.Builder(EventDisplayActivity.this, R.style.AppTheme)
                                            .setDialogStyle(CFAlertDialog.CFAlertStyle.BOTTOM_SHEET)
                                            .setTitle("Remove ?")
                                            .setIcon(R.drawable.ic_warning_black_24dp)
                                            .setMessage("Are you sure you want to remove this event.")
                                            .setCancelable(false)
                                            .addButton("    OK    ", -1, Color.parseColor("#3e3d63"), CFAlertDialog.CFAlertActionStyle.POSITIVE,
                                                    CFAlertDialog.CFAlertActionAlignment.JUSTIFIED,
                                                    new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(final DialogInterface dialog, int which) {

                                                            docIdRef.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    if (task.isSuccessful()) {
                                                                        Toast.makeText(getApplicationContext(), "Removed this event", Toast.LENGTH_SHORT).show();
                                                                        dialog.dismiss();
                                                                        onStart();

                                                                    } else {
                                                                        Log.i(EVENT_DISPLAY, task.getException().getMessage());
                                                                    }
                                                                }
                                                            });

                                                        }
                                                    }).addButton("    CANCEL   ", Color.parseColor("#3e3d63"), Color.parseColor("#e0e0e0"), CFAlertDialog.CFAlertActionStyle.DEFAULT,
                                                    CFAlertDialog.CFAlertActionAlignment.JUSTIFIED,
                                                    new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            dialog.dismiss();
                                                        }
                                                    });
                                    builder.show();
                                }
                            });
                        }else{
                            eventFab.setVisibility(View.INVISIBLE);
                            eventFabRemove.setVisibility(View.INVISIBLE);
                            eventQRFab.setVisibility(View.INVISIBLE);
                        }
                    } else {
                        Log.i(EVENT_DISPLAY, "Document does not exist!");
                        eventFab.setVisibility(View.VISIBLE);
                        eventFabRemove.setVisibility(View.INVISIBLE);
                        eventQRFab.setVisibility(View.INVISIBLE);
                        eventFab.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Map<String, Object> postMap = new HashMap<>();
                                postMap.put("album_id",album_id);
                                postMap.put("participated","no");
                                FirebaseFirestore.getInstance().collection("Users")
                                        .document(currentUser)
                                        .collection("MyEvents")
                                        .document(album_id).set(postMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @SuppressLint("ResourceAsColor")
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if(task.isSuccessful()){
                                            Toast.makeText(getApplicationContext(),"Successfully saved to My Events",Toast.LENGTH_SHORT).show();
                                            onStart();
                                        }else {
                                            Log.i(EVENT_DISPLAY,task.getException().getMessage().toString());
                                        }
                                    }
                                });

                            }
                        });


                    }
                } else {
                    Log.d(EVENT_DISPLAY, "Failed with: ", task.getException());
                }
            }
        });
        
        



    }
}
