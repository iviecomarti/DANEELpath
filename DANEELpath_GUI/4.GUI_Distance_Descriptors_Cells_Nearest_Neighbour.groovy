/*
 * DANEELpath project  is under   GNU GENERAL PUBLIC LICENSE, GPL-3.0 license
 * Please check https://github.com/iviecomarti/DANEELpath if you have any doubts about licensing
 *
 *
 * DANEELpath GUI to compute the Mean and Median Distance from one cell to its NN in one cell cluster  
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




import javafx.application.Platform
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.Tooltip
import javafx.scene.layout.GridPane
import javafx.stage.Stage
import qupath.lib.gui.QuPathGUI
import qupath.fx.dialogs.Dialogs
import qupath.lib.gui.tools.GuiTools
import qupath.fx.utils.GridPaneUtils
import qupath.lib.objects.PathObject
import qupath.lib.objects.PathObjectTools
import qupath.lib.objects.PathObjects
import qupath.lib.objects.classes.PathClass
import qupath.lib.roi.GeometryTools
import javafx.collections.FXCollections
import qupath.fx.utils.FXUtils
import qupath.lib.plugins.parameters.ParameterList;
import qupath.process.gui.commands.ml.ClassificationResolution;
import qupath.lib.images.ImageData;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Spinner;



Platform.runLater { buildStage().show()}

Stage buildStage() {
    def qupath = QuPathGUI.getInstance()
    def pane = new GridPane()
    
    row = 0
    
   nnTittle = new Label("Compute Cells Nearest Neighbours Features ")
   nnTittle.setStyle("-fx-font-weight: bold")
   pane.add(nnTittle, 0,row, 2, 1)
   
   
   def parentAnnotationCombo= new ComboBox<PathClass>(qupath.getAvailablePathClasses())
   parentAnnotationCombo.getSelectionModel().selectFirst()
   parentAnnotationCombo.setTooltip(new Tooltip("Set the parent to compute the N"))
   def labelParentSelection = new Label("Select the parent classification")
   
   row++
   pane.add(labelParentSelection,0,row,1,1)
   pane.add(parentAnnotationCombo,1,row,1,1)
     
   row=+3
   nnFeatureSelection = new Label("This script computes the Distance to Nearest Neighbours \nbetween the cells inside of a annotation. \n\nSelect the Summary Features for each annotation: ")
   pane.add(nnFeatureSelection, 0,row, 2, 1)
   
   
   row++
   def addMean= new CheckBox("Mean")
   addMean.setSelected(false)
   pane.add(addMean, 0,row, 1, 1)
   
   row++
   def addStd= new CheckBox("Std")
   addStd.setSelected(false)
   pane.add(addStd, 0,row, 1, 1)
   
   row++
   def addMedian= new CheckBox("Median")
   addMedian.setSelected(false)
   pane.add(addMedian, 0,row, 1, 1)
   
   row++
   def addMad= new CheckBox("MAD")
   addMad.setSelected(false)
   pane.add(addMad, 0,row, 1, 1)
   
   row++
   def addMin= new CheckBox("Min")
   addMin.setSelected(false)
   pane.add(addMin, 0,row, 1, 1)
   
   row++
   def addMax= new CheckBox("Max")
   addMax.setSelected(false)
   pane.add(addMax, 0,row, 1, 1)
   
    
    
    
    //Option to include the DTNN annotations
    
    //add the checkbox for add the Long Raidus Expansion to the GUI
   row+=2
   addLineAnnotationsTittle = new Label("OPTIONAL : Add The Lines of the Nearest Neighbours")
   addLineAnnotationsTittle.setUnderline(true)
   pane.add(addLineAnnotationsTittle, 0,row, 2, 1)
  
   row++
   def addLineAnnotations= new CheckBox("Add NN Lines As Detections")
   addLineAnnotations.setSelected(false)
   addLineAnnotations.setTooltip(new Tooltip("If checked, will create the NN annotation from each cell to its NN"))
    
   pane.add(addLineAnnotations, 0, row, 1, 1)
    
    
    //Now create the button to run 
   row+=3
   def runActionBtn = new Button("Run Cells Nearest Neighbour Features")
   pane.add(runActionBtn, 0, row, 2, 1)
   
   
   //ACTION OF THE BUTTON
   runActionBtn.setOnAction { e->
   
       
       runNNCellsInsideClusters(parentAnnotationCombo.getValue().toString(),addMean.isSelected(),addStd.isSelected(),addMedian.isSelected(), addMad.isSelected(),addMin.isSelected(),addMax.isSelected(),addLineAnnotations.isSelected()  )
       Dialogs.showInfoNotification("Cells Inside Annotation Nearest Neighbours Features", "Cells Inside Annotation Nearest Neighbours Features Finished!")
   }
   

   
    pane.setHgap(20)
    pane.setVgap(10)
    pane.setPadding(new Insets(10, 10, 10, 5))
    GridPaneUtils.setToExpandGridPaneWidth(parentAnnotationCombo,runActionBtn)

    def scene = new Scene(pane,370,475)
    def stage = new Stage()
    stage.setTitle("Features NNCells Inside Cell Clusters")
    stage.setScene(scene)
    stage.initOwner(qupath.getStage())
    return stage
}







/////////////////////////////
//////FUNCTIONS/////////////
///////////////////////////

//Main function: 

def runNNCellsInsideClusters(referenceAnnotation,putMean, putStd,putMedian,putMad,putMin,putMax, drawConnections) {
   
   clusters = getAnnotationObjects().findAll {
       
       it.getPathClass() != null && it.getPathClass()== getPathClass(referenceAnnotation)
       
   }
   
   
    if(clusters.size() == 0) {
        Dialogs.showErrorMessage("Error Parent Annotation","ERROR: There is not parent annotation classification with cells. \n Please create a reference Annotation\nor check its classification") 
        return 
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
        lineAnnotationNN = PathObjects.createDetectionObject(lineNNROI,getPathClass("NN_Line"))
        
        linesAnnotations.add(lineAnnotationNN)
        
        
    }
    
    addObjects(linesAnnotations)
   
   
}





