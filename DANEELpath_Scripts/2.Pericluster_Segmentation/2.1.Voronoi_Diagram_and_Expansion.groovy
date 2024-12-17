/*
 * DANEELpath project  is under   GNU GENERAL PUBLIC LICENSE, GPL-3.0 license
 * Please check https://github.com/iviecomarti/DANEELpath if you have any doubts about licensing
 *
 * Script to create expansions limited with Voronoi faces
 * 
 *This script uses the cell Clusters, create the influence zones (Voronoi from borders of the annotation).
 * Then the user can set an expansion of that cell Cluster, but this will limit with its Voronoi influence zones.
 *In this way, we can define a limited neighborhood for each cell cluster and compute the tVN_outside or the halo. 
 *
 * To limit the expansion of the voronoi Faces, we need to provide a REFERENCE annotation which contains WITHIN all the Regions of Interest 
 * If the annotations are not strictly within the reference, they will be ignored.
 * If the cell cluster does not have a classification, it will be ignored 
 * If the annotations are not included, theirObjectID will be printed.
 *
 *
 * @author Isaac Vieco-Martí
 * 
 */




//TO COMPLETE
def referenceAnnotationClassificationString = "Tumor" //set the classification of the reference annotation
def expansionMicrons = 15 //set the amount of expansion in microns
def expansionClassificationString = "Expansion" //set the classification of the expansion annotation
def addVoronoiFaces = true //if you want to put the voronoi faces as annotations, set to true





////////////////////////////////////////////
////////DO NOT TOUCH FROM HERE//////////////
////////////////////////////////////////////


runExpansionWithVoronoiLimits(referenceAnnotationClassificationString, expansionMicrons, expansionClassificationString, addVoronoiFaces)



//Main Function
def runExpansionWithVoronoiLimits(referenceAnnotationString, expansionMicrons, expansionClassificationString, addVoronoiFaces) {

    def referenceCheckpoint = getAnnotationObjects().findAll {it.getPathClass() == getPathClass(referenceAnnotationString)}
   
   // Check if the number of reference annotations is greater than 1
    if (referenceCheckpoint.size() > 1) {
        // Throw a message if more than one reference annotation is found
        println "ERROR: There is more than one reference annotation, please merge the reference annotations into one object."
        return  
    }
    
    if(referenceCheckpoint.size() == 0) {
       println "ERROR: There is not reference annotation. \n Please create a reference Annotation, and check its classification" 
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








/////////////////////////////////////////////
////////FUNCTIONS TO CREATE ACTIONS//////////
////////////////////////////////////////////

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


