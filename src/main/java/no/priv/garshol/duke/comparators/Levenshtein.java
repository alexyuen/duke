
package no.priv.garshol.duke.comparators;

import no.priv.garshol.duke.Comparator;

// general background on Levenshtein:
// http://www.let.rug.nl/kleiweg/lev/

// on faster algorithms:
// http://stackoverflow.com/questions/4057513/levenshtein-distance-algorithm-better-than-onm

/**
 * An implementation of the Levenshtein distance metric.
 */
public class Levenshtein implements Comparator {

  public double compare(String s1, String s2) {   
    int len = Math.min(s1.length(), s2.length());

    // we know that if the outcome here is 0.5 or lower, then the
    // property will return the lower probability. so the moment we
    // learn that probability is 0.5 or lower we can return 0.0 and
    // stop. this optimization makes a perceptible improvement in
    // overall performance.
    int maxlen = Math.max(s1.length(), s2.length());
    if ((double) len / (double) maxlen <= 0.5)
      return 0.0;

    // if the strings are equal we can stop right here.
    if (len == maxlen && s1.equals(s2))
      return 1.0;
    
    // we couldn't shortcut, so now we go ahead and compute the full
    // matrix
    int dist = Math.min(cutoffDistance(s1, s2, maxlen), len);
    //int dist = Math.min(distance(s1, s2), len);
    return 1.0 - (((double) dist) / ((double) len));
  }

  public boolean isTokenized() {
    return true;
  }

  // the original, unoptimized implementation. not sure why I am leaving
  // it here.
  public static int distance(String s1, String s2) {
    if (s1.length() == 0)
      return s2.length();
    if (s2.length() == 0)
      return s1.length();

    int s1len = s1.length();
    // we use a flat array for better performance. we address it by
    // s1ix + s1len * s2ix. this modification improves performance
    // by about 30%, which is definitely worth the extra complexity.
    int[] matrix = new int[(s1len + 1) * (s2.length() + 1)];
    for (int col = 0; col <= s2.length(); col++)
      matrix[col * s1len] = col;
    for (int row = 0; row <= s1len; row++)
      matrix[row] = row;

    for (int ix1 = 0; ix1 < s1len; ix1++) {
      char ch1 = s1.charAt(ix1);
      for (int ix2 = 0; ix2 < s2.length(); ix2++) {
        int cost;
        if (ch1 == s2.charAt(ix2))
          cost = 0;
        else
          cost = 1;

        int left = matrix[ix1 + ((ix2 + 1) * s1len)] + 1;
        int above = matrix[ix1 + 1 + (ix2 * s1len)] + 1;
        int aboveleft = matrix[ix1 + (ix2 * s1len)] + cost;
        matrix[ix1 + 1 + ((ix2 + 1) * s1len)] =
          Math.min(left, Math.min(above, aboveleft));
      }
    }

    // for (int ix1 = 0; ix1 <= s1len; ix1++) {
    //   for (int ix2 = 0; ix2 <= s2.length(); ix2++) {
    //     System.out.print(matrix[ix1 + (ix2 * s1len)] + " ");
    //   }
    //   System.out.println();
    // }
    
    return matrix[s1len + (s2.length() * s1len)];
  }
  
  // optimizes by returning 0.0 as soon as we know total difference is
  // larger than 0.5, which happens when the distance is greater than
  // maxlen.
  //
  // on at least one use case, this optimization shaves 15% off the
  // total execution time.
  public static int cutoffDistance(String s1, String s2, int maxlen) {
    if (s1.length() == 0)
      return s2.length();
    if (s2.length() == 0)
      return s1.length();

    int maxdist = Math.min(s1.length(), s2.length()) / 2;

    int s1len = s1.length();
    // we use a flat array for better performance. we address it by
    // s1ix + s1len * s2ix. this modification improves performance
    // by about 30%, which is definitely worth the extra complexity.
    int[] matrix = new int[(s1len + 1) * (s2.length() + 1)];
    for (int col = 0; col <= s2.length(); col++)
      matrix[col * s1len] = col;
    for (int row = 0; row <= s1len; row++)
      matrix[row] = row;

    for (int ix1 = 0; ix1 < s1len; ix1++) {
      char ch1 = s1.charAt(ix1);
      for (int ix2 = 0; ix2 < s2.length(); ix2++) {
        int cost;
        if (ch1 == s2.charAt(ix2))
          cost = 0;
        else
          cost = 1;

        int left = matrix[ix1 + ((ix2 + 1) * s1len)] + 1;
        int above = matrix[ix1 + 1 + (ix2 * s1len)] + 1;
        int aboveleft = matrix[ix1 + (ix2 * s1len)] + cost;
        int distance = Math.min(left, Math.min(above, aboveleft));
        if (ix1 == ix2 && distance > maxdist)
           return distance;
        matrix[ix1 + 1 + ((ix2 + 1) * s1len)] = distance;
      }
    }
    
    return matrix[s1len + (s2.length() * s1len)];
  }

  // // this one tries to reduce the amount of work necessary by only
  // // computing adjacent cells if their values are needed
  // public static int recursiveDistance(String s1, String s2) {
  //   if (s1.length() == 0)
  //     return s2.length();
  //   if (s2.length() == 0)
  //     return s1.length();

  //   int s1len = s1.length();
  //   // we use a flat array for better performance. we address it by
  //   // s1ix + s1len * s2ix. this modification improves performance
  //   // by about 30%, which is definitely worth the extra complexity.
  //   int[] matrix = new int[(s1len + 1) * (s2.length() + 1)];
  //   // FIXME: modify to avoid having to initialize
  //   for (int ix = 1; ix < matrix.length; ix++)
  //     matrix[ix] = -1;
    
  //   return computeRecursively(matrix, s1, s2, s1len, s2.length());
  // }

  // private static int computeRecursively(int[] matrix, String s1, String s2,
  //                                       int ix1, int ix2) {
  //   int s1len = s1.length();
  //   int pos = ix1 + 1 + ((ix2 + 1) * s1len); // our position in the matrix
  //   if (matrix[pos] != -1)
  //     return matrix[pos];
    
  //   int cost;
  //   if (ch1 == s2.charAt(ix2))
  //     cost = 0;
  //   else
  //     cost = 1;

  //   if (matrix[ix1 + (ix2 * s1len)] == -1) {
  //   }
      
  //   int aboveleft = matrix[ix1 + (ix2 * s1len)] + cost;
  //   int left = matrix[ix1 + ((ix2 + 1) * s1len)] + 1;
  //   int above = matrix[ix1 + 1 + (ix2 * s1len)] + 1;
  //   int distance = Math.min(left, Math.min(above, aboveleft));

    
  // }
}