package com.example.mediaplayer.Data.Frame.JFrame;

import com.example.mediaplayer.Data.Frame.Frame;

import java.nio.Buffer;
import java.util.ArrayList;

public class JpegFrame extends Frame {
    ArrayList<QuantizationTable> QuantizationTables;
    ArrayList<HuffmanTable> HuffmanTables;
    ScanHeader ScanHeader;
    FrameHeader FrameHeader;
    ArrayList<MCU> MCUs;
    ArrayList<Long>HuffamnData;


    public JpegFrame(ArrayList<QuantizationTable> quantizationTables, ArrayList<HuffmanTable> huffmanTables,
                     ScanHeader scanHeader,
                     FrameHeader frameHeader, ArrayList<Long>HuffamnData) {
        QuantizationTables = quantizationTables;
        HuffmanTables = huffmanTables;
        ScanHeader = scanHeader;
        FrameHeader = frameHeader;
        this.HuffamnData = HuffamnData;
        MCUs=new ArrayList<>();
        for(MCU Mcu:MCUs)
            Mcu=new MCU();

    }

    public ArrayList<QuantizationTable> getQuantizationTables() {
        return QuantizationTables;
    }

    public void setQuantizationTables(ArrayList<QuantizationTable> quantizationTables) {
        QuantizationTables = quantizationTables;
    }

    public ArrayList<HuffmanTable> getHuffmanTables() {
        return HuffmanTables;
    }

    public void setHuffmanTables(ArrayList<HuffmanTable> huffmanTables) {
        HuffmanTables = huffmanTables;
    }

    public ScanHeader getScanHeader() {
        return ScanHeader;
    }

    public void setScanHeader(ScanHeader scanHeader) {
        ScanHeader = scanHeader;
    }

    public FrameHeader getFrameHeader() {
        return FrameHeader;
    }

    public void setFrameHeader(FrameHeader frameHeader) {
        FrameHeader = frameHeader;
    }

    public ArrayList<MCU> getMCUs() {
        return MCUs;
    }

    public void setMCUs(ArrayList<MCU> MCUs) {
        this.MCUs = MCUs;
    }
    public QuantizationTable getQuantizationTables(int i) {
        return QuantizationTables.get(i);
    }
    public ArrayList<Long> getHuffamnData() {
        return HuffamnData;
    }

    public void setHuffamnData(ArrayList<Long> HuffamnData) {
        this.HuffamnData = HuffamnData;
    }


    public HuffmanTable getHuffmanTable(int TableKind,int TableID) {
        if(TableID==2)TableID--;
        for(int i=0;i<HuffmanTables.size();i++)
        {
            if(HuffmanTables.get(i).getTableKind()==TableKind)
                if(HuffmanTables.get(i).getTableID()==TableID)
                    return HuffmanTables.get(i);
        }
        return null;
    }
    public void print(){
        for(MCU Q: MCUs)
        {
            Q.print();
        }
    }


}
