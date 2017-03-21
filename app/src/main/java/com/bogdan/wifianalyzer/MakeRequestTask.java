package com.bogdan.wifianalyzer;

import android.os.AsyncTask;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.model.AddSheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.DimensionProperties;
import com.google.api.services.sheets.v4.model.DimensionRange;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.UpdateCellsRequest;
import com.google.api.services.sheets.v4.model.UpdateDimensionPropertiesRequest;
import com.google.api.services.sheets.v4.model.ValueRange;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
    private GoogleAccountCredential mCredential;
    private com.google.api.services.sheets.v4.Sheets mService = null;
    private List<List<Object>> wifiResults;

    MakeRequestTask(GoogleAccountCredential credential, List<List<Object>> wifiResults) {
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.sheets.v4.Sheets.Builder(
                transport, jsonFactory, credential)
                .setApplicationName("Google Sheets API Android Quickstart")
                .build();

        //Set the credentials of the user and the wifiResults.
        //The wifiResults represents the data that will be sent to the sheet.
        this.wifiResults = wifiResults;
        this.mCredential = credential;
    }

    /**
     * Background task to call Google Sheets API.
     * @param params no parameters needed for this task.
     */
    @Override
    protected List<String> doInBackground(Void... params) {
        try {
            //Set sheet name
            //Can be any string, I chose to set it to the account name
            Integer sheetID;
            String sheetName = mCredential.getSelectedAccountName();
            String spreadsheetId = "1TRaC86Ehrt2CJunrhc_3VwNnQ5n7GSLCNgSfASehwcs";
            String range = String.format("%s!A1:F",sheetName);//"Sheet1!A1:B";

            //Check if the sheet exists and clear all the values, if not, create a new one with the user's name
            sheetID = getSheetId(spreadsheetId,sheetName);

            if(sheetID == 0) {
                //Add a new sheet and set the formatting
                createNewSheet(spreadsheetId, sheetName);
                setFormatting(spreadsheetId, sheetID);

            } else {
                //Clear the sheet of all existing values and set the formatting
                setFormatting(spreadsheetId, sheetID);
                clearDataFromSheet(spreadsheetId, sheetID);
            }

            writeDataToSheet(spreadsheetId, range);
        } catch (Exception e) {
            cancel(true);
            return null;
        }
        return null;
    }

    private Integer getSheetId(String spreadsheetId, String sheetName) throws IOException {
        Integer sheetID = 0;
        Spreadsheet response1= this.mService.spreadsheets().get(spreadsheetId).setIncludeGridData (false).execute ();
        List<Sheet> workSheetList = response1.getSheets();

        for (Sheet sheet : workSheetList) {
            if(sheet.getProperties().getTitle().equals(sheetName)) {
                sheetID = sheet.getProperties().getSheetId();
                break;
            }
        }
        return sheetID;
    }

    private void writeDataToSheet(String spreadsheetId, String range) throws IOException {

        //Create the valuerange object and set its fields
        ValueRange valueRange = new ValueRange();
        valueRange.setMajorDimension("ROWS");
        valueRange.setRange(range);
        valueRange.setValues(wifiResults);

        //Send the new values to the Spreadsheet
        this.mService.spreadsheets().values()
                .update(spreadsheetId, range, valueRange)
                .setValueInputOption("RAW")
                .execute();
    }

    private void createNewSheet(String spreadsheetId, String sheetName)  throws IOException{

        //Create a new AddSheetRequest
        AddSheetRequest addSheetRequest = new AddSheetRequest();
        SheetProperties sheetProperties = new SheetProperties();

        //Add the sheetName to the sheetProperties
        addSheetRequest.setProperties(sheetProperties);
        addSheetRequest.setProperties(sheetProperties.setTitle(sheetName));

        //Create batchUpdateSpreadsheetRequest
        BatchUpdateSpreadsheetRequest batchUpdateSpreadsheetRequest = new BatchUpdateSpreadsheetRequest();

        //Create requestList and set it on the batchUpdateSpreadsheetRequest
        List<Request> requestsList = new ArrayList<>();
        batchUpdateSpreadsheetRequest.setRequests(requestsList);

        //Create a new request with containing the addSheetRequest and add it to the requestList
        Request request = new Request();
        request.setAddSheet(addSheetRequest);
        requestsList.add(request);

        //Add the requestList to the batchUpdateSpreadsheetRequest
        batchUpdateSpreadsheetRequest.setRequests(requestsList);

        //Call the sheets API to execute the batchUpdate
        this.mService.spreadsheets().batchUpdate(spreadsheetId, batchUpdateSpreadsheetRequest).execute();
    }

    private void clearDataFromSheet(String spreadsheetId, Integer sheetID)  throws IOException {

        //Create a new updateCellsRequest
        String fields = "userEnteredValue";
        UpdateCellsRequest updateCellsRequest = new UpdateCellsRequest();

        //Create a new gridRange with the sheetID property set to the ID of the selected sheet
        GridRange gridRange = new GridRange();
        gridRange.setSheetId(sheetID);

        //Set the range and fields in the updateCellRequest
        updateCellsRequest.setRange(gridRange);
        updateCellsRequest.setFields(fields);

        //Create batchUpdateSpreadsheetRequest
        BatchUpdateSpreadsheetRequest batchUpdateSpreadsheetRequest = new BatchUpdateSpreadsheetRequest();

        //Create requestList and set it on the batchUpdateSpreadsheetRequest
        List<Request> requestsList = new ArrayList<>();
        batchUpdateSpreadsheetRequest.setRequests(requestsList);

        //Create a new request with containing the updateCellsRequest and add it to the requestList
        Request request = new Request();
        request.setUpdateCells(updateCellsRequest);
        requestsList.add(request);

        //Add the requestList to the batchUpdateSpreadsheetRequest
        batchUpdateSpreadsheetRequest.setRequests(requestsList);

        //Call the sheets API to execute the batchUpdate
        this.mService.spreadsheets().batchUpdate(spreadsheetId, batchUpdateSpreadsheetRequest).execute();
    }

    private void setFormatting(String spreadsheetId, Integer sheetID)  throws IOException {

        //Column 0
        //Create a new updateDimensionPropertiesRequest
        UpdateDimensionPropertiesRequest updateDimensionPropertiesRequest1 = new UpdateDimensionPropertiesRequest();

        //Create a dimensionRange with the desired attributes
        DimensionRange dimensionRange1 = new DimensionRange();
        dimensionRange1.setSheetId(sheetID);
        dimensionRange1.setDimension("COLUMNS");
        dimensionRange1.setStartIndex(0);
        dimensionRange1.setEndIndex(1);

        //Create new dimensionProperties specifying the width of the cells
        DimensionProperties dimensionProperties1 = new DimensionProperties();
        dimensionProperties1.setPixelSize(70);

        //Added the range and the properties to the updateDimensionPropertiesRequest
        updateDimensionPropertiesRequest1.setRange(dimensionRange1);
        updateDimensionPropertiesRequest1.setProperties(dimensionProperties1);
        updateDimensionPropertiesRequest1.setFields("pixelSize");

        //Columns 1 to 5
        //Create a new updateDimensionPropertiesRequest
        UpdateDimensionPropertiesRequest updateDimensionPropertiesRequest2 = new UpdateDimensionPropertiesRequest();

        //Create a dimensionRange with the desired attributes
        DimensionRange dimensionRange2 = new DimensionRange();
        dimensionRange2.setSheetId(sheetID);
        dimensionRange2.setDimension("COLUMNS");
        dimensionRange2.setStartIndex(1);
        dimensionRange2.setEndIndex(5);

        //Create new dimensionProperties specifying the width of the cells
        DimensionProperties dimensionProperties2 = new DimensionProperties();
        dimensionProperties2.setPixelSize(200);

        //Added the range and the properties to the updateDimensionPropertiesRequest
        updateDimensionPropertiesRequest2.setRange(dimensionRange2);
        updateDimensionPropertiesRequest2.setProperties(dimensionProperties2);
        updateDimensionPropertiesRequest2.setFields("pixelSize");

        //Column 6
        //Create a new updateDimensionPropertiesRequest
        UpdateDimensionPropertiesRequest updateDimensionPropertiesRequest3 = new UpdateDimensionPropertiesRequest();

        //Create a dimensionRange with the desired attributes
        DimensionRange dimensionRange3 = new DimensionRange();
        dimensionRange3.setSheetId(sheetID);
        dimensionRange3.setDimension("COLUMNS");
        dimensionRange3.setStartIndex(5);
        dimensionRange3.setEndIndex(6);

        //Create new dimensionProperties specifying the width of the cells
        DimensionProperties dimensionProperties3 = new DimensionProperties();
        dimensionProperties3.setPixelSize(400);

        //Added the range and the properties to the updateDimensionPropertiesRequest
        updateDimensionPropertiesRequest3.setRange(dimensionRange3);
        updateDimensionPropertiesRequest3.setProperties(dimensionProperties3);
        updateDimensionPropertiesRequest3.setFields("pixelSize");

        //Create batchUpdateSpreadsheetRequest
        BatchUpdateSpreadsheetRequest batchUpdateSpreadsheetRequest = new BatchUpdateSpreadsheetRequest();

        //Create requestList and set it on the batchUpdateSpreadsheetRequest
        List<Request> requestsList = new ArrayList<>();
        batchUpdateSpreadsheetRequest.setRequests(requestsList);

        //Create a new request list containing all the updateDimensionPropertiesRequests
        Request request1 = new Request();
        request1.setUpdateDimensionProperties(updateDimensionPropertiesRequest1);
        Request request2 = new Request();
        request2.setUpdateDimensionProperties(updateDimensionPropertiesRequest2);
        Request request3 = new Request();
        request3.setUpdateDimensionProperties(updateDimensionPropertiesRequest3);
        requestsList.add(request1);
        requestsList.add(request2);
        requestsList.add(request3);

        //Add the requestList to the batchUpdateSpreadsheetRequest
        batchUpdateSpreadsheetRequest.setRequests(requestsList);

        //Call the sheets API to execute the batchUpdate
        this.mService.spreadsheets().batchUpdate(spreadsheetId, batchUpdateSpreadsheetRequest).execute();
    }

    @Override
    protected void onPreExecute() {

    }

    @Override
    protected void onPostExecute(List<String> output) {

    }

    @Override
    protected void onCancelled() {

    }
}