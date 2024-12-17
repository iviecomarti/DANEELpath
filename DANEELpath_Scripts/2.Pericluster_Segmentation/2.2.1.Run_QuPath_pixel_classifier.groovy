/*
 * DANEELpath project  is under   GNU GENERAL PUBLIC LICENSE, GPL-3.0 license
 * Please check https://github.com/iviecomarti/DANEELpath if you have any doubts about licensing
 *
* Script to create and run a pixel classifier created with QuPath
*
* If the user wants to use a custom pixel classifer for the detections of the objects inside of the expansions, this script is ready to use
*
*
* @author Isaac Vieco-Martí
*
*/




def parentClassification = "Expansion" //Put the classification of the expansions
def classifierName = "tVN_Outside_Detection" //set the name of your pixel classifier
def minArea = 0    //Min area to create the object
def minHoleArea = 0 //Min hole area to create the object
def outputObjectType = "Annotation" //Annotation or Detection




/////////////////////////////////
/////DO NOT TOUCH FROM HERE/////
////////////////////////////////


actionRunPixelClassifier(parentClassification,classifierName,outputObjectType,minArea, minHoleArea )



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
 