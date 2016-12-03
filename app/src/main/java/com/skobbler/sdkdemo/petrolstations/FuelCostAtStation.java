package com.skobbler.sdkdemo.petrolstations;

import android.content.Context;
import android.database.Cursor;

import com.skobbler.ngx.SKCategories;
import com.skobbler.ngx.SKCoordinate;
import com.skobbler.ngx.reversegeocode.SKReverseGeocoderManager;
import com.skobbler.ngx.search.SKNearbySearchSettings;
import com.skobbler.ngx.search.SKSearchListener;
import com.skobbler.ngx.search.SKSearchManager;
import com.skobbler.ngx.search.SKSearchResult;
import com.skobbler.ngx.search.SKSearchResultParent;
import com.skobbler.ngx.search.SKSearchStatus;
import com.skobbler.ngx.util.SKLogging;
import com.skobbler.sdkdemo.database.ResourcesDAO;

import java.util.List;

/**
 * Created by Krzysiek on 02.12.2016.
 */

public class FuelCostAtStation implements SKSearchListener {
    private double avgPetrolLiterCost = 2.0;
    private double avgDieselLiterCost = 2.0;
    private double avgLPGLiterCost = 2.0;
    private double petrolLiterCost = 2.0;
    private double dieselLiterCost = 2.0;
    private double LPGLiterCost = 2.0;

    private boolean nearBorder = false;
    private String countryCode = "";
    private boolean online = false;
    private Context context;

    private static final int[] searchCategories = new int[] {
            SKCategories.SKPOICategory.SKPOI_CATEGORY_BUREAU_DE_CHANGE.getValue(),
            SKCategories.SKPOICategory.SKPOI_CATEGORY_FUEL.getValue(),
            SKCategories.SKPOICategory.SKPOI_CATEGORY_POLICE.getValue(),
            SKCategories.SKPOICategory.SKPOI_CATEGORY_POST_OFFICE.getValue()
    };
    short radius = 20000;   // 20 km
    SKSearchManager searchManager;
    SKNearbySearchSettings searchObject;
    SKSearchStatus status;

    public void calculateFuelCostAtStation(SKCoordinate coordinate, String countryCode, Context app) {

        // get average fuel costs in country
        context = app;
        ResourcesDAO resourcesDAO = ResourcesDAO.getInstance(context);
        resourcesDAO.openDatabase();
        String[] array = new String[] {countryCode};
        String query = "SELECT DISTINCT " + "PetrolCost" + ", " + "DieselCost" + ", " + "LPGCost" +
                        " FROM " + "AvgFuelCosts" + " WHERE " + "CountryCode" + "=?";
        Cursor resultCursor = resourcesDAO.getDatabase().rawQuery(query, array);
        if ((resultCursor != null) && (resultCursor.getCount() > 0)) {
            try {
                resultCursor.moveToFirst();
                avgPetrolLiterCost = Double.parseDouble(resultCursor.getString(0));
                avgDieselLiterCost = Double.parseDouble(resultCursor.getString(1));
                avgLPGLiterCost = Double.parseDouble(resultCursor.getString(2));
                petrolLiterCost = avgPetrolLiterCost;
                dieselLiterCost = avgDieselLiterCost;
                LPGLiterCost = avgLPGLiterCost;
            } finally {
                resultCursor.close();
            }
        } else {
            if (resultCursor != null) {
                resultCursor.close();
            }
        }

        if (online = true) {
            // 1
            SKSearchResult searchResult = SKReverseGeocoderManager.getInstance().reverseGeocodePosition(coordinate);
            if (searchResult != null && searchResult.getParentsList() != null) {
                for (SKSearchResultParent parent : searchResult.getParentsList()) {
                    countryCode = parent.getParentName();
                }
            }
            startSearch(coordinate);

            // 2

            // 3

        }
    }

    private void startSearch(SKCoordinate coordinate) {
        searchManager = new SKSearchManager(this);
        searchObject = new SKNearbySearchSettings();
        searchObject.setLocation(coordinate);
        searchObject.setRadius(radius);
        searchObject.setSearchResultsNumber(100);
        searchObject.setSearchCategories(searchCategories);
        searchObject.setSearchTerm(""); // all
        searchObject.setSearchMode(SKSearchManager.SKSearchMode.OFFLINE);
        status = searchManager.nearbySearch(searchObject);
        if (status != SKSearchStatus.SK_SEARCH_NO_ERROR) {
            SKLogging.writeLog("SKSearchStatus: ", status.toString(), 0);
        }
    }

    @Override
    public void onReceivedSearchResults(final List<SKSearchResult> searchResults) {
        updateCostIfNearBorder(searchResults);
    }

    private void updateCostIfNearBorder(List<SKSearchResult> searchResults) {
        String searchedCountryCode = "";

        for (SKSearchResult result : searchResults) {
            if (nearBorder = false) {
                SKSearchResult searchResult = SKReverseGeocoderManager.getInstance().reverseGeocodePosition(result.getLocation());
                if (searchResult != null && searchResult.getParentsList() != null) {
                    for (SKSearchResultParent parent : searchResult.getParentsList()) {
                        searchedCountryCode = parent.getParentName();
                    }
                }
                if (!searchedCountryCode.equals(countryCode) && !countryCode.equals("")) {
                    nearBorder = true;
                }
            }
        }
        if (nearBorder = true) {
            ResourcesDAO resourcesDAO = ResourcesDAO.getInstance(context);
            resourcesDAO.openDatabase();
            String[] array = new String[] {searchedCountryCode};
            String query = "SELECT DISTINCT " + "PetrolCost" + ", " + "DieselCost" + ", " + "LPGCost" +
                    " FROM " + "AvgFuelCosts" + " WHERE " + "CountryCode" + "=?";
            Cursor resultCursor = resourcesDAO.getDatabase().rawQuery(query, array);
            if ((resultCursor != null) && (resultCursor.getCount() > 0)) {
                try {
                    resultCursor.moveToFirst();
                    double searchedAvgPetrolLiterCost = Double.parseDouble(resultCursor.getString(0));
                    double searchedAvgDieselLiterCost = Double.parseDouble(resultCursor.getString(1));
                    double searchedAvgLPGLiterCost = Double.parseDouble(resultCursor.getString(2));
                    double differencePetrol = avgPetrolLiterCost - searchedAvgPetrolLiterCost;
                    double differenceDiesel = avgDieselLiterCost - searchedAvgDieselLiterCost;
                    double differenceLPG = avgLPGLiterCost - searchedAvgLPGLiterCost;
                    petrolLiterCost -= (0.1 * differencePetrol);
                    dieselLiterCost -= (0.1 * differenceDiesel);
                    LPGLiterCost -= (0.1 * differenceLPG);
                } finally {
                    resultCursor.close();
                }
            } else {
                if (resultCursor != null) {
                    resultCursor.close();
                }
            }
        }
    }

    public double getAvgPetrolLiterCost() {
        return avgPetrolLiterCost;
    }

    public double getAvgDieselLiterCost() {
        return avgDieselLiterCost;
    }

    public double getAvgLPGLiterCost() {
        return avgLPGLiterCost;
    }

    public double getPetrolLiterCost() {
        return petrolLiterCost;
    }

    public double getDieselLiterCost() {
        return dieselLiterCost;
    }

    public double getLPGLiterCost() {
        return LPGLiterCost;
    }

}