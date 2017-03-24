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

    private UpdateDimensionPropertiesRequest setColumnFormat(Integer sheetID, Integer start, Integer end, Integer size) {

        //Create a new updateDimensionPropertiesRequest
        UpdateDimensionPropertiesRequest updateDimensionPropertiesRequest = new UpdateDimensionPropertiesRequest();

        //Create a dimensionRange with the desired attributes
        DimensionRange dimensionRange = new DimensionRange();
        dimensionRange.setSheetId(sheetID);
        dimensionRange.setDimension("COLUMNS");
        dimensionRange.setStartIndex(start);
        dimensionRange.setEndIndex(end);

        //Create new dimensionProperties specifying the width of the cells
        DimensionProperties dimensionProperties = new DimensionProperties();
        dimensionProperties.setPixelSize(size);

        //Added the range and the properties to the updateDimensionPropertiesRequest
        updateDimensionPropertiesRequest.setRange(dimensionRange);
        updateDimensionPropertiesRequest.setProperties(dimensionProperties);
        updateDimensionPropertiesRequest.setFields("pixelSize");

        return updateDimensionPropertiesRequest;
    }

    private void addDimensionUpdateRequest(List<Request> requestsList, UpdateDimensionPropertiesRequest updateDimensionPropertiesRequest) {
        Request request = new Request();
        request.setUpdateDimensionProperties(updateDimensionPropertiesRequest);
        requestsList.add(request);
    }

    private void setFormatting(String spreadsheetId, Integer sheetID)  throws IOException {

        //Column 0
        UpdateDimensionPropertiesRequest updateDimensionPropertiesRequest0 = setColumnFormat(sheetID, 0, 1, 70);
        //Column 1
        UpdateDimensionPropertiesRequest updateDimensionPropertiesRequest1 = setColumnFormat(sheetID, 1, 2, 200);
        //Column 2
        UpdateDimensionPropertiesRequest updateDimensionPropertiesRequest2 = setColumnFormat(sheetID, 2, 3, 160);
        //Column 3
        UpdateDimensionPropertiesRequest updateDimensionPropertiesRequest3 = setColumnFormat(sheetID, 3, 4, 100);
        //Column 4
        UpdateDimensionPropertiesRequest updateDimensionPropertiesRequest4 = setColumnFormat(sheetID, 4, 5, 150);
        //Column 5
        UpdateDimensionPropertiesRequest updateDimensionPropertiesRequest5 = setColumnFormat(sheetID, 5, 6, 400);

        //Create batchUpdateSpreadsheetRequest
        BatchUpdateSpreadsheetRequest batchUpdateSpreadsheetRequest = new BatchUpdateSpreadsheetRequest();

        //Create a new request list containing all the updateDimensionPropertiesRequests
        List<Request> requestsList = new ArrayList<>();
        addDimensionUpdateRequest(requestsList, updateDimensionPropertiesRequest0);
        addDimensionUpdateRequest(requestsList, updateDimensionPropertiesRequest1);
        addDimensionUpdateRequest(requestsList, updateDimensionPropertiesRequest2);
        addDimensionUpdateRequest(requestsList, updateDimensionPropertiesRequest3);
        addDimensionUpdateRequest(requestsList, updateDimensionPropertiesRequest4);
        addDimensionUpdateRequest(requestsList, updateDimensionPropertiesRequest5);

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