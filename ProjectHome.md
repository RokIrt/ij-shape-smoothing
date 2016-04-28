## Description ##
IJShapeSmoothing is an ImageJ Plugin for smoothing objects in binary scenes. For smoothing, each object contour is reconstruced by its first x fourier descriptors. x can be an relative porportion in percent or an absolute value

![http://www.biomedical-imaging.de/upload/ImageJPlugins/shapesmooth_gui.png](http://www.biomedical-imaging.de/upload/ImageJPlugins/shapesmooth_gui.png)

If "output descriptor" is selected, all fourier descriptors up to the selected threshold are reported. All fourier descriptors greater than 1 are scale-,rotation- and translation-invariant.

## Download binary ##
http://figshare.com/articles/IJShapeSmoothing_An_ImageJ_Plugin_for_smoothing_objects_in_binary_scenes/878035

## Installation manually ##
1. Download the latest[ijblob](https://code.google.com/p/ijblob/) and [jtransforms 2.4](http://sourceforge.net/projects/jtransforms/files/jtransforms/2.4/jtransforms-2.4.jar/download) and move it to the imagej plugins/jars directory.

2. Copy the `Shape_Smoothing_.jar` in your ImageJ Plugin directory

## Installation via Fiji update site ##
If you use fiji, you can use our update site: http://sites.imagej.net/Biomedgroup/

## Example ##
An binary image of a cell:

![http://www.biomedical-imaging.de/upload/ImageJPlugins/cell2.png](http://www.biomedical-imaging.de/upload/ImageJPlugins/cell2.png)

Smoothed by the first 5% of the fourier descriptors:

![http://www.biomedical-imaging.de/upload/ImageJPlugins/cell2_5percent.png](http://www.biomedical-imaging.de/upload/ImageJPlugins/cell2_5percent.png)

## Details ##
For connected component labeling the [IJBlob](https://code.google.com/p/ijblob/) library and for the fourier transform the [JTransform](https://sites.google.com/site/piotrwendykier/software/jtransforms) library is used.

This plugin was developed within the scope of a study work of Undral Erdenetsogt (Member of the Biomedical Imaging Group) and is maintained by Thorsten Wagner (Member of the Biomedical Imaging Group).