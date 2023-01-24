package com.example.mediaplayer.Data.Frame.JFrame;

public class FrameComponents {
    long ComponentID;
    long HeightFactor;
    long WidthFactor;
    long QuantizationTableID;

    public long getComponentID() {
        return ComponentID;
    }

    public void setComponentID(long componentID) {
        ComponentID = componentID;
    }

    public long getHeightFactor() {
        return HeightFactor;
    }

    public void setHeightFactor(long heightFactor) {
        HeightFactor = heightFactor;
    }

    public long getWidthFactor() {
        return WidthFactor;
    }

    public void setWidthFactor(long widthFactor) {
        WidthFactor = widthFactor;
    }

    public long getQuantizationTableID() {
        return QuantizationTableID;
    }

    public void setQuantizationTableID(long quantizationTableID) {
        QuantizationTableID = quantizationTableID;
    }

    public FrameComponents(long componentID, long heightFactor, long widthFactor, long quantizationTableID) {
        ComponentID = componentID;
        HeightFactor = heightFactor;
        WidthFactor = widthFactor;
        QuantizationTableID = quantizationTableID;
    }
    public void print ()
    {

        System.out.println();
        System.out.println("Channel Information:");
        System.out.println("Channel ID: "+ComponentID);
        System.out.println("Horizontal Sampling factor: "+HeightFactor);
        System.out.println("Vertical Sampling factor: "+WidthFactor);
        System.out.println("QuantizationTable Table ID: "+QuantizationTableID);

    }
}
