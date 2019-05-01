import java.util.*;
import java.util.concurrent.*;
/***********************************************************
* Oblig 3 - Parallell kode, INF2440 v2016.
*            Stian Masserud, prekode av Arne Maus
************************************************************/
class Toblig3{
	// max er største verdi i a[]. jump er hvor stort arbeidsområde hver tråd skal ha i a[].
	int n, max, numBit, numberOfCores, jump;
	int[] a, b, bit, acumAddr;
	final static int NUM_BIT =7; // alle tall 6-11 OK
	// Parallel data
	CyclicBarrier cb, justThreads;
	int[][] allCount;
	double tid;

	public static void main(String [] args) {
		if (args.length != 1) {
	     	System.out.println(" bruk : >java SekvensiellRadix <n> ");
		} else {
			int n = Integer.parseInt(args[0]);
			
			new Toblig3().doIt(n);

		}
	} // end main

	void doIt (int len) {
		System.out.println("n = " + len);
		double sekTid = new MultiRadix1().doIt(len);
		a = new int[len];
		Random r = new Random(123);
		for (int i =0; i < len;i++) {
		   a[i] = r.nextInt(len);
	    }
	    a = radixMulti(a);
	    System.out.println("Sekvensiell tid: " +sekTid + "ms");
	   	System.out.println("Parallell tid: " +tid + "ms");
	   	System.out.println("Speedup: " + sekTid/tid);
	} // end doIt

    int[] radixMulti(int[] a) {
		long tt = System.nanoTime();		
		int numBit = 2, numDigits, n =a.length;
		numberOfCores =  Runtime.getRuntime().availableProcessors();
		cb = new CyclicBarrier(numberOfCores + 1);
		justThreads = new CyclicBarrier(numberOfCores);

		// a) finn max verdi i a[]
		jump = a.length/numberOfCores;	
		for(int i = 0; i < numberOfCores; i++) {
			if(i == numberOfCores-1) {
				new Thread(new FindMax(jump*i, a.length)).start();
			} else {
				new Thread(new FindMax(jump*i, (jump*i)+jump)).start();
			}
		}

		try{
			cb.await();
		} catch (Exception e) {System.out.println("Wrong with main under find max");}
		while (max >= (1L<<numBit) )numBit++;// antall siffer i max
		 			 
		numDigits = Math.max(1, numBit/NUM_BIT);
		bit = new int[numDigits];
		int rest = (numBit%numDigits), sum =0;;
		
		// fordel bitene vi skal sortere paa jevnt
		for (int i = 0; i < bit.length; i++){
		    bit[i] = numBit/numDigits;
		    if ( rest-- > 0)  bit[i]++;
		}

		b = new int[a.length];
		allCount = new int[numberOfCores + 1][];
		acumAddr = new int[numberOfCores];
		
		// Starter trådene	  	  
		for(int i = 0; i < numberOfCores; i++) {
			if(i == numberOfCores-1) {
				new Thread(new ParaStepB(i, jump*i, a.length, b)).start();
			} else {
				new Thread(new ParaStepB(i, jump*i, (jump*i)+jump,b)).start();
			}
		}
	  
		try {
			cb.await();
		} catch (Exception e) {
			System.out.println("Exception at cb");
		}		  
		/* vet ikke helt hva denne er god for. Var med i prekode.
		if (bit.length%2 != 0 ) {
			// et odde antall sifre, kopier innhold tilbake til original a[] (n� b)
			System.arraycopy (a,0,b,0,a.length);
		}*/

	  	tid = (System.nanoTime() -tt)/1000000.0;
		//System.out.println("\nSorterte "+n+" tall paa:" + tid + "millisek.");
	  	testSort(a);
	  	return a;
	} // end radixMulti

	void testSort(int [] a){
		for (int i = 0; i< a.length-1;i++) {
			if (a[i] > a[i+1]){
		    	System.out.println("SorteringsFEIL på plass: "+i +" a["+i+"]:"+a[i]+" > a["+(i+1)+"]:"+a[i+1]);
		      	return;
		  	}
		  	//System.out.println(a[i]);
	  	}
	}// end simple sorteingstest
	
	synchronized void checkIfBiggest(int test) {
		if(test > max) {
			max = test; 
		}
	}

	// Parallel solution to find biggest number in array 'a'
	class FindMax implements Runnable{
		int localMax, start, stop;

		public FindMax(int start, int stop) {
			this.start = start;
			this.stop = stop;
			localMax = 0;
			if(start != 0) start++;
		}
		public void run() {
			// Steg a, finne max i a. Hver tråd har lokal maxverdi og sammenligner seg med andre i checkIfBigger().
			for(int i = start; i < stop; i++) {
				if(a[i] > localMax) 
					localMax = a[i];
			}
			checkIfBiggest(localMax);
			try{
				cb.await();
			} catch (Exception e) {System.out.println("Wrong with main under find max");}
		}
	}	

	// Intern klasse får å løse radix parallelt. 
	private class ParaStepB implements Runnable {
		int id, start, stop, shift, mask;
		int nrOfBuckets;
		int [] localCount, acumVal, a, t;
		
		public ParaStepB(int id, int start, int stop, int[] a){
			this.id = id;
			this.start = start;
			this.stop = stop;
			this.a = a;
		}		
		
		public void run () {
		
			for(int i = 0; i < bit.length; i++) {	
				mask = (1<<bit[i])-1;
				shift += bit[i];
				localCount = new int [mask+1];
				acumVal = new int [mask+1];
				//if(id == 0) System.out.println(" radixSort maskLen:"+bit[i]+", shift :"+shift);

				// For step b, stores the last pre acumulation values in allcount[numberOfCores]
				if(id == 0){
					allCount[numberOfCores]= new int [localCount.length + 1];
				}	
				for (int j = start; j < stop; j++) {				
					localCount[(a[j]>>> shift) & mask] += 1;				
			 	}
				allCount[id] = localCount;
				
	
				try{
					justThreads.await();
				} catch (Exception e) {
					System.out.println("Sometthing worng with first barrier in ParaStepB");	
				}
				// Kalkulerer hver tråds start og stop i arrayer med lengden = localCount.length
				int helper = localCount.length/numberOfCores;
				int l = helper * id;
				int r;
				if(id == numberOfCores){
					r = localCount.length;
				}else {
					r = (helper * id) + helper;
				}
				// Step c. 
				// Summerer opp hver tråd sin opptelling fra b og legger dem sammen i den siste raden i allCount. Denne raden er ikke brukt før.
				int sum = 0, acumTot = 0;
				for (int j = l; j < r; j++) {
					for (int k = 0; k < numberOfCores; k++) {					
						sum += allCount[k][j];
					}
					allCount[numberOfCores][j] = sum;
					acumTot = sum;
					sum = 0;	
				}
				acumAddr[id] = acumTot;
				try {
					justThreads.await();
				} catch (Exception e) {}

				// Kalkulerer tidligere verdier i forrige steg og akkumulerer disse. 
				int tmp = 0;
				for(int j = 0; j < acumVal.length; j++ ){
					tmp += allCount[numberOfCores][j];
					acumVal[j] = tmp;
					// Minus verdiene som allerede er på en gitt plass i allcount
					for(int k = id; k < numberOfCores; k++){				
						acumVal[j] -= allCount[k][j];
					}
				}
				try {
					justThreads.await();
				} catch (Exception e) {}
				// step d. 
				for(int j = start; j < stop; j++) {		
					b[acumVal[(a[j]>>>shift) & mask]++] = a[j];
				}
							
				try {
					justThreads.await();
				} catch (Exception e) {}
				
				// Oppdater pekere
				t = a;
				a = b;
				b = t;
				
			} // Ferdig sortert
			 
			try {
				cb.await();
			} catch (Exception e) {}		  
		}	
	}
}// end SekvensiellRadix