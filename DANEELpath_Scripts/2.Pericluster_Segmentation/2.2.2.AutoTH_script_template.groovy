/*
 * DANEELpath project  is under   GNU GENERAL PUBLIC LICENSE, GPL-3.0 license
 * Please check https://github.com/iviecomarti/DANEELpath if you have any doubts about licensing
 *
* Script to create and run auto-Threshold methods in the expansions.
*
* Based on the script from @yau-lim (2024):https://github.com/yau-lim/QuPath-Auto-Threshold
* I added more preprocessing methods to be able to replicate the classifiers created with the GUI: https://github.com/iviecomarti/GUI_AutoTH_QuPath
* I also simplified the methods for the specific use of this piepline. So this script just creates the classifiers and applies them. 
*
*
* @author Isaac Vieco-Martí
*
*/



//OBJECTS PARAMETERS

def parentClassification = "Expansion" //set the classification of the expansion

//Set if you are interested in the values above/below the autoTH. For example, if you want to segment the tVN_Corona with DAB and Triangle Method
//if you set above, all the values above the Auto-TH will be classified and the values below will be ignored.
//You can check if the classification is above or below by using the Auto-TH GUI: https://github.com/iviecomarti/GUI_AutoTH_QuPath

def outputClassAbove = "tVN_Corona" 
def outputClassBelow = null


//AUTO-TH PARAMETERS:
def thresholdMethod = "Triangle" // Options: "Default", "Huang", "Intermodes", "IsoData", "IJ_IsoData", "Li", "MaxEntropy", "Mean", "MinError", "Minimum", "Moments", "Otsu", "Percentile", "RenyiEntropy", "Shanbhag", "Triangle", "Yen"
def channel = "Average" // Channel Options: 1)Brightfield_HE_DAB: Red,Green,Blue, Average, Hematoxylin, Eosin, DAB, Residual.  2)Fluorescence: use channel name (DAPI for Example), Average
def prefilterType = "Gaussian" // Prefilter Options: Gaussian, Laplacian, Erosion, Dilation,Opening,Closing,Gradient_Magnitude,Weighted_STD
def prefilterSigma = 0
def thresholdDownsample = 1 //Resolution options to compute Auto-TH: 1:Full, 2:Very high, 4:High, 8:Moderate, 16:Low, 32:Very low, 64:Extremely low



//CLASSIFIER OUTPUT OPTIONS:
def classifierDownsample = 1 //Pixel Classifier Resolution: 1:Full, 2:Very high, 4:High, 8:Moderate, 16:Low, 32:Very low, 64:Extremely low
def outputObjectType = "Annotation" //Annotation or Detection
def minArea = 0    //Min area to create the object
def minHoleArea = 0 //Min hole area to create the object










/////////////////////////////////
/////DO NOT TOUCH FROM HERE/////
////////////////////////////////


runAutoThresholdClassifierFunction(parentClassification, outputClassAbove,outputClassBelow,outputObjectType,thresholdMethod, channel,prefilterType, prefilterSigma,thresholdDownsample,classifierDownsample, minArea, minHoleArea)




// Main Function 

def runAutoThresholdClassifierFunction(parentClassification, outputClassAbove,outputClassBelow,outputObjectType,thresholdMethod, channel,prefilterType, prefilterSigma,thresholdDownsample,classifierDownsample, minArea, minHoleArea){
     
     objectsOfInterest = getAllObjects().findAll {
         it.getPathClass() == getPathClass(parentClassification) 
      }

      if(objectsOfInterest  == null){
        println "ERROR: No parent classifications selected, please check it"
      }

     for(object in objectsOfInterest) {
          
                    
          classifierPX= classifierCreation(object,  channel, thresholdDownsample, thresholdMethod, classifierDownsample,prefilterType,prefilterSigma, outputClassAbove, outputClassBelow)
          
          actionRunClassifier(object,classifierPX,outputObjectType,minArea, minHoleArea)
        } 

        print("Done!")



}



//////////////////////////
//AUTO TH FUNCTIONS///////
//////////////////////////


import qupath.lib.images.servers.TransformedServerBuilder
import qupath.lib.roi.interfaces.ROI
import qupath.imagej.tools.IJTools
import qupath.lib.images.PathImage
import qupath.lib.regions.RegionRequest
import ij.ImagePlus
import ij.process.ImageProcessor
import qupath.opencv.ml.pixel.PixelClassifiers
import qupath.lib.images.servers.ColorTransforms.ColorTransform
import qupath.opencv.ops.ImageOp
import qupath.opencv.ops.ImageOps

//This is the original funtion:  @yau-lim (2024): https://github.com/yau-lim/QuPath-Auto-Threshold
//the modifications are marked with @I Vieco-Martí:
/* FUNCTIONS */

 
def classifierCreation(annotation, channel, thresholdDownsample, thresholdMethod, classifierDownsample,prefilterType,prefilterSigma, classAbove,classBelow) {
    def imageData = getCurrentImageData()
    def imageType = imageData.getImageType()
    def server = imageData.getServer()
    def cal = server.getPixelCalibration()
    def classifierChannel

    if (imageType.toString().contains("Brightfield")) {
        def stains = imageData.getColorDeconvolutionStains()
        def colors =getCurrentServer().getMetadata().getChannels()

        if (channel == "Hematoxylin") {
            server = new TransformedServerBuilder(server).deconvolveStains(stains, 1).build()
            classifierChannel = ColorTransforms.createColorDeconvolvedChannel(stains, 1)
        } else if (channel == "Eosin" | channel == "DAB") {
            //@I Vieco-Martí: Simple modification: for H&E the second is Eosin for H-DAB the second is DAB
            server = new TransformedServerBuilder(server).deconvolveStains(stains, 2).build()
            classifierChannel = ColorTransforms.createColorDeconvolvedChannel(stains, 2)
        } else if (channel == "Residual") {
            server = new TransformedServerBuilder(server).deconvolveStains(stains, 3).build()
            classifierChannel = ColorTransforms.createColorDeconvolvedChannel(stains, 3)
        } else if (channel == "Average") {
            server = new TransformedServerBuilder(server).averageChannelProject().build()
            classifierChannel = ColorTransforms.createMeanChannelTransform()
        }else {
            //@I Vieco-Martí: This is for all the other channels
            server = new TransformedServerBuilder(server).extractChannels(channel).build()
            classifierChannel = ColorTransforms.createChannelExtractor(channel)
        }
    } else if (imageType.toString() == "Fluorescence") {
        if (channel == "Average") {
            server = new TransformedServerBuilder(server).averageChannelProject().build()
            classifierChannel = ColorTransforms.createMeanChannelTransform()
        } else {
            server = new TransformedServerBuilder(server).extractChannels(channel).build()
            classifierChannel = ColorTransforms.createChannelExtractor(channel)
        }
    } else {
        logger.error("Current image type not compatible with auto threshold.")
        return
    }


    // Determine threshold value
    logger.info("Calculating threshold value using ${thresholdMethod} method on ${annotation}")
    ROI pathROI = annotation.getROI() // Get QuPath ROI
    PathImage pathImage = IJTools.convertToImagePlus(server, RegionRequest.createInstance(server.getPath(), thresholdDownsample, pathROI)) // Get PathImage within bounding box of annotation
    def ijRoi = IJTools.convertToIJRoi(pathROI, pathImage) // Convert QuPath ROI into ImageJ ROI
    ImagePlus imagePlus = pathImage.getImage() // Convert PathImage into ImagePlus
    
    ImageProcessor ip = imagePlus.getProcessor() // Get ImageProcessor from ImagePlus
    ip.setRoi(ijRoi) // Add ImageJ ROI to the ImageProcessor to limit the histogram to within the ROI only

    // Apply the selected algorithm
    def validThresholds = ["Default", "Huang", "Intermodes", "IsoData", "IJ_IsoData", "Li", "MaxEntropy", "Mean", "MinError", "Minimum", "Moments", "Otsu", "Percentile", "RenyiEntropy", "Shanbhag", "Triangle", "Yen"]

    if (thresholdMethod in validThresholds){
        ip.setAutoThreshold(thresholdMethod)
    } else {
        logger.error("Invalid auto-threshold method")
        return
    }

    double thresholdValue = ip.getMaxThreshold()
    


    // Define parameters for pixel classifier
    def resolution = cal.createScaledInstance(classifierDownsample, classifierDownsample)
    
    
   //compute radius for some filters and select the filter type
   
   //@I Vieco-Martí: I added all the filters. the structure is the same as here: 
   //https://github.com/qupath/qupath/blob/main/qupath-extension-processing/src/main/java/qupath/process/gui/commands/SimpleThresholdCommand.java
   
   int radius = (int)Math.round(prefilterSigma * 2)
   
   
   if(prefilterType == "Gaussian") {
       prefilter = ImageOps.Filters.gaussianBlur(prefilterSigma)
       
   }else if(prefilterType == "Laplacian") {
       prefilter = ImageOps.Filters.features(Collections.singletonList(MultiscaleFeature.LAPLACIAN), prefilterSigma, prefilterSigma)
       
   }else if(prefilterType == "Erosion") {
       prefilter = ImageOps.Filters.minimum(radius)
       
   }else if(prefilterType == "Dilation") {
        prefilter = ImageOps.Filters.maximum(radius)
       
   }else if(prefilterType == "Opening") {
       prefilter =ImageOps.Filters.opening(radius)
   }else if(prefilterType == "Closing") {
       prefilter =ImageOps.Filters.closing(radius) 
   } else if(prefilterType == "Gradient_Magnitude") {
        prefilter =ImageOps.Filters.features(Collections.singletonList(MultiscaleFeature.GRADIENT_MAGNITUDE), prefilterSigma, prefilterSigma)
   }else if(prefilterType == "Weighted_STD") {
       prefilter = ImageOps.Filters.features(Collections.singletonList(MultiscaleFeature.WEIGHTED_STD_DEV), prefilterSigma, prefilterSigma)
       
   }

    List<ImageOp> ops = new ArrayList<>()
    ops.add(prefilter)
    ops.add(ImageOps.Threshold.threshold(thresholdValue))

    // Assign classification
    def classificationBelow
    if (classBelow instanceof PathClass) {
        classificationBelow = classBelow
    } else if (classBelow instanceof String) {
        classificationBelow = getPathClass(classBelow)
    } else if (classBelow == null) {
        classificationBelow = classBelow
    }
   
    def classificationAbove
    if (classAbove instanceof PathClass) {
        classificationAbove = classAbove
    } else if (classAbove instanceof String) {
        classificationAbove = getPathClass(classAbove)
    } else if (classAbove == null) {
        classificationAbove = classAbove
    }

    Map<Integer, PathClass> classifications = new LinkedHashMap<>()
    classifications.put(0, classificationBelow)
    classifications.put(1, classificationAbove)

    // Create pixel classifier
    def op = ImageOps.Core.sequential(ops)
    def transformer = ImageOps.buildImageDataOp(classifierChannel).appendOps(op)
    def classifier = PixelClassifiers.createClassifier(
        transformer,
        resolution,
        classifications
    )
    
    return classifier 
    
 }
 
 
 //This Runs the autoTH classifier
 // NOTE: Here i remove the post-processing options of "SPLIT", "DELETE_EXISTING", "INCLUDE_IGNORED", "SELECT_NEW"
 //the main reason is because for this pipeline they are not neede. Is assumed that the objects inside the expansions must be one object.
 //if i let the option of SPLIT, someone could try it and then the next script will not work. So I prefer to remove them just in case.
 def actionRunClassifier(object,classifier,output,minArea, minHoleArea ) {
     
     // Apply classifier
    selectObjects(object)
    if (output == "Annotation") {
       
        createAnnotationsFromPixelClassifier(classifier, minArea, minHoleArea)
    }
    if (output == "Detection") {

            createDetectionsFromPixelClassifier(classifier, minArea, minHoleArea)
    }
  
   
 }
 

