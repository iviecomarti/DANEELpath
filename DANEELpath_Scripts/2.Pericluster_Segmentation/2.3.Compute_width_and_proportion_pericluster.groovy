/* 
 * DANEELpath project  is under   GNU GENERAL PUBLIC LICENSE, GPL-3.0 license
 * Please check https://github.com/iviecomarti/DANEELpath if you have any doubts about licensing
 *
 * Function to compute Width and Porportion of objects inside expansion.
 * 
 * This script takes the expansions and objects created by the previous script. 
 * 
 * Measurement width: computes the difference between the  radius of the merged object(cellCluster + expandedObject) , and the cell cluster radius
 * Measurement proportion: computes the proportion of the object inside the expansion with respect the cellCluster
 * 
 * 
 * @author Isaac Vieco-Martí
 * 
 */



//To complete

def expansionClassification = "Expansion"
def objectInsideClassification = "halo"
def deleteExpansionObject = false //chose if delete the expansions, defauld false







/////////////////////////////////
/////DO NOT TOUCH FROM HERE/////
////////////////////////////////

runComputeWidthAndProportionInExpansion(expansionClassification,objectInsideClassification,deleteExpansionObject)





//Main Function
def runComputeWidthAndProportionInExpansion(expansionClassification,objectInsideClassification,deleteExpansionObject) {
    
    
    
    // Get the main QuPath data structures
    def imageData = getCurrentImageData()
    def server = imageData.getServer()
    def cal = server.getPixelCalibration()


    if (!cal.hasPixelSizeMicrons()) {
        println "ERROR: We need the pixel size information here!"
    return
    }

    //confirm objects are present
    def expansionCheck = getAnnotationObjects().findAll {it.getPathClass() == getPathClass(expansionClassification)}   
    if(expansionCheck.size() == 0) {
       
       println "ERROR: There is not expansion annotation.\nPlease check the classification"
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




//////////////////////////////
/////FUNCTIONS////////////////
//////////////////////////////


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
