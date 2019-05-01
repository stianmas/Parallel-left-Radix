import java.util.*;
/***********************************************************
* Oblig 3 - sekvensiell kode, INF2440 v2016.
*            Ifi, Uio, Arne Maus
************************************************************/
class MultiRadix1{
  int n;
  int [] a;
  final static int NUM_BIT =7; // alle tall 6-11 OK
  double tid;

  double doIt (int len) {
    a = new int[len];
    Random r = new Random(123);
    for (int i =0; i < len;i++) {
       a[i] = r.nextInt(len);
      }
      a = radixMulti(a);
      return tid;
  } // end doIt

    int []  radixMulti(int [] a) {
      long tt = System.nanoTime();
      // 1-5 digit radixSort of : a[]
      int max = a[0], numBit = 2, numDigits, n =a.length;
      int [] bit ;

     // a) finn max verdi i a[]
      for (int i = 1 ; i < n ; i++)
         if (a[i] > max) max = a[i];
      while (max >= (1L<<numBit) )numBit++; // antall siffer i max

      // bestem antall bit i numBits sifre
      numDigits = Math.max(1, numBit/NUM_BIT);
      bit = new int[numDigits];
      int rest = (numBit%numDigits), sum =0;;

      // fordel bitene vi skal sortere paa jevnt
      for (int i = 0; i < bit.length; i++){
        bit[i] = numBit/numDigits;
          if ( rest-- > 0)  bit[i]++;
      }

      int[] t=a, b = new int [n];

      for (int i =0; i < bit.length; i++) {
        radixSort( a,b,bit[i],sum );    // i-te siffer fra a[] til b[]
        sum += bit[i];
        // swap arrays (pointers only)
        t = a;
        a = b;
        b = t;
      }
      if (bit.length%2 != 0 ) {
        // et odde antall sifre, kopier innhold tilbake til original a[] (n� b)
        System.arraycopy (a,0,b,0,a.length);
      }

      tid = (System.nanoTime() -tt)/1000000.0;
      //System.out.println("\nSorterte "+n+" tall paa:" + tid + "millisek.");
      testSort(a);
      return a;
   } // end radixMulti

  /** Sort a[] on one digit ; number of bits = maskLen, shiftet up 'shift' bits */
   void radixSort ( int [] a, int [] b, int maskLen, int shift){
    // System.out.println(" radixSort maskLen:"+maskLen+", shift :"+shift);
      int  acumVal = 0, j, n = a.length;
      int mask = (1<<maskLen) -1;
      int [] count = new int [mask+1];

     // b) count=the frequency of each radix value in a
      for (int i = 0; i < n; i++) {
       count[(a[i]>>> shift) & mask]++;
      }

     // c) Add up in 'count' - accumulated values
      for (int i = 0; i <= mask; i++) {
         j = count[i];
        count[i] = acumVal;
        acumVal += j;
       }
     // d) move numbers in sorted order a to b
      for (int i = 0; i < n; i++) {
       b[count[(a[i]>>>shift) & mask]++] = a[i];
      }

  }// end radixSort

  void testSort(int [] a){
    for (int i = 0; i< a.length-1;i++) {
       if (a[i] > a[i+1]){
          System.out.println("SorteringsFEIL p� plass: "+i +" a["+i+"]:"+a[i]+" > a["+(i+1)+"]:"+a[i+1]);
          return;
      }
    }
   }// end simple sorteingstest
}// end SekvensiellRadix
