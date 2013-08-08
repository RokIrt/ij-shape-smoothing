package de.biomedical_imaging.ij.shapeSmoothingPlugin;

import java.awt.AWTEvent;
import java.awt.Scrollbar;
import java.awt.TextField;
import java.awt.Choice;

import ij.IJ;
import ij.ImagePlus;
import ij.blob.ManyBlobs;
import ij.blob.Blob;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.gui.ImageWindow;
import ij.gui.NewImage;
import ij.gui.YesNoCancelDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import de.biomedical_imaging.ij.shapeSmoothing.DialogCancelledException;
import de.biomedical_imaging.ij.shapeSmoothing.ShapeSmoothingUtil;

/**
 * Das ImageJ Plugin macht eine Konturglättung der Formen auf einem Bild. Das Plugin ist nach dem Pipeline-Prinzip entwickelt.
 * 
 * @author Undral Erdenetsogt
 *
 */
public class Shape_Smoothing implements PlugInFilter{
	private int width = 0;
	private int height = 0;
	private ShapeSmoothingUtil shapeSmoothingUtil;
	
	@Override
	public int setup(String arg, ImagePlus imp) {
		
		if (imp == null || imp.getType() != ImagePlus.GRAY8) {
			IJ.error("Only 8-Bit Grayscale Imags are supported");
			return DONE;
		}
		return DOES_8G;
	}
	
	@Override
	public void run(ImageProcessor ip) {		
		width = ip.getWidth();
		height = ip.getHeight();
		
		shapeSmoothingUtil = new ShapeSmoothingUtil();
		
		ImagePlus tempImp = ensureCorrectLUT(IJ.getImage());
		
		// ---  Konturglättung mittels FDs. ---
		try {
			doFourierFilter(tempImp);
		} catch (DialogCancelledException dce) {
			return;
		}
	}
	
	private ImagePlus ensureCorrectLUT(ImagePlus imp) {
		if(imp.isInvertedLut()) {
			ImagePlus newImp = duplicateWindow(imp.getProcessor(), imp.getTitle() + " mit einer 'normalen' LUT");
			ImageProcessor newIp = newImp.getProcessor();
			newIp.invertLut();
			newImp.show();
			
			YesNoCancelDialog dialog = new YesNoCancelDialog(ImageWindow.getFrames()[0], "Inverting LUT",
					"Die LUT des ursprünglichen Bildes ist invertiert. Da dieses Plugin nur mit einer 'normalen' LUT\n" +
					"ausgeführt werden kann, wurde die LUT (erneut) invertiert. Soll das Bild ebenfalls invertiert werden\n" +
					"(um das ursprüngliche Aussehen wiederherzustellen)?");
			if (dialog.yesPressed()) {
				newIp.invert();
				newImp.updateAndRepaintWindow();
			}
			return newImp;
		}
		return imp;
	}
	
	private ImageProcessor doFourierFilter(ImagePlus imp) throws DialogCancelledException {
		IJ.showStatus("Fourier Hin- & Rücktransformation");
		
		GenericDialog gd = new GenericDialog("Shape Smoothing");
		gd.setOKLabel("Run");
		gd.setCancelLabel("Cancel");
		
		
		
		ManyBlobs allBlobs = new ManyBlobs(imp);
		allBlobs.findConnectedComponents();
		
		int minNumOfFDs = Integer.MAX_VALUE;
		int tempMaxNumOfFDs = 0;
		
		for (Blob blob : allBlobs) {
			int numOfFDs = blob.getOuterContour().npoints;
			if (numOfFDs < minNumOfFDs) {
				minNumOfFDs = numOfFDs;
			}
			
			if (numOfFDs > tempMaxNumOfFDs) {
				tempMaxNumOfFDs = numOfFDs;
			}
		}
		
		final int maxNumOfFDs = tempMaxNumOfFDs;
		
		gd.addMessage("There are " + allBlobs.size() + " objects (or contours) with " + minNumOfFDs + " to " + maxNumOfFDs + " Fourier Descriptors (FDs).");		
		
		String[] items = {"Relative proportion of FDs","Absolute number of FDs"};
		gd.addChoice("Keep (for each Blob):", items, items[0]);
		gd.addSlider("Relative proportion FDs (%)", 0, 100, 0);
		
		gd.addSlider("Absolute number FDs", 0, maxNumOfFDs, 0);
		gd.addCheckbox("Draw only contours", false);
		Scrollbar absScroll = (Scrollbar) gd.getSliders().get(1);
		TextField absTextField = (TextField) gd.getNumericFields().get(1);
		absScroll.setEnabled(false);
		absTextField.setEnabled(false);
		gd.addDialogListener(new DialogListener() {
			int percent = 0;
			int absolute = 0;
			@Override
			public boolean dialogItemChanged(GenericDialog geDi, AWTEvent event) {
				double actPercentValue = geDi.getNextNumber();	
				double actAbsoluteValue = geDi.getNextNumber();
				
				Choice modusChoice = (Choice) geDi.getChoices().get(0);
				Scrollbar absScroll = (Scrollbar) geDi.getSliders().get(1);
				TextField absTextField = (TextField) geDi.getNumericFields().get(1);
				Scrollbar relScroll = (Scrollbar) geDi.getSliders().get(0);
				TextField relTextField = (TextField) geDi.getNumericFields().get(0);

				boolean relSelected = (modusChoice.getSelectedItem()==modusChoice.getItem(0));
				absScroll.setEnabled(!relSelected);
				absTextField.setEnabled(!relSelected);
				relScroll.setEnabled(relSelected);
				relTextField.setEnabled(relSelected);
				

			
				//(Choice)(geDi.getChoices().get(0))
				// Validierung
				if (Double.isNaN(actPercentValue) || actPercentValue < 0) {
					IJ.showMessage("'Relativ proportion have to be > 0!");
					return false;
				}
				
				if (Double.isNaN(actAbsoluteValue) || actAbsoluteValue < 0) {
					IJ.showMessage("'Absolute number have to be > 0!");
					return false;
				}
				
				// Kopplung der beiden Slider
				TextField percentTextField = (TextField) geDi.getNumericFields().get(0);			
				Scrollbar percentSlider = (Scrollbar) geDi.getSliders().get(0);
				
				TextField absoluteTextField = (TextField) geDi.getNumericFields().get(1);				
				Scrollbar absoluteSlider = (Scrollbar) geDi.getSliders().get(1);

				if (actPercentValue != percent) {
					percent = (int) actPercentValue;
					absolute = (maxNumOfFDs * percent) / 100;
					absoluteSlider.setValue(absolute);
					absoluteTextField.setText(Integer.toString(absolute));
				}
				else if (actAbsoluteValue != absolute) {
					absolute = (int) actAbsoluteValue;
					percent = (100 * absolute) / maxNumOfFDs;
					percentSlider.setValue(percent);
					percentTextField.setText(Integer.toString(percent));
				}
				return true;
			}
		});

		gd.showDialog();
		if (gd.wasCanceled()) {
			throw new DialogCancelledException();
		}
		
		int thresholdValuePercentual = (int) gd.getNextNumber();
		int thresholdValueAbsolute = (int) gd.getNextNumber();
		Choice modusChoice = (Choice) gd.getChoices().get(0);
		boolean doAbsoluteThreshold = (modusChoice.getSelectedItem()==items[1]);
		boolean drawOnlyContours = gd.getNextBoolean();
		shapeSmoothingUtil.setDrawOnlyContours(drawOnlyContours);
		// Neues Bild ausgeben		
		ImagePlus newImp = NewImage.createByteImage("Smoothed Objects", width, height, 1, 0);
		ImageProcessor newIp = newImp.getProcessor();
		
		if (doAbsoluteThreshold) {
			shapeSmoothingUtil.fourierFilter(imp, newIp, thresholdValueAbsolute, false);
		} else {
			shapeSmoothingUtil.fourierFilter(imp, newIp, thresholdValuePercentual, true);
		}
		
		
		
		newImp.show();
		
		return newIp;
	}
	
	/**
	 * Dupliziert ein Bild (die Kopie erscheint in einem neuen Fenster)
	 * 
	 * @param sourceIp {@link ImageProcessor} des zu duplizierenden Bildes
	 * @param newTitle Überschrift für das neue Bildfenster
	 * @return {@link ImagePlus} des Duplikats
	 */
	public ImagePlus duplicateWindow (ImageProcessor sourceIp, String newTitle) {
		ImagePlus newImp = NewImage.createByteImage(newTitle, sourceIp.getWidth(), sourceIp.getHeight(), 1, NewImage.FILL_BLACK);
		newImp.setProcessor(sourceIp.duplicate());		
		return newImp;
	}
}