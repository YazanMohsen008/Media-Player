package com.example.mediaplayer.Data.Frame.JFrame;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;

public class MCU {



    /**


     DC | AC | AC | AC | AC | AC | AC | AC |
     AC | AC | AC | AC | AC | AC | AC | AC |
     AC | AC | AC | AC | AC | AC | AC | AC |
     AC | AC | AC | AC | AC | AC | AC | AC |
     AC | AC | AC | AC | AC | AC | AC | AC |
     AC | AC | AC | AC | AC | AC | AC | AC |
     AC | AC | AC | AC | AC | AC | AC | AC |

     • The DC is Relative DC Means To get The Real Value You have to add The previous DC
     • First DC is a real Value
     • Defining a DRI Means That The DC with number RI is a Real Value
     */


        long []Y=new long [64];
        long []Cb=new long [64];
        long []Cr=new long [64];
        long width;
        long height;

        public long[] getCb() {
            return Cb;
        }

        public void setCb(long[] Cb) {
            this.Cb = Cb;
        }

        public long[] getCr() {
            return Cr;
        }

        public void setCr(long[] Cr) {
            this.Cr = Cr;
        }

        public long getWidth() {
            return width;
        }

        public void setWidth(long width) {
            this.width = width;
        }

        public long getHeight() {
            return height;
        }

        public void setHeight(long height) {
            this.height = height;
        }


        public MCU() {
            for(int i=0;i<64;i++)
            {
                Y[i]=new Long(0);
                Cb[i]=new Long(0);
                Cr[i]=new Long(0);
            }
        }

        public MCU(long []Y,long []Cb,long []Cr) {
            this();this.Y=Y;
            this.Cb=Cb;
            this.Cr=Cr;
        }


        public long[] getY() {
            return Y;
        }

        public void setY(long[] Y) {
            this.Y = Y;
        }


        public long[] getComponent(int i) {

            switch(i){
                case 1:  return Y;
                case 2:  return Cb;
                case 3:  return Cr;



            }
            return null;
        }

        public void print()
        {
            System.out.println("");
            System.out.println("");

            System.out.println("Luminance");
            for(int i=0;i<64;i++){
                if(i%8==0)
                    System.out.println();
                System.out.print(Y[i]+" " );
            }
            System.out.println("");

            System.out.println("");
            System.out.println("Cb");
            for(int i=0;i<64;i++)
            {if(i%8==0)
                System.out.println();
                System.out.print(Cb[i]+" ");
            }
            System.out.println("");
            System.out.println("");

            System.out.println("Cr");
            for(int i=0;i<64;i++){
                if(i%8==0)
                    System.out.println();

                System.out.print(Cr[i]+" ");
            }

        }


    }
