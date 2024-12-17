/*
 * DANEELpath project  is under   GNU GENERAL PUBLIC LICENSE, GPL-3.0 license
 * Please check https://github.com/iviecomarti/DANEELpath if you have any doubts about licensing
 * 
 * Script to run custom Sematinc Segmentation Model from QuPath
 * 
 * This scripts creates the tiles form the parent classification, an runs a python script 
 * to do the inference of a neural network.
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
 * Parameter "tilePadding":
 *      -Adjust the amount of overlap between the tiles. If it is less than 10 or more than 250 
 *       the script will not run.
 *      
 * 
 * Since we are running a Python Script, supports CUDA GPU if aviable. If not, will use cpu
 * Parameter: "batch_size":
 *     -Ajust the amount of images pushed to the GPU in each iteration. The default batch_size is 8.
 * 
 * The parameters: 
 * daneelSegmentationFolder: is the path to the folder where the model checkpoints and inference script are located
 * pathPythonEnviromentEXE: is the path to the conda Python EXE. 
 * 
 * To obtain "pathPythonEnviromentEXE" path, activate the enviroment and run in the terminal:
 *     -Windows: where python
 *     -macOS, Linux: which python
 * 
 * 
 * @author Isaac Vieco-Marti
 */




//To complete
def parentClassificationString = "Tumor"
def clusterModelStain = "HE"
def tilePadding = 64
def batch_size = 32
def outputClassificationString = "cluster"

//Python conda settings
def daneelFolder = "path/DANEELpath/folder"
def pathPythonEnviromentEXE = "path/python.exe"






////////////////////////////////////////////
////////DO NOT TOUCH FROM HERE//////////////
////////////////////////////////////////////

runSemanticSegmentation(parentClassificationString,outputClassificationString,clusterModelStain,tilePadding,batch_size,daneelFolder,pathPythonEnviromentEXE)




def runSemanticSegmentation(parentClassificationString,outputClassificationString,clusterModelStain,tilePadding,batch_size,daneelSegmentationFolder,pathPythonEnviromentEXE) {
   
   //create temporalFolder
   folders = createTemporalFolderStructure()
   
   //export parent Tiles
   createTilesParentObjects(parentClassificationString,tilePadding, folders.tiles)
   
   //run the inference in python
   runSMPInference(pathPythonEnviromentEXE,clusterModelStain,daneelSegmentationFolder,folders.mainFolder,batch_size)
   
   println "Inference completed, importing the masks"
   
   //import the masks
   importSegmentationMasks(folders.output,parentClassificationString, outputClassificationString)
   
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
