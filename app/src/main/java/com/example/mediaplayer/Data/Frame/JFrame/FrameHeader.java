package com.example.mediaplayer.Data.Frame.JFrame;

public class FrameHeader {
    long FrameHeaderLength;
    long Level;
    long FrameHeight;
    long FrameWidth;
    long NumberOfComponents;
    com.example.mediaplayer.Data.Frame.JFrame.FrameComponents[]FrameComponents;

    public long getFrameHeaderLength() {
        return FrameHeaderLength;
    }

    public void setFrameHeaderLength(long frameHeaderLength) {
        FrameHeaderLength = frameHeaderLength;
    }

    public long getLevel() {
        return Level;
    }

    public void setLevel(long level) {
        Level = level;
    }

    public long getFrameHeight() {
        return FrameHeight;
    }

    public void setFrameHeight(long frameHeight) {
        FrameHeight = frameHeight;
    }

    public long getFrameWidth() {
        return FrameWidth;
    }

    public void setFrameWidth(long frameWidth) {
        FrameWidth = frameWidth;
    }

    public long getNumberOfComponents() {
        return NumberOfComponents;
    }

    public void setNumberOfComponents(long numberOfComponents) {
        NumberOfComponents = numberOfComponents;
    }

    public com.example.mediaplayer.Data.Frame.JFrame.FrameComponents[] getFrameComponents() {
        return FrameComponents;
    }

    public void setFrameComponents(com.example.mediaplayer.Data.Frame.JFrame.FrameComponents[] frameComponents) {
        FrameComponents = frameComponents;
    }

    public FrameHeader(long frameHeaderLength, long level, long frameHeight, long frameWidth,
                       long numberOfComponents, com.example.mediaplayer.Data.Frame.JFrame.FrameComponents[] frameComponents) {
        FrameHeaderLength = frameHeaderLength;
        Level = level;
        FrameHeight = frameHeight;
        FrameWidth = frameWidth;
        NumberOfComponents = numberOfComponents;
        FrameComponents = frameComponents;
    }
    public void print ()
    {
        System.out.println();
        System.out.println("Frame Header Information:");
        System.out.println("Precision: "+Level);
        System.out.println("Height: "+FrameHeight);
        System.out.println("Width: "+FrameWidth);
        System.out.println("Number Of Channels: "+NumberOfComponents);
        System.out.println();
        System.out.print("Frame Components Information:");

        for(int i=0;i<NumberOfComponents;i++)
            FrameComponents[i].print();
        System.out.println("End of Frame Components Information:");

        System.out.println("End of Frame Header Information:");
    }

}
