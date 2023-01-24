package com.example.mediaplayer.Data.Frame.JFrame;

public class ScanComponents {
    long ComponentID;
    long DCHuffmanTable;
    long ACHuffmanTable;

    public long getComponentID() {
        return ComponentID;
    }

    public void setComponentID(long componentID) {
        ComponentID = componentID;
    }

    public long getDCHuffmanTable() {
        return DCHuffmanTable;
    }

    public void setDCHuffmanTable(long DCHuffmanTable) {
        this.DCHuffmanTable = DCHuffmanTable;
    }

    public long getACHuffmanTable() {
        return ACHuffmanTable;
    }

    public void setACHuffmanTable(long ACHuffmanTable) {
        this.ACHuffmanTable = ACHuffmanTable;
    }

    public ScanComponents(long componentID, long DCHuffmanTable, long ACHuffmanTable) {
        ComponentID = componentID;
        this.DCHuffmanTable = DCHuffmanTable;
        this.ACHuffmanTable = ACHuffmanTable;
    }
    public void print()
    {

        System.out.println("Component  ID: "+ComponentID);
        System.out.println("Huffman DC ID: "+DCHuffmanTable);
        System.out.println("Huffman AC ID: "+ACHuffmanTable);
        System.out.println();
    }
}
