package com.example.mediaplayer.MediaControl;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.example.mediaplayer.Data.Container.Container;
import com.example.mediaplayer.Data.Container.mp4.MB4Container;
import com.example.mediaplayer.Data.Frame.JFrame.MCU;

import java.util.ArrayList;

public class VideoRender {
    ArrayList<MCU> MCUs;

    public VideoRender(Container container, long FrameWidth,ArrayList<MCU> MCUS) {

        MCUs=new ArrayList<>();
        this.MCUs=MCUS;
        ((MB4Container)container).getTraks().get(0);

    }
    public void draw(){
        Paint myPaint = new Paint();
        int left=0;
        int top=8;
        int right=8;
        int bottom=0;
        for(MCU MCU:MCUs) {
            myPaint.setColor(Color.rgb((int) MCU.getY()[0], (int) MCU.getCb()[0], (int) MCU.getCr()[0]));
            left+=8;
            right+=8;
                if(MCU.getWidth()==left) {
                    left = 0;
                    top+=8;
                    bottom+=8;

                }
                    new Canvas().drawRect(left, top, right, bottom, myPaint);

        }
    }
}
