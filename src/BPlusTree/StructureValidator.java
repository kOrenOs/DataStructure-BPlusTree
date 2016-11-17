/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package BPlusTree;

import ConvertTypes.IntegerKey;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Random;

/**
 * Nahodny generator oeracii. Vytvori novy datovy subor, naplni ho nahodnymi
 * datami, ktorych pocet zadame. Nasledne znova nacita subor a nahodne z neho
 * vymaze urcity pocet dat. Subor sa znova otvori a nahodne sa pridavaju a
 * odoberaju data. Zadat treba meno suboru, pocet zaznamov na jeden blok, dlzku
 * jednej datovej casti a typ, ktory budeme vkladat do struktury.
 *
 * @author korenciak.marek
 */
public class StructureValidator {

    public static void main(String[] args) {
        String saveFile = "test.txt";
        BPlusTree<IntegerKey> test = new BPlusTree(IntegerKey.class, 5, 4, saveFile, true);
        ArrayList<Integer> inserted = new ArrayList<>();

        int unsuccessful = 0;
        int seed = 100000;
        Random randomInstance = new Random();
        ByteBuffer buffer = null;

        IntegerKey temp = null;
        int insertNumber = 0;

        inserted.clear();
        for (int i = 0; i < seed; i++) {
            insertNumber = randomInstance.nextInt(2 * seed);
            temp = new IntegerKey(insertNumber);
            buffer = ByteBuffer.allocate(4).putInt(insertNumber);
            System.out.println(insertNumber);
            if (test.insert(temp, buffer.array())) {
                if (!inserted.add(insertNumber)) {
                    System.out.println(temp);
                }
            } else {
                unsuccessful++;
            }
        }

        test = new BPlusTree(IntegerKey.class, saveFile, true);

        System.out.println("");
        System.out.println("1. round finished: ");
        System.out.println("Inserted: " + test.count());
        System.out.println("Duplicities (uninserted): " + unsuccessful);
        System.out.println("Sum all: " + (test.count() + unsuccessful));
        System.out.println("Count of insert actions: " + seed);
        System.out.println("Structure check " + test.validateStructure());
        System.out.println("");

        int iter = 0;
        int tempInt = 0;
        for (; seed / 2 < inserted.size();) {
            tempInt = randomInstance.nextInt(inserted.size());
            if (test.delete(new IntegerKey(inserted.get(tempInt)))) {
                inserted.remove(tempInt);
                iter--;
                System.out.println(iter);
            } else {
                System.out.println("");
            }
        }

        test = new BPlusTree(IntegerKey.class, saveFile, true);

        System.out.println("2. round finished: " + test.count());
        System.out.println("Structure check " + test.validateStructure());
        System.out.println("");
 
        double tempDouble = 0;
        int computeEndNumber = 51 * seed / 100;
        iter = 0;

        while (inserted.size() < computeEndNumber) {
            iter += 1;
            tempDouble = randomInstance.nextDouble();
            if (tempDouble < 0.8) {
                insertNumber = randomInstance.nextInt(2 * seed);
                temp = new IntegerKey(insertNumber);
                if (test.insert(temp, new byte[5])) {
                    inserted.add(insertNumber);
                    System.out.println("action no. " + iter + " insert: " + test.count() + "/" + computeEndNumber);
                    System.out.println("Structure check " + test.validateStructure());
                    System.out.println("");
                } else {
                    System.out.println("action no. " + iter + " insert: result- number inserted yet");
                    System.out.println("Structure check " + test.validateStructure());
                    System.out.println("");
                }
            } else {
                tempInt = randomInstance.nextInt(inserted.size());
                if (test.delete(new IntegerKey(inserted.get(tempInt)))) { 
                    inserted.remove(tempInt);
                    System.out.println("action no. " + iter + " delete: " + test.count() + "/" + computeEndNumber);
                    System.out.println("Structure check " + test.validateStructure());
                    System.out.println("");
                } else {
                    System.out.println("action no. " + iter + " delete: result- number is not in data structure");
                    System.out.println("Structure check " + test.validateStructure());
                    System.out.println("");
                }
            }
        }
    }
}
