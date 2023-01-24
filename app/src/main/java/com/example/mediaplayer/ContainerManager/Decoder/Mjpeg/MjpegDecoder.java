package com.example.mediaplayer.ContainerManager.Decoder.Mjpeg;

import android.content.Context;
import android.util.Log;

import com.example.mediaplayer.ContainerManager.Decoder.Decoder;
import com.example.mediaplayer.Data.Container.Container;
import com.example.mediaplayer.Data.Frame.JFrame.*;

import java.io.File;
import java.util.ArrayList;

public class MjpegDecoder extends Decoder {
    MjpegParser MjpegParser;
    File JpegFile;
    static ArrayList<JpegFrame> MjpegStream=new ArrayList<JpegFrame>();

    public MjpegDecoder(Container container, File File) {
        super(container);
        this.JpegFile=File;
        MjpegParser=new MjpegParser(this.JpegFile);
    }

    private ArrayList<JpegFrame> Parse ()
    {
        return MjpegParser.ReadMjpeg();
    }
    @Override
    public void decode() {
          MjpegStream = Parse();
          DecodeHuffmandata(MjpegStream.get(0));

    }
    static Long []previousDC;
    /*** This Function is Decoding Every MCU . */
    private static void DecodeHuffmandata(JpegFrame JpegFrame) {
        long MCUHeight = (JpegFrame.getFrameHeader().getFrameHeight() + 7) / 8;
        long MCUWidth = (JpegFrame.getFrameHeader().getFrameWidth() + 7) / 8;

        previousDC=new Long[3];
        for(int i=0;i<3;i++)previousDC[i]=(long)0;

        /** We have MCUHeight*MCUWidth in photo */
        MCU[] MCUs = new MCU[(int) (MCUHeight * MCUWidth)];
        BitReader HuffmanDataBitReader = new BitReader(JpegFrame.getHuffamnData());

        for (int i = 0; i < MCUHeight * MCUWidth; i++) {
            MCUs[i]=new MCU();
        }

        for (int i = 0; i < MCUHeight * MCUWidth; i++) {
            for (int j = 0; j < JpegFrame.getFrameHeader().getNumberOfComponents(); j++) {
                /** Here We Decode Every Channel of Every MCU */
               decodeMCUComponent(HuffmanDataBitReader, MCUs[i].getComponent(j+1),
                        JpegFrame.getHuffmanTable(0, j), JpegFrame.getHuffmanTable(1, j),j);
            }
        }
        /*
        decodeMCUComponent(HuffmanDataBitReader, MCUs[0].getComponent(1),
                JpegFrame.getHuffmanTable(0, 0), JpegFrame.getHuffmanTable(1, 0),
                0);
        decodeMCUComponent(HuffmanDataBitReader, MCUs[0].getComponent(2),
                JpegFrame.getHuffmanTable(0, 1), JpegFrame.getHuffmanTable(1, 1),
                1);*/
        DeQuantize(JpegFrame,MCUs);
       InverseDCTMCU(JpegFrame,MCUs);
        YCbCrToRGB(JpegFrame,MCUs);
//        for (int i = 0; i < MCUHeight * MCUWidth; i++) {
//            MCUs[i].print();
//        }
    }
    private static boolean decodeMCUComponent(BitReader HuffmanDataBitReader, long [] Component,
                                              HuffmanTable DCHuffmanTable, HuffmanTable ACHuffmanTable,int ComponentID)
    {
        long length = GetNextSymbol(HuffmanDataBitReader, DCHuffmanTable);
//        System.out.println("Length: "+length);
        if (length == -1 || length > 11) {
        //    System.err.println("invalid Length" + length);
        }
        /** DC Component */
        long coff = HuffmanDataBitReader.readBit(length);/*

        /**
         * Coff might be negative so 3 bit can represent -7,-6,-5,-4,4,5,6,7
         * instead 0 ,1 , 2, 3, 4, 5, 6 ,7 so if Coff is less than 2^(length-1)
         * if(ex:coff is 0 less than 4 :2^(3-1)) so we will subtract from coff
         * (2^length)-1(ex:coff is 0 -=(2^3)-1)so 0 -> -7 .
         *
         */
        if (length != 0 && coff < (1 << (length - 1))) {
            coff -= (1 << length) - 1;
        }
        coff+=previousDC[ComponentID];
        previousDC[ComponentID]=coff;
        Component[0]=coff;



        /*** AC Component */
        int i = 1;
        ZigZagMap ZigZagMap = new ZigZagMap();
        while (i < 64) {
            long Symbol = GetNextSymbol(HuffmanDataBitReader, ACHuffmanTable);
          //  System.out.println("Symbol: "+Symbol);

            if (Symbol == -1) {
         //       System.err.println("invalid Symbol" + Symbol);
            }

            /**if we have Special Symbol 0x00 continue The Rest with zeros.*/
            if (Symbol == 0x00) {
                for (; i < 64; i++) {
                    Component[ZigZagMap.getZigZagIndex(i)] = 0;
                }

                return true;
            }

            long numZeros = Symbol >> 4;
            long CoffLength = Symbol & 0x0f;

            if (Symbol == 0xF0) {
                numZeros = 16;
            }

            for (int zerosCounter = 0; i<64 && zerosCounter < numZeros; zerosCounter++, i++) {
                Component[ZigZagMap.getZigZagIndex(i)] = 0;
            }

            if (CoffLength == 0 || CoffLength > 10) {
                System.err.println("Coff Length is invalid " + CoffLength);

            }

            coff = HuffmanDataBitReader.readBit(CoffLength);

            if (coff == -1) {
             //   System.err.println("Coff  is invalid " + coff);
            }

            if (coff < (1 << (CoffLength - 1))) {
                coff -= (1 << CoffLength) - 1;
            }
            if(i<64)
            Component[ZigZagMap.getZigZagIndex(i)] = 0;
            i++;

        }
        return true;

    }
    /** This Function is For Getting The Next Symbol From The Huffman Table By its code . */
    private static long    GetNextSymbol(BitReader HuffmanDataBitReader, HuffmanTable HuffmanTable) {

        /**Reading DC .*/
        long currentCode = 0;
        long currentCodeSize = 1;


        for (int i = 0; i < 16; i++) {
            long bit = HuffmanDataBitReader.readBit();
            if (bit == -1) {
                return -1;
            }
            currentCode = currentCode << 1 | bit;
            for (int j = (int) HuffmanTable.getOffset()[i]; j < HuffmanTable.getOffset()[i + 1]; j++) {
                if (currentCodeSize==HuffmanTable.getCodesSize()[j]&&
                        currentCode == HuffmanTable.getCodes()[j]) {
                    {
                        return HuffmanTable.getSymbols()[j];
                    }
                }
            }
            currentCodeSize++;
        }
        return -1;
    }


    /**DeQuantization*/

    private static void  DeQuantize(JpegFrame JpegFrame ,MCU[] MCUs)
    {
        long MCUHeight=( JpegFrame.getFrameHeader().getFrameHeight()+7)/8;
        long MCUWidth = (JpegFrame.getFrameHeader().getFrameWidth()+7)/8;

        for (int i = 0; i < MCUHeight * MCUWidth; i++) {
            for (int j = 0; j < JpegFrame.getFrameHeader().getNumberOfComponents(); j++) {
                /**Here We Decode Every Channel of Every MCU */

                dequantizeMCUComponent(JpegFrame.getQuantizationTables((int)JpegFrame.getFrameHeader().getFrameComponents()[j].
                                getQuantizationTableID()), MCUs[i].getComponent(j+1));

            }
        }
    }

    private static void dequantizeMCUComponent(QuantizationTable QuantizationTable, long[]Components)
    {

        for(int i=0;i<64;i++)
            Components[i]*=QuantizationTable.getValues()[i];
    }

    private static void  InverseDCTMCU(JpegFrame JpegFrame,MCU[] MCUs)
    {
        long MCUHeight=( JpegFrame.getFrameHeader().getFrameHeight()+7)/8;
        long MCUWidth = (JpegFrame.getFrameHeader().getFrameWidth()+7)/8;

        for (int i = 0; i < MCUHeight * MCUWidth; i++) {
            for (int j = 0; j < JpegFrame.getFrameHeader().getNumberOfComponents(); j++) {

                /**Here We Decode Every Channel of Every MCU .*/
                InverseDCTComponent( MCUs[i].getComponent(j+1));

            }
        }
    }
    private static void  InverseDCTComponent(long []Component)
    {
        long []result=new long[64];
        for(int y=0;y<8;y++ )
            for(int x=0;x<8;x++ )
            {
                double sum=0;
                for(int v=0;v<8;v++ )
                    for(int u=0;u<8;u++ )
                    {
                        double cv=1.0;
                        double cu=1.0;
                        if(v==0)cv=1.0/Math.sqrt(2);
                        if(u==0)cu=1.0/Math.sqrt(2);
                        sum+=cv*cu*Component[v*8+u]*Math.cos((2.0*x+1.0)*u*Math.PI/16)*Math.cos((2.0*y+1.0)*v*Math.PI/16);
                    }
                sum/=4;
                result[y*8+x]=(long)sum;
            }
        for(int i=0;i<64;i++ )
            Component[i]=result[i];
    }


    private static void  YCbCrToRGB(JpegFrame JpegFrame,MCU[] MCUs)
    {

        long MCUHeight=( JpegFrame.getFrameHeader().getFrameHeight()+7)/8;
        long MCUWidth = (JpegFrame.getFrameHeader().getFrameWidth()+7)/8;

        for (int i = 0; i < MCUHeight * MCUWidth; i++)
            YCbCrToRGBMCU(MCUs[i]);




    }

    private static void  YCbCrToRGBMCU(MCU MCU)
    {

        for(int i=0;i<64;i++)
        {
            long R=MCU.getComponent(1)[i]+(long)1.402f*MCU.getComponent(3)[i]+128;
            long G=MCU.getComponent(1)[i]-(long)0.344f*MCU.getComponent(2)[i]-(long)0.714f*MCU.getComponent(3)[i]+128;
            long B=MCU.getComponent(1)[i]+(long)1.772f*MCU.getComponent(2)[i]+128;

            if(R<0)
                R=0;

            if(G<0)
                G=0;

            if(B<0)
                B=0;

            if(R>255)
                R=255;


            if(G>255)
                G=255;


            if(G>255)
                G=255;

            MCU.getComponent(1)[i]=R;
            MCU.getComponent(2)[i]=G;
            MCU.getComponent(3)[i]=B;
            MjpegStream.get(0).getMCUs().add(MCU);
        }

    }

    public static ArrayList<JpegFrame> getMjpegStream() {
        return MjpegStream;
    }
}
