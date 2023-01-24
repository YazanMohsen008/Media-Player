package com.example.mediaplayer.Data.Frame.JFrame;

public class ScanHeader {
long ScanHeaderLength;
long NumberOfComponents;
ScanComponents[] ScanComponents;

    public ScanHeader(long scanHeaderLength, long numberOfComponents,ScanComponents[] scanComponents) {
        ScanHeaderLength = scanHeaderLength;
        NumberOfComponents = numberOfComponents;
        ScanComponents = scanComponents;
    }

    public long getScanHeaderLength() {
        return ScanHeaderLength;
    }

    public void setScanHeaderLength(long scanHeaderLength) {
        ScanHeaderLength = scanHeaderLength;
    }

    public long getNumberOfComponents() {
        return NumberOfComponents;
    }

    public void setNumberOfComponents(long numberOfComponents) {
        NumberOfComponents = numberOfComponents;
    }

    public ScanComponents[] getScanComponents() {
        return ScanComponents;
    }

    public void setScanComponents(ScanComponents[] scanComponents) {
        ScanComponents = scanComponents;
    }

    public void print ()
    {
        System.out.println();
        System.out.println("Scan Header Information:");
        System.out.println("ScanHeader  Length:   "+ScanHeaderLength);
        System.out.println("Number of components: "+NumberOfComponents);
        System.out.println("Scan Components: ");

        for(int i=0;i<NumberOfComponents;i++)
            ScanComponents[i].print();
        System.out.println("End of Scan Components: ");
        /*
        System.out.println("start of selection  : "+SS);
        System.out.println("End of selection  : "+SE);
        System.out.println("AH  : "+AH);
        System.out.println("AI  : "+AI);
        */
        System.out.println();
        System.out.println("End OF Scan Header Information:");



    }
}
