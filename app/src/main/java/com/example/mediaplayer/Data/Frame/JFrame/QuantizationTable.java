package com.example.mediaplayer.Data.Frame.JFrame;
/**
 <
 * This class is Table inside The Jpeg File Used To
 * Know The DCT Values .

 * Every Value on @Values Matrix corresponding to value(pixel) from
 * the MCU Channel

 * We Can Define More Than one Table with Same Tag @DQT(FFDB)
 But The @LQ Length will involve The Size of the Two Tables


 QValue | QValue | QValue | QValue | QValue | QValue | QValue | QValue |
 QValue | QValue | QValue | QValue | QValue | QValue | QValue | QValue |
 QValue | QValue | QValue | QValue | QValue | QValue | QValue | QValue |
 QValue | QValue | QValue | QValue | QValue | QValue | QValue | QValue |
 QValue | QValue | QValue | QValue | QValue | QValue | QValue | QValue |
 QValue | QValue | QValue | QValue | QValue | QValue | QValue | QValue |
 QValue | QValue | QValue | QValue | QValue | QValue | QValue | QValue |
 QValue | QValue | QValue | QValue | QValue | QValue | QValue | QValue |

 >
 */
public class QuantizationTable {
    long   TableLength;
    long   TableKind;
    long[] Values;

    public QuantizationTable(long tableLength, long tableKind, long[] values) {
        TableLength = tableLength;
        TableKind = tableKind;
        Values = values;
    }

    public long getTableLength() {
        return TableLength;
    }

    public void setTableLength(long tableLength) {
        TableLength = tableLength;
    }

    public long getTableKind() {
        return TableKind;
    }

    public void setTableKind(long tableKind) {
        TableKind = tableKind;
    }

    public long[] getValues() {
        return Values;
    }

    public void setValues(long[] values) {
        this.Values = values;
    }
    public void print ()
    {
        System.out.println("QuantizationTable : ");
        System.out.println("Table Length : "+TableLength);
        System.out.println("Table Kind : "+TableKind);
        for (int i=1;i<=Values.length;i++){
            System.out.print(Values[i-1]+" | ");
            if(i!=0&&i%8==0)
                System.out.println();
        }
        System.out.println();

    }
}
