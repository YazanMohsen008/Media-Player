package com.example.mediaplayer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.session.MediaSession;
import android.os.Build;
import android.support.v4.media.session.MediaSessionCompat;

import com.example.mediaplayer.services.NotificationActionService;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;


public class MediaNotificatioin {
    public static final String CHANNEL_ID = "1";
    public static final String PREVIOUS = "actionprevious";
    public static final String PLAY = "actionplay";
    public static final String NEXT = "actionnext";

    static Notification notification;

    public static void createNotification(Context context , String name,int playButton,int pos,int size){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManagerCompat nMCompat = NotificationManagerCompat.from(context);
            MediaSessionCompat mSCompat = new MediaSessionCompat(context, "tag");
            Bitmap icon = BitmapFactory.decodeResource(context.getResources() , R.drawable.album3);

            PendingIntent pIPrevious ;
            int drw_previous;
            if(pos == 0){
                pIPrevious= null;
                drw_previous = 0;

            }else {
                Intent intentPrevious = new Intent(context , NotificationActionService.class).setAction(PREVIOUS);
                pIPrevious = PendingIntent.getBroadcast(context, 0, intentPrevious,
                        PendingIntent.FLAG_UPDATE_CURRENT);
                drw_previous = R.drawable.previous_notifi;
            }

            Intent playIntent = new Intent(context , NotificationActionService.class).setAction(PLAY);
            PendingIntent pIPlay = PendingIntent.getBroadcast(context, 0, playIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            PendingIntent pINext ;
            int drw_next;
            if(pos == size){
                pINext= null;
                drw_next = 0;

            }else {
                Intent nextIntent = new Intent(context , NotificationActionService.class).setAction(NEXT);
                pINext = PendingIntent.getBroadcast(context, 0, nextIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
                drw_next = R.drawable.next_notifi;
            }

            notification = new NotificationCompat.Builder(context,CHANNEL_ID)
                    .setLargeIcon(icon)
                    .setSmallIcon(R.drawable.music_notifi)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setShowWhen(false)
                    .setOnlyAlertOnce(true)
                    .setContentTitle(name)
                    .addAction(drw_previous,"previous" ,pIPrevious)
                    .addAction(playButton,"play",pIPlay)
                    .addAction(drw_next,"next",pINext)
                    .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0 , 1 , 2)
                        .setMediaSession(mSCompat.getSessionToken()))
                    .build();

            nMCompat.notify(1,notification);
        }
    }
}
