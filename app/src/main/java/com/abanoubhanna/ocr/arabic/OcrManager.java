package com.abanoubhanna.ocr.arabic;

import android.graphics.Bitmap;

import com.googlecode.leptonica.android.Pix;
import com.googlecode.tesseract.android.TessBaseAPI;

class OcrManager {
    private TessBaseAPI baseAPI = null;
    void initAPI() {
        baseAPI = new TessBaseAPI();
        //baseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK);
        //baseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO);
        //baseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO_OSD);
        baseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO_ONLY); // what i used prev

        // after copy, my path to trainned data is getExternalFilesDir(null)+"/tessdata/"+"ara.traineddata";
        // but init() function just need parent folder path of "tessdata", so it is getExternalFilesDir(null)
        String dataPath = MainApplication.instance.getTessDataParentDirectory();
        baseAPI.init(dataPath,"ara", TessBaseAPI.OEM_LSTM_ONLY);
        // language code is name of trained data file, except extension part
        // "ara.traineddata" => language code is "ara"

        // first param is datapath which is  part to the your trainned data, second is language code
        // now, your trainned data stored in assets folder, we need to copy it to another external storage folder.
        // It is better do this work when application start firt time
    }

    String startRecognize(Bitmap bitmap) {
        if(baseAPI ==null)
            initAPI();
        baseAPI.setImage(bitmap);
        String OCRedText;
        try {
            OCRedText = baseAPI.getUTF8Text().trim(); //trim extracted text
        }catch (Exception e){
            return "ERROR 120";
        }
        baseAPI.end();
        return OCRedText;
    }

    /*
    testing the ability to detect low probability characters
     */
    String startTextOCR(Bitmap bitmap) {
        if(baseAPI ==null)
            initAPI();
        baseAPI.setImage(bitmap);

//        baseAPI.getBoxText(0); //get the boxed text with the value and x, y of the box
//        baseAPI.getConnectedComponents(); //get chars that connected which make words
//        baseAPI.getHOCRText(0); //get HTML formatted text
//        baseAPI.getRegions(); //get the bounding boxes
//        baseAPI.getResultIterator(); //iterator of the recognized text or analyzed layout
//        baseAPI.getStrips(); //get textlines n strips
//        baseAPI.getTextlines(); //get the textlines
//        baseAPI.getThresholdedImage(); //thresholded image by tesseract engine
//        baseAPI.getWords(); //get words with bounding boxes

        baseAPI.end();
        return "";
    }
}

