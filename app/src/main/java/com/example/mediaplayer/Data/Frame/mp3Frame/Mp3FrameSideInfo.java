package com.example.mediaplayer.Data.Frame.mp3Frame;

public class Mp3FrameSideInfo {
    private int mainDataBegin;
    //side info data
    public int[] scfsi;
    //these data are for each granule
    public int[] part2_3_length;
    public int[] big_values;
    public int[] global_gain;
    public int[] scalefac_compress;
    public int[] win_switch_flag;
    public int[] block_type;
    public int[] mixed_block_flag;
    public int[] table_select;
    public int[] subblock_gain;
    public int[] region0_count;
    public int[] region1_count;
    public int[] preflag;
    public int[] scalefac_scale;
    public int[] count1table_select;


    public Mp3FrameSideInfo(int stereo) {
        scfsi = new int[stereo * 4];//if 1 then it tempDecodedData 4 bits else 8 bits
        //these data are for each granule
        part2_3_length = new int[stereo * 2];
        big_values = new int[stereo * 2];
        global_gain = new int[stereo * 2];
        scalefac_compress = new int[stereo * 2];
        win_switch_flag = new int[stereo * 2];
        block_type = new int[stereo * 2];
        mixed_block_flag = new int[stereo * 2];
        table_select = new int[stereo * 2 * 3];
        subblock_gain = new int[stereo * 2 * 3];
        region0_count = new int[stereo * 2];
        region1_count = new int[stereo * 2];
        preflag = new int[stereo * 2];
        scalefac_scale = new int[stereo * 2];
        count1table_select = new int[stereo * 2];
    }


    public int[] getScfsi() {
        return scfsi;
    }

    public int[] getPart2_3_length() {
        return part2_3_length;
    }

    public int[] getBig_values() {
        return big_values;
    }

    public int[] getGlobal_gain() {
        return global_gain;
    }

    public int[] getScalefac_compress() {
        return scalefac_compress;
    }

    public int[] getWin_switch_flag() {
        return win_switch_flag;
    }

    public int[] getBlock_type() {
        return block_type;
    }

    public int[] getMixed_block_flag() {
        return mixed_block_flag;
    }

    public int[] getTable_select() {
        return table_select;
    }

    public int[] getSubblock_gain() {
        return subblock_gain;
    }

    public int[] getRegion0_count() {
        return region0_count;
    }

    public int[] getRegion1_count() {
        return region1_count;
    }

    public int[] getPreflag() {
        return preflag;
    }

    public int[] getScalefac_scale() {
        return scalefac_scale;
    }

    public int[] getCount1table_select() {
        return count1table_select;
    }

    public int getMainDataBegin() {
        return mainDataBegin;
    }

    public void setMainDataBegin(int mainDataBegin) {
        this.mainDataBegin = mainDataBegin;
    }

}

