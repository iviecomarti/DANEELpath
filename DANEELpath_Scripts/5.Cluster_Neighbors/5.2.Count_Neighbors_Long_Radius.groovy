/*
 * DANEELpath project  is under   GNU GENERAL PUBLIC LICENSE, GPL-3.0 license
 * Please check https://github.com/iviecomarti/DANEELpath if you have any doubts about licensing
 *
 * Script to count the number of annotation neighbours from LongRadius expansion. 
 * 
 *This script uses the cell Clusters, create an expansion "a neibhbourhood" of the Long Raidus 
 *and counts the number of annotations that intersects with it
 * 
 * If you have an annotation covering all the clusters, like "Hidrogel", you can set it, to avoid to create an expansion.
 *
 * If desired you can add the expansion annotations for visual purposes, but it will take a little bit more of time.
 *
 * @author Isaac Vieco-Martí
 * 
 */




//PARAMETERS TO COMPLETE
def countByClassification = true //count the neighbors and also count them by classification
def referenceAnnotation = "Hydrogel" //put the reference classification if it is present to avoid the expansion. For example an annotation like "Hidrogel" containing all the sample

// OPTIONS IF YOU WANT TO ADD THE EXPANSIONS
def addExpansions = false
def removeInterior = false





////////////////////////////////////////////
////////DO NOT TOUCH FROM HERE//////////////
////////////////////////////////////////////


runNeighboursLongRadiusExpansion(countByClassification, referenceAnnotation,addExpansions,removeInterior)


//Main Function
def runNeighboursLongRadiusExpansion(countByClassification, referenceAnnotation,addExpansions,removeInterior){

   
        geomMap = extractAnnotationGeometriesAndLongRadius(referenceAnnotation)

        if(countByClassification == true){
            countNeighboursWithClassificationLongRadius(geomMap,referenceAnnotation)
        }else {
            countNeighboursLongRadius(geomMap)
        }
        

        if(addExpansions == true){
            addLongRadiusExpansions(geomMap,removeInterior)
        }

    println "Count Long Radius Neighbours Finished!"

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