/*
 * DANEELpath project  is under   GNU GENERAL PUBLIC LICENSE, GPL-3.0 license
 * Please check https://github.com/iviecomarti/DANEELpath if you have any doubts about licensing
 *
 * 
 *  
 * Script to create a GUI for Peri Cluster segmentations
 *
 *
 *EXPANSION LIMITED TAB
 *
 * Uses the cell Clusters, create the influence zones (Voronoi from borders of the annotation).
 * Then the user can set an expansion of that cell Cluster, but this will limit with its Voronoi influence zones.
 *In this way, we can define a limited neighborhood for each cell cluster and compute the tVN_outside or the halo. 
 *
 * To limit the expansion of the voronoi Faces, we need to provide a REFERENCE annotation which contains WITHIN all the Regions of Interest 
 * If the annotations are not strictly within the reference, they will be ignored.
 * If the cell cluster does not have a classification, it will be ignored 
 *
 *
 *DETECT INSIDE EXPANSION TAB
 *
 * Facilitates to run run a pixel classifier created with QuPath. If the user wants to use a custom pixel classifer for the detections of the objects inside of the expansions, this script is ready to use
 * This tab also provides the option to access to the GUI autoTH QuPath: https://github.com/iviecomarti/GUI_AutoTH_QuPath
 *
 *
 *COMPUTE FEATURES TAB 
 *
 * Compute Width and Porportion of objects inside expansion.
 * 
 * Takes the expansions and objects created by the previous tab. 
 * 
 * Measurement width: computes the difference between the  radius of the merged object(cellCluster + expandedObject) , and the cell cluster radius
 * Measurement proportion: computes the proportion of the object inside the expansion with respect the cellCluster
 * 
 *
 * 
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
import qupath.lib.gui.tools.PaneTools
import javafx.scene.control.*
import qupath.lib.classifiers.pixel.PixelClassifier;
import qupath.lib.classifiers.pixel.PixelClassifierMetadata;
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent


Platform.runLater { buildStage().show()}

Stage buildStage() {
    def qupath = QuPathGUI.getInstance()
    
    def tabPane = new TabPane()
    
    def voronoiTab = new Tab("Expansion Limited")
    def voronoiLimitPane = new GridPane()
    
    row = 0
    
   tittlePane = new Label("Create expansions limiting with Voronoi Diagram ")
   tittlePane.setStyle("-fx-font-weight: bold")
   voronoiLimitPane.add(tittlePane, 0,row, 2, 1)
   
   row++
   def limitAnnotationCombo= new ComboBox<PathClass>(qupath.getAvailablePathClasses())
   limitAnnotationCombo.getSelectionModel().selectFirst()
   limitAnnotationCombo.setTooltip(new Tooltip("Set the annotation to limit Voronoi Diagram"))
   def limitAnnotationComboLabel = new Label("Select Annotation Stop Voronoi")
   voronoiLimitPane.add(limitAnnotationComboLabel,0,row,1,1)
   voronoiLimitPane.add(limitAnnotationCombo,1,row,1,1)
    
    row++
    def expansionSpinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.1, 1000, 10.0, 1.0));
    expansionSpinner.setEditable(true);
    FXUtils.restrictTextFieldInputToNumber(expansionSpinner.getEditor(), true);
    expansionSpinner.setTooltip(new Tooltip("Set the expansion microns"))
    def expansionSpinnerLabel = new Label("Microns to expand the Annotation")
    voronoiLimitPane.add(expansionSpinnerLabel,0,row,1,1)
    voronoiLimitPane.add(expansionSpinner,1,row,1,1)
    
    
    row++
    def expansionAnnotationCombo= new ComboBox<PixelClassifierMetadata>(qupath.getAvailablePathClasses())
   expansionAnnotationCombo.getSelectionModel().selectFirst()
   expansionAnnotationCombo.setTooltip(new Tooltip("Select the classification for the expansion"))
   def expansionAnnotationComboLabel = new Label("Select Expansion Classification")
   voronoiLimitPane.add(expansionAnnotationComboLabel,0,row,1,1)
   voronoiLimitPane.add(expansionAnnotationCombo,1,row,1,1)
    
    

    
    //add the checkbox for add the Voronoi Face
   row+=3
   addVoronoiFaceLabel = new Label("OPTIONAL : Add Voronoi Face")
   addVoronoiFaceLabel.setUnderline(true)
   voronoiLimitPane.add(addVoronoiFaceLabel, 0,row, 2, 1)
  
   row++
   def addVoronoiAnnotations= new CheckBox("Add Voronoi Face")
   addVoronoiAnnotations.setSelected(false)
   addVoronoiAnnotations.setTooltip(new Tooltip("If checked, will create the Voronoi Face annotaions"))
    
   voronoiLimitPane.add(addVoronoiAnnotations, 0, row, 1, 1)
    
    
    //Now create the button to run 
   row+=5
   def runActionBtn = new Button("Run Expansions Limited Voronoi")
   voronoiLimitPane.add(runActionBtn, 0, row, 2, 1)
   
   
   //ACTION OF THE BUTTON RUN EXPANSION LIMIT VORONOI
   runActionBtn.setOnAction { e->
   
   
       runExpansionWithVoronoiLimits(limitAnnotationCombo.getValue().toString(), expansionSpinner.getValue(), expansionAnnotationCombo.getValue().toString(), addVoronoiAnnotations.isSelected()) 
       Dialogs.showInfoNotification("Peri-cluster segmentation helpers", "Expansion Limit Voronoi Finished!")
      
   }
 
 
 
   
   voronoiLimitPane.setHgap(20)
    voronoiLimitPane.setVgap(10)
    voronoiLimitPane.setPadding(new Insets(10, 10, 10, 5))
    GridPaneUtils.setToExpandGridPaneWidth(expansionSpinner,limitAnnotationCombo,expansionAnnotationCombo,runActionBtn)
    voronoiTab.setContent(voronoiLimitPane)
   
   /////////////////////////////////
   //////DETECT INSIDE EXPANSIONS///
   /////////////////////////////////
   
   def detectTab = new Tab("Detect Inside Expansion")
   def detectPane = new GridPane()
    
   rowDetect = 0
   
   tittlePane = new Label("Detect Inside Expansions")
   tittlePane.setStyle("-fx-font-weight: bold")
   detectPane.add(tittlePane, 0,rowDetect, 2, 1)
   
   //select the expansion classification to run a pixel classifier
   rowDetect++
   def parentExpansionCombo= new ComboBox<PathClass>(qupath.getAvailablePathClasses())
   parentExpansionCombo.getSelectionModel().selectFirst()
   parentExpansionCombo.setTooltip(new Tooltip("Set the annotation to limit Voronoi Diagram"))
   def parentExpansionComboLabel = new Label("Select Expansion Classification")
   detectPane.add(parentExpansionComboLabel,0,rowDetect,1,1)
   detectPane.add(parentExpansionCombo,1,rowDetect,1,1)
   
   //list the pixel classifiers to run the pixel classifier
   rowDetect++
   def pixelClassifierNames = getProject().getPixelClassifiers().getNames()
   def pixelClassifierCombo= new ComboBox<>(FXCollections.observableArrayList(pixelClassifierNames))
   pixelClassifierCombo.setTooltip(new Tooltip("Select the pixel classifier"))
   def pixelClassifierComboLabel = new Label("Select Pixel Classifer")
   detectPane.add(pixelClassifierComboLabel,0,rowDetect,1,1)
   detectPane.add(pixelClassifierCombo,1,rowDetect,1,1)
   
   
   //spinner for the area and the holes
   rowDetect++
    def areaSpinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 10000, 0.0, 1.0));
    areaSpinner.setEditable(true);
    FXUtils.restrictTextFieldInputToNumber(areaSpinner.getEditor(), true);
    areaSpinner.setTooltip(new Tooltip("Set the minimum area filtering"))
    def areaSpinnerLabel = new Label("Minimum Area")
    detectPane.add(areaSpinnerLabel,0,rowDetect,1,1)
    detectPane.add(areaSpinner,1,rowDetect,1,1)
    
    rowDetect++
    def areaHolesSpinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 10000, 0.0, 1.0));
    areaHolesSpinner.setEditable(true);
    FXUtils.restrictTextFieldInputToNumber(areaHolesSpinner.getEditor(), true);
    areaHolesSpinner.setTooltip(new Tooltip("Set the minimum hole area"))
    def areaHolesLabel = new Label("Minimum Hole Area")
    detectPane.add(areaHolesLabel,0,rowDetect,1,1)
    detectPane.add(areaHolesSpinner,1,rowDetect,1,1)
    
    //list the output possibilities for pixel classifier
   rowDetect++
   def outputTypeOptions = ["Annotation","Detection"]
   def outputTypeCombo= new ComboBox<>(FXCollections.observableArrayList(outputTypeOptions))
   outputTypeCombo.getSelectionModel().selectFirst()
   outputTypeCombo.setTooltip(new Tooltip("Select the output type"))
   def outputTypeComboLabel = new Label("Select Output")
   detectPane.add(outputTypeComboLabel,0,rowDetect,1,1)
   detectPane.add(outputTypeCombo,1,rowDetect,1,1)
   
    //Now create the button to run 
   rowDetect+=3
   def runPixelClassifierBtn = new Button("Run Pixel Classifier")
   detectPane.add(runPixelClassifierBtn, 0, rowDetect, 2, 1)
   
   
   //ACTION RUN PIXEL CLASSIFIER
   runPixelClassifierBtn.setOnAction { e->
   
       actionRunPixelClassifier(parentExpansionCombo.getValue().toString(), pixelClassifierCombo.getValue(),outputTypeCombo.getValue(),areaSpinner.getValue(), areaHolesSpinner.getValue() )
       Dialogs.showInfoNotification("Peri-cluster segmentation helpers", "Pixel Classifier Finished!")
   }
   
   
   
  
  
  //alternative GUI auto-TH
  
   //add the checkbox for add the Voronoi Face
   rowDetect+=2
   guiAutoTH = new Label("OPTIONAL : Use GUI Auto-Threshold Methods")
   guiAutoTH.setUnderline(true)
   detectPane.add(guiAutoTH, 0,rowDetect, 2, 1)
  
  rowDetect+=1
  def copyLinkButton = new Button("Copy to Clipboard ")
  def copyLinkLabel = new Label("Copy Link GUI Auto-Threshold ")
  detectPane.add(copyLinkButton, 1, rowDetect, 1, 1)
  detectPane.add(copyLinkLabel, 0, rowDetect, 1, 1)
  
  def link = "https://github.com/iviecomarti/GUI_AutoTH_QuPath"
   // Set an action for the button
    copyLinkButton.setOnAction { event ->
        // Create a ClipboardContent instance
        def clipboardContent = new ClipboardContent()
    
        // Set the link to the content
        clipboardContent.putString(link)
    
        // Get the system clipboard
        def clipboard = Clipboard.getSystemClipboard()
    
        // Set the content to the clipboard
        clipboard.setContent(clipboardContent)
    
        // You can also show a message or log the action
        //println "Link copied to clipboard: $link"
    }
    
    
    
    
   
   detectPane.setHgap(20)
    detectPane.setVgap(10)
    detectPane.setPadding(new Insets(10, 10, 10, 5))
    GridPaneUtils.setToExpandGridPaneWidth(parentExpansionCombo,pixelClassifierCombo,areaSpinner,areaHolesSpinner,outputTypeCombo,runPixelClassifierBtn,copyLinkButton)
    detectTab.setContent(detectPane)
   
   
   
   ///////////////////////////////////
   //////COMPUTE FEATURES SECRETION///
   ///////////////////////////////////
   
   def featuresTab = new Tab("Compute features")
   def featuresPane = new GridPane()
    
   rowFeatures = 0
   
   tittleFeaturesPane = new Label("Detect Width and Proportion")
   tittleFeaturesPane.setStyle("-fx-font-weight: bold")
   featuresPane.add(tittleFeaturesPane, 0,rowFeatures, 2, 1)
   
   //select the expansion classification to check objects inside
   rowFeatures++
   def parentFeaturesCombo= new ComboBox<PathClass>(qupath.getAvailablePathClasses())
   parentFeaturesCombo.getSelectionModel().selectFirst()
   parentFeaturesCombo.setTooltip(new Tooltip("Select the expnasion classification"))
   def parentFeaturesComboLabel = new Label("Select Expansion Classification")
   featuresPane.add(parentFeaturesComboLabel,0,rowFeatures,1,1)
   featuresPane.add(parentFeaturesCombo,1,rowFeatures,1,1)
   
   //select the classification of the object inside
   rowFeatures++
   def objectInsideCombo= new ComboBox<PathClass>(qupath.getAvailablePathClasses())
   objectInsideCombo.getSelectionModel().selectFirst()
   objectInsideCombo.setTooltip(new Tooltip("Select the classification of the object inside expansion"))
   def objectInsideComboLabel = new Label("Select Target Classification")
   featuresPane.add(objectInsideComboLabel,0,rowFeatures,1,1)
   featuresPane.add(objectInsideCombo,1,rowFeatures,1,1)
   
   
    
   
   rowFeatures+=3
   removeExpansionsLabel = new Label("OPTIONAL : Remove Expansions After Measurements")
   removeExpansionsLabel.setUnderline(true)
   featuresPane.add(removeExpansionsLabel, 0,rowFeatures, 2, 1)
  
   rowFeatures++
   def removeExpansions= new CheckBox("Remove Expansions")
   removeExpansions.setSelected(false)
   removeExpansions.setTooltip(new Tooltip("If checked, will remove the expansions"))
    
   featuresPane.add(removeExpansions, 0, rowFeatures, 1, 1)
    
    
    //Now create the button to run 
   rowFeatures+=5
   def runComputeFeatures= new Button("Compute Features")
   featuresPane.add(runComputeFeatures, 0, rowFeatures, 2, 1)
   
   
   runComputeFeatures.setOnAction { e->
       
        runComputeWidthAndProportionInExpansion(parentFeaturesCombo.getValue().toString(),objectInsideCombo.getValue().toString(),removeExpansions.isSelected())
        Dialogs.showInfoNotification("Peri-cluster segmentation helpers", "Compute Features Finished!")
   }
   
   
   
   featuresPane.setHgap(20)
    featuresPane.setVgap(10)
    featuresPane.setPadding(new Insets(10, 10, 10, 5))
    GridPaneUtils.setToExpandGridPaneWidth(parentFeaturesCombo,objectInsideCombo,runComputeFeatures)
    featuresTab.setContent(featuresPane)
   
   


    tabPane.tabs.addAll(voronoiTab,detectTab,featuresTab)
    def scene = new Scene(tabPane,400,375)
    def stage = new Stage()
    stage.setTitle("Peri-cluster segmentation helpers")
    stage.setScene(scene)
    stage.initOwner(qupath.getStage())
    return stage
}









/////////////////////////////////////////
//////////EXPANSION VORONOI FUNCTIONS////
////////////////////////////////////////


//Main Function
def runExpansionWithVoronoiLimits(referenceAnnotationString, expansionMicrons, expansionClassificationString, addVoronoiFaces) {
   
   def referenceCheckpoint = getAnnotationObjects().findAll {it.getPathClass() == getPathClass(referenceAnnotationString)}
   
   // Check if the number of reference annotations is greater than 1
    if (referenceCheckpoint.size() > 1) {
        // Throw a message if more than one reference annotation is found
        Dialogs.showErrorMessage("Error ReferenceAnnotation","ERROR: There is more than one reference annotation, please merge the reference annotations into one object.")
        return  
    }
    
    if(referenceCheckpoint.size() == 0) {
        Dialogs.showErrorMessage("Error ReferenceAnnotation","ERROR: There is not reference annotation. \n Please create a reference Annotation, and check its classification") 
        return 
    }
   
   
   println "Voronoi Diagram started"
   //create the faces
   voronoiFaces = createVoronoiFaceWithoutClassification(referenceAnnotationString)
   
   println "Voronoi Faces created, expansion starts"
   //count the neighbours
   
   createExpansionsLimitVoronoi(voronoiFaces,expansionMicrons,expansionClassificationString)
   
   //add the faces
   if(addVoronoiFaces == true) {
      addVoronoiAnnotations(voronoiFaces)
      
   }
   
   print("Done")
   
}



import qupath.lib.analysis.DelaunayTools
import qupath.lib.objects.PathObjects



//Create Voronoi Faces Using QuPath DelaunayTools 
def createVoronoiFaceWithoutClassification(referenceAnnotationString) {
    
   def reference = getAnnotationObjects().findAll {it.getPathClass() == getPathClass(referenceAnnotationString)}
   
   // Check if the number of reference annotations is greater than 1
    if (reference.size() > 1) {
        // Throw a message if more than one reference annotation is found
        println "ERROR: There is more than one reference annotation, please merge the reference annotations into one object."
        return 
    }
    
    if(reference.size() == 0) {
       println "ERROR: There is not reference annotation. Please create a reference Annotation"
        return 
    }
    
    //Here makes sense to do [0], since we have the check if it is more than one
   def referenceGeom = reference[0].getROI().getGeometry()
    
    
   //Check the annotations inside the reference. avoid null classifications  
   def annoInterest = getAnnotationObjects().findAll {
       it.getPathClass() != null && 
       it.getPathClass() != getPathClass(referenceAnnotationString) &&
       it.getROI().getGeometry().within(referenceGeom) == true
       }
   
   //Check the annotations outside the reference or without classification
   def annoExcludedFormTheAnalysis = getAnnotationObjects().findAll {
       (it.getPathClass() != getPathClass(referenceAnnotationString) && it.getROI().getGeometry().within(referenceGeom) == false) | 
        it.getPathClass() == null
       
       }
   
   //log if annotations are not included in the analysis
   if(annoExcludedFormTheAnalysis.size() > 0) {
       
       print("A total of " + annoExcludedFormTheAnalysis.size()+ " annotations have been excluded from the analysis because they are not inside reference or because they are unclassified")
       annoExcludedFormTheAnalysis.forEach {
           
           print("Annotation with ID: " +it.getID() + " excluded" )
           
       }
       
   }
   
   
   //create the voronoi faces
   def voronoiDiagram = DelaunayTools.createFromGeometryCoordinates(annoInterest,false,4.0).getVoronoiFaces()
   
   
    //Create Voronoi annotations, limit the expansion and set the class of its origin annotation
    voronoiDiagram = voronoiDiagram.collectEntries { annotationObj, voronoiFace ->
    
        // Get the class and ID of the original object

        def originalObjID = annotationObj.getID().toString()

        // Calculate the intersection of the voronoiFace and the reference geometry
        def intersect = voronoiFace.intersection(referenceGeom)

        // Create a map entry with the originalObjID as key, and the geometry
        [(originalObjID): [geometry: intersect, classification: getPathClass("VoronoiFace")] ]
    } 
        
    return voronoiDiagram
}



//Limit the expansions with the Voronoi Faces using Java Topology Suite.
def createExpansionsLimitVoronoi(voronoiFaceMap, expansionMicrons,expansionClassification) {
   
   def imageData = getCurrentImageData()
   def server = imageData.getServer()
   def cal = server.getPixelCalibration()
   def plane = ImagePlane.getPlane(0, 0)
   
   //transform microns to pixels
   expansionPixels = expansionMicrons/ cal.getPixelWidthMicrons()
    
   def expansionsLimitVoronoi = []
   
   voronoiFaceMap.each {originalObjID, geom ->
   
       voronoiGeom = geom.geometry
       originalObjID = originalObjID.toString()
       
       
       //select the original objectID
       targetCluster = getAnnotationObjects().find {
          it.getID().toString() == originalObjID 
       }
       
       
       //obtain the geometry of the original object and expand the amount of distance given. 
       targetClusterGeom = targetCluster.getROI().getGeometry()
       
       //expand pixels
       geomClusterExpanded = targetClusterGeom.buffer(+expansionPixels)
       
       //remove the original cluster to obtain a donut-like shape
       geomClusterExpanded = geomClusterExpanded.difference(targetClusterGeom)
       
       //limit the expansion of the donut to the voronoi border
       geomClusterExpanded = geomClusterExpanded.intersection(voronoiGeom)
       
       
       //create the ROI, then the annotation with the classification, the original objectID and put to the list. 
       expansionROI = GeometryTools.geometryToROI(geomClusterExpanded, plane)
       expansionAnnotation = PathObjects.createAnnotationObject(expansionROI)
       expansionAnnotation.setPathClass(getPathClass(expansionClassification))
       expansionAnnotation.setName(originalObjID)
       
       expansionsLimitVoronoi.add(expansionAnnotation)
       
   }
   
   addObjects(expansionsLimitVoronoi)
   
   
   
}





//create a function to add the voronoi annotations if needed.
def addVoronoiAnnotations(voronoiFaceMap) {
   
    def plane = ImagePlane.getPlane(0, 0)
    
    // Create an empty list 
    def annotationsList = []
    
    // Iterate over the voronoiFaceMap and create annotations
    voronoiFaceMap.each { originalObjID, data ->
        def geom = data.geometry
        def classificationOringialObject = data.classification.toString()
        def originalObjIDStr = originalObjID.toString()

        // Create an annotation object from the geometry and classification
        def roi = GeometryTools.geometryToROI(geom, plane)
        def annotation = PathObjects.createAnnotationObject(roi,getPathClass(classificationOringialObject))
        
        // Optionally set the name for tracking
        annotation.setName(originalObjIDStr)

        // Add the annotation to the list
        annotationsList.add(annotation)
    }
    
    // Add the list of annotations to the project
    addObjects(annotationsList)
    
    println("Annotations added successfully!")
}




////////////////////////////////////////
/////////RUN PIXEL CLASSIFIER FUNCITON//
////////////////////////////////////////


//Simple function just in case that the users preferes detecitons instead of annotations as output.
// NOTE: Here i remove the post-processing options of "SPLIT", "DELETE_EXISTING", "INCLUDE_IGNORED", "SELECT_NEW"
 //the main reason is because for this pipeline they are not neede. Is assumed that the objects inside the expansions must be one object.
 //if i let the option of SPLIT, someone could try it and then the next script will not work. So I prefer to remove them just in case.

 def actionRunPixelClassifier(parentClassification, classifier,outputObjectType,minArea, minHoleArea ) {

      objectsOfInterest = getAllObjects().findAll {
         it.getPathClass() == getPathClass(parentClassification) 
      }

     if(objectsOfInterest  == null){
                println "ERROR: No parent classifications selected, please check it"
         }  


     
    if (outputObjectType == "Annotation") {
        
        selectObjects(objectsOfInterest)
        createAnnotationsFromPixelClassifier(classifier, minArea, minHoleArea)
    }
    if (outputObjectType == "Detection") {
            
            selectObjects(objectsOfInterest)
            createDetectionsFromPixelClassifier(classifier, minArea, minHoleArea)
    }

   println "Pixel classifier finished!" 
  
   
 }




////////////////////////////////////////
////COMPUTE WIDTH AND PROPORTION///////
//////////////////////////////////////




//Main Function
def runComputeWidthAndProportionInExpansion(expansionClassification,objectInsideClassification,deleteExpansionObject) {
    
    
    
    // Get the main QuPath data structures
    def imageData = getCurrentImageData()
    def server = imageData.getServer()
    def cal = server.getPixelCalibration()

    //check if pixel size is present
    if (!cal.hasPixelSizeMicrons()) {
        
        Dialogs.showErrorMessage("Error calibration","ERROR: We need the pixel size information here!")
    return
    }
    
    
    //confirm objects are present
    def expansionCheck = getAnnotationObjects().findAll {it.getPathClass() == getPathClass(expansionClassification)}   
    if(expansionCheck.size() == 0) {
       
       Dialogs.showErrorMessage("Error calibration","ERROR: There is not expansion annotation.\nPlease check the classification")
        return 
    }
    
   
    
    
    println "Check expansions with/without objects inside"
    valuesFirstFunction = extractGeometriesObjectsInsideExpansions(expansionClassification, objectInsideClassification)


    //set the name of the measurements 
   measurementWidthName = "Width_" + objectInsideClassification + "_um" 
   measurementProportionName = "Proportion_" + objectInsideClassification
   
    println "Put measurements in empty expansions"
    putMeasurementsToClustersWithoutObjectInExpansion(valuesFirstFunction.emptyExpansions,measurementWidthName,measurementProportionName)
    
    
    println "Put measurements in expansions with objects"
    putMeasurementsToClustersWithObjectsInExpansion(valuesFirstFunction.expansionsWithObjects,measurementWidthName, measurementProportionName)
    
    
    
    if(deleteExpansionObject == true) {
       
       println "Expansion annotations deleted"
       toDel = getAllObjects().findAll {
          it.getPathClass() ==  getPathClass(expansionClassification)
       }
       
       removeObjects(toDel,true)
    }
    
    println "Done!"
   
}




import org.locationtech.jts.geom.Geometry
import qupath.lib.common.GeneralTools
import qupath.lib.objects.PathObject
import qupath.lib.objects.PathObjects
import qupath.lib.roi.GeometryTools
import qupath.lib.roi.ROIs
import org.locationtech.jts.algorithm.MinimumBoundingCircle
import static qupath.lib.gui.scripting.QPEx.*





def extractGeometriesObjectsInsideExpansions(expansionClassification, objectInsideClassification) {
    
    expansion = getAnnotationObjects().findAll {
        //if you have an expansion without the parent name, for whatever reason, remove from the analysis
       it.getPathClass() == getPathClass(expansionClassification) && it.getName()!= null
    }
    print("Number of expansions in the analysis: " + expansion.size())
    
    
    //this is an empty list to store the IDstrings of the original objects(clusters here) wthout anything inside the expansion
    def emptyExpansions = []
    
    //this is a map with the originalObjectID and 
    def expansionsWithObjects = [:] 
    
    expansion.forEach { expansion->
       
       originalClusterID = expansion.getName().toString()
       expansionGeom = expansion.getROI().getGeometry()
       
       
       objectInside = getAllObjects(false).find {
       it.getPathClass() == getPathClass(objectInsideClassification) &&
       it.getROI().getGeometry().buffer(-0.01).within(expansionGeom) // Tiny negative buffer to exclude boundary because if the target touches the expansion border will not be considered as within
       
        }
    
    if(objectInside == null) {
       emptyExpansions.add(originalClusterID)
       
    }else {
        
        objectInside.setName(originalClusterID)
        objectInsideGeom = objectInside.getROI().getGeometry()
        
        expansionsWithObjects[originalClusterID] = objectInsideGeom
      }
    
       
    }
    print("Number of expansions without objects within: " + emptyExpansions.size())
    
     return [emptyExpansions: emptyExpansions, expansionsWithObjects: expansionsWithObjects]
    
}





def putMeasurementsToClustersWithObjectsInExpansion(objectsGeometryMap,measurementWidthName, measurementProportionName) {
   
   def imageData = getCurrentImageData()
    
   def server = imageData.getServer()

   def cal = server.getPixelCalibration()
   
   
   
   objectsGeometryMap.each {originalObjectID, geomObjectExpansion ->
       
       
       originalObject = getAllObjects().find {
          it.getID().toString() == originalObjectID 
       }
       
       originalObjectGeometry = originalObject.getROI().getGeometry()

       mergeOriginalAndCorona = geomObjectExpansion.union(originalObjectGeometry)
       
       //extract the radius of the original cluster and the merged geometry
       radiusOriginalGeometryPx = computeRadiusOfGeometry(originalObjectGeometry)
       radiusMergeGeometryPx = computeRadiusOfGeometry(mergeOriginalAndCorona)
       
       //compute the width in microns
       widthPixels = radiusMergeGeometryPx - radiusOriginalGeometryPx
       widthMicrons = widthPixels * cal.getPixelWidthMicrons()
       
       //Compute the proportion of the object in expansion and the original object
       proportionOutsideToCluster = geomObjectExpansion.getArea() / originalObjectGeometry.getArea()
       
       originalObject.getMeasurementList().put(measurementWidthName, widthMicrons)
       originalObject.getMeasurementList().put(measurementProportionName, proportionOutsideToCluster)
       
      
      
   }
   
   
}





//If there is nothing inside of the expansion, put the measurements to 0 for consistency when exporting the data. 
def putMeasurementsToClustersWithoutObjectInExpansion(emptyExpansions,measurementWidthName, measurementProportionName) {
   
   emptyExpansions.forEach { emptyExpansion ->
      
      emptyObject = getAllObjects(false).find {
         it.getID().toString() ==  emptyExpansion
      }
      
      
      emptyObject.getMeasurementList().put(measurementWidthName, 0)
      emptyObject.getMeasurementList().put(measurementProportionName, 0)
      
   }
   
   
}





def computeRadiusOfGeometry(geometry) {
     
   //create the MBC of a geometry and divide it by 2. This will give you the radius in pixels
   def MBC = new MinimumBoundingCircle(geometry)
   radiusPx = MBC.getRadius()
      
   return radiusPx
   
}










