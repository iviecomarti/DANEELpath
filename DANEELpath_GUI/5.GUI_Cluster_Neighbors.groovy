/*
 * DANEELpath project  is under   GNU GENERAL PUBLIC LICENSE, GPL-3.0 license
 * Please check https://github.com/iviecomarti/DANEELpath if you have any doubts about licensing
 *
 * Script to count the number of annotation neighbours with three criteria
 *
 *
 *GIVEN EXPANSION NEIGHBOURS TAB
 *
 * Uses the cell Clusters, create an expansion "a neibhbourhood" of the Given Expansion
 *and counts the number of annotations that intersects with it
 * 
 * If you have an annotation covering all the clusters, like "Hidrogel", you can set it, to avoid to create an expansion.
 * 
 *LONG RADIUS NEIGHBOURS TAB
 * Uses the cell Clusters, create an expansion "a neibhbourhood" of the Long Raidus 
 * and counts the number of annotations that intersects with it
 * 
 * If you have an annotation covering all the clusters, like "Hidrogel", you can set it, to avoid to create an expansion.
 *
 *VORONOI NEIGHBOURS
 *
 *Voronoi Touching Neighbours
 * 
 * Creates a Voronoi Diagram form the Annotations and counts their neighbours
 * NOTE: Techniclly, we are doing Influence Zones, since the expansions strart from the border of the annotations. 
 * 
 * You need to put a reference Annotation so, the expanisons of Voronoi will finish when touch that reference. 
 * To run this script, you need to provide its classification. 
 * The annotations that are not WITHIN the reference are excluded form the analysis (if happens its ID will be printed)
 * If the annotation has no classification will be excluded from the analysis(if happens its ID will be printed)
 * The touching with the reference annotation is NOT computed 
 *
 * 
 * With the option: "countWithClassifications"  is set to "false", just the number of touching neighbours will appear in the original object.
 * If "countWithClassifications" is set to "true" you will obtain the touching neighbours and the count for each of the classifictions
 * 
 * If desired you can add with the three methos the expanded annotations for visual purposes, but it will take a little bit more of time.
 *
 * @author Isaac Vieco-Martí
 * 
 */

import javafx.application.Platform
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.VBox
import javafx.stage.Stage
import qupath.lib.gui.QuPathGUI
import qupath.lib.gui.dialogs.Dialogs
import qupath.lib.gui.tools.PaneTools
import static qupath.lib.gui.scripting.QPEx.*
import qupath.fx.utils.FXUtils

Platform.runLater { buildStage().show() }

Stage buildStage() {
    def qupath = QuPathGUI.getInstance()
    
    // Create a TabPane
    def tabPane = new TabPane()
    
    ////////////////////
    /////VORONOI PANE//
    ///////////////////
    def voronoiTab = new Tab("Voronoi Neighbours")
    def voronoiPane = new GridPane()
    
    rowVoronoi = 0
    rowVoronoi++

   voronoiTittle = new Label("Count Voronoi Touching Neighbours")
   voronoiTittle.setStyle("-fx-font-weight: bold")
   voronoiPane.add(voronoiTittle, 0,rowVoronoi, 1, 1)

    def referenceAnnotationVoronoiCombo = new ComboBox<PathClass>(qupath.getAvailablePathClasses())
    referenceAnnotationVoronoiCombo.getSelectionModel().selectFirst()
    referenceAnnotationVoronoiCombo.setTooltip(new Tooltip("Set the classification of Reference annotation to limit Voronoi Expansions"))
    def labelComboVoronoi = new Label("Annotation to limit Voronoi expansion")
    //voronoiPane.setLabelFor(referenceAnnotationVoronoiCombo)
    rowVoronoi++
    voronoiPane.add(labelComboVoronoi,0,rowVoronoi,1,1)
    voronoiPane.add(referenceAnnotationVoronoiCombo,1,rowVoronoi,1,1)
    
    
    //add the checkbox for Count Classified Neighbors
    rowVoronoi++
    def voronoiClassifiedNeighbors= new CheckBox("Count Neighbours By Classification")
    voronoiClassifiedNeighbors.setSelected(true)
    voronoiClassifiedNeighbors.setTooltip(new Tooltip("If checked, will count the neighbours and the number for each classification"))
    
    voronoiPane.add(voronoiClassifiedNeighbors, 0, rowVoronoi, 1, 1)
    
    
    
    //add the checkbox for add the voronoi faces to the GUI
    rowVoronoi+=3
    addVoronoiFaceTittle = new Label("OPTIONAL : Add Voronoi Faces")
    addVoronoiFaceTittle.setUnderline(true)
    voronoiPane.add(addVoronoiFaceTittle, 0,rowVoronoi, 1, 1)
    
    rowVoronoi++
    def addVoronoiFaceAnnotation= new CheckBox("Add Voronoi Faces As Annotations")
    addVoronoiFaceAnnotation.setSelected(false)
    addVoronoiFaceAnnotation.setTooltip(new Tooltip("If checked, will create the Voronoi Face Annotations"))
    
     voronoiPane.add(addVoronoiFaceAnnotation, 0, rowVoronoi, 1, 1)
     
     
    def removeInteriorVoronoiFace= new CheckBox("Remove Interior")
    removeInteriorVoronoiFace.setSelected(false)
    removeInteriorVoronoiFace.setTooltip(new Tooltip("If checked, will remove the Original cluster from Voronoi Face"))
    
    
    voronoiPane.add(removeInteriorVoronoiFace, 1, rowVoronoi, 1, 1)
    
    
    //Now create the button to run 
    rowVoronoi+=3
    def btnVoronoi = new Button("Count Voronoi Neighbours")
    btnVoronoi.setTooltip(new Tooltip("Run the function"))
    voronoiPane.add(btnVoronoi, 0, rowVoronoi, 2, 1)
    //this is for display
    GridPaneUtils.setToExpandGridPaneWidth(referenceAnnotationVoronoiCombo, btnVoronoi)
    voronoiPane.setHgap(20)
    voronoiPane.setVgap(10)
    voronoiPane.setPadding(new Insets(10, 10, 10, 5)) 

    voronoiTab.setContent(voronoiPane)
    
    
    //set the action of the voronoi Button:
    
    btnVoronoi.setOnAction {e->
        
        runVoronoiNeighbours(referenceAnnotationVoronoiCombo.getValue().toString(),voronoiClassifiedNeighbors.isSelected(),addVoronoiFaceAnnotation.isSelected(),removeInteriorVoronoiFace.isSelected())
        Dialogs.showInfoNotification("Voronoi Neighbours", "Count Voronoi Neighbours Finished!")
    }
    
    
    ////////////////////
    //LONG RADIUS PANE//
    ////////////////////
    def longRadiusTab = new Tab("LongRadius Neighbours")
    def longRadiusPane = new GridPane()
    
    rowLongRadius = 0
    rowLongRadius++

    longRadiusTittle = new Label("Count Neighbours in 1 Long Radius")
    longRadiusTittle.setStyle("-fx-font-weight: bold")
    longRadiusPane.add(longRadiusTittle, 0,rowLongRadius, 2, 1)
    
    //Parent annotaiton to be ignroed when doing the expansion
    def referenceAnnotationLongRadiusCombo = new ComboBox<PathClass>(qupath.getAvailablePathClasses())
    referenceAnnotationLongRadiusCombo.getSelectionModel().selectFirst()
    referenceAnnotationLongRadiusCombo.setTooltip(new Tooltip("Set the classification of Reference annotation to be ignored in the expansion"))
    def labelLongRadiusCombo = new Label("Parent Annotation to ignore expansion")
    rowLongRadius++
    longRadiusPane.add(labelLongRadiusCombo,0,rowLongRadius,1,1)
    longRadiusPane.add(referenceAnnotationLongRadiusCombo,1,rowLongRadius,1,1)
    
    //add the checkbox for Count Classified Neighbors
    rowLongRadius++
    def longRadiusClassifiedNeighbors= new CheckBox("Count Neighbours By Classification")
    longRadiusClassifiedNeighbors.setSelected(true)
    longRadiusClassifiedNeighbors.setTooltip(new Tooltip("If checked, will count the neighbours and the number for each classification"))
    longRadiusPane.add(longRadiusClassifiedNeighbors, 0, rowLongRadius, 1, 1)
    
   
    
    //add the checkbox for add the Long Raidus Expansion to the GUI
    rowLongRadius+=3
    addLongRadiusTittle = new Label("OPTIONAL : Add The Long Radius Expansion")
    addLongRadiusTittle.setUnderline(true)
    longRadiusPane.add(addLongRadiusTittle, 0,rowLongRadius, 2, 1)
    
    rowLongRadius++
    def addLongRadiusAnnotation= new CheckBox("Add Long Radius Annotations")
    addLongRadiusAnnotation.setSelected(false)
    addLongRadiusAnnotation.setTooltip(new Tooltip("If checked, will create the Long Radius Annotation"))
    
     longRadiusPane.add(addLongRadiusAnnotation, 0, rowLongRadius, 1, 1)
     
     
    def removeInteriorLongRadius= new CheckBox("Remove Interior")
    removeInteriorLongRadius.setSelected(false)
    removeInteriorLongRadius.setTooltip(new Tooltip("If checked, will remove the Original from the expanson"))
    
    
    longRadiusPane.add(removeInteriorLongRadius, 1, rowLongRadius, 1, 1)
    
    
    //Now create the button to run 
    rowLongRadius+=3
    def btnLongRadius = new Button("Count Long Radius Neighbours")
    btnLongRadius.setTooltip(new Tooltip("Run the function"))
    longRadiusPane.add(btnLongRadius, 0, rowLongRadius, 2, 1)
    
    longRadiusPane.setHgap(20)
    longRadiusPane.setVgap(10)
    longRadiusPane.setPadding(new Insets(10, 10, 10, 5)) 
    longRadiusTab.setContent(longRadiusPane)
    
    GridPaneUtils.setToExpandGridPaneWidth(referenceAnnotationLongRadiusCombo, btnLongRadius)
    
    
    //SET THE ACTION OF THE BUTTON LONG RADIUS
    
    btnLongRadius.setOnAction {e->
        
        //runningMode, countByClassification,radius, referenceAnnotation,addExpansions,removeInterior
        runNeighboursExpansion("LongRadius", longRadiusClassifiedNeighbors.isSelected(), 0,referenceAnnotationLongRadiusCombo.getValue().toString(),addLongRadiusAnnotation.isSelected(),removeInteriorLongRadius.isSelected())
        Dialogs.showInfoNotification("Long Radius Neighbours", "Count Long Radius Neighbours Finished!")
    }
    
    ////////////////////
    //GIVE RADIUS PANE//
    ////////////////////
    def giveRadiusTab = new Tab("GivenExpansion Neighbours")
    def giveRadiusPane = new GridPane()
    
    rowGiveRadius = 0
    rowGiveRadius++

    giveRadiusTittle = new Label("Count Neighbours given a Expansion in microns")
    giveRadiusTittle.setStyle("-fx-font-weight: bold")
    giveRadiusPane.add(giveRadiusTittle, 0,rowGiveRadius, 2, 1)
    
    rowGiveRadius++
    def radiusSpinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.1, 1000, 10.0, 1.0));
    radiusSpinner.setEditable(true);
    FXUtils.restrictTextFieldInputToNumber(radiusSpinner.getEditor(), true);
    radiusSpinner.setTooltip(new Tooltip("Set the radius expansion in microns"))
    def radiusSpinnerLabel = new Label("Microns to expand the Annotation")
    giveRadiusPane.add(radiusSpinnerLabel,0,rowGiveRadius,1,1)
    giveRadiusPane.add(radiusSpinner,1,rowGiveRadius,1,1)
    
    
    //Parent annotaiton to be ignroed when doing the expansion
    def referenceAnnotationGiveRadiusCombo = new ComboBox<PathClass>(qupath.getAvailablePathClasses())
    referenceAnnotationGiveRadiusCombo.getSelectionModel().selectFirst()
    referenceAnnotationGiveRadiusCombo.setTooltip(new Tooltip("Set the classification of Reference annotation to be ignored in the expansion"))
    def labelGiveRadiusCombo = new Label("Parent Annotation to ignore expansion")
    rowGiveRadius++
    giveRadiusPane.add(labelGiveRadiusCombo,0,rowGiveRadius,1,1)
    giveRadiusPane.add(referenceAnnotationGiveRadiusCombo,1,rowGiveRadius,1,1)
    
    //add the checkbox for Count Classified Neighbors
    rowGiveRadius++
    def giveRadiusClassifiedNeighbors= new CheckBox("Count Neighbours By Classification")
    giveRadiusClassifiedNeighbors.setSelected(true)
    giveRadiusClassifiedNeighbors.setTooltip(new Tooltip("If checked, will count the neighbours and the number for each classification"))
    giveRadiusPane.add(giveRadiusClassifiedNeighbors, 0, rowGiveRadius, 1, 1)
    
    
    
    //add the checkbox for add the Long Raidus Expansion to the GUI
    rowGiveRadius+=3
    addGiveRadiusTittle = new Label("OPTIONAL : Add The Expansion")
    addGiveRadiusTittle.setUnderline(true)
    giveRadiusPane.add(addGiveRadiusTittle, 0,rowGiveRadius, 2, 1)
    
    rowGiveRadius++
    def addGiveRadiusAnnotation= new CheckBox("Add Expansion Annotations")
    addGiveRadiusAnnotation.setSelected(false)
    addGiveRadiusAnnotation.setTooltip(new Tooltip("If checked, will create the Expansion Annotation"))
    
     giveRadiusPane.add(addGiveRadiusAnnotation, 0, rowGiveRadius, 1, 1)
     
     
    def removeInteriorGiveRadius= new CheckBox("Remove Interior")
    removeInteriorGiveRadius.setSelected(false)
    removeInteriorGiveRadius.setTooltip(new Tooltip("If checked, will remove the Original from the expanson"))
    
    
    giveRadiusPane.add(removeInteriorGiveRadius, 1, rowGiveRadius, 1, 1)
    

    //Now create the button to run 
    rowGiveRadius+=3
    def btnGiveRadius = new Button("Count Expansion Neighbours")
    btnGiveRadius.setTooltip(new Tooltip("Run the function"))
    giveRadiusPane.add(btnGiveRadius, 0, rowGiveRadius, 2, 1)

    
    giveRadiusPane.setHgap(20)
    giveRadiusPane.setVgap(10)
    giveRadiusPane.setPadding(new Insets(10, 10, 10, 5)) 
    giveRadiusTab.setContent(giveRadiusPane)
    
    GridPaneUtils.setToExpandGridPaneWidth(radiusSpinner,referenceAnnotationGiveRadiusCombo,btnGiveRadius)

    btnGiveRadius.setOnAction {e->
        
        //runningMode, countByClassification,radius, referenceAnnotation,addExpansions,removeInterior
        runNeighboursExpansion("GiveRadius", giveRadiusClassifiedNeighbors.isSelected(), radiusSpinner.getValue(),referenceAnnotationGiveRadiusCombo.getValue().toString(),addGiveRadiusAnnotation.isSelected(),removeInteriorGiveRadius.isSelected())
        Dialogs.showInfoNotification("Give Expansion Neighbours", "Count Expansion Neighbours Finished!")
    }

    //annotationsPane.children.addAll(labelCombo, combo, labelSpinner, spinner, cbHoles, cbTight, btnCreate)
    //annotationsTab.setContent(annotationsPane)
    //, imageTab, annotationsTab
    
    // Add all tabs to TabPane
    tabPane.tabs.addAll(giveRadiusTab,longRadiusTab,voronoiTab)
    
    // Create the scene and add the tabPane to it
    def scene = new Scene(tabPane, 475, 300)
    def stage = new Stage()
    stage.setTitle("Count Neighbours")
    stage.setScene(scene)
    stage.initOwner(qupath.getStage())
    
    return stage
}















//////////////////////////////////////////////////
////////VORONOI NEIGHBOURS FUNCTIONS/////////////
////////////////////////////////////////////////


//Main Function
def runVoronoiNeighbours(referenceAnnotationString, countWithClassifications, addVoronoiFaces,removeInterior) {
   
   println "Voronoi Diagram started"
   //create the faces
   voronoiFaces = createVoronoiFace(referenceAnnotationString)
   
   println "Voronoi Faces created, computing touching Neighbours"
   //count the neighbours
   if(countWithClassifications == false) {
       
      countTouchingNeighbours(voronoiFaces)
      
   }else if(countWithClassifications == true) {
      
      countTouchingNeighboursWithClassification(referenceAnnotationString,voronoiFaces)
   }
   
   //add the faces
   if(addVoronoiFaces == true) {
      addVoronoiAnnotations(voronoiFaces,removeInterior)
      
   }
   
   
   
}




import qupath.lib.analysis.DelaunayTools
import qupath.lib.objects.PathObjects


//Create Voronoi Faces Using QuPath DelaunayTools 
def createVoronoiFace(referenceAnnotationString) {
    
   def reference = getAnnotationObjects().findAll {it.getPathClass() == getPathClass(referenceAnnotationString)}
   
   // Check if the number of reference annotations is greater than 1
    if (reference.size() > 1) {
        // Throw a message if more than one reference annotation is found
        Dialogs.showErrorMessage("Error ReferenceAnnotation","ERROR: There is more than one reference annotation, please merge the reference annotations into one object.")
        return  
    }
    
    if(reference.size() == 0) {
        Dialogs.showErrorMessage("Error ReferenceAnnotation","ERROR: There is not reference annotation. \n Please create a reference Annotation, and check its classification") 
        return 
    }
    
    
   def referenceGeom = reference[0].getROI().getGeometry()
    
    
   //Check the annotations inside the reference. avoid null classifications  
   def annoInterest = getAnnotationObjects().findAll {
       it.getPathClass() != null && 
       it.getPathClass() != getPathClass(referenceAnnotationString) &&
       it.getROI().getGeometry().within(referenceGeom) == true &&
       !it.getPathClass().toString().contains("Voronoi_") &&
       !it.getPathClass().toString().contains("LongRadiusExpansion") &&
       !it.getPathClass().toString().contains("Expansion")
       
       }
   
   //I LEAVE HERE THE FUNCTION TO TRACK THE ONES WITHOUN INCLUDE... PERHAPS CAN BE USEFULL IN SOME CASES.
   //Check the annotations outside the reference or without classification
   //def annoExcludedFormTheAnalysis = getAnnotationObjects().findAll {
       //(it.getPathClass() != getPathClass(referenceAnnotationString) && it.getROI().getGeometry().within(referenceGeom) == false) | 
        //it.getPathClass() == null
       
       //}
   
   //log if annotations are not included in the analysis
   //if(annoExcludedFormTheAnalysis.size() > 0) {
       
       //print("A total of " + annoExcludedFormTheAnalysis.size()+ " annotations have been excluded from the analysis because they are not inside reference or because they are unclassified")
       //annoExcludedFormTheAnalysis.forEach {
           
           //print("Annotation with ID: " +it.getID() + " excluded" )
           
      // }
       
   //}
   
   
   def voronoiDiagram = DelaunayTools.createFromGeometryCoordinates(annoInterest,false,4.0).getVoronoiFaces()
   
   
    //Create Voronoi annotations, limit the expansion and set the class of its origin annotation
    voronoiDiagram = voronoiDiagram.collectEntries { annotationObj, voronoiFace ->
    
        // Get the class and ID of the original object
        def originalObjClass = annotationObj.getPathClass()
        def originalObjID = annotationObj.getID().toString()


        // Calculate the intersection of the voronoiFace and the reference geometry
        def intersect = voronoiFace.intersection(referenceGeom)

        // Create a map entry with the originalObjID as key, and the geometry and class as values
        [(originalObjID): [geometry: intersect, classification: originalObjClass]]
    } 
        
    return voronoiDiagram
}




// Count Voronoi Faces Without classifications using Java Topology Suite
def countTouchingNeighbours(voronoiFaceMap) {

    // Iterate over each annotation's entry in the map
    voronoiFaceMap.each { originalObjID1, data1 ->
        
        // Initialize the variable to count touching neighbors
        def touchingNeighborsCount = 0
        def geom1 = data1.geometry // Get geometry from the map
        
        // Iterate over all other annotations in the map
        voronoiFaceMap.each { originalObjID2, data2 ->
            def geom2 = data2.geometry // Get geometry from the map
    
            // Skip comparing the annotation with itself
            if (originalObjID1 != originalObjID2) {
                // Check if the current geometries touch each other
                if (geom1.touches(geom2)) {
                    touchingNeighborsCount++
                }
            }
        }
        
        // Find the original annotation by its ID
        def originalAnno = getAllObjects().find {
           it.getID().toString() == originalObjID1.toString()
        }
        
        // Add the count of touching neighbors to the original annotation's measurements
        if (originalAnno != null) {
            originalAnno.getMeasurementList().put('Voronoi_Touching_Neighbors', touchingNeighborsCount)
        }
    }
}





// Count Voronoi Faces With classifications using Java Topology Suite
def countTouchingNeighboursWithClassification(referenceAnnotationString, voronoiFaceMap) {
    
    // Iterate over each annotation entry in the voronoiFaceMap
    voronoiFaceMap.each { originalObjID1, data1 ->
        
        // Create the empty map to track touching neighbors for this annotation
        def touchingNeighborsMap = updateAnnotationsList(referenceAnnotationString)
        
        // Get the geometry for the first annotation
        def geom1 = data1.geometry
        
        // Iterate over all other annotations in the map
        voronoiFaceMap.each { originalObjID2, data2 ->
            def geom2 = data2.geometry
    
            // Skip comparing the annotation with itself
            if (originalObjID1 != originalObjID2) {
                // Check if the geometries touch each other
                if (geom1.touches(geom2)) {
                    
                    // Sum to the total number of touching neighbors
                    touchingNeighborsMap["Voronoi_Touching_Neighbors"] += 1
                    
                    // Get the classification of the touching annotation and increment it
                    annotation2Classification = data2.classification.toString()
                    classificationKey = "Voronoi_Touching_" + annotation2Classification
                    touchingNeighborsMap[classificationKey] += 1
                   
                }
            }
        }
        
        // Find the original annotation by its ID
        def originalObject = getAllObjects().find {
           it.getID().toString() == originalObjID1.toString()
        }
        
        // Add the measurements to the original annotation
        if (originalObject != null) {
            touchingNeighborsMap.each { key, value ->
                // Add each key-value pair from the map as a measurement
                originalObject.getMeasurementList().put(key, value)
            }
        }
    }
}




//Funciton to update the classifications of the present annotations. 
def updateAnnotationsList(referenceClassification) {
    // Get the list of annotations
    def annotations = getAnnotationObjects().findAll {
        !it.getPathClass().toString().contains("Voronoi_") &&
        !it.getPathClass().toString().contains("LongRadiusExpansion") &&
        !it.getPathClass().toString().contains("Expansion") 
    }
    
    // Initialize an empty map for annotation counts
    def annotationsMap = [:]
    annotationsMap["Voronoi_Touching_Neighbors"] = 0
    // Loop through each annotation
    
    annotations.each { annotation ->
        // Get the class of the annotation
        def annoClass = annotation.getPathClass().toString()
        
            
         if (annoClass != "null" && annoClass != referenceClassification.toString()) {
          // Add the annotation type to the map with an initial value of 0 if it's not already present
            
            //add the prefix to have the same values
            annoClass = "Voronoi_Touching_" + annoClass
              if (!annotationsMap.containsKey(annoClass)) {
               annotationsMap[annoClass] = 0
              }
          }
   
    }
    
    // Return the map with annotation types initialized to 0
    return annotationsMap
    
}




//create a function to add the voronoi annotations if needed.
def addVoronoiAnnotations(voronoiFaceMap,removeInterior) {
   
    def plane = ImagePlane.getPlane(0, 0)
    
    // Create an empty list 
    def annotationsList = []
    
    // Iterate over the voronoiFaceMap and create annotations
    voronoiFaceMap.each { originalObjID, data ->
        def geom = data.geometry
        def classificationOringialObject = data.classification.toString()
        def originalObjIDStr = originalObjID.toString()


        if(removeInterior == true){

            parent = getAllObjects().find{
                it.getID().toString() == originalObjIDStr
            }

            parentGeom = parent.getROI().getGeometry()

            geom = geom.difference(parentGeom)

            def roi = GeometryTools.geometryToROI(geom, plane)
            
            classificationVoronoiFace = "Voronoi_" + classificationOringialObject
            def annotation = PathObjects.createAnnotationObject(roi,getPathClass(classificationVoronoiFace))
        
            //Set the name for tracking
            annotation.setName(originalObjIDStr)
            
            //put the measurements of neighbors to the voronoi face annotation just in case is needed. 
            parentMeasurements = parent.getMeasurementList()
            voronoiMeasurements = parentMeasurements.keySet().findAll { it.contains("Voronoi_") }.collectEntries { key ->
                [key, parentMeasurements[key]]
            }
            
            voronoiMeasurements.each { key, value ->
                // Add each key-value pair from the map as a measurement
                annotation.getMeasurementList().put(key, value)
            }
            
            
            // Add the annotation to the list
            annotationsList.add(annotation)


        }else if(removeInterior == false) {
           
           parent = getAllObjects().find{
                it.getID().toString() == originalObjIDStr
            } 

             // Create an annotation object from the geometry and classification
            def roi = GeometryTools.geometryToROI(geom, plane)
            classificationVoronoiFace = "Voronoi_" + classificationOringialObject
            def annotation = PathObjects.createAnnotationObject(roi,getPathClass(classificationVoronoiFace))
        
            // Set the name for tracking
            annotation.setName(originalObjIDStr)
            
            //put the measurements of neighbors to the voronoi face annotation just in case is needed.
            parentMeasurements = parent.getMeasurementList()
            voronoiMeasurements = parentMeasurements.keySet().findAll { it.contains("Voronoi_") }.collectEntries { key ->
                [key, parentMeasurements[key]]
            }
            
            voronoiMeasurements.each { key, value ->
                // Add each key-value pair from the map as a measurement
                annotation.getMeasurementList().put(key, value)
            }

            // Add the annotation to the list
            annotationsList.add(annotation)
            
        }

       
    }
    
    // Add the list of annotations to the project
    addObjects(annotationsList)
    
    println("Annotations added successfully!")
}




//////////////////////////////////////////////////
////////EXPANSION NEIGHBOURS FUNCTIONS/////////////
////////////////////////////////////////////////


//Main Function
def runNeighboursExpansion(runningMode, countByClassification,radius, referenceAnnotation,addExpansions,removeInterior){

    if(runningMode == "LongRadius"){
        geomMap = extractAnnotationGeometriesAndLongRadius(referenceAnnotation)

        if(countByClassification == true){
            countNeighboursWithClassificationLongRadius(geomMap,referenceAnnotation)
        }else {
            countNeighboursLongRadius(geomMap)
        }
        

        if(addExpansions == true){
            addLongRadiusExpansions(geomMap,removeInterior)
        }

    } else if(runningMode == "GiveRadius") {
       
        def imageData = QP.getCurrentImageData()
        def server = imageData.getServer()
        def cal = server.getPixelCalibration()
    
        if (!cal.hasPixelSizeMicrons()) {
               Dialogs.showErrorMessage("Error GivenExpansion","ERROR: We need the pixel size information here! ")
              return
            }

        geomMap = extractAnnotationGeometries(referenceAnnotation)

        if(countByClassification == true){
            countNeighboursWithClassificationGivenARadius(geomMap,radius,referenceAnnotation)
        }else {
            countNeighboursGivenARadius(geomMap,radius)
        }

        if(addExpansions == true){
            addGivenRadiusExpansions(geomMap,radius,removeInterior)
        }



    }


}








/////////////////////////////////////////////
////////FUNCTIONS TO CREATE ACTIONS//////////
////////////////////////////////////////////




import qupath.lib.scripting.QP
import org.locationtech.jts.geom.Geometry
import qupath.lib.common.GeneralTools
import qupath.lib.objects.PathObject
import qupath.lib.objects.PathObjects
import qupath.lib.roi.GeometryTools
import qupath.lib.roi.ROIs
import org.locationtech.jts.algorithm.MinimumBoundingCircle
import org.locationtech.jts.operation.distance.DistanceOp



def extractAnnotationGeometries(referenceToAvoid){
    
    def geometriesMap = [:]

    annotationsOfInterest = QP.getAnnotationObjects().findAll{
        it.getPathClass() != null &&
        it.getPathClass().toString() != referenceToAvoid &&
        !it.getPathClass().toString().contains("Voronoi_") &&
        !it.getPathClass().toString().contains("LongRadiusExpansion") &&
        !it.getPathClass().toString().contains("Expansion")
    }

    annotationsOfInterest.forEach{
        classification = it.getPathClass().toString()
        id = it.getID().toString()
        geom = it.getROI().getGeometry()

        geometriesMap[id] = [classification: classification, geometry: geom]

    }

    return geometriesMap

}


def extractAnnotationGeometriesAndLongRadius(referenceToAvoid){

    def geometriesMap = [:]

    annotationsOfInterest = QP.getAnnotationObjects().findAll{
        it.getPathClass() != null && 
        it.getPathClass().toString() != referenceToAvoid &&
        !it.getPathClass().toString().contains("Voronoi_") &&
        !it.getPathClass().toString().contains("LongRadiusExpansion") &&
        !it.getPathClass().toString().contains("Expansion")
    }

    annotationsOfInterest.forEach{
        classification = it.getPathClass().toString()
        id = it.getID().toString()
        geom = it.getROI().getGeometry()
        longRadiusPx = computeRadiusOfGeometry(geom)

        geometriesMap[id] = [classification: classification, geometry: geom, radiusPx: longRadiusPx]

    }

    return geometriesMap

}




def computeRadiusOfGeometry(geometry) {
     
   //create the MBC of a geometry and divide it by 2. This will give you the radius in pixels
   def MBC = new MinimumBoundingCircle(geometry)
   radiusPx = MBC.getRadius()
      
   return radiusPx
   
}



def countNeighboursGivenARadius(mapGeometries, radius){

    def imageData = QP.getCurrentImageData()
    def server = imageData.getServer()
    def cal = server.getPixelCalibration()

    radiusPx = radius/cal.getAveragedPixelSizeMicrons()

     // Iterate over each annotation's entry in the map
    mapGeometries.each { originalObjID1, data1 ->
        
        // Initialize the variable to count touching neighbors
        def neighborsCount = 0
        def geom1 = data1.geometry // Get geometry from the map
        
        // Iterate over all other annotations in the map
        mapGeometries.each { originalObjID2, data2 ->
            def geom2 = data2.geometry // Get geometry from the map
    
            // Skip comparing the annotation with itself
            if (originalObjID1 != originalObjID2) {
                // Check if the current geometries touch each other
                if (DistanceOp.isWithinDistance(geom1,geom2,radiusPx)) {
                    neighborsCount++
                }
            }
        }
        
        // Find the original annotation by its ID
        def originalAnno = getAllObjects().find {
           it.getID().toString() == originalObjID1.toString()
        }
        
        // Add the count of touching neighbors to the original annotation's measurements
        if (originalAnno != null) {

            measurementName = "Num_Neigh_" + radius +"um"
            originalAnno.getMeasurementList().put(measurementName, neighborsCount)
        }
    }
}





def countNeighboursWithClassificationGivenARadius(mapGeometries, radius, referenceToAvoid){

    def imageData = QP.getCurrentImageData()
    def server = imageData.getServer()
    def cal = server.getPixelCalibration()

    radiusPx = radius/cal.getAveragedPixelSizeMicrons()

     // Iterate over each annotation's entry in the map
    mapGeometries.each { originalObjID1, data1 ->
        
        // Initialize the variable to count touching neighbors
        numbNeighboursMeasurementMap = updateAnnotationsListGivenARadius(radius,referenceToAvoid)
        
        
        def geom1 = data1.geometry // Get geometry from the map
        
        // Iterate over all other annotations in the map
        mapGeometries.each { originalObjID2, data2 ->
            def geom2 = data2.geometry // Get geometry from the map
    
            // Skip comparing the annotation with itself
            if (originalObjID1 != originalObjID2) {
                // Check if the current geometries touch each other
                if (DistanceOp.isWithinDistance(geom1,geom2,radiusPx)) {
                    
                    totalMeasurementkey = "Numb_Neigh_"  +radius+"_um"
                    //put measurement in global classification
                    numbNeighboursMeasurementMap[totalMeasurementkey] +=1

                    //put measurement in and its classification
                    annotation2Classification = data2.classification.toString()
                    classificationKey = "Numb_Neigh_" + annotation2Classification +"_in_" +radius+"_um"
                    
                    numbNeighboursMeasurementMap[classificationKey] +=1
                }
            }
        }
        
        // Find the original annotation by its ID
        def originalObject = getAllObjects().find {
           it.getID().toString() == originalObjID1.toString()
        }
        
        // Add the measurements to the original annotation
        if (originalObject != null) {
            numbNeighboursMeasurementMap.each { key, value ->
                // Add each key-value pair from the map as a measurement
                originalObject.getMeasurementList().put(key, value)
            }
        }
    }
}



//Check the number of the objects located at one long radius from the original object
def countNeighboursLongRadius(mapGeometries){


     // Iterate over each annotation's entry in the map
    mapGeometries.each { originalObjID1, data1 ->
        
        // Initialize the variable to count touching neighbors
        def neighborsCount = 0
        def geom1 = data1.geometry // Get geometry from the map
        def longRadiusPx = data1.radiusPx

        // Iterate over all other annotations in the map
        mapGeometries.each { originalObjID2, data2 ->
            def geom2 = data2.geometry // Get geometry from the map
    
            // Skip comparing the annotation with itself
            if (originalObjID1 != originalObjID2) {
                // Check if the current geometries touch each other
                if (DistanceOp.isWithinDistance(geom1,geom2,longRadiusPx)) {
                    neighborsCount++
                }
            }
        }
        
        // Find the original annotation by its ID
        def originalAnno = getAllObjects().find {
           it.getID().toString() == originalObjID1.toString()
        }
        
        // Add the count of touching neighbors to the original annotation's measurements
        if (originalAnno != null) {

            measurementName = "Numb_Neigh_LongRadius"
            originalAnno.getMeasurementList().put(measurementName, neighborsCount)
        }
    }
}



//Check the number and classification of the objects located at one long radius from the original object
def countNeighboursWithClassificationLongRadius(mapGeometries, referenceToAvoid){


     // Iterate over each annotation's entry in the map
    mapGeometries.each { originalObjID1, data1 ->
        
        // Initialize the variable to count touching neighbors
        numbNeighboursMeasurementMap = updateAnnotationsListWithLongRadius(referenceToAvoid)
        
        
        def geom1 = data1.geometry // Get geometry from the map
        def longRadiusPx = data1.radiusPx
        // Iterate over all other annotations in the map
        mapGeometries.each { originalObjID2, data2 ->
            def geom2 = data2.geometry // Get geometry from the map
    
            // Skip comparing the annotation with itself
            if (originalObjID1 != originalObjID2) {
                // Check if the current geometries touch each other
                if (DistanceOp.isWithinDistance(geom1,geom2,longRadiusPx)) {
                    
                    totalMeasurementkey = "Numb_Neigh_LongRadius"
                    //put measurement in global classification
                    numbNeighboursMeasurementMap[totalMeasurementkey] +=1

                    //put measurement in and its classification
                    annotation2Classification = data2.classification.toString()
                    classificationKey = "Numb_Neigh_" + annotation2Classification +"_in_LongRadius"
                    
                    numbNeighboursMeasurementMap[classificationKey] +=1
                }
            }
        }
        
        // Find the original annotation by its ID
        def originalObject = getAllObjects().find {
           it.getID().toString() == originalObjID1.toString()
        }
        
        // Add the measurements to the original annotation
        if (originalObject != null) {
            numbNeighboursMeasurementMap.each { key, value ->
                // Add each key-value pair from the map as a measurement
                originalObject.getMeasurementList().put(key, value)
            }
        }
    }
}




//Funciton to update the classifications of the present annotations. 
def updateAnnotationsListGivenARadius(radiusMicron,referenceClassification) {

    // Get the list of annotations
    def annotations = getAnnotationObjects().findAll {
        !it.getPathClass().toString().contains("Voronoi_") &&
        !it.getPathClass().toString().contains("LongRadiusExpansion") &&
        !it.getPathClass().toString().contains("Expansion") 
    }
    
    // Initialize an empty map for annotation counts
    def annotationsMap = [:]
    globalMeasurement = "Numb_Neigh_"  +radiusMicron+"_um"
    annotationsMap[globalMeasurement] = 0
    // Loop through each annotation
    
    annotations.each { annotation ->
        // Get the class of the annotation
        def annoClass = annotation.getPathClass().toString()
        
            
         if (annoClass != "null" && annoClass != referenceClassification.toString()) {
          // Add the annotation type to the map with an initial value of 0 if it's not already present
            
            //add the prefix to have the same values
            annoClass = "Numb_Neigh_" + annoClass +"_in_" +radiusMicron+"_um"
              if (!annotationsMap.containsKey(annoClass)) {
               annotationsMap[annoClass] = 0
              }
          }
   
    }
    
    // Return the map with annotation types initialized to 0
    return annotationsMap
    
}




//Funciton to update the classifications of the present annotations. 
def updateAnnotationsListWithLongRadius(referenceClassification) {

    // Get the list of annotations
    def annotations = getAnnotationObjects().findAll {
        !it.getPathClass().toString().contains("Voronoi_") &&
        !it.getPathClass().toString().contains("LongRadiusExpansion") &&
        !it.getPathClass().toString().contains("Expansion") 
    }
    
    // Initialize an empty map for annotation counts
    def annotationsMap = [:]
    globalMeasurement = "Numb_Neigh_LongRadius"
    annotationsMap[globalMeasurement] = 0
    // Loop through each annotation
    
    annotations.each { annotation ->
        // Get the class of the annotation
        def annoClass = annotation.getPathClass().toString()
        
            
         if (annoClass != "null" && annoClass != referenceClassification.toString()) {
          // Add the annotation type to the map with an initial value of 0 if it's not already present
            
            //add the prefix to have the same values
            annoClass = "Numb_Neigh_" + annoClass +"_in_LongRadius"
              if (!annotationsMap.containsKey(annoClass)) {
               annotationsMap[annoClass] = 0
              }
          }
   
    }
    
    // Return the map with annotation types initialized to 0
    return annotationsMap
    
}







def addGivenRadiusExpansions(geomMap,radius,removeInside){


    def imageData = QP.getCurrentImageData()
    def server = imageData.getServer()
    def cal = server.getPixelCalibration()

    radiusPx = radius/cal.getAveragedPixelSizeMicrons()

    def plane = ImagePlane.getPlane(0, 0)
    def expansions = []


    geomMap.each{ originalObjID, data ->

        def originalGeom = data.geometry
        //def longRadius = data.radiusPx
        def originalObjIDStr = originalObjID.toString()
        
        def classification = "Expansion_" +radius+ "_um"

        
        geometryExpansion = originalGeom.buffer(+radiusPx)

        
        if(removeInside == true){
             geometryExpansion = geometryExpansion.difference(originalGeom)
        }else{
            geometryExpansion = geometryExpansion
        }
        

        //create the ROI
        def roi = GeometryTools.geometryToROI(geometryExpansion, plane)
        def annotation = PathObjects.createAnnotationObject(roi,getPathClass(classification))
        
        // Optionally set the name for tracking
        annotation.setName(originalObjIDStr)

        // Add the annotation to the list
        expansions.add(annotation)


    }

    addObjects(expansions)
    
    println("Expansions added!")

}





def addLongRadiusExpansions(geomMapWithRadius,removeInside){

    def plane = ImagePlane.getPlane(0, 0)
    def expansions = []


    geomMapWithRadius.each{ originalObjID, data ->

        def originalGeom = data.geometry
        def longRadius = data.radiusPx
        def originalObjIDStr = originalObjID.toString()
        def classification = "LongRadiusExpansion"

        
        geometryExpansion = originalGeom.buffer(+longRadius)

        
        if(removeInside == true){
             geometryExpansion = geometryExpansion.difference(originalGeom)
        }else{
            geometryExpansion = geometryExpansion
        }
        

        //create the ROI
        def roi = GeometryTools.geometryToROI(geometryExpansion, plane)
        def annotation = PathObjects.createAnnotationObject(roi,getPathClass(classification))
        
        // Optionally set the name for tracking
        annotation.setName(originalObjIDStr)

        // Add the annotation to the list
        expansions.add(annotation)


    }

    addObjects(expansions)
    
    println("Expansions added!")

}
