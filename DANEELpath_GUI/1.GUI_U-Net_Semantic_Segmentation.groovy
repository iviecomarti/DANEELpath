/*
 * DANEELpath project  is under   GNU GENERAL PUBLIC LICENSE, GPL-3.0 license
 * Please check https://github.com/iviecomarti/DANEELpath if you have any doubts about licensing
 *
 *
 * SEGMENTATION TAB
 *GUI to run Cell Cluster Semantic Segmentation Models.
 * 
 * Previous to run the GUI, please make sure the path to the conda Python EXE
 * and the path to the folder where the model checkpoints and inference script are located is correct
 *
 * The parameters: 
 * daneelSegmentationFolder: is the path to the folder where the model checkpoints and inference script are located
 * pathPythonEnviromentEXE: is the path to the conda Python EXE. 
 *
 * To obtain "pathPythonEnviromentEXE" path, activate the enviroment and run in the terminal:
 *     -Windows: where python
 *     -macOS, Linux: which python
 * 
 * The settings are the same as the Script Version. 
 * 
 * Once you run the script, you can close the script editor;
 * 
 * 
 * We are using the Virtual Enviroment Runner from Cellpose extension of BIOP to run the python script from QuPath
 * The repo for Cellpose extension is:  https://github.com/BIOP/qupath-extension-cellpose
 * NOTE: this is not a  Cellpose, model we use a functionality of the extension, so you just need to install at least this extension.
 * 
 * The neural network architechture is a U-Net with a Efficient-NetB4 backbone.
 * The packages to create the model are: segmentation-models-pytorch, pytorch_lightning.
 * 
 * We provide two model checkpoints defined in "clusterModelStain":
 *     -"HE": segments cell clusters in HE stained Hidrogels
 *     -"VN": segments cell clusters in Vitronectin (DAB) stained Hidrogels
 * 
 * 
 * The model is trained on 256x256 tiles
 * Parameter "tilePadding": Adjust the amount of overlap between the tiles. If it is less than 10 or more than 250  the script will not run.
 *      
 * 
 * Since we are running a Python Script, supports CUDA GPU if aviable. If not, will use cpu
 * Parameter: "batch_size": Ajust the amount of images pushed to the GPU in each iteration. The default batch_size is 8.
 * 
 * POSTPROCESSING TAB
 *
 * Postprocessing Semantic segmentation
 * 
 * Does a small post processing to the load mask from the neural network inference
 * 
 * Parameters:
 * 
 * fillHoles: fill the holes inside the hole annotation
 *
 * minArea: objecst with less area in um2 are deleted
 * 
 * maxEccentricity: objects with more eccentricity are deleted
 * 
 * minSolidity: objects with less solidity are deleted
 *
 *
  * WATERSHED TAB
 * 
 * Applies the watershed algorithm to the merged clusters to split them
 * 
 * Parameters:
 * 
 * inputClassificationObjectsString: classification of the objects to split
 * outputClassificationString: classification for the output objects
 * 
 * removeInputObjects: true/false. if you want to remove or keep the original objects
 * 
 * @author: Isaac Vieco-Marti
 * 
 */


//modify the paths if needed
def pythonElements() {
    
    def daneelFolder = "path/DANEELpath/folder"
    def pathPythonEnviromentEXE = "path/python.exe"
    
    return [modelsFolder:daneelFolder,exePath:pathPythonEnviromentEXE]
    
}






////////////////////////////////////////////
////////DO NOT TOUCH FROM HERE//////////////
////////////////////////////////////////////





import javafx.util.Duration
import javafx.animation.PauseTransition
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.VBox
import javafx.stage.Stage
import qupath.lib.gui.QuPathGUI
import qupath.fx.dialogs.Dialogs
import qupath.lib.gui.tools.PaneTools
import static qupath.lib.gui.scripting.QPEx.*
import qupath.fx.utils.FXUtils
import javafx.collections.FXCollections
import qupath.lib.gui.commands.LogViewerCommand
import javafx.application.Platform
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.ButtonType
import javafx.concurrent.Task
import javafx.scene.layout.HBox
import javafx.geometry.Insets



Platform.runLater { buildStage().show() }

Stage buildStage() {
    def qupath = QuPathGUI.getInstance()
    
    // Create a TabPane
    def tabPane = new TabPane()
    
    ////////////////////
    /////VORONOI PANE//
    ///////////////////
    def segmentationTab = new Tab("Semantic Segmentation")
    def segmentationPane = new GridPane()
    
    rowSegmentation = 0
    rowSegmentation++

   segmentationTittle = new Label("Cell Clusters Semantic Segmentation U-Net")
   segmentationTittle.setStyle("-fx-font-weight: bold")
   segmentationPane.add(segmentationTittle, 0,rowSegmentation, 2, 1)

    def parentClassificationCombo = new ComboBox<PathClass>(qupath.getAvailablePathClasses())
    parentClassificationCombo.getSelectionModel().selectFirst()
    parentClassificationCombo.setTooltip(new Tooltip("Set the classification of the parent annotation"))
    def parentClassificationComboLabel = new Label("Parent Classification")
    
    rowSegmentation++
    segmentationPane.add(parentClassificationComboLabel,0,rowSegmentation,1,1)
    segmentationPane.add(parentClassificationCombo,1,rowSegmentation,1,1)
    
    
    // select the model 
   def modelOptions = ["HE","VN"]
   def stainingCombo= new ComboBox<>(FXCollections.observableArrayList(modelOptions))
   stainingCombo.getSelectionModel().selectFirst()
   stainingCombo.setTooltip(new Tooltip("Select the Staining to load the segmentation model"))
   def stainingComboLabel = new Label("Select Model Staining")
   
   rowSegmentation++
   segmentationPane.add(stainingComboLabel,0,rowSegmentation,1,1)
   segmentationPane.add(stainingCombo,1,rowSegmentation,1,1)
   
   
   //tilepadding

   def paddingOptions = [16,32,48,64,80,96]
   def paddingCombo= new ComboBox<>(FXCollections.observableArrayList(paddingOptions))
   paddingCombo.getSelectionModel().selectFirst()
   paddingCombo.setTooltip(new Tooltip("Select the padding of the tiles.\nMore Padding will create more overlaped tiles"))
   def paddingComboLabel = new Label("Select Tiles Padding")
   
   rowSegmentation++
   segmentationPane.add(paddingComboLabel,0,rowSegmentation,1,1)
   segmentationPane.add(paddingCombo,1,rowSegmentation,1,1)
    
    
   //batch_size
   def batchSizeOptions = [8,16,32]
   def batchSizeCombo= new ComboBox<>(FXCollections.observableArrayList(batchSizeOptions))
   batchSizeCombo.getSelectionModel().selectFirst()
   batchSizeCombo.setTooltip(new Tooltip("Select the Batch Size for PyTorch Dataloader"))
   def  batchSizeComboLabel = new Label("Select Batch Size")
   
   rowSegmentation++
   segmentationPane.add(batchSizeComboLabel,0,rowSegmentation,1,1)
   segmentationPane.add(batchSizeCombo,1,rowSegmentation,1,1)
    
    
    //outputSegmentationClassification
    def outputClassificationCombo = new ComboBox<PathClass>(qupath.getAvailablePathClasses())
    outputClassificationCombo.getSelectionModel().selectFirst()
    outputClassificationCombo.setTooltip(new Tooltip("Set the classification of the output annotation"))
    def outputClassificationComboLabel = new Label("Output Classification")
    
    rowSegmentation++
    segmentationPane.add(outputClassificationComboLabel,0,rowSegmentation,1,1)
    segmentationPane.add(outputClassificationCombo,1,rowSegmentation,1,1)
    
       
   
    
    
    //Now create the button to run 
    rowSegmentation+=3
    def btnRunSegmentation = new Button("Run U-Net")
    btnRunSegmentation.setTooltip(new Tooltip("Run U-Net semantic Segmentation"))
    segmentationPane.add(btnRunSegmentation, 0, rowSegmentation, 2, 1)
    //this is for display
    GridPaneUtils.setToExpandGridPaneWidth(parentClassificationCombo,stainingCombo,paddingCombo, batchSizeCombo ,outputClassificationCombo,btnRunSegmentation)
    segmentationPane.setHgap(20)
    segmentationPane.setVgap(10)
    segmentationPane.setPadding(new Insets(10, 10, 10, 5)) 

    segmentationTab.setContent(segmentationPane)
    
    
    //set the action of the voronoi Button:
    
   btnRunSegmentation.setOnAction { e ->

        // The log window and related commands are no longer declared or used

        // Define the task for the long-running operation
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    // Long running task
                    def pythonFeatures = pythonElements()

                    runSemanticSegmentation(
                        parentClassificationCombo.getValue().toString(),
                        outputClassificationCombo.getValue().toString(),
                        stainingCombo.getValue().toString(),
                        paddingCombo.getValue(),
                        batchSizeCombo.getValue(),
                        pythonFeatures.modelsFolder,
                        pythonFeatures.exePath
                    )
                } catch (Exception ex) {
                    // Log or print the error
                    System.err.println("Error during segmentation: ${ex.message}")
                    throw ex  // Rethrow the exception so the `failed` method is called
                }
                return null
            }

            @Override
            protected void succeeded() {
                // Notify success without showing a log window
                Platform.runLater {
                    Dialogs.showInfoNotification("DANEEL: Segment Cell Clusters", "Semantic Segmentation Finished!")
                }
            }

            @Override
            protected void failed() {
                // Notify failure without showing a log window
                Platform.runLater {
                    Dialogs.showErrorNotification("Error", "Segmentation process failed! Please check the annotations and try again.")
                }
            }
        }

        // Start the task in a background thread
        new Thread(task).start()
    }

    
    
    
    
    
    
    
    ////////////////////////
    ////POSTPROCESSING TAB//
    ////////////////////////
    
    def postTab = new Tab("Post-Processing")
    def postPane = new GridPane()
    
    rowPost = 0
    rowPost++

   postTittle = new Label("Post-Processing U-Net")
   postTittle.setStyle("-fx-font-weight: bold")
   postPane.add(postTittle, 0,rowPost, 2, 1)

    def outputPostCombo = new ComboBox<PathClass>(qupath.getAvailablePathClasses())
    outputPostCombo.getSelectionModel().selectFirst()
    outputPostCombo.setTooltip(new Tooltip("Set the classification of the parent annotation"))
    def outputPostComboLabel = new Label("Output Classification")
    
    rowPost++
    postPane.add(outputPostComboLabel,0,rowPost,1,1)
    postPane.add(outputPostCombo,1,rowPost,1,1)
   
    rowPost++
    def minAreaSpinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.1, 100000, 80.0, 5.0));
    minAreaSpinner.setEditable(true);
    FXUtils.restrictTextFieldInputToNumber(minAreaSpinner.getEditor(), true);
    minAreaSpinner.setTooltip(new Tooltip("Set the minimum Area"))
    def minAreaSpinnerLabel = new Label("Min Area microns^2")
    postPane.add(minAreaSpinnerLabel,0,rowPost,1,1)
    postPane.add(minAreaSpinner,1,rowPost,1,1)
   
   
    rowPost++
    def maxEccentricity = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.1, 1, 0.9, 0.1));
    maxEccentricity.setEditable(true);
    FXUtils.restrictTextFieldInputToNumber(maxEccentricity.getEditor(), true);
    maxEccentricity.setTooltip(new Tooltip("Set the maximum eccentrcity of the annotations\nMore eccentricity means more elongated objects"))
    def maxEccentricityLabel = new Label("Max Eccentricity")
    postPane.add(maxEccentricityLabel,0,rowPost,1,1)
    postPane.add(maxEccentricity,1,rowPost,1,1)
    
    rowPost++
    def minSolidity = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.1, 1, 0.3, 0.1));
    minSolidity.setEditable(true);
    FXUtils.restrictTextFieldInputToNumber(minSolidity.getEditor(), true);
    minSolidity.setTooltip(new Tooltip("Set the minimum Solidity of the objects \nLow Solidity objects removed"))
    def minSolidityLabel = new Label("Min Solidity")
    postPane.add(minSolidityLabel,0,rowPost,1,1)
    postPane.add(minSolidity,1,rowPost,1,1)
    
    rowPost++
   def fillHolesCheckbox= new CheckBox("Fill Annotation Holes")
   fillHolesCheckbox.setSelected(true)
   fillHolesCheckbox.setTooltip(new Tooltip("If checked, will fill the holes of the annotations"))
    
   postPane.add(fillHolesCheckbox, 0, rowPost, 1, 1)
   
   
   
    //Now create the button to run 
    rowPost+=3
    def btnPostProcessing = new Button("Run Post-Processing")
    btnRunSegmentation.setTooltip(new Tooltip("Run Post-Processing"))
    postPane.add(btnPostProcessing, 0, rowPost, 2, 1)
    
    btnPostProcessing.setOnAction { e ->
    
        runPostProcessingSegmentation(outputPostCombo.getValue().toString(),
        fillHolesCheckbox.isSelected(),
        minAreaSpinner.getValue(),
        maxEccentricity.getValue(),
        minSolidity.getValue()
        )
        
        Dialogs.showInfoNotification("DANEEL: Segment Cell Clusters", "Post-Processing Finished!")
        
    }
   
    
    GridPaneUtils.setToExpandGridPaneWidth(outputPostCombo,minAreaSpinner,maxEccentricity,minSolidity,btnPostProcessing)
    postPane.setHgap(20)
    postPane.setVgap(10)
    postPane.setPadding(new Insets(10, 10, 10, 5)) 

    postTab.setContent(postPane)
    
    
    
    //////////////////////////
    ////WATERSHED TAB////////
    ////////////////////////
    
    def watershedTab = new Tab("Watershed")
    def watershedPane = new GridPane()
    
    rowWater = 0
    rowWater++

   waterTittle = new Label("Watershed Post-Processing")
   waterTittle.setStyle("-fx-font-weight: bold")
   watershedPane.add(waterTittle, 0,rowWater, 2, 1)

    def inputWaterCombo = new ComboBox<PathClass>(qupath.getAvailablePathClasses())
    inputWaterCombo.getSelectionModel().selectFirst()
    inputWaterCombo.setTooltip(new Tooltip("Set the classification of the original object"))
    def inputWaterComboLabel = new Label("Input Classification")
    
    rowWater++
    watershedPane.add(inputWaterComboLabel,0,rowWater,1,1)
    watershedPane.add(inputWaterCombo,1,rowWater,1,1)
    
    def outputWaterCombo = new ComboBox<PathClass>(qupath.getAvailablePathClasses())
    outputWaterCombo.getSelectionModel().selectFirst()
    outputWaterCombo.setTooltip(new Tooltip("Set the classification for the output annotations"))
    def outputWaterComboLabel = new Label("Output Classification")
    
    rowWater++
    watershedPane.add(outputWaterComboLabel,0,rowWater,1,1)
    watershedPane.add(outputWaterCombo,1,rowWater,1,1)
    
    
    rowWater++
   def removeOriginalCheckbox= new CheckBox("Remove Input Objects")
   removeOriginalCheckbox.setSelected(false)
   removeOriginalCheckbox.setTooltip(new Tooltip("If checked, will remove the input objects"))
    
   watershedPane.add(removeOriginalCheckbox, 0, rowWater, 1, 1)
   
   
   
    //Now create the button to run 
    rowWater+=5
    def btnWatershed = new Button("Run Watershed on Clusters")
    btnWatershed.setTooltip(new Tooltip("Run Watershed"))
    watershedPane.add(btnWatershed, 0, rowWater, 2, 1)
    
    
    btnWatershed.setOnAction{ e->
        
        runWatershedPostprocessingAnnotations(inputWaterCombo.getValue().toString(),outputWaterCombo.getValue().toString(),removeOriginalCheckbox.isSelected())        
        Dialogs.showInfoNotification("DANEEL: Segment Cell Clusters", "Watershed Finished!")
    }
    
    
    
    GridPaneUtils.setToExpandGridPaneWidth(inputWaterCombo,outputWaterCombo,btnWatershed)
    watershedPane.setHgap(20)
    watershedPane.setVgap(10)
    watershedPane.setPadding(new Insets(10, 10, 10, 5)) 

    watershedTab.setContent(watershedPane)
    
    
    
    
    
    // Add all tabs to TabPane
    tabPane.tabs.addAll(segmentationTab,postTab,watershedTab)
    
    // Create the scene and add the tabPane to it
    def scene = new Scene(tabPane, 350, 340)
    def stage = new Stage()
    stage.setTitle("DANEEL: Segment Cell Clusters")
    stage.setScene(scene)
    stage.initOwner(qupath.getStage())
    
    return stage
}





//////////////////////////////////////////////
//////FUNCTIONS TO RUN SEMANTIC SEGMENTATION///
///////////////////////////////////////////////



def runSemanticSegmentation(parentClassificationString,outputClassificationString,clusterModelStain,tilePadding,batch_size,daneelSegmentationFolder,pathPythonEnviromentEXE) {
   
   //create temporalFolder
   folders = createTemporalFolderStructure()
   
   //export parent Tiles
   createTilesParentObjects(parentClassificationString,tilePadding, folders.tiles)
   
   //run the inference in python
   runSMPInference(pathPythonEnviromentEXE,clusterModelStain,daneelSegmentationFolder,folders.mainFolder,batch_size)
   
   println "Inference completed, importing the masks"
   
   //import the masks
   importSegmentationMasks(folders.output, parentClassificationString, outputClassificationString)
   
   //delete temporal folder
   deleteTemporalFolder(folders.mainFolder)
   
   println "Done!"
   
}


import qupath.ext.biop.cmd.VirtualEnvironmentRunner
import ij.gui.Wand
import qupath.lib.objects.PathObjects
import qupath.lib.regions.ImagePlane
import ij.IJ
import ij.process.ColorProcessor
import qupath.imagej.processing.RoiLabeling
import qupath.imagej.tools.IJTools
import java.util.regex.Matcher
import java.util.regex.Pattern


def runSMPInference(pythonEXEDir, clusterModelStain,daneelSegmentationFolder, mainFolderTilesOutput,batchSize) {
    
    if(clusterModelStain == "HE") {
       segmentationModel = buildFilePath(daneelSegmentationFolder, "U-Net_checkpoints/best_cluster_he.ckpt")
    }else if(clusterModelStain == "VN") {      
      segmentationModel = buildFilePath(daneelSegmentationFolder, "U-Net_checkpoints/best_cluster_vn.ckpt")
    }

    // Check if the file exists
    def segmentationFile = new File(segmentationModel)

    if (!segmentationFile.exists()) {
        throw new FileNotFoundException("ERROR: The file ${segmentationModel} does not exist. Please check the path.")
    }
   
    
    inferenceScript = buildFilePath(daneelSegmentationFolder,"U-Net_checkpoints/inference_unet_qupath.py")
    
    def veRunner = new VirtualEnvironmentRunner(pythonEXEDir, VirtualEnvironmentRunner.EnvType.EXE, "DANEELpath.0.1")
    def pythonInferenceFile = new File(inferenceScript)
    
    
    def Arguments = [pythonInferenceFile.toString(), "--parent_dir", mainFolderTilesOutput, "--model",segmentationModel, "--batch_size",batchSize.toString()]

    veRunner.setArguments(Arguments)


    // Run the command with waiting for completion and capture log
    veRunner.runCommand(true)
    
   
   
}



def createTilesParentObjects(parentClassification, tilePadding, tileFolder) {
    
    
   
    def imageData = getCurrentImageData()
    def server = getCurrentServer()
    def path = server.getPath()

    toSegment = getAnnotationObjects().findAll {
       it.getPathClass().toString() == parentClassification 
    }
    
    if(toSegment.size()== 0 ) {
        throw new RuntimeException("ERROR: There is no parent annotations, please check the classification")
    }
    
        
    
    if(tilePadding < 10 | tilePadding > 250) {
       throw new RuntimeException("ERROR: TileOverlap is less than 10 or bigger than 250, please adjust the value")
    }


    //export regions. by the moment let the downsample to be 1. In a near future I will add the option to set the downsample.
    downsample = 1
    toSegment.forEach {
       def request = RegionRequest.createInstance(path, downsample, it.getROI())
       parentID = it.getID().toString()
       extensionImg = "_" + parentID + ".tif"
   
        // Create an exporter that requests corresponding tiles from the original & labelled image servers
        new TileExporter(imageData)
            .downsample(downsample)   // Define export resolution
            .imageExtension(extensionImg)   // Define file extension for original pixels (often .tif, .jpg, '.png' or '.ome.tif')
            .tileSize(256)            // Define size of each tile, in pixels
            .region(request)
            .overlap(tilePadding)              // Define overlap, in pixel units at the export resolution
            .annotatedTilesOnly(true)
            .includePartialTiles(true)
            .writeTiles(tileFolder)   // Write tiles to the specified directory

    print 'Tiles exported'

   
    }
  
   
}



def createTemporalFolderStructure() {
   
   basePath = buildFilePath(QP.PROJECT_BASE_DIR)

    // Define output path (here, relative to project)
    //def name = GeneralTools.getNameWithoutExtension(imageData.getServer().getMetadata().getName())
    def folder = buildFilePath(PROJECT_BASE_DIR, 'inference_smp')
    mkdirs(folder)

    def tiles = buildFilePath(folder, 'tiles')
    mkdirs(tiles)

    def masks = buildFilePath(folder, 'output')
    mkdirs(masks)
    
    return [mainFolder:folder, tiles:tiles, output:masks ]
   
   
    }


//This function is based on the @Mike Nelson script: https://forum.image.sc/t/qupath-transferring-objects-from-one-image-to-the-other/48122/3
def importSegmentationMasks(masksPath,parentClassification,clustersClassification) {
    
    //our label mask contains the cluster segmentation at the value of 1
    def nclass= 1
    def downsample =1 //by the moment let the downsample to be 1. In a near future I will add the option to set the downsample.
    def plane = ImagePlane.getPlane(0, 0)

    File folder = new File(masksPath);
    File[] listOfFiles = folder.listFiles();
    
    //list the files of the current image
    currentImport = listOfFiles.findAll{ GeneralTools.getNameWithoutExtension(it.getPath()).contains(GeneralTools.stripExtension(getCurrentImageName())) &&  it.toString().contains(".tif")}
     
    
    
    parentAnno = getAnnotationObjects().findAll {
       it.getPathClass() == getPathClass(parentClassification) 
    }
    
    //here we will store the annotations to add them to the image
    objectsToImport = []
    
    parentAnno.forEach {
       
       ID = it.getID().toString()
       
       //select those that have the parent ID in the name of the export
       childsParent = currentImport.findAll {
          it.toString().contains(ID) 
       }
       
       def roisToMerge = []
       
       childsParent.forEach {
           //To extract the x and y values for translation of the annotations. 
           fileName= it.getName()
           //We use this expression to find those two values in brackets
           def pattern = /\[x=(\d+),y=(\d+)/
           def matcher = (fileName =~ pattern)

           //find and save the xy values 
           def xCoord, yCoord
            if (matcher.find()) {
                xCoord = matcher.group(1) as int
                yCoord = matcher.group(2) as int
            }
            
            path = it.getPath()
            
           //start imageJ and chenk that there is some object inside, if not return
           def imp = IJ.openImage(path)
           
           int n = imp.getStatistics().max as int
           if (n == 0) {
               print 'No objects found!'
               return
           }

           def ip = imp.getProcessor()
           if (ip instanceof ColorProcessor) {
               throw new IllegalArgumentException("RGB images are not supported!")
           }
           
           //We create connected ROIs for the elements of the labelled image.
   
           def roisIJ = RoiLabeling.labelsToConnectedROIs(ip, nclass)
   
           def rois = roisIJ.collect {
               if (it == null)
                   return
           //here we put the coords for translation with a minus before 
               return IJTools.convertToROI(it, -xCoord , -yCoord, downsample, plane);
            }
   
   
        //we have our cluster element as the 0th element of rois.
           roisToMerge.add(rois[0])
           
           // close the imp to save memory
           imp.close()
             
          
       }//childs
       
       //we net to do the intersection between the original parent and the inference to make sure
       //that the objects fall inside the boundaries. 
       
       roisToIntersect = []
       
       //merge all the childs from one parent
       mergedObject= RoiTools.union(roisToMerge)
       roisToIntersect << mergedObject
       
       parentROI = it.getROI()
       roisToIntersect << parentROI
       
       intersectionObjects = RoiTools.intersection(roisToIntersect)
    
       annoChilds = PathObjects.createAnnotationObject(intersectionObjects,getPathClass(clustersClassification))
       
       objectsToImport << annoChilds
       
    }//for each parent
    
    addObjects(objectsToImport)
   
    
    println "Masks imported!"
}




//Delete directory with the patches
import org.apache.commons.io.FileUtils;
import java.io.File

def deleteTemporalFolder(mainFolder) { 

    folderDelete = new File(mainFolder)
    FileUtils.deleteDirectory(folderDelete) 
}







def runPostProcessingSegmentation(outputClassificationString,fillHoles,minArea,maxExcentricity,minSolidity) {
    
    def server = getCurrentServer()
    def cal = server.getPixelCalibration()
    double pixelWidth = cal.getPixelWidthMicrons()
    double pixelHeight = cal.getPixelHeightMicrons()
   
    toProcess = getAnnotationObjects().findAll {
       it.getPathClass().toString() == outputClassificationString 
    }
    
    if(toProcess.size()== 0 ) {
        throw new RuntimeException("ERROR: There is no output annotations, please check the classification")
    }
    
    
    if(fillHoles == true) {
       
       selectObjects(toProcess)
       runPlugin('qupath.lib.plugins.objects.FillAnnotationHolesPlugin', '{}')
       selectObjects(toProcess)
       runPlugin('qupath.lib.plugins.objects.SplitAnnotationsPlugin', '{}')
       
    }else if(fillHoles == false) {
        
       selectObjects(toProcess)
       runPlugin('qupath.lib.plugins.objects.SplitAnnotationsPlugin', '{}')
        
    }
    
    
    //now compute the features for each object using QuPath tools
    
    toProcessSplit = getAnnotationObjects().findAll {
       it.getPathClass().toString() == outputClassificationString 
    }
    
    selectObjects(toProcessSplit)
    addShapeMeasurements("AREA", "LENGTH", "CIRCULARITY", "SOLIDITY", "MAX_DIAMETER", "MIN_DIAMETER")
    
    //compute the eccentricity, usefull when you have elongated shapes, usually in borders.
    computeEccentricity(toProcessSplit)
    
    
    //now remove the objects filtered by area and eccentricity and solidity
     

    def toDelete = getAnnotationObjects().findAll { it.getPathClass() == getPathClass(outputClassificationString) && 
    (it.getROI().getScaledArea(pixelWidth, pixelHeight) < minArea ||
    it.measurements["Eccentricity"] > maxExcentricity ||
    it.measurements["Solidity"] < minSolidity )
    }
    
    
    removeObjects(toDelete, true)
    
    resetSelection()
    
   
   
   
}



def computeEccentricity(objects) {
   
   objects.forEach {
      majAxis = it.measurements['Max diameter µm']
      minAxis=it.measurements['Min diameter µm']
      eccentricity = 2*Math.sqrt(((majAxis * majAxis * 0.25) - (minAxis * minAxis * 0.25))) / majAxis
      it.getMeasurementList().put('Eccentricity',eccentricity )
      
   }
}





///////////////////////////////
/////FUCNTIONS FOR WATERSHED///
///////////////////////////////

import qupath.imagej.tools.IJTools
import qupath.lib.images.servers.LabeledImageServer
import qupath.lib.regions.RegionRequest
import ij.plugin.filter.EDM
import qupath.imagej.processing.RoiLabeling


def runWatershedPostprocessingAnnotations(inputClassificationString, outputClassificationString,removeInputObjects) {
   
   
   qupathAnnotationsWatersheed =[]
   
   annotationsToPostprocess = getAnnotationObjects().findAll {
      it.getPathClass() == getPathClass(inputClassificationString) 
   }
   
   if(annotationsToPostprocess.size()== 0 ) {
        throw new RuntimeException("ERROR: There is no parent annotations, please check the classification")
    }
  
   
   
   //apply the watershed
   annotationsToPostprocess.forEach {
      
      whatershedROI = watershedSingleAnnotationIJ(it,inputClassificationString )
      
      //split the rois and convert to annotations in QuPath
      whatershedROI.forEach {
          roisSplited = RoiTools.splitROI(it)
          
          roisSplited.forEach {
              annotations = PathObjects.createAnnotationObject(it,getPathClass(outputClassificationString))
              qupathAnnotationsWatersheed << annotations
          }
          
      }
      
   }
   
   
 
   
   
   addObjects(qupathAnnotationsWatersheed)
   
   //check if i ahve a bolean, and if it is true remove the objects
   if(removeInputObjects instanceof Boolean && removeInputObjects) {
      removeObjects(annotationsToPostprocess,true) 
   }
   
   
   println "Watershed Postprocesing Finished!"
   
}




//Watershed single annotation. 


def watershedSingleAnnotationIJ (annotation,inputClassificationString) {
   
   def imageData = getCurrentImageData()
   def plane = ImagePlane.getPlane(0, 0)
   def downsample = 1
   
   //create the label server to export the label in IJ. IMPORTANT: the label here has a value of 1.

   def labelServer = new LabeledImageServer.Builder(imageData)
    .backgroundLabel(0, ColorTools.BLACK) // Background label
    .addLabel(inputClassificationString, 1)// Add label for annotations
    .downsample(downsample)          // Define resolution
    .multichannelOutput(false)       // Single-channel output
    .build()
   
   //coords to place the rois back in QuPath
   xCoord = annotation.getROI().getBoundsX()
   yCoord = annotation.getROI().getBoundsY()
   
   //request the labelled region
   def regionRequest = RegionRequest.createInstance(labelServer.getPath(), downsample, annotation.getROI())
   
   //transform the region to imgPlus for IJ processing
   def pathImgPlus = IJTools.convertToImagePlus(labelServer, regionRequest)
   def imgPlus = pathImgPlus.getImage()
   
   //get the processor and do the Euclidian Distance Map and watershed
   def ip = imgPlus.getProcessor()
   def edm = new EDM()
   edm.toWatershed(ip)
   
   //now we can convert the watersheed result to ROIs. We set nclass to 1, since the label is 1
   def nclass= 1
   def roisIJ = RoiLabeling.labelsToConnectedROIs(ip, nclass)
   
   def roisQuPath = roisIJ.collect {
   if (it == null)
          return
           //here we put the coords for translation with a minus before 
    return IJTools.convertToROI(it, -xCoord , -yCoord, downsample, plane);
   }

   
   //Close the imgPlus and return the roi, althoug splited stil is one object.
   //will do the postprocessing to the final split later.
   imgPlus.close()
   
   
   return roisQuPath
}

















