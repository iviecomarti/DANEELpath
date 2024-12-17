/*
 * DANEELpath project  is under   GNU GENERAL PUBLIC LICENSE, GPL-3.0 license
 * Please check https://github.com/iviecomarti/DANEELpath if you have any doubts about licensing
 *
 * Script with a GUI to create Expansions of equal area
 * 
 * This script takes a Annotation and creates n ( n must be an integer) regions of the same area, in both directions, inside/outside of the original
 * 
 * If we set 2 Expansions  inside, will divide the origianl area in a center and a periphery, both having approximatelly the same area.
 * 
 * If the direction is outside, will create a region equal to the original, and a expansion to the outside with approximatelly the same area 
 * 
 * With the parameter "computePercenDifferenceBetweenRegions", the Absolute Percent difference between the regions 
 * will appear in the parent annotation measurements.
 *
 * This enables to divide a 2D Annotation in n regions of the same area
 *
 * @author Isaac Vieco-Martí
 * 
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
import ij.IJ;
import qupath.opencv.tools.MultiscaleFeatures.MultiscaleFeature;

//GUI//

Platform.runLater { buildStage().show()}

Stage buildStage() {
    def qupath = QuPathGUI.getInstance()
    def pane = new GridPane()
    
    row = 0
    
   dilationsTittle = new Label("Create Expansions of equal area")
   dilationsTittle.setStyle("-fx-font-weight: bold")
   pane.add(dilationsTittle, 0,row, 2, 1)
   
   
   def parentAnnotationCombo= new ComboBox<PathClass>(qupath.getAvailablePathClasses())
    parentAnnotationCombo.getSelectionModel().selectFirst()
    parentAnnotationCombo.setTooltip(new Tooltip("Set the parent classification"))
    def labelParentSelection = new Label("Select the parent classification")
   
    row++
    pane.add(labelParentSelection,0,row,1,1)
    pane.add(parentAnnotationCombo,1,row,1,1)
    
    
    //create a list with the options of expansions. We will put 10 as limit
    
    numberOfDilations = [2,3,4,5,6,7,8,9,10]
    def comboNumberExpansions = new ComboBox<>(FXCollections.observableArrayList(numberOfDilations))
    comboNumberExpansions.getSelectionModel().selectFirst()
    comboNumberExpansions.setTooltip(new Tooltip("Select the number of expansions"))
    def labelNuumberExpansions = new Label("Select Number of Expansions")
    
    row++
    pane.add(labelNuumberExpansions,0,row,1,1)
    pane.add(comboNumberExpansions,1,row,1,1)
   
    
    //create a list with the options of expansions. We will put 10 as limit
    
    directionDilations = ["Inside", "Outside"]
    
    def comboDirectionDilations = new ComboBox<>(FXCollections.observableArrayList(directionDilations))
    comboDirectionDilations.getSelectionModel().selectFirst()
    comboDirectionDilations.setTooltip(new Tooltip("Select the direction of the dilations"))
    def labelDirectionDilations = new Label("Select Expansion Direction")
    
    row++
    pane.add(labelDirectionDilations,0,row,1,1)
    pane.add(comboDirectionDilations,1,row,1,1)
    
    
    
    
    // Optional compute Absolute Percent difference Between regions
    
    //add the checkbox for add the Long Raidus Expansion to the GUI
    row+=3
    addDifferenceRegionsTittle= new Label("OPTIONAL : Compute Absolute Percent Difference\n Between Expansions")
    addDifferenceRegionsTittle.setUnderline(true)
    pane.add(addDifferenceRegionsTittle, 0,row, 3, 1)
    
    row++
    def addDiiferenceBetweenRegions= new CheckBox("Add to Parent Difference Between Regions Measurement")
    addDiiferenceBetweenRegions.setSelected(false)
    addDiiferenceBetweenRegions.setTooltip(new Tooltip("If checked, will compute the Difference Between Regions and put it in the parent Annotation"))
    
     pane.add(addDiiferenceBetweenRegions, 0, row, 2, 1)
    
    
    //cretate the button to run the action
    row+=3
    def btnExpansions = new Button("Run Expansions")
    btnExpansions.setTooltip(new Tooltip("Run the function"))
    
    pane.add(btnExpansions, 0, row, 2, 1)
    
    
    
    ///SET THE ACTION OF THE BUTTON
    btnExpansions.setOnAction{ e->
        
        runDilationsSameAreaParentsClassified(parentAnnotationCombo.getValue().toString(),comboNumberExpansions.getValue(),comboDirectionDilations.getValue(),addDiiferenceBetweenRegions.isSelected() )
        Dialogs.showInfoNotification("Expansions Same area", "Expansions finished!")
    }    
    

   
    pane.setHgap(20)
    pane.setVgap(10)
    pane.setPadding(new Insets(10, 10, 10, 5))
    GridPaneUtils.setToExpandGridPaneWidth(parentAnnotationCombo,comboNumberExpansions,comboDirectionDilations,btnExpansions)

    def scene = new Scene(pane,370,300)
    def stage = new Stage()
    stage.setTitle("Expansions of equal area")
    stage.setScene(scene)
    stage.initOwner(qupath.getStage())
    return stage
}














//////////////////////////////////////////
////FUNCTIONS TO CREATE THE EXPANSIONS///
////////////////////////////////////////




//Main Function

def runDilationsSameAreaParentsClassified(parentObjectsClassification, numberOfDilations, directionOfDilation,computePercenDifferenceBetweenRegions ){

    // Check if numberOfDilations is not an integer or is 1
    if (!(numberOfDilations instanceof Integer) || numberOfDilations == 1) {
        println "ERROR: The numberOfDilations must be an integer greater than 1 (e.g., 2, 3, 4, ...)."
        return // Exit the function
    }

    parentObjects = getAllObjects().findAll{
        it.getPathClass()!= null && it.getPathClass() == getPathClass(parentObjectsClassification)
    }

    if(parentObjects.size() == 0){
        Dialogs.showErrorMessage("Error Parent","ERROR: we do not have any classified parents.\n Please check parents classification") 
        return
    }

    if(directionOfDilation == "Inside"){

        parentObjects.each{
            dilationsInsideParent(it,numberOfDilations )
        }


    } else if(directionOfDilation == "Outside"){

        parentObjects.each{
            dilationsOutsideParent(it,numberOfDilations )
        }
    }else{
        println "ERROR: the direction is not set propperly."
        return
    }
    
    
    if(computePercenDifferenceBetweenRegions == true) {
        runComputeDifferenceBetweenRegions()
    }
    
    println "Done!"

}



import qupath.lib.scripting.QP
import org.locationtech.jts.geom.Geometry
import qupath.lib.common.GeneralTools
import qupath.lib.objects.PathObject
import qupath.lib.objects.PathObjects
import qupath.lib.roi.GeometryTools
import qupath.lib.roi.ROIs



/////////////////////////////////
///////DILATIONS INSIDE PARENT///
////////////////////////////////

def dilationsInsideParent(parentObject,numberOfDilations){


    def subdivisions = [:]
    //we will set the parent ID to the name of the subdivisions
    parentID = parentObject.getID().toString()
    
    print("Processing Object with ID: " + parentID)

    for (int i = 1; i < numberOfDilations; i++){

        dilationFactor = i / numberOfDilations
        firstDilationFeatures = firstDilation(parentObject,dilationFactor)
        finalGeomSubdivision = adjustDilationTargetAreaLoop(firstDilationFeatures)

        classificationSubzone = "Expansion_Inside_" + i
        
        print("AbsolutePercentDifference of " + classificationSubzone+ " and TargetArea is "+ finalGeomSubdivision[1])

        subdivisions[classificationSubzone] = finalGeomSubdivision[0]


    }

    geomParent = parentObject.getROI().getGeometry()
    classificationSubzoneParent = "Expansion_Inside_" + numberOfDilations
    subdivisions[classificationSubzoneParent] = geomParent

    

    //Get the parent plane. This will help to add the annotations to the propper plane. Shold work for stacks
    parentPlane = parentObject.getROI().getImagePlane()
    
    //with this function we will obtain the annotations
    annotationsToAdd = createSubtractedAnnotationsDilationsInsideAndOutside(subdivisions,parentID,parentPlane)
    
    //finally we add the annotations
    addObjects(annotationsToAdd)
    

}





/////////////////////////////////
///////DILATIONS OUTSIDE PARENT/
////////////////////////////////


//This is basically the same function as above, but I am ordering the dilations, so the smallest is the first element and the biggest is the last one.


def dilationsOutsideParent(parentObject, numberOfDilations){

    def subdivisions = [:]
    
    // Get the parent ID
    parentID = parentObject.getID().toString()
    
    print("Processing Object with ID: " + parentID)

    // Get the geometry of the parent object first and add it to the map
    geomParent = parentObject.getROI().getGeometry()
    classificationSubzoneParent = "Expansion_Outside_1"  // The parent is the first element
    subdivisions[classificationSubzoneParent] = geomParent

    // Loop for the dilations starting from 2, since 1 is the parent element
    for (int i = 2; i <= numberOfDilations; i++) {

        dilationFactor = i // Since you want dilation 2 at i=2, dilationFactor starts at 1
        firstDilationFeatures = firstDilation(parentObject, dilationFactor)
        finalGeomSubdivision = adjustDilationTargetAreaLoop(firstDilationFeatures)

        classificationSubzone = "Expansion_Outside_" + i
        print("AbsolutePercentDifference of " + classificationSubzone+ " and TargetArea is "+ finalGeomSubdivision[1])
        subdivisions[classificationSubzone] = finalGeomSubdivision[0]
    }

    

    //Get the parent plane. This will help to add the annotations to the propper plane. Shold work for stacks
    parentPlane = parentObject.getROI().getImagePlane()
    
    // Create annotations based on subdivisions
    annotationsToAdd = createSubtractedAnnotationsDilationsInsideAndOutside(subdivisions, parentID,parentPlane)
    
    // Add annotations to the parent object
    addObjects(annotationsToAdd)
}




//////////////////////////////////////////////////////////////////////////
///////FUNCTION TO CREATE THE INSTERSECTIONS BETWEEN THE ANNOTAITONS/////
/////////////////////////////////////////////////////////////////////////

def createSubtractedAnnotationsDilationsInsideAndOutside(geometryMapInsideDirection, parentID,parentPlane) {
    def annotationsList = []
    def plane = parentPlane
    
    // Convert the map entries to a list.
    //In this case The map si sorted, so the first one is the smallest and the last one is the largest. 
    def entries = geometryMapInsideDirection.entrySet().toList()
    
    
    // Iterate over the sorted geometries in reverse (largest to smallest)
    for (int i = entries.size() - 1; i > 0; i--) {
        def currentEntry = entries[i]
        def nextEntry = entries[i - 1]
        
        def currentGeom = currentEntry.value  // Larger geometry
        def nextGeom = nextEntry.value        // Smaller geometry
        
        // Subtract the smaller geometry from the larger one
        def differenceGeom = currentGeom.difference(nextGeom)
        
        // Create an annotation from the difference geometry and put the classificaiton
        def roi = GeometryTools.geometryToROI(differenceGeom, plane)
        def annotation = PathObjects.createAnnotationObject(roi,getPathClass(currentEntry.key))
        
        // Optionally, set the classification from the map's key
        annotation.setName(parentID.toString())
        
        // Add the annotation to the list
        annotationsList << annotation
        
        // Print debugging information
        
    }
    
    // Add the smallest geometry directly as an annotation
    def smallestEntry = entries[0]
    def smallestGeom = smallestEntry.value
    def smallestRoi = GeometryTools.geometryToROI(smallestGeom, plane)
    def smallestAnnotation = PathObjects.createAnnotationObject(smallestRoi,getPathClass(smallestEntry.key))
    
    // Optionally, set the classification for the smallest geometry
    smallestAnnotation.setName(parentID.toString())
    
    // Add the smallest annotation to the list
    annotationsList << smallestAnnotation
    
    // Return the list of annotations
    return annotationsList
}




///////////////////////////////////////////////////////////////////
///////FUNCTIONS TO OBTAIN THE PROPPER DILATIONS OUTSIDE PARENT/////
////////////////////////////////////////////////////////////////////

//Here we are going to do the fisrt dilation, aproximated by the formula x = (2*Area(1-sqrt(n)))/Perimeter
//Here n is the dilation factor. If it is 0.5 it will compute how much we need to reduce the geometry to obtain an area of the half of the original
//If n is 2, this will compute a geometry that is the double of the original area.
//This approximation works super nice in shapes like circles, squares, pentagons etc.
//If your shape has low solidity or has a lot of perimeter relative to the area, gives an approximation that is pretty good.
//To be able to reduce this difference we create a loop to minimize this approximation.
//In this way using JTS we can obtain the dilation of a 2D shape with a minimum difference from the theorical dilation. 
//In all the test that I have done i obtain differences arround the 0.5% between the theorical dilation and the obtained dilation.
//Since we are working directly with geometries the proces speed increases a lot.
//If your geometry has a lot of vertices takes more time to do the first dilation. 

def firstDilation(parentObject,dilationFactor){

    firstDilationFeatures = [:]

    //Extract the parent info
    parentID = parentObject.getID()

    parentGeom = parentObject.getROI().getGeometry()

    perimeterParent = parentGeom.getLength()

    areaParent = parentGeom.getArea()

    s= (2*areaParent)/perimeterParent

    //Now extract the target area and the amouunt of s to decrease/increase to obtain the dilation: 
    targetArea = areaParent * dilationFactor
    firstDilationPx = s*(1-Math.sqrt(dilationFactor))
    
    firstDilationGeom = parentGeom.buffer( -firstDilationPx)

    //extract the info to know if the loop must be increasing or decreasing
    geomDilationArea = firstDilationGeom.getArea()
    differencePercentArea = (100*(geomDilationArea - targetArea))/targetArea

    if(differencePercentArea < 0 ){

        loopDirection = "IncreasingLoop"
    }else if(differencePercentArea >0 ){
        loopDirection = "DecreasingLoop"
    }

    firstAbsolutePercentDifference = Math.abs(differencePercentArea)
    
    firstDilationFeatures[parentID] = [geometry: firstDilationGeom, targetAreaPx: targetArea, s: s, firstDilationPx:firstDilationPx ,loopDirection:loopDirection,firstAbsolutePercentDifference :firstAbsolutePercentDifference ]
    
    return firstDilationFeatures
}



//In this fucntion we check if the first approximation "works"
//Here we say "works" if after the first dilation the difference between the targetArea and the obtained area in absolute value is less than 0.1%.
//If not, the loop starts. It can go in two directions, so if after the first dilation the value of the firstDilationArea-targetArea is negative, goes to the increasing loop
//If the difference between firstDilationArea-targetArea is positive goes to the decreasing loop.
def adjustDilationTargetAreaLoop(firstDilationFeatures){

    differenceGeomMap = [:]
    def finalGeom = null
    def finalAbsolutePercentDifference = 0
    

    firstDilationFeatures.each { objectID, data ->

        if (data.firstAbsolutePercentDifference < 0.1) {
            print("First Dilation is okay, absolute percent difference: " + data.firstAbsolutePercentDifference)
            finalGeom = data.geometry
            finalAbsolutePercentDifference = data.firstAbsolutePercentDifference
            
        } else {
            loopDirection = data.loopDirection.toString()

            geomFirstDilation = data.geometry
            s = data.s
            dilationPX = 0
            optimizingArea = geomFirstDilation.getArea()
            targetArea = data.targetAreaPx

            differenceGeomMap[Math.abs(optimizingArea - targetArea)] = geomFirstDilation

            if (loopDirection == "DecreasingLoop") {
                while (optimizingArea > (targetArea * 0.99)) {
                    percentDifference = (100 * (Math.abs(optimizingArea - targetArea))) / targetArea
                    valueExpansion = percentDifference > 10 ? 0.02 * s : 0.002 * s
                    dilationPX += valueExpansion
                    geomOptimmizer = geomFirstDilation.buffer(-dilationPX)
                    optimizingArea = geomOptimmizer.getArea()
                    differenceGeomMap[Math.abs(optimizingArea - targetArea)] = geomOptimmizer
                }
            } else if (loopDirection == "IncreasingLoop") {
                while (optimizingArea < (targetArea * 1.01)) {
                    percentDifference = (100 * (Math.abs(optimizingArea - targetArea))) / targetArea
                    valueExpansion = percentDifference > 10 ? 0.02 * s : 0.002 * s
                    dilationPX -= valueExpansion
                    geomOptimmizer = geomFirstDilation.buffer(-dilationPX)
                    optimizingArea = geomOptimmizer.getArea()
                    differenceGeomMap[Math.abs(optimizingArea - targetArea)] = geomOptimmizer
                }
            }
        }
    }

    // Now return finalGeom after processing all entries
    if (finalGeom == null && !differenceGeomMap.isEmpty()) {
        // Fallback to using the minimum difference geometry
        minDifference = differenceGeomMap.keySet().min()
        finalGeom = differenceGeomMap[minDifference]
        finalAbsolutePercentDifference = (100*Math.abs(finalGeom.getArea() - targetArea))/targetArea
    }

    // If still null, print a warning
    if (finalGeom == null) {
        println "Warning: No valid geometry was found."
    }

    return [finalGeom,finalAbsolutePercentDifference]
}







//here computeDifferences is a boolean
def runComputeDifferenceBetweenRegions() {
    
       
      // Assume we have a list of regions (objects) in QuPath
       regions = getAnnotationObjects().findAll {
          it.getPathClass().toString().contains("Expansion") 
       }
    

       // Group regions by their parent names and annotation types (Inside vs Outside)
       //.withDefault { [:] } is to create an empty map and do not return a null. 
       //this makes possible to put different parents without an initiation first
       //thanks chatGPT for providing the solution :)
       def groupedRegions = [:].withDefault { [:] }

        regions.each { region ->
           def parentName = region.getName()  // Get the parent name (assuming it comes from object.getName())
           def annotationType = ""

           // Check whether the annotation is "Expansion_Outside" or "Expansion_Inside"
           if (region.getPathClass().getName().contains("Expansion_Outside")) {
               annotationType = "Outside"
           } else if (region.getPathClass().getName().contains("Expansion_Inside")) {
               annotationType = "Inside"
           }
    
           if (annotationType) {
               if (!groupedRegions[parentName].containsKey(annotationType)) {
                   groupedRegions[parentName][annotationType] = []
               }
               groupedRegions[parentName][annotationType] << region
           }
       }

       // Now loop through each parent and annotation type, comparing areas only within those groups
       groupedRegions.each { parentName, annotations ->
           annotations.each { annotationType, regionsList ->
               println "Comparing regions for parent: ${parentName}, annotation type: ${annotationType}"
        
               for (int i = 0; i < regionsList.size(); i++) {
                   for (int j = i + 1; j < regionsList.size(); j++) {
                       def region1 = regionsList[i]
                       def region2 = regionsList[j]
                
                       // Get the areas of the regions
                       def area1 = region1.getROI().getArea()
                       def area2 = region2.getROI().getArea()
                
                       // Calculate the percentage difference between the areas
                       def percentDifference = calculatePercentageDifference(area1, area2)
                
                       // Round the result to 4 decimals
                       def roundedDifference = Math.round(percentDifference * 10000) / 10000.0
                
                       // Construct the measurement name, now distinguishing Inside and Outside
                       def measurementName = "Abs_Diff_Percent_Expansion_${annotationType}_${i+1}v${j+1}"
                
                       // Find the parent annotation by using the parent name (assuming parentName directly contains the ID)
                       def parentAnnotation = getAnnotationObjects().find { it.getID().toString() == parentName }
                
                       // If the parent annotation is found, add the new measurement
                       if (parentAnnotation) {
                           parentAnnotation.getMeasurementList().put(measurementName, roundedDifference)
                           println "Added measurement: ${measurementName} = ${roundedDifference} to parent ${parentName}"
                       } else {
                           println "Parent annotation with name ${parentName} not found!"
                       }
                   }
               }
           }
       }
       
     
}


// Define a method to calculate the percentage difference between two regions
def calculatePercentageDifference(area1, area2) {
        return (Math.abs(area1 - area2) / ((area1 + area2) / 2)) * 100
}
