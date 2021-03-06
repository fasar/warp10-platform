//
//   Copyright 2018  SenX S.A.S.
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//

package io.warp10.script.fwt;

/**
 * Base class for describing wavelets. This code is inspired by that of JWave.
 * 
 * @see https://github.com/cscheiblich/JWave
 * 
 * The Wavelet_XXX classes which extend this base class are generated by the
 * shell script below.
 * 
 * The coefficients are scraped from http://wavelets.pybytes.com/.
 * 
 * The script is called using:
 * 
 * cat wavelets |while read f h; do curl "http://wavelets.pybytes.com/wavelet/$f/"|sh extract $f 2>/dev/null; done
 * 
 * Where wavelets contains the name of each wavelet to generate (part of the URI Path, see above).
 *
 */
//#!/bin/sh
//
//FULLNAME=$1
//NAME=`echo $1|sed -e 's,\\.,,'`
//
//cat > $$.tmp
//
//FAMILY=`grep 'Family: ' $$.tmp|sed -e 's,.*/family/,,' -e 's/Properties: //' -e 's/.*(//' -e 's/).*//'`
//
//SCALINGDECOM=`cat $$.tmp|tr '\n' ' '|sed -e 's/copy/copy#/g'|tr '#' '\n'|grep cols=\"24\"|sed -e 's/.*\"24\">//' -e 's/<.*//' -e 's/ /, /g'|head -n 1`
//WAVELETDECOM=`cat $$.tmp|tr '\n' ' '|sed -e 's/copy/copy#/g'|tr '#' '\n'|grep cols=\"24\"|sed -e 's/.*\"24\">//' -e 's/<.*//' -e 's/ /, /g'|tail -n 2|head -n 1`
//SCALINGRECON=`cat $$.tmp|tr '\n' ' '|sed -e 's/copy/copy#/g'|tr '#' '\n'|grep cols=\"24\"|sed -e 's/.*\"24\">//' -e 's/<.*//' -e 's/ /, /g'|head -n 2|tail -n 1`
//WAVELETRECON=`cat $$.tmp|tr '\n' ' '|sed -e 's/copy/copy#/g'|tr '#' '\n'|grep cols=\"24\"|sed -e 's/.*\"24\">//' -e 's/<.*//' -e 's/ /, /g'|tail -n 1`
//
//rm $$.tmp
//
//echo "  register(\"${FULLNAME}\", new Wavelet_${NAME}());"
//
//cat <<EOF > Wavelet_${NAME}.java
//package io.warp10.script.fwt.wavelets;
//
//import io.warp10.script.fwt.Wavelet;
//
//public class Wavelet_${NAME} extends Wavelet {
//
//  private static final int transformWavelength = 2;
//
//  private static final double[] scalingDeComposition = new double[] { $SCALINGDECOM };
//  private static final double[] waveletDeComposition = new double[] { $WAVELETDECOM };
//
//  private static final double[] scalingReConstruction = new double[] { $SCALINGRECON };
//  private static final double[] waveletReConstruction = new double[] { $WAVELETRECON };
//
//  static {
//    //
//    // Reverse the arrays as we do convolutions
//    //
//    reverse(scalingDeComposition);
//    reverse(waveletDeComposition);
//  }
//
//  private static final void reverse(double[] array) {
//    int i = 0;
//    int j = array.length - 1;
//  
//    while (i < j) {
//      double tmp = array[i];
//      array[i] = array[j];
//      array[j] = tmp;
//      i++;
//      j--;
//    }
//  }
//
//  public int getTransformWavelength() {
//    return transformWavelength;
//  }
//
//  public int getMotherWavelength() {
//    return waveletReConstruction.length;
//  }
//
//  public double[] getScalingDeComposition() {
//    return scalingDeComposition;
//  }
//
//  public double[] getWaveletDeComposition() {
//    return waveletDeComposition;
//  }
//
//  public double[] getScalingReConstruction() {
//    return scalingReConstruction;
//  }
//
//  public double[] getWaveletReConstruction() {
//    return waveletReConstruction;
//  }
//}
//
//EOF

public abstract class Wavelet {
  
  public double[] forward(double[] input, int len) {
    
    double[] scalingDeComposition = getScalingDeComposition();
    double[] waveletDeComposition = getWaveletDeComposition();
    int motherWavelength = getMotherWavelength();
    
    double[] output = new double[len];

    int h = output.length >> 1; // .. -> 8 -> 4 -> 2 .. shrinks in each step by half wavelength

    for(int i = 0; i < h; i++) {

      //output[i] = output[i + h] = 0.0D; // set to zero before sum up

      for( int j = 0; j < motherWavelength; j++ ) {

        int k = (i * 2) + j; // int k = ( i << 1 ) + j;

        while( k >= output.length ) {
          k -= output.length; // circulate over arrays if scaling and wavelet are are larger
        }
        
        output[i] += input[k] * scalingDeComposition[j]; // low pass filter for the energy (approximation)
        output[i + h] += input[k] * waveletDeComposition[j]; // high pass filter for the details

      } // Sorting each step in patterns of: { scaling coefficients | wavelet coefficients }

    } // h = 2^(p-1) | p = { 1, 2, .., N } .. shrinks in each step by half wavelength 

    return output;
  }
  
  public double[] reverse(double[] input, int len) {
    
    double[] scalingReConstruction = getScalingReConstruction();
    double[] waveletReConstruction = getWaveletReConstruction();
    int motherWavelength = getMotherWavelength();
    
    double[] output = new double[len];

    int h = output.length >> 1; // .. -> 8 -> 4 -> 2 .. shrinks in each step by half wavelength

    for(int i = 0; i < h; i++) {

      for(int j = 0; j < motherWavelength; j++) {

        int k = ( i * 2) + j; // int k = ( i << 1 ) + j;

        while( k >= output.length ) {
          k -= output.length; // circulate over arrays if scaling and wavelet are larger
        }
        
        // adding up energy from low pass (approximation) and details from high pass filter
        output[ k ] += (input[ i ] * scalingReConstruction[j]) + (input[i + h] * waveletReConstruction[j]);
      } // Reconstruction from patterns of: { scaling coefficients | wavelet coefficients }

    } // h = 2^(p-1) | p = { 1, 2, .., N } .. shrink in each step by half wavelength 

    return output;
  }
  
  public abstract int getMotherWavelength();
  public abstract int getTransformWavelength();
  public abstract double[] getScalingDeComposition();
  public abstract double[] getWaveletDeComposition();
  public abstract double[] getScalingReConstruction();
  public abstract double[] getWaveletReConstruction();  
}
