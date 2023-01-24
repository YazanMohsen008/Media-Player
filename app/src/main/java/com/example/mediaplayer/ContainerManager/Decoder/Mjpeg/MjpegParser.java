package com.example.mediaplayer.ContainerManager.Decoder.Mjpeg;


import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.example.mediaplayer.Data.Frame.JFrame.*;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;

public class MjpegParser {
    static File SourceImage ;
    static FileInputStream JpegStream;
    static int BytesToRead = 90000000;
    static byte Buffer[] = new byte[(int)BytesToRead];
    static int CompleteAfter = 0;
    static ArrayList<HuffmanTable> HuffmanTables = new ArrayList<HuffmanTable>();
    static ArrayList<QuantizationTable> QuantizationTables = new ArrayList< QuantizationTable>();

    public MjpegParser(File SourceImage) {
        this.SourceImage=SourceImage;
    }

    /**
     * this method reads @numberOfBytes From left to Right
     * From Buffer
     * Starting from @Position
     * and make the Bytes as one Value Using Shift
     * */
    private static long BigEndianRead(byte[] buffer, int position, int numberOfBytes) {
        numberOfBytes--;
        long value = buffer[position] & 0xFF;
        for (int b = 0; b < numberOfBytes; b++) {
            value = (value << 8) + (buffer[++position] & 0xFF);
        }
        return value;
    }

    private static ArrayList<QuantizationTable> ReadQuantizationTables() {

        long LQ = BigEndianRead(Buffer, CompleteAfter, 2);
        CompleteAfter += 2;

        /** < at most cases we will have 1 or 2 tables (up to 4)>*/

        long Length = LQ - 2;

        ZigZagMap ZigZagMap = new ZigZagMap();

        while (Length != 0) {
            long PQTQ = BigEndianRead(Buffer, CompleteAfter, 1);
            CompleteAfter += 1;
            Length--;
            long PQ = PQTQ >> 4;
            /** < Getting leftmost  4bits >*/
            long TQ = PQTQ & 0xf;
            /** < Getting Rightmost 4bits >*/
            int QSize = 0;
            if (PQ == 0) {
                QSize = 64;
            }
            if (PQ == 1) {
                QSize = 128;
            }
            long Q[] = new long[QSize];
            for (int i = 0; i < QSize; i++) {
                Q[ZigZagMap.getZigZagIndex(i)] = BigEndianRead(Buffer, CompleteAfter + i, 1);
            }
            Length = Length - QSize;
             QuantizationTables.add(new QuantizationTable(LQ,TQ, Q));
            LQ -= Length;
        }

        CompleteAfter = CompleteAfter + (int) LQ - 3;
        return QuantizationTables;
    }

    private static ArrayList<HuffmanTable>      ReadHuffmanTables() {


        long LH = BigEndianRead(Buffer, CompleteAfter, 2);
        CompleteAfter += 2;

        LH -= 2;
        while (LH != 0) {

            long TableKind = BigEndianRead(Buffer, CompleteAfter, 1);
            CompleteAfter++;

            LH--;

            long TableID = TableKind & 0xf;
            TableKind = TableKind >> 4;

            long[] CodesLengthCount = new long[16];

            long allSymbols = 0;
            long[] offset = new long[17];
            for(int i=0;i<17;i++)
                offset[i] = 0;

            for (int i = 0; i < 16; i++) {
                CodesLengthCount[i] = BigEndianRead(Buffer, CompleteAfter, 1);

                allSymbols += CodesLengthCount[i];
                CompleteAfter++;
                /**
                 * The Offset store The Begin of every New Length .
                 */
                offset[i+1] = allSymbols;
                LH--;
            }

            long[] Symbols = new long[(int) allSymbols];
            for (int i = 0; i < allSymbols; i++) {
                Symbols[i] = BigEndianRead(Buffer, CompleteAfter, 1);
                CompleteAfter++;
                LH--;

            }
            HuffmanTables.add(new HuffmanTable(TableKind, TableID, CodesLengthCount, Symbols, offset));
        }

        /** Generating The Code from The Length code count .*/
       return HuffmanTables;
}
    /** This Function is generating code for Symbols . */
    private static void generateCodes(HuffmanTable HuffmanTable) {

        long code = 0;
        int ShiftAmount=0;

        for (int i = 0; i < 16; i++) {
            //System.out.println("i " +Integer.toString(i)+"  "+HuffmanTable.getOffset()[i]);
          //  System.out.println("i+1 "+ Integer.toString(i+1)+"  "+HuffmanTable.getOffset()[i+1]);
            System.out.println();
                for (int j = (int) HuffmanTable.getOffset()[i]; j < HuffmanTable.getOffset()[i + 1]; j++) {
            //        System.out.print("L: "+i+" n: "+j+" Code: "+Long.toBinaryString(code)+" ");
                    HuffmanTable.setCodes(i+1,code, j);
                    code++;
                }
                    code <<= 1;
        }

    }

    private static FrameHeader ReadFrameHeader() {

        long FL = BigEndianRead(Buffer, CompleteAfter, 2);
        CompleteAfter += 2;

        long P = BigEndianRead(Buffer, CompleteAfter, 1);
        CompleteAfter += 1;

        long Y = BigEndianRead(Buffer, CompleteAfter, 2);
        CompleteAfter += 2;

        long X = BigEndianRead(Buffer, CompleteAfter, 2);
        CompleteAfter += 2;

        long NF = BigEndianRead(Buffer, CompleteAfter, 1);
        CompleteAfter += 1;

        FrameComponents[] FrameComponents = new FrameComponents[(int) NF];
        for (int i = 0; i < NF; i++) {
            long c = BigEndianRead(Buffer, CompleteAfter + i * 3, 1);
            long HV = BigEndianRead(Buffer, CompleteAfter + 1 + i * 3, 1);

            long H = HV >> 4;
            long V = HV & 0xf;

            // The Quantization Table ID For This Component */
            long Tq = BigEndianRead(Buffer, CompleteAfter + 2 + i * 3, 1);
            FrameComponents[i] = new FrameComponents(c, H, V, Tq);
        }

        CompleteAfter = CompleteAfter + (int) (FL - 8);
        return new FrameHeader(FL, P, Y, X, NF, FrameComponents);

    }

    private static ScanHeader ReadScanHeader() {

        long LS = BigEndianRead(Buffer, CompleteAfter, 2);
        CompleteAfter += 2;

        long NS = BigEndianRead(Buffer, CompleteAfter, 1);
        CompleteAfter += 1;

        ScanComponents[] ScanComponents = new ScanComponents[(int) NS];
        for (int i = 0; i < NS; i++) {
            long cs = BigEndianRead(Buffer, CompleteAfter + i * 2, 1);

            long TATD = BigEndianRead(Buffer, CompleteAfter + 1 + i * 2, 1);

            /**
             * The DCHuffman For This Component.
             */
            long TD = TATD & 0xf;

            /**
             * The ACHuffman For This Component.
             */
            long TA = TATD >> 4;

            ScanComponents[i] = new ScanComponents(cs, TD, TA);
        }
        CompleteAfter = CompleteAfter + (int) NS * 2;

        long SS = BigEndianRead(Buffer, CompleteAfter, 1);
        CompleteAfter++;
        long SE = BigEndianRead(Buffer, CompleteAfter, 1);
        CompleteAfter++;
        long AHAI = BigEndianRead(Buffer, CompleteAfter, 1);
        CompleteAfter++;
        long AH = AHAI >> 4;
        long AI = AHAI & 0xf;

        return new ScanHeader(LS, NS, ScanComponents);

    }

    private static ArrayList<Long>             ReadHuffmanData() {



        ArrayList<Long> HuffmanData=new ArrayList<>();
        long CurrentByte = BigEndianRead(Buffer, CompleteAfter, 1);
        CompleteAfter++;
        long LastByte;
        while (true) {

            LastByte = CurrentByte;
            CurrentByte = BigEndianRead(Buffer, CompleteAfter, 1);
            CompleteAfter++;
            /**
             * this is a marker .
             */
            if (LastByte == 0xff) {

                if (CurrentByte == Tags.EOI) {
                    /** End oF Image */
                    break;
                }
                if (CurrentByte >= 0xD0 && CurrentByte <= 0xD7) {
                    /**
                     * overWrite Maker with next Byte.
                     */
                    CurrentByte = (byte) BigEndianRead(Buffer, CompleteAfter, 1);
                    CompleteAfter++;
                }

                if (CurrentByte == 0x00) {
                    HuffmanData.add(LastByte);
                    /**
                     * Add Tha Last Byte To Bitstream and ignore The Current .
                     */
                    CurrentByte = BigEndianRead(Buffer, CompleteAfter, 1);
                    CompleteAfter++;

                }
                if (CurrentByte == 0xff) {
                    /** Do nothing .*/
                    continue;
                }

            }
            else {
                /** This is the Huffman Data .*/
                HuffmanData.add(LastByte);
            }

        }
        return HuffmanData;
    }

    /**
     * This Function Reads The Jpeg File
     * And Put it in A Jpeg Frame Object
     * */

    public static JpegFrame ReadJpeg() {
        try {
            JpegStream = new FileInputStream(SourceImage);
            JpegStream.read(Buffer, 0, BytesToRead);
        } catch (Exception ex) {
            System.out.println(ex);
        }

        FrameHeader FrameHeader = null;
        ScanHeader ScanHeader = null;

        long CurrentByte = BigEndianRead(Buffer, CompleteAfter, 1);
        CompleteAfter++;
        long LastByte = 0;

        while (true) {
            LastByte = CurrentByte;
            CurrentByte = BigEndianRead(Buffer, CompleteAfter, 1);
            CompleteAfter++;
            if (LastByte == Tags.TagStart) {

                /** <Start Of Image >
                 */
                if (CurrentByte == Tags.SOI) {
                    System.out.println("SOI Reading");
                }

                /**
                 * <Start Of JIJF
                 */
                if (CurrentByte == Tags.AAP0) {
                    System.out.println("AAP0::JIJF Reading");
                    int JIJFLength = (int) BigEndianRead(Buffer, CompleteAfter, 2);
                    CompleteAfter += JIJFLength;

                }

                /**
                 * <Start Of COM
                 */
                if (CurrentByte == Tags.COM) {
                    System.out.println("Comment Reading");
                    int COMLength = (int) BigEndianRead(Buffer, CompleteAfter, 2);
                    CompleteAfter += COMLength;
                }

                /**
                 * <Start Of ee
                 */
                if (CurrentByte == 0xee) {
                    System.out.println("ee Reading");
                    int eeLength = (int) BigEndianRead(Buffer, CompleteAfter, 2);
                    CompleteAfter += eeLength;
                }

                /**
                 * <Start Of ec
                 */
                if (CurrentByte == 0xec) {
                    System.out.println("ec Reading");
                    int ecLength = (int) BigEndianRead(Buffer, CompleteAfter, 2);
                    CompleteAfter += ecLength;
                }

                /**
                 * <Start Of c2
                 */
                if (CurrentByte == 0xc2) {

                    System.out.println("c2 Reading");
                    int c2Length = (int) BigEndianRead(Buffer, CompleteAfter, 2);
                    CompleteAfter += c2Length;

                }

                /**
                 * <Start Of Quantization Table
                 */
                if (CurrentByte == Tags.DQT) {
                    System.out.println("Quantization Table reading");
                    QuantizationTables = ReadQuantizationTables();
                }

                /**
                 * <Start Of Huffman Tables
                 */
                if (CurrentByte == Tags.DHT) {
                    System.out.println("Huffman Table reading");
                    HuffmanTables = ReadHuffmanTables();
                }

                /**
                 * <Start Of Frame
                 */
                if (CurrentByte == Tags.SOF) {
                    System.out.println("Frame Header reading");
                    FrameHeader = ReadFrameHeader();
                }

                /**
                 * <Start Of Scan
                 */
                if (CurrentByte == Tags.SOS) {
                    System.out.println("Scan Header reading");
                    ScanHeader = ReadScanHeader();
                    break;
                }

            }

        }



        for (HuffmanTable HuffmanTable : HuffmanTables) {
            generateCodes(HuffmanTable);
//            HuffmanTable.print();
//            for(Long i :HuffmanTable.getOffset())
//            System.out.print(i+"   ");
//            System.out.println();
        }
       return new JpegFrame(QuantizationTables,HuffmanTables, ScanHeader,FrameHeader,ReadHuffmanData());

      }
        public ArrayList<JpegFrame> ReadMjpeg()
        {
            ArrayList<JpegFrame>MjpegStream=new ArrayList<JpegFrame>();
            MjpegStream.add(ReadJpeg());
            return MjpegStream;
        }
}

