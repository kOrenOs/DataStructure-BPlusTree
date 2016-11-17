/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ApplicationParts;

import BPlusTree.BPlusTree;
import ConvertTypes.PropertyKey;
import java.util.ArrayList;
import java.util.Random;

/**
 * Generuje data potrebne na testovanie aplikacie. Vygeneruje sa ztanoveny pocet
 * nehnutelnosti v kazdom katastri (ktorych pocet sa tiez zada). tieto
 * vygenerovane nehnutelnosti sa vkladaju do arraylistu z ktoreho sa nasledne
 * nahodne vkladaju do struktury B+ stromu. Keby sa vkladali priamo (postupne)
 * tak moze dojst ku rovnomernemu rozlozeniu struktury, kedy sa neda dostatocne
 * otestovat spravnost.
 *
 * @author korenciak.marek
 */
public class DataGenerator {

    public DataGenerator(String fileName, int cadasterCount, int propertyPerCadasterCount) {
        Property pattern = new Property();
        BPlusTree structure = new BPlusTree(PropertyKey.class, 2, pattern.serializeLength(), fileName, false);
        ArrayList<Property> insertReady = new ArrayList<>();

        BorderPoint bordPoint = null;
        String description = null;
        String cadasterName = null;
        int propertyNumber = 0;
        int temp = 0;
        boolean latitude = false;
        boolean longitude = false;

        Random rand = new Random();

        for (int i = 0; i < cadasterCount; i++) {
            cadasterName = "CadasterNO" + i;             //rand.nextInt(1000)
            for (int j = 0; j < propertyPerCadasterCount; j++) {
                propertyNumber = j;

                description = "Description " + cadasterName + " " + propertyNumber;

                pattern = new Property(cadasterName, propertyNumber, description);
                temp = rand.nextInt(87) + 4;

                for (int k = 0; k < temp; k++) {
                    if (rand.nextDouble() < 0.5) {
                        latitude = true;
                    } else {
                        latitude = false;
                    }
                    if (rand.nextDouble() < 0.5) {
                        longitude = true;
                    } else {
                        longitude = false;
                    }
                    bordPoint = new BorderPoint(longitude, rand.nextDouble() * 180, latitude, rand.nextDouble() * 90);
                    pattern.addBorderPoint(bordPoint);
                }
                insertReady.add(pattern);
            }
        }
        propertyNumber = 0;
        while (!insertReady.isEmpty()) {
            temp = rand.nextInt(insertReady.size());
            pattern = insertReady.get(temp);
            insertReady.remove(temp);
            structure.insert(new PropertyKey(pattern.getPropertyNumber(), pattern.getCadasterName()), pattern.serialize());
            propertyNumber++;
            if (propertyNumber % 1000 == 0) {
                System.out.println(propertyNumber + " properties inserted.");
            }
        }
    }
}
