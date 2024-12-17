/*
 * DANEELpath project  is under   GNU GENERAL PUBLIC LICENSE, GPL-3.0 license
 * Please check https://github.com/iviecomarti/DANEELpath if you have any doubts about licensing
 *
 *Script to compute different metrics from one cell centroid to its Nearest Neigbhor cell inside the cluster
 * 
 *
 * You need to provide a parent classification. The distances will be computed in all the objects with that classification
 *
 * IMPORTANT: If the cell cluster has just one cell inside, the measurement 0 will be added to the list of measurements to be consistent when exporting the data
 * 
 * 
 * With option: "drawNNConnections": If "true" the Connections between the cells will be added as Line annotations classified as "NN_Line"
 * 
 * @author: Isaac Vieco-Martí
 */




//TO COMPLETE
def parentObjectClassification = "cluster" //set the classification of the reference annotation
def putMean=  true
def putStd= true
def putMedian= true
def putMad= true
def putMin= true
def putMax= true


def drawConnections = false //true or false
 







////////////////////////////////////////////
////////DO NOT TOUCH FROM HERE//////////////
////////////////////////////////////////////



runNNCellsInsideClusters(parentObjectClassification,putMean, putStd,putMedian,putMad,putMin,putMax, drawConnections)






//Main function: 

def runNNCellsInsideClusters(referenceAnnotation,putMean, putStd,putMedian,putMad,putMin,putMax, drawConnections) {
   
   clusters = getAnnotationObjects().findAll {
       
       it.getPathClass() != null && it.getPathClass()== getPathClass(referenceAnnotation)
       
   }
   clusters.forEach {
   
       childs = it.getChildObjects()
       NNFeatures = computeMeanMedianNearestNeighborMeanDistance(childs)
       
       if(putMean == true) {
           it.getMeasurementList().put("Mean_Distance_Cells_NN", NNFeatures.mean)    
       }
       
       if(putStd== true) {
           it.getMeasurementList().put("STD_Distance_Cells_NN", NNFeatures.std)    
       }
       
       if(putMedian == true) {
           it.getMeasurementList().put("Median_Distance_Cells_NN", NNFeatures.median)    
       }
       
       if(putMad == true) {
           it.getMeasurementList().put("MAD_Distance_Cells_NN", NNFeatures.mad)    
       }
       

       if(putMin == true) {
           it.getMeasurementList().put("Min_Distance_Cells_NN", NNFeatures.min)    
       }
       if(putMax == true) {
           it.getMeasurementList().put("Max_Distance_Cells_NN", NNFeatures.max)    
       }
       
   
      if(drawConnections == true) {
          drawNNConnections(childs)
      }
      
      
    }
   
}




import qupath.lib.objects.PathObject
import qupath.lib.analysis.DelaunayTools
import qupath.lib.analysis.DistanceTools

// Get the current image data
def imageData = getCurrentImageData()


/////////////////////////////
///////FUNCTIONS/////////////
/////////////////////////////
def computeMeanMedianNearestNeighborMeanDistance(childsCluster) {
    
    def imageData = QP.getCurrentImageData()
    def server = imageData.getServer()
    def cal = server.getPixelCalibration()

    // Initialize the result map
    def resultMap = [
        mean: 0,
        median: 0,
        min: 0,
        max: 0,
        std: 0,
        mad: 0
    ]
    
    // If the cluster has only one or fewer children, return the resultMap with zeros
    if (childsCluster.size() <= 1) {
        return resultMap  // Return the map with zeros
    } else {
        subdivision = DelaunayTools.createFromCentroids(childsCluster, true)

        // Initialize the list to store the distances
        def listDistances = []

        // Iterate through each child in the cluster
        childsCluster.forEach {
            def NN = subdivision.getNearestNeighbor(it)
            def Distance = RoiTools.getCentroidDistance(it.getROI(), NN.getROI())
            def DistanceUm = Distance * cal.getAveragedPixelSizeMicrons()
            listDistances.add(DistanceUm)
        }

        // Calculate the mean of the list of distances
        def meanDistance = 0
        if (listDistances.size() > 0) {
            def totalDistance = listDistances.sum()
            meanDistance = totalDistance / listDistances.size()
        }

        // Sort the distances to calculate the median
        def sortedDistances = listDistances.sort()

        // Calculate the median of the sorted list
        def medianDistance = 0
        if (sortedDistances.size() % 2 == 1) {
            // If odd, the median is the middle element
            medianDistance = sortedDistances[sortedDistances.size() / 2]
        } else {
            // If even, the median is the average of the two middle elements
            def middle1 = sortedDistances[(sortedDistances.size() / 2) - 1]
            def middle2 = sortedDistances[sortedDistances.size() / 2]
            medianDistance = (middle1 + middle2) / 2
        }

        // Calculate the minimum and maximum
        def minDistance = listDistances.min()
        def maxDistance = listDistances.max()

        // Calculate the standard deviation (STD)
        def variance = listDistances.collect { (it - meanDistance) ** 2 }.sum() / listDistances.size()
        def stdDistance = Math.sqrt(variance)

        // Calculate the Median Absolute Deviation (MAD)
        def absDeviations = listDistances.collect { Math.abs(it - medianDistance) }
        def mad = 0
        if (absDeviations.size() > 0) {
            absDeviations.sort()
            if (absDeviations.size() % 2 == 1) {
                mad = absDeviations[absDeviations.size() / 2]
            } else {
                def middle1 = absDeviations[(absDeviations.size() / 2) - 1]
                def middle2 = absDeviations[absDeviations.size() / 2]
                mad = (middle1 + middle2) / 2
            }
        }

        // Update the resultMap with the computed values
        resultMap.mean = meanDistance
        resultMap.median = medianDistance
        resultMap.min = minDistance
        resultMap.max = maxDistance
        resultMap.std = stdDistance
        resultMap.mad = mad

        // Return the resultMap with the computed values
        return resultMap
    }
}




//function to draw the connections
def drawNNConnections(childsCluster) {
   
   
    def imageData = QP.getCurrentImageData()
    def server = imageData.getServer()
    def cal = server.getPixelCalibration()
    def plane = ImagePlane.getPlane(0, 0)
    // Return 0 if there is only one child or none
    if (childsCluster.size() <= 1) {
        return 
    }
    
    subdivision = DelaunayTools.createFromCentroids(childsCluster,true)
    // Initialize the list to store the distances
    def linesAnnotations = []

    // Iterate through each child in the cluster
    childsCluster.forEach { 
        
        def NN = subdivision.getNearestNeighbor(it)
        
        centroidXOriginal = it.getROI().getCentroidX()
        centroidYOriginal = it.getROI().getCentroidY()
        
        NNcentroidX = NN.getROI().getCentroidX()
        NNcentroidY = NN.getROI().getCentroidY()
        
        lineNNROI = ROIs.createLineROI(centroidXOriginal,centroidYOriginal,NNcentroidX,NNcentroidY, plane)
        lineAnnotationNN = PathObjects.createAnnotationObject(lineNNROI,getPathClass("NN_Line"))
        
        linesAnnotations.add(lineAnnotationNN)
        
        
    }
    
    addObjects(linesAnnotations)
   
   
}
