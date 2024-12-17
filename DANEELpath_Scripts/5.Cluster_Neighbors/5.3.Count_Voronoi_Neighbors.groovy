/*
 * DANEELpath project  is under   GNU GENERAL PUBLIC LICENSE, GPL-3.0 license
 * Please check https://github.com/iviecomarti/DANEELpath if you have any doubts about licensing
 *
 *Script to count the Voronoi Touching Neighbours
 * 
 *This script creates a Voronoi Diagram form the Annotations and counts their neighbours
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
 *
 * With option: "addVoronoiFaces" If "true" the Voronoi Faces can be added to the QuPath as annotations.
 * 
 *
 *
 * @author: Isaac Vieco-Martí
 */




//TO COMPLETE
def referenceAnnotationClassificationString = "Tumor" //set the classification of the reference annotation
def countWithClassifications = false //true or false

//OPTIONAl: Add the Coronoi Faces as annotations
def addVoronoiFaces = true //true or false
def removeInterior = false // true or false





////////////////////////////////////////////
////////DO NOT TOUCH FROM HERE//////////////
////////////////////////////////////////////


runVoronoiNeighbours(referenceAnnotationClassificationString,countWithClassifications,addVoronoiFaces,removeInterior)



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
   
   print("Done")
   
}



/////////////////////////////////////////////
////////FUNCTIONS TO CREATE ACTIONS//////////
////////////////////////////////////////////

import qupath.lib.analysis.DelaunayTools
import qupath.lib.objects.PathObjects


//Create Voronoi Faces Using QuPath DelaunayTools 
def createVoronoiFace(referenceAnnotationString) {
    
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
       
      // }
   
   //log if annotations are not included in the analysis
  // if(annoExcludedFormTheAnalysis.size() > 0) {
       
      // print("A total of " + annoExcludedFormTheAnalysis.size()+ " annotations have been excluded from the analysis because they are not inside reference or because they are unclassified")
       //annoExcludedFormTheAnalysis.forEach {
           
           //print("Annotation with ID: " +it.getID() + " excluded" )
           
       //}
       
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
            voronoiMeasurements = parentMeasurements.keySet().findAll { it.contains("Voronoi") }.collectEntries { key ->
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

