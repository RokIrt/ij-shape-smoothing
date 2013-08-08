package de.biomedical_imaging.ij.shapeSmoothing;

import ij.ImagePlus;
import ij.blob.Blob;
import ij.blob.ManyBlobs;
import ij.process.ImageProcessor;

import java.awt.Polygon;
import java.util.Vector;

import de.biomedical_imaging.ij.shapeSmoothingSlow.ComplexNumber;
import de.biomedical_imaging.ij.shapeSmoothingSlow.MyUsefulMethods;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

/**
 * 
 * @author Undral Erdenetsogt
 *
 */
public class ShapeSmoothingUtil {	

	private boolean onlyContours = false;;
	/**
	 * Fourier-Hintransformation, Filterung der Fourierdeskriptoren (FD) und Fourier-Rücktransformation
	 * für alle Blobs auf einem Bild (referenziert mittels imp). 
	 * 
	 * @param imp {@link ImagePlus} des Bildes auf dem nach Blobs gesucht wird
	 * @param newIp {@link ImageProcessor} des Bildes, worauf rücktransformierte Blobs gezeichnet werden
	 * @param thresholdValue Schwellenwert für die Filterung der FDs - es gibt an, wie viele FDs beibehalten werden (absolut oder prozentual)
	 * @param thresholdIsPercentual Gibt an, ob thresholdValue eine prozentuale Angabe ist, oder ob es bereits die Anzahl der zu behaltenden FDs enthält 
	 */
	public void fourierFilter(ImagePlus imp, ImageProcessor newIp, int thresholdValue, boolean thresholdIsPercentual) {
		// Konturpunkte erfassen	
		ManyBlobs allBlobs = new ManyBlobs(imp); // Extended ArrayList
		allBlobs.findConnectedComponents(); // Formen erkennen	
		
		for (Blob blob: allBlobs) {
			fourierEngine(blob.getOuterContour(), thresholdValue, thresholdIsPercentual, newIp);
		}
	}
	
	public void setDrawOnlyContours(boolean b){
		onlyContours = b;
	}
	
	/**
	 * Fourier-Hintransformation, Filterung der Fourierdeskriptoren (FD) und Fourier-Rücktransformation
	 * für einen Blob (referenziert mittels contourPolygon). 
	 * 
	 * @param contourPolygon Kontur eines Blobs
	 * @param thresholdValue Schwellenwert für die Filterung der FDs (prozentual oder absolut): die ersten thresholdValue/2 und die letzten thresholdValue/2 FDs werden
	 * beibehalten und die Restlichen auf 0 gesetzt
	 * @param thresholdIsPercentual Gibt an, ob thresholdValue eine prozentuale Angabe ist, oder ob es bereits die Anzahl der zu behaltenden FDs enthält 
	 * @param ip {@link ImageProcessor} des Bildes, worauf das rücktransformierte Blob gezeichnet wird
	 */
	private void fourierEngine(Polygon contourPolygon, int thresholdValue, boolean thresholdIsPercentual, ImageProcessor ip) {
		
		int numOfContourPoints = contourPolygon.npoints;
		
		if (thresholdIsPercentual) {
			thresholdValue = (thresholdValue * numOfContourPoints) / 100;
		} else {
			// thresholdValue darf nicht die Anzahl der Konturpunkte übersteigen!
			if (thresholdValue > numOfContourPoints) {
				thresholdValue = numOfContourPoints;
			}
		}
			
		// Konturpunkte in die "richtige" Datenstrukturübertragen

		/*
		 * a[2*k] = Re[k], 
		 * a[2*k+1] = Im[k], 0<=k<n
		 */
		double[] contourPoints = new double[2 * numOfContourPoints];
		
		int j = 0;
		for(int i = 0; i < numOfContourPoints; i++) {
			contourPoints[j] = contourPolygon.xpoints[i];
			contourPoints[j+1] = contourPolygon.ypoints[i];
			j=j+2;
		}
		DoubleFFT_1D ft = new DoubleFFT_1D(numOfContourPoints);
				
		// Fourier-Hintransformation
		ft.complexForward(contourPoints);		
		
		
		
		// Filterung	
		int loopFrom = thresholdValue;
		if (loopFrom % 2 != 0) {
			loopFrom = loopFrom + 1;
		}
		int loopUntil = contourPoints.length - thresholdValue;
		if (loopUntil % 2 != 0) {
			loopUntil = loopUntil + 1;
		}
		
		for (int i = loopFrom; i < loopUntil; i++) {
			contourPoints[i] = 0;
		}
		
			
		// Rücktransformation
		ft.complexInverse(contourPoints, true);
		int[] xpoints = new int[numOfContourPoints];
		int[] ypoints = new int[numOfContourPoints];
		
		j=0;
		for(int i = 0; i < contourPoints.length; i=i+2) {
			xpoints[j] = (int) Math.round(contourPoints[i]);
			ypoints[j] = (int) Math.round(contourPoints[i+1]);
			j++;
		}
		
		// Zeichnen
		if(onlyContours){
			ip.drawPolygon(new Polygon(xpoints, ypoints, j));
		}else{
			ip.fillPolygon(new Polygon(xpoints, ypoints, j));
		}
	}
	
	// Es ist nur für Zeitmesszwecke da	
	@SuppressWarnings("unused")
	@Deprecated
	private void fourierEngineSlow (Polygon contourPolygon, int thresholdValue, boolean thresholdIsPercentual, ImageProcessor ip) {
	
		if (thresholdIsPercentual) {
			thresholdValue = (thresholdValue * contourPolygon.npoints) / 100;
		}
		
		// Konturpunkte in die "richtige" Datenstruktur übertragen
		Vector<ComplexNumber> contourPoints = new Vector<ComplexNumber>();
		for (int i = 0; i < contourPolygon.npoints; i++) {
			contourPoints.add(new ComplexNumber(contourPolygon.xpoints[i], contourPolygon.ypoints[i]));
		}
		
		
		// Fourier-Hintransformation
		Vector<ComplexNumber> FDs = MyUsefulMethods.dft(contourPoints);		
		
		// Filterung		
		int loopFrom = thresholdValue/2;
		if (thresholdValue % 2 != 0) {
			loopFrom = loopFrom + 1; // bei ungeradem threshold aufrunden;
		}
		int loopUntil = FDs.size() - thresholdValue/2; // wird 'von allein' abgerundet
		ComplexNumber complexZero = new ComplexNumber(0, 0);
		for (int i = loopFrom; i < loopUntil ; i++ ) {
			FDs.setElementAt(complexZero, i);
		}
		
		
		// Rücktransformation
		Vector<ComplexNumber> newPoints = MyUsefulMethods.dftInverse(FDs);
	
		
		// Zeichnen
		for (ComplexNumber newPoint: newPoints) {
			ip.putPixel((int) Math.round(newPoint.getRe()), (int) Math.round(newPoint.getIm()), 0);
		}

	}
		
	
}
