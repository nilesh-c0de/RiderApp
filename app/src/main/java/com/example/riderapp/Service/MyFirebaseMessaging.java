package com.example.riderapp.Service;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.riderapp.Model.Token;
import com.example.riderapp.RouteActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessaging extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        updateTokenToServer(refreshedToken);
    }

    private void updateTokenToServer(String refreshedToken) {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference tokens = db.getReference("Tokens");

        Token token = new Token(refreshedToken);


        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            tokens.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(token);
        }
    }

    @Override
    public void onMessageReceived(final RemoteMessage remoteMessage) {

        //nothing

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MyFirebaseMessaging.this, "" + remoteMessage.getNotification().getBody(), Toast.LENGTH_SHORT).show();

            }
        });


        String information = remoteMessage.getNotification().getBody();
        if(information!=null)
        {
            if(information.equals("arrived") || information.equals("trip") || information.equals("completed"))
            {
                Intent message = new Intent("shortMessage");
                message.putExtra("sm", information);
                sendBroadcast(message);
            }
        }

        // storing title of notification
        String msg = remoteMessage.getNotification().getTitle();



        // checking
        if (msg != null) {


            if (msg.equals("Accepted")) {

                Intent acceptIntent = new Intent("MyMessage");
                acceptIntent.putExtra("msg", "Accepted");
                sendBroadcast(acceptIntent);


                Intent intent = new Intent(getBaseContext(), RouteActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("driverUid", remoteMessage.getNotification().getBody());
                startActivity(intent);
            }
            if (msg.equals("Rejected")) {
                Intent canceltIntent = new Intent("MyMessage");
                canceltIntent.putExtra("msg", "Rejected");
                sendBroadcast(canceltIntent);
            }

        }


    }
}
