package com.example.mediaplayer.Data.Frame.JFrame;
/**
 There is  Huffman Table
 For Every MCU

 Huffman Tables is 4 Kinds :
 * Luminance DC Huffman Table
 * Luminance AC Huffman Table
 * Color     DC Huffman Table
 * Color     AC Huffman Table



 *Codes Length Array
 * codes which Length 0 bit :x
 * codes which Length 1 bit :x
 * codes which Length 2 bit :x
 * codes which Length 3 bit :x
 * codes which Length 4 bit :x
 * codes which Length 5 bit :X
 * codes which Length 6 bit :x
 *                   .
 *                   .
 *                   .

 * codes which Length 16 bit :x

 Decoding Using Huffman Tables

 Code - > Symbol

 * we Generate a Code for every Symbol Depending on the length of Codes Array
 * when we read A Huffman Data we Read bit by bit to get one of the Codes then will get the Symbol
 * The Symbol Tell us How Many Bits The Coefficient is
 .


 */

public class HuffmanTable {
    long    TableKind;// Ac/Dc
    long    TableID; // Luminance/chrome

    long [] CodesLength=new long [16];
    long [] Symbols =new long [162];
    long [] Offset=new long [17];
    long    allSymbols;
    long [] Codes=new long [162];
    long [] CodesSize=new long [162];


    public HuffmanTable() {
    }

    public HuffmanTable(long tableKind, long tableID, long[] CodesLength, long[] symbols,
                        long[] offset){
        TableKind = tableKind;
        TableID = tableID;
        this.CodesLength = CodesLength;
        Symbols = symbols;
        Offset = offset;
        for (int i = 0; i < 16; i++) {
            allSymbols += CodesLength[i];}
    }

    public long getTableKind() {
        return TableKind;
    }

    public void setTableKind(long tableKind) {
        TableKind = tableKind;
    }

    public long getTableID() {
        return TableID;
    }

    public void setTableID(long tableID) {
        TableID = tableID;
    }

    public long[] getCodesLength() {
        return CodesLength;
    }

    public void setCodesLength(long[] CodesLength) {
        this.CodesLength = CodesLength;
    }

    public long[] getSymbols() {
        return Symbols;
    }

    public void setSymbols(long[] symbols) {
        Symbols = symbols;
    }

    public long[] getOffset() {
        return Offset;
    }

    public void setOffset(long[] offset) {
        Offset = offset;
    }

    public long[] getCodes() {
        return Codes;
    }
    public long[] getCodesSize() {
        return CodesSize;
    }

    public void setCodes(long[] codes) {
        Codes = codes;
    }
    public void setCodes(long CodeSize,long Code,int index) {
        this.Codes[index] = Code;
        this.CodesSize[index] = CodeSize;

    }
    public void print ()

    {
        System.out.println();
        System.out.println("HuffmanTable: ");
        if(TableKind==0)
            System.out.print("DC ");

        if(TableKind==1)
            System.out.print("AC ");

        if(TableID==0)
            System.out.println("Luminance Table");

        if(TableID==1)
            System.out.println("Color Table");
            /*
        // To print every  Code Length's Count
         for (int CodeLength = 1; CodeLength<=16; CodeLength++) {
         System.out.println("Codes Their Length is "+ CodeLength +" bit are :"+ CodesLengthCount[CodeLength-1]);
         }
         System.out.println("");
            */

        /** To print ALL Symbol
         for (int Symbol = 1 ; Symbol . <=AllSymbols; Symbol++) {
         System.out.println("Symbol "+ Symbol +"  is :"+ Symbols[Symbol-1]);
         }
         . */


        for (int i = 0 ; i <16; i++) {
            System.out.println();
            System.out.print("Symbols Length: "+(i+1)+" bit are: ");
            for(int j=(int)Offset[i];j<Offset[i+1];j++)
                System.out.print(Long.toHexString(Symbols[j])+"  ");
        }
        System.out.println();

        System.out.print("Code"+"     ");
        System.out.println("Symbol");

        for(int i=0;i<Symbols.length;i++)
        {
            System.out.print(Long.toBinaryString(Codes[i])+"      ");

            System.out.print(Long.toHexString(Symbols[i])+"  ");
            System.out.println();

        }




    }


}
