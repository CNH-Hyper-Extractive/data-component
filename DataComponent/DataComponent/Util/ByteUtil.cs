﻿// -----------------------------------------------------------------------
//  Copyright (c) 2014 Tom Bulatewicz, Kansas State University
//  
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files (the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions:
//  
//  The above copyright notice and this permission notice shall be included in all
//  copies or substantial portions of the Software.
//  
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//  SOFTWARE.
// -----------------------------------------------------------------------

using System;
using System.Text;

namespace KState.Util
{
    public class ByteUtil
    {
        /* ========================= */
        /* "primitive type --> byte[] data" Methods */
        /* ========================= */

        public static byte[] toByta(byte data)
        {
            return new[] {data};
        }

        public static byte[] toByta(byte[] data)
        {
            return data;
        }

        /* ========================= */

        public static byte[] toByta(short data)
        {
            return new[]
            {
                (byte)((data >> 8) & 0xff),
                (byte)((data >> 0) & 0xff)
            };
        }

        public static byte[] toByta(short[] data)
        {
            if (data == null) return null;
            // ----------
            var byts = new byte[data.Length*2];
            for (var i = 0; i < data.Length; i++)
                Array.Copy(toByta(data[i]), 0, byts, i*2, 2);
            return byts;
        }

        /* ========================= */

        public static byte[] toByta(char data)
        {
            return new[]
            {
                (byte)((data >> 8) & 0xff),
                (byte)((data >> 0) & 0xff)
            };
        }

        public static byte[] toByta(char[] data)
        {
            if (data == null) return null;
            // ----------
            var byts = new byte[data.Length*2];
            for (var i = 0; i < data.Length; i++)
                Array.Copy(toByta(data[i]), 0, byts, i*2, 2);
            return byts;
        }

        /* ========================= */

        public static byte[] toByta(int data)
        {
            return new[]
            {
                (byte)((data >> 24) & 0xff),
                (byte)((data >> 16) & 0xff),
                (byte)((data >> 8) & 0xff),
                (byte)((data >> 0) & 0xff)
            };
        }

        public static byte[] toByta(int[] data)
        {
            if (data == null) return null;
            // ----------
            var byts = new byte[data.Length*4];
            for (var i = 0; i < data.Length; i++)
                Array.Copy(toByta(data[i]), 0, byts, i*4, 4);
            return byts;
        }

        /* ========================= */

        public static byte[] toByta(long data)
        {
            return new[]
            {
                (byte)((data >> 56) & 0xff),
                (byte)((data >> 48) & 0xff),
                (byte)((data >> 40) & 0xff),
                (byte)((data >> 32) & 0xff),
                (byte)((data >> 24) & 0xff),
                (byte)((data >> 16) & 0xff),
                (byte)((data >> 8) & 0xff),
                (byte)((data >> 0) & 0xff)
            };
        }

        public static byte[] toByta(long[] data)
        {
            if (data == null) return null;
            // ----------
            var byts = new byte[data.Length*8];
            for (var i = 0; i < data.Length; i++)
                Array.Copy(toByta(data[i]), 0, byts, i*8, 8);
            return byts;
        }

        /* ========================= */

        public static byte[] toByta(float data)
        {
            return toByta(BitConverter.DoubleToInt64Bits(data));
        }

        public static byte[] toByta(float[] data)
        {
            if (data == null) return null;
            // ----------
            var byts = new byte[data.Length*4];
            for (var i = 0; i < data.Length; i++)
                Array.Copy(toByta(data[i]), 0, byts, i*4, 4);
            return byts;
        }

        /* ========================= */

        public static byte[] toByta(double data)
        {
            return toByta(BitConverter.DoubleToInt64Bits(data));
        }

        public static byte[] toByta(double[] data)
        {
            if (data == null) return null;
            // ----------
            var byts = new byte[data.Length*8];
            for (var i = 0; i < data.Length; i++)
                Array.Copy(toByta(data[i]), 0, byts, i*8, 8);
            return byts;
        }

        /* ========================= */

        public static byte[] toByta(bool data)
        {
            return new[] {(byte)(data ? 0x01 : 0x00)}; // bool -> {1 byte}
        }

        public static byte[] toByta(bool[] data)
        {
            // Advanced Technique: The byte array containts information
            // about how many boolean values are involved, so the exact
            // array is returned when later decoded.
            // ----------
            if (data == null) return null;
            // ----------
            var len = data.Length;
            var lena = toByta(len); // int conversion; length array = lena
            var byts = new byte[lena.Length + (len/8) + (len%8 != 0 ? 1 : 0)];
            // (Above) length-array-length + sets-of-8-booleans +? byte-for-remainder
            Array.Copy(lena, 0, byts, 0, lena.Length);
            // ----------
            // (Below) algorithm by Matthew Cudmore: boolean[] -> bits -> byte[]
            for (int i = 0, j = lena.Length, k = 7; i < data.Length; i++)
            {
                byts[j] |= (byte)((data[i] ? 1 : 0) << k--);
                if (k < 0)
                {
                    j++;
                    k = 7;
                }
            }
            // ----------
            return byts;
        }

        /* ========================= */

        public static byte[] toByta(String data)
        {
            return (data == null) ? null : new UTF8Encoding().GetBytes(data.ToCharArray());
        }

        public static byte[] toByta(String[] data)
        {
            // Advanced Technique: Generates an indexed byte array
            // which contains the array of Strings. The byte array
            // contains information about the number of Strings and
            // the length of each String.
            // ----------
            if (data == null) return null;
            // ---------- flags:
            var totalLength = 0; // Measure length of final byte array
            var bytesPos = 0; // Used later
            // ----- arrays:
            var dLen = toByta(data.Length); // byte array of data length
            totalLength += dLen.Length;
            var sLens = new int[data.Length]; // String lengths = sLens
            totalLength += (sLens.Length*4);
            var strs = new byte[data.Length][]; // array of String bytes
            // ----- pack strs:
            for (var i = 0; i < data.Length; i++)
            {
                if (data[i] != null)
                {
                    strs[i] = toByta(data[i]);
                    sLens[i] = strs[i].Length;
                    totalLength += strs[i].Length;
                }
                else
                {
                    sLens[i] = 0;
                    strs[i] = new byte[0]; // prevent null entries
                }
            }
            // ----------
            var bytes = new byte[totalLength]; // final array
            Array.Copy(dLen, 0, bytes, 0, 4);
            var bsLens = toByta(sLens); // byte version of String sLens
            Array.Copy(bsLens, 0, bytes, 4, bsLens.Length);
            // -----
            bytesPos += 4 + bsLens.Length; // mark position
            // -----
            foreach (var sba in strs)
            {
                Array.Copy(sba, 0, bytes, bytesPos, sba.Length);
                bytesPos += sba.Length;
            }
            // ----------
            return bytes;
        }

        /* ========================= */
        /* "byte[] data --> primitive type" Methods */
        /* ========================= */

        public static byte toByte(byte[] data)
        {
            return (data == null || data.Length == 0) ? (byte)0x0 : data[0];
        }

        public static byte[] toByteA(byte[] data)
        {
            return data;
        }

        /* ========================= */

        public static short toShort(byte[] data)
        {
            if (data == null || data.Length != 2) return 0x0;
            // ----------
            return (short)(
                (0xff & data[0]) << 8 |
                (0xff & data[1]) << 0
                );
        }

        public static short[] toShortA(byte[] data)
        {
            if (data == null || data.Length%2 != 0) return null;
            // ----------
            var shts = new short[data.Length/2];
            for (var i = 0; i < shts.Length; i++)
            {
                shts[i] = toShort(new[]
                {
                    data[(i*2)],
                    data[(i*2) + 1]
                });
            }
            return shts;
        }

        /* ========================= */

        public static char toChar(byte[] data)
        {
            if (data == null || data.Length != 2) return (char)0x0;
            // ----------
            return (char)(
                (0xff & data[0]) << 8 |
                (0xff & data[1]) << 0
                );
        }

        public static char[] toCharA(byte[] data)
        {
            if (data == null || data.Length%2 != 0) return null;
            // ----------
            var chrs = new char[data.Length/2];
            for (var i = 0; i < chrs.Length; i++)
            {
                chrs[i] = toChar(new[]
                {
                    data[(i*2)],
                    data[(i*2) + 1]
                });
            }
            return chrs;
        }

        /* ========================= */

        public static int toInt(byte[] data)
        {
            if (data == null || data.Length != 4) return 0x0;
            // ----------
            return (0xff & data[0]) << 24 |
                   (0xff & data[1]) << 16 |
                   (0xff & data[2]) << 8 |
                   (0xff & data[3]) << 0;
        }

        public static int[] toIntA(byte[] data)
        {
            if (data == null || data.Length%4 != 0) return null;
            // ----------
            var ints = new int[data.Length/4];
            for (var i = 0; i < ints.Length; i++)
                ints[i] = toInt(new[]
                {
                    data[(i*4)],
                    data[(i*4) + 1],
                    data[(i*4) + 2],
                    data[(i*4) + 3]
                });
            return ints;
        }

        /* ========================= */

        public static long toLong(byte[] data)
        {
            if (data == null || data.Length != 8) return 0x0;
            // ----------
            return (long)(0xff & data[0]) << 56 |
                   (long)(0xff & data[1]) << 48 |
                   (long)(0xff & data[2]) << 40 |
                   (long)(0xff & data[3]) << 32 |
                   (long)(0xff & data[4]) << 24 |
                   (long)(0xff & data[5]) << 16 |
                   (long)(0xff & data[6]) << 8 |
                   (long)(0xff & data[7]) << 0;
        }

        public static long[] toLongA(byte[] data)
        {
            if (data == null || data.Length%8 != 0) return null;
            // ----------
            var lngs = new long[data.Length/8];
            for (var i = 0; i < lngs.Length; i++)
            {
                lngs[i] = toLong(new[]
                {
                    data[(i*8)],
                    data[(i*8) + 1],
                    data[(i*8) + 2],
                    data[(i*8) + 3],
                    data[(i*8) + 4],
                    data[(i*8) + 5],
                    data[(i*8) + 6],
                    data[(i*8) + 7]
                });
            }
            return lngs;
        }

        /* ========================= */

        public static float toFloat(byte[] data)
        {
            if (data == null || data.Length != 4) return 0x0;
            // ---------- simple:
            return (float)BitConverter.Int64BitsToDouble(toInt(data));
        }

        public static float[] toFloatA(byte[] data)
        {
            if (data == null || data.Length%4 != 0) return null;
            // ----------
            var flts = new float[data.Length/4];
            for (var i = 0; i < flts.Length; i++)
            {
                flts[i] = toFloat(new[]
                {
                    data[(i*4)],
                    data[(i*4) + 1],
                    data[(i*4) + 2],
                    data[(i*4) + 3]
                });
            }
            return flts;
        }

        /* ========================= */

        public static double toDouble(byte[] data)
        {
            if (data == null || data.Length != 8) return 0x0;
            // ---------- simple:
            return BitConverter.Int64BitsToDouble(toLong(data));
        }

        public static double[] toDoubleA(byte[] data)
        {
            if (data == null) return null;
            // ----------
            if (data.Length%8 != 0) return null;
            var dbls = new double[data.Length/8];
            for (var i = 0; i < dbls.Length; i++)
            {
                dbls[i] = toDouble(new[]
                {
                    data[(i*8)],
                    data[(i*8) + 1],
                    data[(i*8) + 2],
                    data[(i*8) + 3],
                    data[(i*8) + 4],
                    data[(i*8) + 5],
                    data[(i*8) + 6],
                    data[(i*8) + 7]
                });
            }
            return dbls;
        }

        /* ========================= */

        public static bool toBoolean(byte[] data)
        {
            return (data == null || data.Length == 0) ? false : data[0] != 0x00;
        }

        public static bool[] toBooleanA(byte[] data)
        {
            // Advanced Technique: Extract the boolean array's length
            // from the first four bytes in the char array, and then
            // read the boolean array.
            // ----------
            if (data == null || data.Length < 4) return null;
            // ----------
            var len = toInt(new[] {data[0], data[1], data[2], data[3]});
            var bools = new bool[len];
            // ----- pack bools:
            for (int i = 0, j = 4, k = 7; i < bools.Length; i++)
            {
                bools[i] = ((data[j] >> k--) & 0x01) == 1;
                if (k < 0)
                {
                    j++;
                    k = 7;
                }
            }
            // ----------
            return bools;
        }

        /* ========================= */

        public static String toString(byte[] data)
        {
            return (data == null) ? null : new UTF8Encoding().GetString(data);
        }

        public static String[] toStringA(byte[] data)
        {
            // Advanced Technique: Extract the String array's length
            // from the first four bytes in the char array, and then
            // read the int array denoting the String lengths, and
            // then read the Strings.
            // ----------
            if (data == null || data.Length < 4) return null;
            // ----------
            var bBuff = new byte[4]; // Buffer
            // -----
            Array.Copy(data, 0, bBuff, 0, 4);
            var saLen = toInt(bBuff);
            if (data.Length < (4 + (saLen*4))) return null;
            // -----
            bBuff = new byte[saLen*4];
            Array.Copy(data, 4, bBuff, 0, bBuff.Length);
            var sLens = toIntA(bBuff);
            if (sLens == null) return null;
            // ----------
            var strs = new String[saLen];
            for (int i = 0, dataPos = 4 + (saLen*4); i < saLen; i++)
            {
                if (sLens[i] > 0)
                {
                    if (data.Length >= (dataPos + sLens[i]))
                    {
                        bBuff = new byte[sLens[i]];
                        Array.Copy(data, dataPos, bBuff, 0, sLens[i]);
                        dataPos += sLens[i];
                        strs[i] = toString(bBuff);
                    }
                    else return null;
                }
            }
            // ----------
            return strs;
        }
    }
}