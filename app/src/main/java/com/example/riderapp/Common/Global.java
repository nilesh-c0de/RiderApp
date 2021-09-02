package com.example.riderapp.Common;

import com.example.riderapp.Remote.FCMClient;
import com.example.riderapp.Remote.IFCMService;

public class Global {

    private static final String fcmURL = "https://fcm.googleapis.com/";

    public static IFCMService getFCMService() {
        return FCMClient.getClient(fcmURL).create(IFCMService.class);
    }

}
