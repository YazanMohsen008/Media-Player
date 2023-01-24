package com.example.mediaplayer.ContainerManager.Decoder.mp3Decoder;

import com.example.mediaplayer.ContainerManager.Decoder.Decoder;
import com.example.mediaplayer.ContainerManager.Parser.WavParser.WavFileException;
import com.example.mediaplayer.Data.Container.Container;
import com.example.mediaplayer.Data.Frame.mp3Frame.Mp3StandardData;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class Mp3Decoder  {

    //SoundData soundData;

    public static SoundData init(InputStream in) throws IOException {
        if(in == null)
            return null;
        Buffer buffer = new Buffer(in);

        while (buffer.lastByte != -1) {
            SoundData soundData = new SoundData();
            soundData.buffer = buffer;
            try {
                if (Mp3Decoder.decodeFrame(soundData)) {

                    FrameHeader adjacentHeader = Mp3Decoder.findNextHeader(soundData, 1);
                    if (adjacentHeader != null) {

                        adjacentHeader.unRead(soundData);
                        return soundData;
                    }
                }

            } catch (NegativeArraySizeException e) {
            } catch (NullPointerException e) {
            } catch (ArrayIndexOutOfBoundsException e) {
            }
        }
        return null;
    }

    private static FrameHeader findNextHeader(SoundData soundData) {
        return Mp3Decoder.findNextHeader(soundData, Integer.MAX_VALUE);
    }

    private static FrameHeader findNextHeader(SoundData soundData, int maxBytesSkipped) {

        try {
            FrameHeader header = new FrameHeader(soundData);

            int skipped = 0;
            while (!header.isValid()) {
                if (soundData.buffer.lastByte == -1 || skipped >= maxBytesSkipped) {
                    return null;
                }
                skipped++;
                soundData.buffer.in.reset();

                soundData.buffer.lastByte = soundData.buffer.in.read();
                header.set(soundData);
            }
            return header;

        } catch (IOException e) {

            return null;
        }
    }


    public static boolean decodeFrame(SoundData soundData) throws IOException {
        if (soundData.buffer.lastByte == -1) {
            return false;
        }

        FrameHeader header = Mp3Decoder.findNextHeader(soundData);
        if (header == null) {
            return false;
        }


        if (soundData.frequency == -1) {
            soundData.frequency = Mp3StandardData.SAMPLING_FREQUENCY[header.samplingFrequency];
        }

        if (soundData.stereo == -1) {
            if (header.mode == 0b11 /* single_channel */) {
                soundData.stereo = 0;
            } else {
                soundData.stereo = 1;
            }
            if (header.layer == 0b01 /* layer III */) {
                if (header.mode == 0b11 /* single_channel */) {
                    soundData.mainData = new byte[1024];
                    soundData.store = new float[32 * 18];
                    soundData.v = new float[1024];
                } else {
                    soundData.mainData = new byte[2 * 1024];
                    soundData.store = new float[2 * 32 * 18];
                    soundData.v = new float[2 * 1024];
                }
                soundData.mainDataReader = new MainDataReader(soundData.mainData);
            } else {
                if (header.mode == 0b11 /* single_channel */) {
                    soundData.synthOffset = new int[]{64};
                    soundData.synthBuffer = new float[1024];
                } else {
                    soundData.synthOffset = new int[]{64, 64};
                    soundData.synthBuffer = new float[2 * 1024];
                }
            }
        }

        int bound = header.modeExtension == 0b0 ? 4 : header.modeExtension == 0b01 ? 8 : header.modeExtension == 0b10 ? 12 : header.modeExtension == 0b11 ? 16 : -1;

        if (header.protectionBit == 0) {

            read(soundData.buffer, 16);
        }

        if (header.layer == 0b11 /* layer I */) {
            float[] sampleDecoded = null;
            if (header.mode == 0b11 /* single_channel */) {
                sampleDecoded = samples_I(soundData.buffer, 1, -1);
            } else if (header.mode == 0b0 /* stereo */ || header.mode == 0b10 /* dual_channel */) {
                sampleDecoded = samples_I(soundData.buffer, 2, -1);
            } else if (header.mode == 0b01 /* intensity_stereo */) {
                sampleDecoded = samples_I(soundData.buffer, 2, bound);
            }
            if (header.mode == 0b11 /* single_channel */) {
                synth(soundData, sampleDecoded, soundData.synthOffset, soundData.synthBuffer, 1);
            } else {
                synth(soundData, sampleDecoded, soundData.synthOffset, soundData.synthBuffer, 2);
            }
        } else if (header.layer == 0b10 /* layer II */) {
            float[] sampleDecoded = null;
            int bitrate = Mp3StandardData.BITRATE_LAYER_II[header.bitrateIndex];
            if (header.mode == 0b11 /* single_channel */) {
                sampleDecoded = samples_II(soundData.buffer, 1, -1, bitrate, soundData.frequency);
            } else if (header.mode == 0b0 /* stereo */ || header.mode == 0b10 /* dual_channel */) {
                sampleDecoded = samples_II(soundData.buffer, 2, -1, bitrate, soundData.frequency);
            } else if (header.mode == 0b01 /* intensity_stereo */) {
                sampleDecoded = samples_II(soundData.buffer, 2, bound, bitrate, soundData.frequency);
            }
            if (header.mode == 0b11 /* single_channel */) {
                synth(soundData, sampleDecoded, soundData.synthOffset, soundData.synthBuffer, 1);
            } else {
                synth(soundData, sampleDecoded, soundData.synthOffset, soundData.synthBuffer, 2);
            }
        } else if (header.layer == 0b01 /* layer III */) {
            int frameSize = (144 * Mp3StandardData.BITRATE_LAYER_III[header.bitrateIndex]) / Mp3StandardData.SAMPLING_FREQUENCY[header.samplingFrequency] + header.paddingBit;
            if (frameSize > 2000) {
                System.err.println("Frame too large! " + frameSize);
            }
            samples_III(soundData.buffer, soundData.stereo == 1 ? 2 : 1, soundData.mainDataReader, frameSize, header.samplingFrequency, header.mode, header.modeExtension, soundData.store, soundData.v, soundData);
        }

        if (soundData.buffer.current != 0) {
            read(soundData.buffer, 8 - soundData.buffer.current);
        }
        return true;
    }

    private static void samples_III(Buffer buffer, int stereo, MainDataReader mainDataReader, int frameSize, int samplingFrequency, int mode, int modeExtension, float[] store, float[] v, SoundData soundData) throws IOException {
        int[] scfsi = new int[stereo * 4];
        int[] part2_3_length = new int[stereo * 2];
        int[] big_values = new int[stereo * 2];
        int[] global_gain = new int[stereo * 2];
        int[] scalefac_compress = new int[stereo * 2];
        int[] win_switch_flag = new int[stereo * 2];
        int[] block_type = new int[stereo * 2];
        int[] mixed_block_flag = new int[stereo * 2];
        int[] table_select = new int[stereo * 2 * 3];
        int[] subblock_gain = new int[stereo * 2 * 3];
        int[] region0_count = new int[stereo * 2];
        int[] region1_count = new int[stereo * 2];
        int[] preflag = new int[stereo * 2];
        int[] scalefac_scale = new int[stereo * 2];
        int[] count1table_select = new int[stereo * 2];
        int[] count1 = new int[stereo * 2];
        int[] scalefac_l = new int[stereo * 2 * 21];
        int[] scalefac_s = new int[stereo * 2 * 12 * 3];
        float[] is = new float[stereo * 2 * 576];
        int mainDataBegin = read(buffer, 9);
        read(buffer, stereo == 1 ? 5 : 3);

        for (int ch = 0; ch < stereo; ch++) {
            for (int scaleband = 0; scaleband < 4; scaleband++) {
                scfsi[ch * 4 + scaleband] = read(buffer, 1);
            }
        }
        for (int gr = 0; gr < 2; gr++) {
            for (int ch = 0; ch < stereo; ch++) {
                part2_3_length[ch * 2 + gr] = read(buffer, 12);
                big_values[ch * 2 + gr] = read(buffer, 9);
                global_gain[ch * 2 + gr] = read(buffer, 8);
                scalefac_compress[ch * 2 + gr] = read(buffer, 4);

                win_switch_flag[ch * 2 + gr] = read(buffer, 1);

                if (win_switch_flag[ch * 2 + gr] == 1) {
                    block_type[ch * 2 + gr] = read(buffer, 2);
                    mixed_block_flag[ch * 2 + gr] = read(buffer, 1);
                    for (int region = 0; region < 2; region++) {
                        table_select[ch * 2 * 3 + gr * 3 + region] = read(buffer, 5);
                    }
                    for (int window = 0; window < 3; window++) {
                        subblock_gain[ch * 2 * 3 + gr * 3 + window] = read(buffer, 3);
                    }
                    if ((block_type[ch * 2 + gr] == 2) &&
                            (mixed_block_flag[ch * 2 + gr] == 0)) {
                        region0_count[ch * 2 + gr] = 8;
                    } else {
                        region0_count[ch * 2 + gr] = 7;
                    }

                    region1_count[ch * 2 + gr] =
                            20 - region0_count[ch * 2 + gr];
                } else {
                    for (int region = 0; region < 3; region++) {
                        table_select[ch * 2 * 3 + gr * 3 + region] = read(buffer, 5);
                    }
                    region0_count[ch * 2 + gr] = read(buffer, 4);
                    region1_count[ch * 2 + gr] = read(buffer, 3);
                    block_type[ch * 2 + gr] = 0;
                }
                preflag[ch * 2 + gr] = read(buffer, 1);
                scalefac_scale[ch * 2 + gr] = read(buffer, 1);
                count1table_select[ch * 2 + gr] = read(buffer, 1);
            }
        }

        System.arraycopy(mainDataReader.array, mainDataReader.top - mainDataBegin, mainDataReader.array, 0, mainDataBegin);

        int mainDataSize = frameSize - (stereo == 2 ? 32 : 17) - 4;
        readInto(buffer, mainDataReader.array, mainDataBegin, mainDataSize);
        mainDataReader.index = 0;
        mainDataReader.current = 0;
        mainDataReader.top = mainDataBegin + mainDataSize;

        for (int gr = 0; gr < 2; gr++) {
            for (int ch = 0; ch < stereo; ch++) {

                int part_2_start = mainDataReader.index * 8 + mainDataReader.current;

                /* Number of bits in the bitstream for the bands */
                int slen1 = Mp3StandardData.SCALEFACTOR_SIZES_LAYER_III[scalefac_compress[ch * 2 + gr] * 2];
                int slen2 = Mp3StandardData.SCALEFACTOR_SIZES_LAYER_III[scalefac_compress[ch * 2 + gr] * 2 + 1];

                if ((win_switch_flag[ch * 2 + gr] != 0) &&
                        (block_type[ch * 2 + gr] == 2)) {
                    if (mixed_block_flag[ch * 2 + gr] != 0) {
                        for (int sfb = 0; sfb < 8; sfb++) {
                            scalefac_l[ch * 2 * 21 + gr * 21 + sfb] = read(mainDataReader, slen1);
                        }
                        for (int sfb = 3; sfb < 12; sfb++) {
                            int nbits;
                            if (sfb < 6) {	/* slen1 is for bands 3-5, slen2 for 6-11 */
                                nbits = slen1;
                            } else {
                                nbits = slen2;
                            }

                            for (int win = 0; win < 3; win++) {
                                scalefac_s[ch * 2 * 12 * 3 + gr * 12 * 3 + sfb * 3 + win] =
                                        read(mainDataReader, nbits);
                            }
                        }
                    } else {
                        for (int sfb = 0; sfb < 12; sfb++) {
                            int nbits;
                            if (sfb < 6) {	/* slen1 is for bands 3-5, slen2 for 6-11 */
                                nbits = slen1;
                            } else {
                                nbits = slen2;
                            }

                            for (int win = 0; win < 3; win++) {
                                scalefac_s[ch * 2 * 12 * 3 + gr * 12 * 3 + sfb * 3 + win] =
                                        read(mainDataReader, nbits);
                            }
                        }
                    }
                } else { /* block_type == 0 if winswitch == 0 */

                    /* Scale factor bands 0-5 */
                    if ((scfsi[ch * 4 + 0] == 0) || (gr == 0)) {
                        for (int sfb = 0; sfb < 6; sfb++) {
                            scalefac_l[ch * 2 * 21 + gr * 21 + sfb] = read(mainDataReader, slen1);
                        }
                    } else if ((scfsi[ch * 4 + 0] == 1) && (gr == 1)) {
                        /* Copy Mp3StandardData.scalefactors from granule 0 to granule 1 */
                        for (int sfb = 0; sfb < 6; sfb++) {
                            scalefac_l[ch * 2 * 21 + 1 * 21 + sfb] =
                                    scalefac_l[ch * 2 * 21 + 0 * 21 + sfb];
                        }
                    }

                    /* Scale factor bands 6-10 */
                    if ((scfsi[ch * 4 + 1] == 0) || (gr == 0)) {
                        for (int sfb = 6; sfb < 11; sfb++) {
                            scalefac_l[ch * 2 * 21 + gr * 21 + sfb] = read(mainDataReader, slen1);
                        }
                    } else if ((scfsi[ch * 4 + 1] == 1) && (gr == 1)) {
                        /* Copy Mp3StandardData.scalefactors from granule 0 to granule 1 */
                        for (int sfb = 6; sfb < 11; sfb++) {
                            scalefac_l[ch * 2 * 21 + 1 * 21 + sfb] =
                                    scalefac_l[ch * 2 * 21 + 0 * 21 + sfb];
                        }
                    }

                    /* Scale factor bands 11-15 */
                    if ((scfsi[ch * 4 + 2] == 0) || (gr == 0)) {
                        for (int sfb = 11; sfb < 16; sfb++) {
                            scalefac_l[ch * 2 * 21 + gr * 21 + sfb] = read(mainDataReader, slen2);
                        }
                    } else if ((scfsi[ch * 4 + 2] == 1) && (gr == 1)) {
                        /* Copy Mp3StandardData.scalefactors from granule 0 to granule 1 */
                        for (int sfb = 11; sfb < 16; sfb++) {
                            scalefac_l[ch * 2 * 21 + 1 * 21 + sfb] =
                                    scalefac_l[ch * 2 * 21 + 0 * 21 + sfb];
                        }
                    }

                    /* Scale factor bands 16-20 */
                    if ((scfsi[ch * 4 + 3] == 0) || (gr == 0)) {
                        for (int sfb = 16; sfb < 21; sfb++) {
                            scalefac_l[ch * 2 * 21 + gr * 21 + sfb] = read(mainDataReader, slen2);
                        }
                    } else if ((scfsi[ch * 4 + 3] == 1) && (gr == 1)) {
                        /* Copy Mp3StandardData.scalefactors from granule 0 to granule 1 */
                        for (int sfb = 16; sfb < 21; sfb++) {
                            scalefac_l[ch * 2 * 21 + 1 * 21 + sfb] =
                                    scalefac_l[ch * 2 * 21 + 0 * 21 + sfb];
                        }
                    }
                }

                /* Check that there is any data to decode. If not, zero the array. */
                if (part2_3_length[ch * 2 + gr] != 0) {


                    /* Calculate bit_pos_end which is the index of the last bit for this part. */
                    int bit_pos_end = part_2_start + part2_3_length[ch * 2 + gr] - 1;

                    int region_1_start;
                    int region_2_start;
                    int table_num;
                    int is_pos;

                    int[] huffman = new int[4];

                    /* Determine region boundaries */
                    if ((win_switch_flag[ch * 2 + gr] == 1) &&
                            (block_type[ch * 2 + gr] == 2)) {

                        region_1_start = 36;  /* sfb[9/3]*3=36 */
                        region_2_start = 576; /* No Region2 for short block case. */
                    } else {
                        region_1_start =
                                Mp3StandardData.SCALEFACTOR_BAND_INDICES_LAYER_III[samplingFrequency * (23 + 14) + 0 + region0_count[ch * 2 + gr] + 1];
                        region_2_start =
                                Mp3StandardData.SCALEFACTOR_BAND_INDICES_LAYER_III[samplingFrequency * (23 + 14) + 0 + region0_count[ch * 2 + gr] + region1_count[ch * 2 + gr] + 2];
                    }

                    /* Read big_values using tables according to region_x_start */
                    for (is_pos = 0; is_pos < big_values[ch * 2 + gr] * 2; is_pos++) {

                        if (is_pos < region_1_start) {
                            table_num = table_select[ch * 2 * 3 + gr * 3 + 0];
                        } else if (is_pos < region_2_start) {
                            table_num = table_select[ch * 2 * 3 + gr * 3 + 1];
                        } else {
                            table_num = table_select[ch * 2 * 3 + gr * 3 + 2];
                        }

                        /* Get next Huffman coded words */
                        huffman_III(mainDataReader, table_num, huffman);

                        /* In the big_values area there are two freq lines per Huffman word */
                        is[ch * 2 * 576 + gr * 576 + is_pos++] = huffman[0];
                        is[ch * 2 * 576 + gr * 576 + is_pos] = huffman[1];
                    }

                    /* Read small values until is_pos = 576 or we run out of huffman data */
                    table_num = count1table_select[ch * 2 + gr] + 32;
                    for (is_pos = big_values[ch * 2 + gr] * 2;
                         (is_pos <= 572) && (mainDataReader.index * 8 + mainDataReader.current <= bit_pos_end); is_pos++) {

                        /* Get next Huffman coded words */
                        huffman_III(mainDataReader, table_num, huffman);

                        is[ch * 2 * 576 + gr * 576 + is_pos++] = huffman[2];
                        if (is_pos >= 576) {
                            break;
                        }

                        is[ch * 2 * 576 + gr * 576 + is_pos++] = huffman[3];
                        if (is_pos >= 576) {
                            break;
                        }

                        is[ch * 2 * 576 + gr * 576 + is_pos++] = huffman[0];
                        if (is_pos >= 576) {
                            break;
                        }

                        is[ch * 2 * 576 + gr * 576 + is_pos] = huffman[1];
                    }

                    /* Check that we didn't read past the end of this section */
                    if (mainDataReader.index * 8 + mainDataReader.current > (bit_pos_end + 1)) {
                        /* Remove last words read */
                        is_pos -= 4;
                    }

                    /* Setup count1 which is the index of the first sample in the rzero reg. */
                    count1[ch * 2 + gr] = is_pos;

                    /* Zero out the last part if necessary */
                    for (/* is_pos comes from last for-loop */; is_pos < 576; is_pos++) {
                        is[ch * 2 * 576 + gr * 576 + is_pos] = 0.0f;
                    }

                    /* Set the bitpos to point to the next part to read */
                    mainDataReader.index = (bit_pos_end + 1) / 8;
                    mainDataReader.current = (bit_pos_end + 1) % 8;
                }
            } /* end for (gr... */
        }

        if(soundData.samplesBuffer == null)
            soundData.samplesBuffer = new byte[18 * 32 * 2 * stereo * 2];

        for (int gr = 0; gr < 2; gr++) {
            for (int ch = 0; ch < stereo; ch++) {



                /* Determine type of block to process */
                if ((win_switch_flag[ch * 2 + gr] == 1) &&
                        (block_type[ch * 2 + gr] == 2)) { /* Short blocks */

                    /* Check if the first two subbands
                     * (=2*18 samples = 8 long or 3 short sfb's) uses long blocks */
                    if (mixed_block_flag[ch * 2 + gr] != 0) { /* 2 longbl. sb  first */

                        /*
                         * First process the 2 long block subbands at the start
                         */
                        int sfb = 0;
                        int next_sfb = Mp3StandardData.SCALEFACTOR_BAND_INDICES_LAYER_III[samplingFrequency * (23 + 14) + 0 + sfb + 1];
                        for (int i = 0; i < 36; i++) {
                            if (i == next_sfb) {
                                sfb++;
                                next_sfb = Mp3StandardData.SCALEFACTOR_BAND_INDICES_LAYER_III[samplingFrequency * (23 + 14) + 0 + sfb + 1];
                            } /* end if */
                            requantize_long_III(gr, ch, scalefac_scale, preflag, global_gain, scalefac_l, is, i, sfb);
                        }

                        /*
                         * And next the remaining, non-zero, bands which uses short blocks
                         */
                        sfb = 3;
                        next_sfb = Mp3StandardData.SCALEFACTOR_BAND_INDICES_LAYER_III[samplingFrequency * (23 + 14) + 23 + sfb + 1] * 3;
                        int win_len = Mp3StandardData.SCALEFACTOR_BAND_INDICES_LAYER_III[samplingFrequency * (23 + 14) + 23 + sfb + 1] -
                                Mp3StandardData.SCALEFACTOR_BAND_INDICES_LAYER_III[samplingFrequency * (23 + 14) + 23 + sfb];

                        for (int i = 36; i < count1[ch * 2 + gr]; /* i++ done below! */) {

                            /* Check if we're into the next scalefac band */
                            if (i == next_sfb) {	/* Yes */
                                sfb++;
                                next_sfb = Mp3StandardData.SCALEFACTOR_BAND_INDICES_LAYER_III[samplingFrequency * (23 + 14) + 23 + sfb + 1] * 3;
                                win_len = Mp3StandardData.SCALEFACTOR_BAND_INDICES_LAYER_III[samplingFrequency * (23 + 14) + 23 + sfb + 1] -
                                        Mp3StandardData.SCALEFACTOR_BAND_INDICES_LAYER_III[samplingFrequency * (23 + 14) + 23 + sfb];
                            } /* end if (next_sfb) */

                            for (int win = 0; win < 3; win++) {
                                for (int j = 0; j < win_len; j++) {
                                    requantize_short_III(gr, ch, scalefac_scale, subblock_gain, global_gain, scalefac_s, is, i, sfb, win);
                                    i++;
                                } /* end for (win... */
                            } /* end for (j... */
                        }	/* end for (i... */
                    } else {			/* Only short blocks */

                        int sfb = 0;
                        int next_sfb = Mp3StandardData.SCALEFACTOR_BAND_INDICES_LAYER_III[samplingFrequency * (23 + 14) + 23 + sfb + 1] * 3;
                        int win_len = Mp3StandardData.SCALEFACTOR_BAND_INDICES_LAYER_III[samplingFrequency * (23 + 14) + 23 + sfb + 1] -
                                Mp3StandardData.SCALEFACTOR_BAND_INDICES_LAYER_III[samplingFrequency * (23 + 14) + 23 + sfb];

                        for (int i = 0; i < count1[ch * 2 + gr]; /* i++ done below! */) {

                            /* Check if we're into the next scalefac band */
                            if (i == next_sfb) {	/* Yes */
                                sfb++;
                                next_sfb = Mp3StandardData.SCALEFACTOR_BAND_INDICES_LAYER_III[samplingFrequency * (23 + 14) + 23 + sfb + 1] * 3;
                                win_len = Mp3StandardData.SCALEFACTOR_BAND_INDICES_LAYER_III[samplingFrequency * (23 + 14) + 23 + sfb + 1] -
                                        Mp3StandardData.SCALEFACTOR_BAND_INDICES_LAYER_III[samplingFrequency * (23 + 14) + 23 + sfb];
                            } /* end if (next_sfb) */

                            for (int win = 0; win < 3; win++) {
                                for (int j = 0; j < win_len; j++) {
                                    requantize_short_III(gr, ch, scalefac_scale, subblock_gain, global_gain, scalefac_s, is, i, sfb, win);
                                    i++;
                                } /* end for (win... */
                            } /* end for (j... */
                        }	/* end for (i... */
                    } /* end else (only short blocks) */
                } else {			/* Only long blocks */

                    int sfb = 0;
                    int next_sfb = Mp3StandardData.SCALEFACTOR_BAND_INDICES_LAYER_III[samplingFrequency * (23 + 14) + 0 + sfb + 1];
                    for (int i = 0; i < count1[ch * 2 + gr]; i++) {
                        if (i == next_sfb) {
                            sfb++;
                            next_sfb = Mp3StandardData.SCALEFACTOR_BAND_INDICES_LAYER_III[samplingFrequency * (23 + 14) + 0 + sfb + 1];
                        } /* end if */
                        requantize_long_III(gr, ch, scalefac_scale, preflag, global_gain, scalefac_l, is, i, sfb);
                    }
                } /* end else (only long blocks) */





                outer:
                while (true) {


                    /* Only reorder short blocks */
                    if ((win_switch_flag[ch * 2 + gr] == 1) &&
                            (block_type[ch * 2 + gr] == 2)) { /* Short blocks */

                        float[] re = new float[576];

                        int i = 0;
                        int sfb = 0;
                        int next_sfb;
                        int win_len;

                        /* Check if the first two subbands
                         * (=2*18 samples = 8 long or 3 short sfb's) uses long blocks */
                        if (mixed_block_flag[ch * 2 + gr] != 0) { /* 2 longbl. sb  first */
                            sfb = 3;
                            i = 36;
                        }
                        next_sfb = Mp3StandardData.SCALEFACTOR_BAND_INDICES_LAYER_III[samplingFrequency * (23 + 14) + 23 + sfb + 1] * 3;
                        win_len = Mp3StandardData.SCALEFACTOR_BAND_INDICES_LAYER_III[samplingFrequency * (23 + 14) + 23 + sfb + 1] -
                                Mp3StandardData.SCALEFACTOR_BAND_INDICES_LAYER_III[samplingFrequency * (23 + 14) + 23 + sfb];

                        for (; i < 576; /* i++ done below! */) {

                            /* Check if we're into the next scalefac band */
                            if (i == next_sfb) {	/* Yes */

                                /* Copy reordered data back to the original vector */
                                for (int j = 0; j < 3 * win_len; j++) {
                                    is[ch * 2 * 576 + gr * 576 + 3 * (Mp3StandardData.SCALEFACTOR_BAND_INDICES_LAYER_III[samplingFrequency * (23 + 14) + 23 + sfb]) + j] =
                                            re[j];
                                }

                                /* Check if this band is above the rzero region, if so we're done */
                                if (i >= count1[ch * 2 + gr]) {
                                    /* Done */
                                    break outer;
                                }

                                sfb++;
                                next_sfb = Mp3StandardData.SCALEFACTOR_BAND_INDICES_LAYER_III[samplingFrequency * (23 + 14) + 23 + sfb + 1] * 3;
                                win_len = Mp3StandardData.SCALEFACTOR_BAND_INDICES_LAYER_III[samplingFrequency * (23 + 14) + 23 + sfb + 1] -
                                        Mp3StandardData.SCALEFACTOR_BAND_INDICES_LAYER_III[samplingFrequency * (23 + 14) + 23 + sfb];
                            } /* end if (next_sfb) */

                            /* Do the actual reordering */
                            for (int win = 0; win < 3; win++) {
                                for (int j = 0; j < win_len; j++) {
                                    re[j * 3 + win] = is[ch * 2 * 576 + gr * 576 + i];
                                    i++;
                                } /* end for (j... */
                            } /* end for (win... */
                        }	/* end for (i... */

                        /* Copy reordered data of the last band back to the original vector */
                        for (int j = 0; j < 3 * win_len; j++) {
                            is[ch * 2 * 576 + gr * 576 + 3 * (Mp3StandardData.SCALEFACTOR_BAND_INDICES_LAYER_III[samplingFrequency * (23 + 14) + 23 + 12]) + j] = re[j];
                        }
                    }
                    break;
                }
            }


            /* Do nothing if joint stereo is not enabled */
            if ((mode == 1) && (modeExtension != 0)) {

                /* Do Middle/Side ("normal") stereo processing */
                if ((modeExtension & 0x2) != 0) {

                    int max_pos;
                    /* Determine how many frequency lines to transform */
                    if (count1[0 * 2 + gr] > count1[1 * 2 + gr]) {
                        max_pos = count1[0 * 2 + gr];
                    } else {
                        max_pos = count1[1 * 2 + gr];
                    }

                    /* Do the actual processing */
                    for (int i = 0; i < max_pos; i++) {
                        float left = (is[0 * 2 * 576 + gr * 576 + i] + is[1 * 2 * 576 + gr * 576 + i])
                                * (Mp3StandardData.INV_SQUARE_2);
                        float right = (is[0 * 2 * 576 + gr * 576 + i] - is[1 * 2 * 576 + gr * 576 + i])
                                * (Mp3StandardData.INV_SQUARE_2);
                        is[0 * 2 * 576 + gr * 576 + i] = left;
                        is[1 * 2 * 576 + gr * 576 + i] = right;
                    } /* end for (i... */
                } /* end if (ms_stereo... */

                /* Do intensity stereo processing */
                if ((modeExtension & 0x1) != 0) {

                    /* The first band that is intensity stereo encoded is the first band
                     * scale factor band on or above the count1 frequency line.
                     * N.B.: Intensity stereo coding is only done for the higher subbands,
                     * but the logic is still included to process lower subbands.
                     */

                    /* Determine type of block to process */
                    if ((win_switch_flag[0 * 2 + gr] == 1) &&
                            (block_type[0 * 2 + gr] == 2)) { /* Short blocks */

                        /* Check if the first two subbands
                         * (=2*18 samples = 8 long or 3 short sfb's) uses long blocks */
                        if (mixed_block_flag[0 * 2 + gr] != 0) { /* 2 longbl. sb  first */

                            /*
                             * First process the 8 sfb's at the start
                             */
                            for (int sfb = 0; sfb < 8; sfb++) {

                                /* Is this scale factor band above count1 for the right channel? */
                                if (Mp3StandardData.SCALEFACTOR_BAND_INDICES_LAYER_III[samplingFrequency * (23 + 14) + 0 + sfb] >= count1[1 * 2 + gr]) {
                                    stereo_long_III(is, scalefac_l, gr, sfb, samplingFrequency);
                                }
                            } /* end if (sfb... */

                            /*
                             * And next the remaining bands which uses short blocks
                             */
                            for (int sfb = 3; sfb < 12; sfb++) {

                                /* Is this scale factor band above count1 for the right channel? */
                                if (Mp3StandardData.SCALEFACTOR_BAND_INDICES_LAYER_III[samplingFrequency * (23 + 14) + 23 + sfb] * 3 >= count1[1 * 2 + gr]) {

                                    /* Perform the intensity stereo processing */
                                    stereo_short_III(is, scalefac_s, gr, sfb, samplingFrequency);
                                }
                            }
                        } else {			/* Only short blocks */

                            for (int sfb = 0; sfb < 12; sfb++) {

                                /* Is this scale factor band above count1 for the right channel? */
                                if (Mp3StandardData.SCALEFACTOR_BAND_INDICES_LAYER_III[samplingFrequency * (23 + 14) + 23 + sfb] * 3 >= count1[1 * 2 + gr]) {

                                    /* Perform the intensity stereo processing */
                                    stereo_short_III(is, scalefac_s, gr, sfb, samplingFrequency);
                                }
                            }
                        } /* end else (only short blocks) */
                    } else {			/* Only long blocks */

                        for (int sfb = 0; sfb < 21; sfb++) {

                            /* Is this scale factor band above count1 for the right channel? */
                            if (Mp3StandardData.SCALEFACTOR_BAND_INDICES_LAYER_III[samplingFrequency * (23 + 14) + 0 + sfb] >= count1[1 * 2 + gr]) {

                                /* Perform the intensity stereo processing */
                                stereo_long_III(is, scalefac_l, gr, sfb, samplingFrequency);
                            }
                        }
                    } /* end else (only long blocks) */
                } /* end if (intensity_stereo processing) */
            }

            for (int ch = 0; ch < stereo; ch++) {



                /* No antialiasing is done for short blocks */
                if (!((win_switch_flag[ch * 2 + gr] == 1) &&
                        (block_type[ch * 2 + gr] == 2) &&
                        (mixed_block_flag[ch * 2 + gr]) == 0)) {

                    int sblim;

                    /* Setup the limit for how many subbands to transform */
                    if ((win_switch_flag[ch * 2 + gr] == 1) &&
                            (block_type[ch * 2 + gr] == 2) &&
                            (mixed_block_flag[ch * 2 + gr]) == 1) {
                        sblim = 2;
                    } else {
                        sblim = 32;
                    }

                    /* Do the actual antialiasing */
                    for (int sb = 1; sb < sblim; sb++) {
                        for (int i = 0; i < 8; i++) {
                            int li = 18 * sb - 1 - i;
                            int ui = 18 * sb + i;
                            float lb = is[ch * 2 * 576 + gr * 576 + li] * Mp3StandardData.CS_ALIASING_LAYER_III[i] - is[ch * 2 * 576 + gr * 576 + ui] * Mp3StandardData.CA_ALIASING_LAYER_III[i];
                            float ub = is[ch * 2 * 576 + gr * 576 + ui] * Mp3StandardData.CS_ALIASING_LAYER_III[i] + is[ch * 2 * 576 + gr * 576 + li] * Mp3StandardData.CA_ALIASING_LAYER_III[i];
                            is[ch * 2 * 576 + gr * 576 + li] = lb;
                            is[ch * 2 * 576 + gr * 576 + ui] = ub;
                        }
                    }
                }


                /* Loop through all 32 subbands */
                for (int sb = 0; sb < 32; sb++) {

                    int bt;

                    /* Determine blocktype for this subband */
                    if ((win_switch_flag[ch * 2 + gr] == 1) &&
                            (mixed_block_flag[ch * 2 + gr] == 1) && (sb < 2)) {
                        bt = 0;			/* Long blocks in first 2 subbands */
                    } else {
                        bt = block_type[ch * 2 + gr];
                    }

                    float[] rawout = new float[36];


                    /* Do the inverse modified DCT and windowing */


                    int offset = ch * 2 * 576 + gr * 576 + sb * 18;

                    if (bt == 2) {
                        for (int j = 0; j < 3; j++) {
                            for (int p = 0; p < 12; p++) {
                                float sum = 0;
                                for (int m = 0; m < 6; m++) {
                                    sum += is[offset + j+3*m] * Mp3StandardData.COS_12_LAYER_III[m * 12 + p];
                                }
                                rawout[6*j+p+6] += sum * Mp3StandardData.IMDCT_WINDOW_LAYER_III[bt * 36 + p];
                            }
                        }
                    } else {
                        for (int p = 0; p < 36; p++) {
                            float sum = 0;
                            for (int m = 0; m < 18; m++) {
                                sum += is[offset + m] * Mp3StandardData.COS_36_LAYER_III[m * 36 + p];
                            }
                            rawout[p] = sum * Mp3StandardData.IMDCT_WINDOW_LAYER_III[bt * 36 + p];
                        }
                    }



                    /* Overlapp add with stored vector into main_data vector */
                    for (int i = 0; i < 18; i++) {

                        is[ch * 2 * 576 + gr * 576 + sb * 18 + i] = rawout[i] + store[ch * 32 * 18 + sb * 18 + i];
                        store[ch * 32 * 18 + sb * 18 + i] = rawout[i + 18];
                    } /* end for (i... */
                } /* end for (sb... */




                for (int sb = 1; sb < 32; sb += 2) {
                    for (int i = 1; i < 18; i += 2) {
                        is[ch * 2 * 576 + gr * 576 + sb * 18 + i] = -is[ch * 2 * 576 + gr * 576 + sb * 18 + i];
                    }
                }




                float[] u = new float[512];
                float[] s = new float[32];

                /* Loop through the 18 samples in each of the 32 subbands */
                for (int ss = 0; ss < 18; ss++) {

                    for (int i = 1023; i > 63; i--)  /* Shift up the V vector */ {
                        v[ch * 1024 + i] = v[ch * 1024 + i - 64];
                    }

                    /* Copy the next 32 time samples to a temp vector */
                    for (int i = 0; i < 32; i++) {
                        s[i] = is[ch * 2 * 576 + gr * 576 + i * 18 + ss];
                    }

                    for (int i = 0; i < 64; i++) { /* Matrix multiply input with n_win[][] matrix */
                        float sum = 0.0f;
                        for (int j = 0; j < 32; j++) {
                            sum += Mp3StandardData.SYNTH_WINDOW_TABLE_LAYER_III[i * 32 + j] * s[j];
                        }
                        v[ch * 1024 + i] = sum;
                    } /* end for(i... */

                    /* Build the U vector */
                    for (int i = 0; i < 8; i++) {
                        for (int j = 0; j < 32; j++) {
                            u[i * 64 + j] = v[ch * 1024 + i * 128 + j];
                            u[i * 64 + j + 32] = v[ch * 1024 + i * 128 + j + 96];
                        }
                    } /* end for (i... */

                    /* Window by u_vec[i] with g_synth_dtbl[i] */
                    for (int i = 0; i < 512; i++) {
                        u[i] *= Mp3StandardData.DI_COEFFICIENTS[i];
                    }

                    /* Calculate 32 samples and store them in the outdata vector */
                    for (int i = 0; i < 32; i++) {
                        float sum = 0.0f;
                        for (int j = 0; j < 16; j++) {
                            sum += u[j * 32 + i];
                        }

                        /* sum now contains time sample 32*ss+i. Convert to 16-bit signed int */
                        int samp = (int) (sum * 32767.0f);
                        if (samp > 32767) {
                            samp = 32767;
                        } else if (samp < -32767) {
                            samp = -32767;
                        }
                        samp &= 0xffff;

                        if (stereo > 1) {
                            soundData.samplesBuffer[gr * 18 * 32 * 2 * 2 + ss * 32 * 2 * 2 + i * 2 * 2 + ch * 2] = (byte) samp;
                            soundData.samplesBuffer[gr * 18 * 32 * 2 * 2 + ss * 32 * 2 * 2 + i * 2 * 2 + ch * 2 + 1] = (byte) (samp >>> 8);
                        } else {
                            soundData.samplesBuffer[gr * 18 * 32 * 2 + ss * 32 * 2 + i * 2] = (byte) samp;
                            soundData.samplesBuffer[gr * 18 * 32 * 2 + ss * 32 * 2 + i * 2 + 1] = (byte) (samp >>> 8);
                        }
                    } /* end for (i... */
                } /* end for (ss... */
            }
        }
    }

    private static void stereo_short_III(float[] is, int[] scalefac_s, int gr, int sfb, int samplingFrequency) {
        /* The window length */
        int win_len = Mp3StandardData.SCALEFACTOR_BAND_INDICES_LAYER_III[samplingFrequency * (23 + 14) + 23 + sfb + 1] - Mp3StandardData.SCALEFACTOR_BAND_INDICES_LAYER_III[samplingFrequency * (23 + 14) + 23 + sfb];

        /* The three windows within the band has different Mp3StandardData.scalefactors */
        for (int win = 0; win < 3; win++) {

            int is_pos;

            /* Check that ((is_pos[sfb]=scalefac) != 7) => no intensity stereo */
            if ((is_pos = scalefac_s[0 * 2 * 12 * 3 + gr * 12 * 3 + sfb * 3 + win]) != 7) {

                int sfb_start = Mp3StandardData.SCALEFACTOR_BAND_INDICES_LAYER_III[samplingFrequency * (23 + 14) + 23 + sfb] * 3 + win_len * win;
                int sfb_stop = sfb_start + win_len;

                float is_ratio_l;
                float is_ratio_r;

                /* tan((6*PI)/12 = PI/2) needs special treatment! */
                if (is_pos == 6) {
                    is_ratio_l = 1.0f;
                    is_ratio_r = 0.0f;
                } else {
                    is_ratio_l = Mp3StandardData.IS_RATIOS_LAYER_III[is_pos] / (1.0f + Mp3StandardData.IS_RATIOS_LAYER_III[is_pos]);
                    is_ratio_r = 1.0f / (1.0f + Mp3StandardData.IS_RATIOS_LAYER_III[is_pos]);
                }

                /* Now decode all samples in this scale factor band */
                for (int i = sfb_start; i < sfb_stop; i++) {
                    is[0 * 2 * 576 + gr * 576 + i] *= is_ratio_l;
                    is[1 * 2 * 576 + gr * 576 + i] *= is_ratio_r;
                }
            } /* end if (not illegal is_pos) */
        } /* end for (win... */
    }

    private static void stereo_long_III(float[] is, int[] scalefac_l, int gr, int sfb, int samplingFrequency) {
        int is_pos;
        /* Check that ((is_pos[sfb]=scalefac) != 7) => no intensity stereo */
        if ((is_pos = scalefac_l[0 * 2 * 21 + gr * 21 + sfb]) != 7) {

            int sfb_start = Mp3StandardData.SCALEFACTOR_BAND_INDICES_LAYER_III[samplingFrequency * (23 + 14) + 0 + sfb];
            int sfb_stop = Mp3StandardData.SCALEFACTOR_BAND_INDICES_LAYER_III[samplingFrequency * (23 + 14) + 0 + sfb + 1];

            float is_ratio_l;
            float is_ratio_r;

            /* tan((6*PI)/12 = PI/2) needs special treatment! */
            if (is_pos == 6) {
                is_ratio_l = 1.0f;
                is_ratio_r = 0.0f;
            } else {
                is_ratio_l = Mp3StandardData.IS_RATIOS_LAYER_III[is_pos] / (1.0f + Mp3StandardData.IS_RATIOS_LAYER_III[is_pos]);
                is_ratio_r = 1.0f / (1.0f + Mp3StandardData.IS_RATIOS_LAYER_III[is_pos]);
            }

            /* Now decode all samples in this scale factor band */
            for (int i = sfb_start; i < sfb_stop; i++) {
                is[0 * 2 * 576 + gr * 576 + i] *= is_ratio_l;
                is[1 * 2 * 576 + gr * 576 + i] *= is_ratio_r;
            }
        }
    }

    private static void requantize_short_III(int gr, int ch, int[] scalefac_scale, int[] subblock_gain, int[] global_gain, int[] scalefac_s, float[] is, int is_pos, int sfb, int win) {

        float sf_mult = scalefac_scale[ch * 2 + gr] != 0 ? 1.0f : 0.5f;

        float tmp1;

        if (sfb < 12) {
            tmp1 = (float) Math.pow(2, -(sf_mult * scalefac_s[ch * 2 * 12 * 3 + gr * 12 * 3 + sfb * 3 + win]));
        } else {
            tmp1 = 1.0f;
        }

        float tmp2 = (float) Math.pow(2, 0.25f * (global_gain[ch * 2 + gr] - 210.0f -
                8.0f * (subblock_gain[ch * 2 * 3 + gr * 3 + win])));

        float tmp3;
        if (is[ch * 2 * 576 + gr * 576 + is_pos] < 0.0) {
            tmp3 = -Mp3StandardData.POWTAB_LAYER_III[(int) -is[ch * 2 * 576 + gr * 576 + is_pos]];
        } else {
            tmp3 = Mp3StandardData.POWTAB_LAYER_III[(int) is[ch * 2 * 576 + gr * 576 + is_pos]];
        }

        is[ch * 2 * 576 + gr * 576 + is_pos] = tmp1 * tmp2 * tmp3;
    }

    private static void requantize_long_III(int gr, int ch, int[] scalefac_scale, int[] preflag, int[] global_gain, int[] scalefac_l, float[] is, int is_pos, int sfb) {


        float sf_mult = scalefac_scale[ch * 2 + gr] != 0 ? 1.0f : 0.5f;



        float tmp1;

        if (sfb < 21) {
            float pf_x_pt = preflag[ch * 2 + gr] * Mp3StandardData.REQUANTIZE_LONG_PRETAB_LAYER_III[sfb];

            tmp1 = (float) Math.pow(2, -(sf_mult * (scalefac_l[ch * 2 * 21 + gr * 21 + sfb] + pf_x_pt)));
        } else {
            tmp1 = 1.0f;
        }

        float tmp2 = (float) Math.pow(2, 0.25f * (global_gain[ch * 2 + gr] - 210));

        float tmp3;
        if (is[ch * 2 * 576 + gr * 576 + is_pos] < 0.0) {
            tmp3 = -Mp3StandardData.POWTAB_LAYER_III[(int) -is[ch * 2 * 576 + gr * 576 + is_pos]];
        } else {
            tmp3 = Mp3StandardData.POWTAB_LAYER_III[(int) is[ch * 2 * 576 + gr * 576 + is_pos]];
        }

        is[ch * 2 * 576 + gr * 576 + is_pos] = tmp1 * tmp2 * tmp3;
    }

    private static void huffman_III(MainDataReader mainDataReader, int table_num, int[] array) {
        /* Table entries are 16 bits each:
         * Bit(s)
         * 15     hit/miss (1/0)
         * 14-13  codeword size (1-4 bits)
         * 7-0    codeword (bits 4-7=x, 0-3=y) if hit
         * 12-0   start offset of next table if miss
         */

        int point = 0;
        int currpos;

        /* Check for empty tables */
        if (Mp3StandardData.HUFFMAN_TREELEN_LAYER_III[table_num] == 0) {
            array[0] = array[1] = array[2] = array[3] = 0;
            return;
        }

        int treelen = Mp3StandardData.HUFFMAN_TREELEN_LAYER_III[table_num];
        int linbits =Mp3StandardData.HUFFMAN_LINBITS_LAYER_III[table_num];
        int offset = Mp3StandardData.HUFFMAN_TABLE_OFFSET_LAYER_III[table_num];

        int error = 1;
        int bitsleft = 32;

        do {   /* Start reading the Huffman code word,bit by bit */
            /* Check if we've matched a code word */
            if ((Mp3StandardData.HUFFMAN_TABLE_LAYER_III[offset + point] & 0xff00) == 0) {
                error = 0;
                array[0] = (Mp3StandardData.HUFFMAN_TABLE_LAYER_III[offset + point] >> 4) & 0xf;
                array[1] = Mp3StandardData.HUFFMAN_TABLE_LAYER_III[offset + point] & 0xf;
                break;
            }
            if (read(mainDataReader, 1) != 0) { /* Go right in tree */
                while ((Mp3StandardData.HUFFMAN_TABLE_LAYER_III[offset + point] & 0xff) >= 250) {
                    point += Mp3StandardData.HUFFMAN_TABLE_LAYER_III[offset + point] & 0xff;
                }
                point += Mp3StandardData.HUFFMAN_TABLE_LAYER_III[offset + point] & 0xff;
            } else { /* Go left in tree */
                while ((Mp3StandardData.HUFFMAN_TABLE_LAYER_III[offset + point] >> 8) >= 250) {
                    point += Mp3StandardData.HUFFMAN_TABLE_LAYER_III[offset + point] >> 8;
                }
                point += Mp3StandardData.HUFFMAN_TABLE_LAYER_III[offset + point] >> 8;
            }
        } while ((--bitsleft > 0) && (point < treelen));
        if (error != 0) {  /* Check for error. */
            array[0] = array[1] = 0;
            throw new IllegalStateException("Illegal Huff code in data. bleft = %d,point = %d. tab = %d." +
                    bitsleft + " " + point + " " + table_num);
        }
        /* Process sign encodings for quadruples tables. */
        if (table_num > 31) {
            array[2] = (array[1] >> 3) & 1;
            array[3] = (array[1] >> 2) & 1;
            array[0] = (array[1] >> 1) & 1;
            array[1] = array[1] & 1;

            if (array[2] > 0) {
                if (read(mainDataReader, 1) == 1) {
                    array[2] = -array[2];
                }
            }
            if (array[3] > 0) {
                if (read(mainDataReader, 1) == 1) {
                    array[3] = -array[3];
                }
            }
            if (array[0] > 0) {
                if (read(mainDataReader, 1) == 1) {
                    array[0] = -array[0];
                }
            }
            if (array[1] > 0) {
                if (read(mainDataReader, 1) == 1) {
                    array[1] = -array[1];
                }
            }
        } else {
            /* Get linbits */
            if ((linbits > 0) && (array[0] == 15)) {
                array[0] += read(mainDataReader, linbits);
            }

            /* Get sign bit */
            if (array[0] > 0) {
                if (read(mainDataReader, 1) == 1) {
                    array[0] = -array[0];
                }
            }

            /* Get linbits */
            if ((linbits > 0) && (array[1] == 15)) {
                array[1] += read(mainDataReader, linbits);
            }

            /* Get sign bit */
            if (array[1] > 0) {
                if (read(mainDataReader, 1) == 1) {
                    array[1] = -array[1];
                }
            }
        }
    }

    private static float[] samples_I(Buffer buffer, int stereo, int bound) throws IOException {
        if (bound < 0) {
            bound = 32;
        }
        int[] allocation = new int[32 - bound];
        int[] allocationChannel = new int[stereo * bound];
        int[] scalefactorChannel = new int[stereo * 32];
        float[] sampleDecoded = new float[stereo * 32 * 12];
        for (int sb = 0; sb < bound; sb++) {
            for (int ch = 0; ch < stereo; ch++) {
                allocationChannel[ch * bound + sb] = read(buffer, 4);
            }
        }
        for (int sb = bound; sb < 32; sb++) {
            allocation[sb - bound] = read(buffer, 4);
        }
        for (int sb = 0; sb < bound; sb++) {
            for (int ch = 0; ch < stereo; ch++) {
                if (allocationChannel[ch * bound + sb] != 0) {
                    scalefactorChannel[ch * 32 + sb] = read(buffer, 6);
                }
            }
        }
        for (int sb = bound; sb < 32; sb++) {
            for (int ch = 0; ch < stereo; ch++) {
                if (allocation[sb - bound] != 0) {
                    scalefactorChannel[ch * 32 + sb] = read(buffer, 6);
                }
            }
        }
        for (int s = 0; s < 12; s++) {
            for (int sb = 0; sb < bound; sb++) {
                for (int ch = 0; ch < stereo; ch++) {
                    int n = allocationChannel[ch * bound + sb];
                    if (n == 0) {
                        sampleDecoded[ch * 32 * 12 + sb * 12 + s] = 0;
                    } else {
                        int read = read(buffer, n + 1);
                        float fraction = 0;
                        if (((read >> n) & 0b1) == 0) {
                            fraction = -1;
                        }
                        fraction += (float) (read & ((0b1 << n) - 1)) / (0b1 << n) + 1f / (0b1 << n);
                        sampleDecoded[ch * 32 * 12 + sb * 12 + s] = Mp3StandardData.SCALEFACTORS[scalefactorChannel[ch * 32 + sb]] * Mp3StandardData.PRE_FRACTOR_LAYER_I[n + 1] * fraction;
                    }
                }
            }
            for (int sb = bound; sb < 32; sb++) {
                int n = allocationChannel[sb - bound];
                if (n == 0) {
                    sampleDecoded[0 * 32 * 12 + sb * 12 + s] = sampleDecoded[1 * 32 * 12 + sb * 12 + s] = 0;
                } else {
                    int read = read(buffer, n + 1);
                    float fraction = 0;
                    if (((read >> n) & 0b1) == 0) {
                        fraction = -1;
                    }
                    fraction += (float) (read & ((0b1 << n) - 1)) / (0b1 << n) + 1f / (0b1 << n);
                    for (int ch = 0; ch < 2; ch++) {
                        sampleDecoded[ch * 32 * 12 + sb * 12 + s] = Mp3StandardData.SCALEFACTORS[scalefactorChannel[ch * 32 + sb]] * Mp3StandardData.PRE_FRACTOR_LAYER_I[n + 1] * fraction;
                    }
                }
            }
        }
        return sampleDecoded;
    }

    private static float[] samples_II(Buffer buffer, int stereo, int bound, int bitrate, int frequency) throws IOException {
        int sbIndex = 0;
        if (frequency != 48000 && (bitrate >= 96000 || bitrate == 0)) {
            sbIndex = 1;
        } else if (frequency != 32000 && (bitrate > 0 && bitrate <= 48000)) {
            sbIndex = 2;
        } else if (frequency == 32000 && (bitrate > 0 && bitrate <= 48000)) {
            sbIndex = 3;
        }
        int sbLimit = Mp3StandardData.SB_LIMIT[sbIndex];
        if (bound < 0) {
            bound = sbLimit;
        }
        int[] allocation = new int[sbLimit - bound];
        int[] allocationChannel = new int[stereo * bound];
        int[] scfsi = new int[stereo * sbLimit];
        int[] scalefactorChannel = new int[stereo * sbLimit * 3];
        float[] sampleDecoded = new float[stereo * 32 * 12 * 3];
        for (int sb = 0; sb < bound; sb++) {
            for (int ch = 0; ch < stereo; ch++) {
                allocationChannel[ch * bound + sb] = read(buffer, Mp3StandardData.NBAL[sbIndex][sb]);
            }
        }
        for (int sb = bound; sb < sbLimit; sb++) {
            allocation[sb - bound] = read(buffer, Mp3StandardData.NBAL[sbIndex][sb]);
        }
        for (int sb = 0; sb < bound; sb++) {
            for (int ch = 0; ch < stereo; ch++) {
                if (allocationChannel[ch * bound + sb] != 0) {
                    scfsi[ch * sbLimit + sb] = read(buffer, 2);
                }
            }
        }
        for (int sb = bound; sb < sbLimit; sb++) {
            for (int ch = 0; ch < stereo; ch++) {
                if (allocation[sb - bound] != 0) {
                    scfsi[ch * sbLimit + sb] = read(buffer, 2);
                }
            }
        }
        for (int sb = 0; sb < bound; sb++) {
            for (int ch = 0; ch < stereo; ch++) {
                if (allocationChannel[ch * bound + sb] != 0) {
                    int offset = ch * sbLimit * 3 + sb * 3;
                    if (scfsi[ch * sbLimit + sb] == 0) {
                        scalefactorChannel[offset + 0] = read(buffer, 6);
                        scalefactorChannel[offset + 1] = read(buffer, 6);
                        scalefactorChannel[offset + 2] = read(buffer, 6);
                    } else if (scfsi[ch * sbLimit + sb] == 1) {
                        scalefactorChannel[offset + 0] = scalefactorChannel[offset + 1] = read(buffer, 6);
                        scalefactorChannel[offset + 2] = read(buffer, 6);
                    } else if (scfsi[ch * sbLimit + sb] == 2) {
                        scalefactorChannel[offset + 0] = scalefactorChannel[offset + 1] = scalefactorChannel[offset + 2] = read(buffer, 6);
                    } else if (scfsi[ch * sbLimit + sb] == 3) {
                        scalefactorChannel[offset + 0] = read(buffer, 6);
                        scalefactorChannel[offset + 1] = scalefactorChannel[offset + 2] = read(buffer, 6);
                    }
                }
            }
        }
        for (int sb = bound; sb < sbLimit; sb++) {
            for (int ch = 0; ch < stereo; ch++) {
                if (allocation[sb - bound] != 0) {
                    int offset = ch * sbLimit * 3 + sb * 3;
                    if (scfsi[ch * sbLimit + sb] == 0) {
                        scalefactorChannel[offset + 0] = read(buffer, 6);
                        scalefactorChannel[offset + 1] = read(buffer, 6);
                        scalefactorChannel[offset + 2] = read(buffer, 6);
                    } else if (scfsi[ch * sbLimit + sb] == 1) {
                        scalefactorChannel[offset + 0] = scalefactorChannel[offset + 1] = read(buffer, 6);
                        scalefactorChannel[offset + 2] = read(buffer, 6);
                    } else if (scfsi[ch * sbLimit + sb] == 2) {
                        scalefactorChannel[offset + 0] = scalefactorChannel[offset + 1] = scalefactorChannel[offset + 2] = read(buffer, 6);
                    } else if (scfsi[ch * sbLimit + sb] == 3) {
                        scalefactorChannel[offset + 0] = read(buffer, 6);
                        scalefactorChannel[offset + 1] = scalefactorChannel[offset + 2] = read(buffer, 6);
                    }
                }
            }
        }
        for (int gr = 0; gr < 12; gr++) {
            for (int sb = 0; sb < bound; sb++) {
                for (int ch = 0; ch < stereo; ch++) {
                    int n = allocationChannel[ch * bound + sb];
                    int offset = ch * 32 * 12 * 3 + sb * 12 * 3 + gr * 3;
                    if (n == 0) {
                        sampleDecoded[offset] = sampleDecoded[offset + 1] = sampleDecoded[offset + 2] = 0;
                    } else {
                        int index = Mp3StandardData.QUANTIZATION_INDEX_LAYER_II[sbIndex][sb][n - 1];
                        int[] sampleInt = new int[3];
                        int sampleBits = Mp3StandardData.BITS_LAYER_II[index];
                        int nlevels = Mp3StandardData.NLEVELS[index];
                        if (Mp3StandardData.GROUPING_LAYER_II[index]) {
                            int samplecode = read(buffer, sampleBits);
                            sampleInt[0] = samplecode % nlevels;
                            samplecode /= nlevels;
                            sampleInt[1] = samplecode % nlevels;
                            samplecode /= nlevels;
                            sampleInt[2] = samplecode % nlevels;
                        } else {
                            sampleInt[0] = read(buffer, sampleBits);
                            sampleInt[1] = read(buffer, sampleBits);
                            sampleInt[2] = read(buffer, sampleBits);
                        }
                        int msb = 0;
                        while ((0b1 << msb) <= nlevels) {
                            msb++;
                        }
                        msb--;
                        for (int i = 0; i < 3; i++) {
                            float sample = 0;
                            if (((sampleInt[i] >> msb) & 0b1) == 0) {
                                sample = -1;
                            }
                            sample += (float) (sampleInt[i] & ((0b1 << msb) - 1)) / (0b1 << msb);
                            sample += Mp3StandardData.D_LAYER_II[index];
                            sample *= Mp3StandardData.C_LAYER_II[index];
                            sample *= Mp3StandardData.SCALEFACTORS[scalefactorChannel[ch * sbLimit * 3 + sb * 3 + gr / 4]];
                            sampleDecoded[offset + i] = sample;
                        }
                    }
                }
            }
            for (int sb = bound; sb < sbLimit; sb++) {
                int n = allocation[sb - bound];
                int offset = sb * 12 * 3 + gr * 3;
                if (n == 0) {
                    for (int ch = 0; ch < stereo; ch++) {
                        sampleDecoded[offset + ch * 32 * 12 * 3] = sampleDecoded[offset + ch * 32 * 12 * 3 + 1] = sampleDecoded[offset + ch * 32 * 12 * 3 + 2] = 0;
                    }
                } else {
                    int index = Mp3StandardData.QUANTIZATION_INDEX_LAYER_II[sbIndex][sb][n - 1];
                    int[] sampleInt = new int[3];
                    int sampleBits = Mp3StandardData.BITS_LAYER_II[index];
                    int nlevels = Mp3StandardData.NLEVELS[index];
                    if (Mp3StandardData.GROUPING_LAYER_II[index]) {
                        int samplecode = read(buffer, sampleBits);
                        sampleInt[0] = samplecode % nlevels;
                        samplecode /= nlevels;
                        sampleInt[1] = samplecode % nlevels;
                        samplecode /= nlevels;
                        sampleInt[2] = samplecode % nlevels;
                    } else {
                        sampleInt[0] = read(buffer, sampleBits);
                        sampleInt[1] = read(buffer, sampleBits);
                        sampleInt[2] = read(buffer, sampleBits);
                    }
                    int msb = 0;
                    while ((0b1 << msb) <= nlevels) {
                        msb++;
                    }
                    msb--;
                    for (int i = 0; i < 3; i++) {
                        float sample = 0;
                        if (((sampleInt[i] >> msb) & 0b1) == 0) {
                            sample = -1;
                        }
                        sample += (float) (sampleInt[i] & ((0b1 << msb) - 1)) / (0b1 << msb);
                        sample += Mp3StandardData.D_LAYER_II[index];
                        sample *= Mp3StandardData.C_LAYER_II[index];
                        for (int ch = 0; ch < stereo; ch++) {
                            sampleDecoded[offset + ch * 32 * 12 * 3 + i] = sample * Mp3StandardData.SCALEFACTORS[scalefactorChannel[ch * sbLimit * 3 + sb * 3 + gr / 4]];
                        }
                    }
                }
            }
        }
        return sampleDecoded;
    }

    private static void synth(SoundData soundData, float[] samples, int[] synthOffset, float[] synthBuffer, int stereo) {
        int size = samples.length / stereo / 32;
        float[] pcm = new float[size * 32 * stereo];
        for (int ch = 0; ch < stereo; ch++) {
            for (int s = 0; s < size; s++) {
                synthOffset[ch] = (synthOffset[ch] - 64) & 0x3ff;
                for (int i = 0; i < 64; i++) {
                    float sum = 0;
                    for (int k = 0; k < 32; k++) {
                        sum += Mp3StandardData.NIK_COEFFICIENTS[i * 32 + k] * samples[ch * 32 * size + k * size + s];
                    }
                    synthBuffer[ch * 1024 + synthOffset[ch] + i] = sum;
                }
                for (int j = 0; j < 32; j++) {
                    float sum = 0;
                    for (int i = 0; i < 16; i++) {
                        int k = j + (i << 5);
                        sum += Mp3StandardData.DI_COEFFICIENTS[k] * synthBuffer[ch * 1024 + ((synthOffset[ch] + (k + (((i + 1) >> 1) << 6))) & 0x3FF)];
                    }
                    pcm[s * 32 * stereo + j * stereo + ch] = sum;
                }
            }
        }
        if(soundData.samplesBuffer == null)
            soundData.samplesBuffer = new byte[size * 32 * stereo * 2];
        for (int i = 0; i < size * 32 * stereo; i++) {
            int sample = (int) (pcm[i] * 32768);
            if (sample >= 32768) {
                sample = 32767;
            } else if (sample < -32768) {
                sample = -32768;
            }
            soundData.samplesBuffer[i * 2] = (byte) sample;
            soundData.samplesBuffer[i * 2 + 1] = (byte) (sample >>> 8);
        }
    }

    private static int read(MainDataReader reader, int bits) {
        int number = 0;
        while (bits > 0) {
            int advance = Math.min(bits, 8 - reader.current);
            bits -= advance;
            reader.current += advance;
            number |= ((reader.array[reader.index] >>> (8 - reader.current)) & (0xFF >>> (8 - advance))) << bits;
            if (reader.current == 8) {
                reader.current = 0;
                reader.index++;
            }
        }
        return number;
    }

    private static int read(Buffer buffer, int bits) throws IOException {
        int number = 0;
        while (bits > 0) {
            int advance = Math.min(bits, 8 - buffer.current);
            bits -= advance;
            buffer.current += advance;
            if (bits != 0 && buffer.lastByte == -1) {
                throw new EOFException("Unexpected EOF reached in MPEG data");
            }
            number |= ((buffer.lastByte >>> (8 - buffer.current)) & (0xFF >>> (8 - advance))) << bits;
            if (buffer.current == 8) {
                buffer.current = 0;
                buffer.lastByte = buffer.in.read();
            }
        }
        return number;
    }

    private static void readInto(Buffer buffer, byte[] array, int offset, int length) throws IOException {
        if (buffer.current != 0)
        {
            throw new IllegalStateException("buffer current is " + buffer.current);
        }
        if (length == 0) {
            return;
        }
        if (buffer.lastByte == -1) {
            throw new EOFException("Unexpected EOF reached in MPEG data");
        }
        array[offset] = (byte) buffer.lastByte;
        int read = 1;
        while (read < length) {
            read += buffer.in.read(array, offset + read, length - read);
        }
        buffer.lastByte = buffer.in.read();
    }


    private static final class FrameHeader {

        int sigBytes;
        int version;
        int layer;
        int protectionBit;
        int bitrateIndex;
        int samplingFrequency;
        int paddingBit;
        int privateBit;
        int mode;
        int modeExtension;

        private FrameHeader(SoundData soundData) throws IOException {
            this.set(soundData);
        }

        private void set(SoundData soundData) throws IOException {


            soundData.buffer.current = 0;

            soundData.buffer.in.mark(4);
            try {
                this.sigBytes = read(soundData.buffer, 12);
                this.version = read(soundData.buffer, 1);
                this.layer = read(soundData.buffer, 2);
                this.protectionBit = read(soundData.buffer, 1);
                this.bitrateIndex = read(soundData.buffer, 4);
                this.samplingFrequency = read(soundData.buffer, 2);
                this.paddingBit = read(soundData.buffer, 1);
                this.privateBit = read(soundData.buffer, 1);
                this.mode = read(soundData.buffer, 2);
                this.modeExtension = read(soundData.buffer, 2);

                read(soundData.buffer, 4);
            } catch (EOFException e) {

                this.sigBytes = 0;
            }
        }

        private void unRead(SoundData soundData) throws IOException {
            // soundData.buffer.in.reset();
            soundData.buffer.lastByte = this.sigBytes >>> 4;
        }

        private boolean isValid() {
            return this.sigBytes == 0b111111111111 &&

                    this.layer != 0b0 &&
                    this.bitrateIndex != 0b1111 &&
                    this.samplingFrequency != 0b11;
        }
    }

    private static final class MainDataReader {
        public final byte[] array;
        public int top = 0;
        public int index = 0;
        public int current = 0;

        public MainDataReader(byte[] array) {
            this.array = array;
        }
    }

    private static final class Buffer {
        public final InputStream in;
        public int current = 0;
        public int lastByte = -1;

        public Buffer(InputStream inputStream) throws IOException {
            this.in = inputStream;
            this.lastByte = this.in.read();
        }
    }

    static final class SoundData {
        private Buffer buffer;

        int frequency = -1;
        int stereo = -1;

        private int[] synthOffset;
        private float[] synthBuffer;

        private byte[] mainData;
        private MainDataReader mainDataReader;

        private float[] store;
        private float[] v;

        byte[] samplesBuffer;
    }

}
