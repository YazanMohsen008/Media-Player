package com.example.mediaplayer.ContainerManager.Decoder.Mjpeg;

public class Tags {

    public static final long  TagStart =0xff;  /**start of Tag . */
    public static final long  SOI=0xd8;       /**start of Image . */
    public static final long  EOI=0xd9;       /**End of Image . */
    public static final long  AAP0=0xe0;      /** JIJF App . */
    public static final long  COM=0xfe;       /** Comment. */
    public static final long  DQT=0xdb;       /** Quantization Table. */
    public static final long  DHT=0xc4;       /** Huffman Table . */
    public static final long  SOF=0xc0;       /**Start of Frame (Baseline (FFC0) That means We Have only one Haffman Table . */
    public static final long  SOS=0xda;       /**Start of Scan . */
    public static final long  DRI=0xDD;       /** <   We Might have here DRI:  Restart Interval is set to tell That Every number of DCs
     Don't Add  previous Dc to the Current.*/


}