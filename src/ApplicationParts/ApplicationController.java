/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ApplicationParts;

import BPlusTree.BPlusTree;
import ConvertTypes.PropertyKey;

/**
 * Trieda spravujuca hlavne okno GUI. V hlavnom okne je implementovana sprava
 * aplikacie (vkladanie, updatovanie, hladanie nehnutelnosti) ktora je dokladne
 * oddelena od vykonnej casti programu. Taktiez sa tu vsak nachadza sekcie File
 * visitor, v ktorej sa program priamo napoji na subor a prehliada jeho bloky.
 * Tato sekcia je iba testovacia.
 *
 * @author korenciak.marek
 */
public class ApplicationController {

    private BPlusTree<PropertyKey> propertiesStored = null;
    private Property prop = new Property();
    private String fileName = null;
    private boolean deleteWholeBlocks = false;

    public static void main(String[] args) {
        ApplicationController s = new ApplicationController("PropertiesStored.txt", false);
    }

    public ApplicationController(String initFileName, boolean initDeleteWholeBlocks) {
        deleteWholeBlocks = initDeleteWholeBlocks;
        fileName = initFileName;
        propertiesStored = new BPlusTree<PropertyKey>(PropertyKey.class, fileName, deleteWholeBlocks);
    }

    public void generateData(int cadasterCount, int propertyPerCadasterCount) {
        new DataGenerator(fileName, cadasterCount, propertyPerCadasterCount);
        propertiesStored = new BPlusTree<PropertyKey>(PropertyKey.class, fileName, deleteWholeBlocks);
    }

    public boolean find(int propertyNumber, String cadasterName) {
        byte[] temp = propertiesStored.findData(new PropertyKey(propertyNumber, cadasterName));
        if (temp == null) {
            return false;
        }
        prop.deserialize(temp);
        return true;
    }

    public boolean delete(int propertyNumber, String cadasterName) {
        return propertiesStored.delete(new PropertyKey(propertyNumber, cadasterName));
    }

    public void createProperty(int propertyNumber, String cadasterName, String description) {
        prop = new Property();
        prop.setCadasterName(cadasterName);
        prop.setDescription(description);
        prop.setPropertyNumber(propertyNumber);
    }

    public boolean addBorderPoint(boolean initLongitudeWest, double initLongitudeAxis, boolean initLatitudeNorth, double initLatitudeAxis) {
        BorderPoint temp = new BorderPoint(initLongitudeWest, initLongitudeAxis, initLatitudeNorth, initLatitudeAxis);
        return prop.addBorderPoint(temp);
    }

    public boolean insertProperty() {
        return propertiesStored.insert(new PropertyKey(prop.getPropertyNumber(), prop.getCadasterName()), prop.serialize());
    }

    public boolean commitUpdate() {
        return propertiesStored.updateDataOfKey(new PropertyKey(prop.getPropertyNumber(), prop.getCadasterName()), prop.serialize());
    }

    public void updateDescription(String description) {
        prop.setDescription(description);
    }

    public boolean removeBorderPoint(int index) {
        return prop.removeBorderPoint(index);
    }

    public Object[][] getBorders() {
        return prop.getBordesrs();
    }

    public Object[] getBorder(int index) {
        return prop.getBorder(index);
    }

    public void updateBorderPoint(int index, boolean initLongitudeWest, double initLongitudeAxis, boolean initLatitudeNorth, double initLatitudeAxis) {
        prop.updateBorderPoint(index, initLongitudeWest, initLongitudeAxis, initLatitudeNorth, initLatitudeAxis);
    }

    public Object[] getProperty() {
        Object[] temp = new Object[3];
        temp[0] = prop.getCadasterName();
        temp[1] = prop.getPropertyNumber() + "";
        temp[2] = prop.getDescription();

        return temp;
    }

    public void resetProperty() {
        prop = new Property();
    }

    public boolean deleteBorderPoint(int index) {
        return prop.deleteBorderPoint(index);
    }
}
