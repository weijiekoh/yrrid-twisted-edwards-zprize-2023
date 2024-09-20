/***

Copyright (c) 2023-2024, Yrrid Software, Inc. and Snarkify Inc.  All rights reserved.
Dual licensed under the MIT License or the Apache License, Version 2.0.
See LICENSE for details.

Author(s):  Niall Emmart

***/

import java.util.*;
import java.math.*;

// We use Java to demonstrate how integer math is accomplished using floating point FMA hardware.
// We generate 1000 random pairs of 51-bit numbers, and we compute the products using the standard
// bignum routines, and our FP64 technique.  We print out both results.
//
// The main idea for this approach was published in the following paper:
//    "Faster Modular Exponentiation Using Double Precision Floating Point Arithmetic on the GPU."
//    2018 IEEE 25th Symposium on Computer Arithmetic (ARITH). IEEE Computer Society, 2018.
//    By Emmart, Zheng and Weems.
//
// But note, we have don't have explicit control of the rounding mode in WASM, so, we use 51 bits
// per limb and the low word can be positive (if we rounded down), or negative (if we rounded up).


public class FP51 {
  static public double p(int count) {
    double x=1;

    for(int i=0;i<count;i++)
      x=x*2;
    return x;
  }

  static public long low(BigInteger a, BigInteger b) {
    return a.longValue() * b.longValue() & 0x7FFFFFFFFFFFFL;
  }

  static public long high(BigInteger a, BigInteger b) {
    return a.multiply(b).shiftRight(51).longValue();
  }

  static public long[] fpLow(BigInteger a, BigInteger b) {
    double ad=(double)a.longValue(), bd=(double)b.longValue();
    double lo, hi;
    double c1=p(103);
    double c2=p(103)+3*p(51);

    // System.out.printf("%016X %016X\n", Double.doubleToRawLongBits(c1), Double.doubleToRawLongBits(c2));

    hi=Math.fma(ad, bd, c1);
    lo=c2-hi;
    lo=Math.fma(ad, bd, lo);

    long mask = 0x7ffffffffffffL;
    long hi_long = (Double.doubleToRawLongBits(hi) - Double.doubleToRawLongBits(c1)) & mask;
    long lo_long = (Double.doubleToRawLongBits(lo) - Double.doubleToRawLongBits(3*p(51))) & mask;

    System.out.printf("%016X\n", Double.doubleToRawLongBits(3*p(51)));

    //System.out.printf("%016X %016X\n", hi_long, lo_long);
    //System.out.printf("%016X %016X\n", Double.doubleToRawLongBits(hi)-Double.doubleToRawLongBits(c1), Double.doubleToRawLongBits(lo)-Double.doubleToRawLongBits(3*p(51)));
    long[] r = { hi_long, lo_long };
    return r;
  }

  static public void main(String[] args) {
    Random     r=new Random(0);
    BigInteger a, b;
    double     c1=p(103);
    double     c2=p(103)+3*p(51);

    //System.out.printf("%016X %016X\n", Double.doubleToRawLongBits(c1), Double.doubleToRawLongBits(c2));

    for(int i=0;i<1000;i++) {
      //a = new BigInteger("1125899906842624");
      //b = new BigInteger("1125899906842623");
      a=new BigInteger(48, r);
      b=new BigInteger(48, r);
      System.out.printf("a: %d\n", a);
      System.out.printf("b: %d\n", b);

      long bn_hi = high(a, b); 
      long bn_lo = low(a, b); 

      long[] fp_result = fpLow(a, b);

      System.out.printf("Using BN: %016X %016X\n", bn_hi, bn_lo);
      System.out.print("Using FP: ");
      System.out.printf("%016X %016X\n", fp_result[0], fp_result[1]);

      if (fp_result[0] != bn_hi || fp_result[1] != bn_lo) {
          System.out.println("Mismatch!");
          break;
      }

      //fpLow(a, b);
      System.out.println();
    }
  }
}
