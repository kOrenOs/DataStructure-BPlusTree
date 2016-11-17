/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package BPlusTree;

import ConvertTypes.IntegerKey;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Pradstavuje zakladnu triedu B+ stromu. Spravuje nacitavanie novych blokov do
 * pamati, traverzovanie blokmi na disku, mazanie/vkladanie. TAktiez obsahuje
 * metody na validovanie celej struktury. Poskytuje interface umoznujuci pracu
 * so strukturou.
 *
 * @author korenciak.marek
 */
public class BPlusTree<Key extends IByteConverter> {

    private BPlusTreeComponent root = null;
    private Class<Key> genericInstance = null;
    private int internalComponentNumber = -1;
    private int leafComponentNumber = -1;
    private long firstOfLinkedListLeaf = -1;
    private ArrayList<BPlusTreeComponent> path = new ArrayList<BPlusTreeComponent>();
    private String structureFileName = null;
    private int dataByteArrayLength = 0;
    private int serializedLength = 0;
    FileCommander structureFileCommander = null;
    private int keyLength = -1;

    private Key createInstanceOfKey(Class<Key> clazz) {
        try {
            return clazz.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            return null;
        }
    }

    public BPlusTree(Class<Key> genInstance, String loadFileName, boolean deleteWholeBlock) {
        structureFileName = loadFileName;
        genericInstance = genInstance;
        keyLength = createInstanceOfKey(genInstance).convertedLength();

        structureFileCommander = new FileCommander(loadFileName, serializedLength, false, deleteWholeBlock);
        serializedLength = structureFileCommander.getSerializedLength();
        structureFileCommander = new FileCommander(loadFileName, serializedLength, false, deleteWholeBlock);

        dataByteArrayLength = structureFileCommander.getDataByteArrayLength();
        leafComponentNumber = structureFileCommander.getLeafComponentNumber();
        firstOfLinkedListLeaf = structureFileCommander.getFirstOfChainedList();
        internalComponentNumber = Math.round((float) ((leafComponentNumber * dataByteArrayLength + leafComponentNumber * keyLength - 8) / (keyLength + 8.0)));

        long address = structureFileCommander.getRootAddress();
        if (address == -1) {
            root = null;
        } else {
            root = new BPlusTreeComponent(genericInstance, leafComponentNumber, -1, null, null, 0, structureFileCommander, dataByteArrayLength, serializedLength, false, keyLength);
            root.deserialize(structureFileCommander.read(address), address);
        }
    }

    public BPlusTree(Class<Key> genInstance, int initComponentCount, int initDataByteArrayLength, String initStructureFileName, boolean deleteWholeBlock) {
        keyLength = createInstanceOfKey(genInstance).convertedLength();
        genericInstance = genInstance;
        dataByteArrayLength = initDataByteArrayLength;
        if (initComponentCount * initDataByteArrayLength + keyLength * initComponentCount >= 2 * keyLength + 3 * 8) {
            leafComponentNumber = initComponentCount;
            internalComponentNumber = (int) ((initComponentCount * dataByteArrayLength + initComponentCount * keyLength - 8) / (keyLength + 8.0));
        } else {
            internalComponentNumber = 2;
            leafComponentNumber = (int) Math.ceil((internalComponentNumber * keyLength + (internalComponentNumber + 1) * 8) / ((dataByteArrayLength + keyLength) * 1.0));
        }

        structureFileName = initStructureFileName;
        serializedLength = serializeSize(keyLength);
        structureFileCommander = new FileCommander(structureFileName, serializedLength, true, deleteWholeBlock);
        structureFileCommander.setDataByteArrayLength(dataByteArrayLength);
        structureFileCommander.setFirstOfChainedList(firstOfLinkedListLeaf);
        structureFileCommander.setLeafComponentNumber(leafComponentNumber);
        structureFileCommander.setSerializedLength(serializedLength);
    }

    public boolean insert(Key key, byte[] data) {
        if (root == null) {
            root = new BPlusTreeComponent(genericInstance, leafComponentNumber, -1, key, data, 0, structureFileCommander, dataByteArrayLength, serializedLength, false, keyLength);
            writeIntoFileAndSetAddress(root, true);
            firstOfLinkedListLeaf = root.getAddress();
            structureFileCommander.setFirstOfChainedList(firstOfLinkedListLeaf);
            return true;
        }
        BPlusTreeComponent temp = find(key);
        if (temp.findKeyIndex(key) != -1) {
            System.out.println(key.toString() + " exist yet.");
            return false;
        }
        BPlusTreeComponent parent = null;

        if (temp.insertToLeaf(key, data)) {
            writeIntoFile(temp, false);
            return true;
        }
        while (temp != null) {
            if (temp.getInternal()) {
                if (temp.getParent() == -1) {
                    root = new BPlusTreeComponent(genericInstance, internalComponentNumber, -1, temp.getMinKey(), null, parent.getAddress(), structureFileCommander, dataByteArrayLength, serializedLength, true, keyLength);
                    writeIntoFileAndSetAddress(root, true);
                    root.insertToInternal(temp);
                    parent.setParent(root.getAddress());
                    temp.deleteMinAndMove();

                    writeIntoFile(root, false);
                    writeIntoFile(parent, false);
                    writeIntoFile(temp, false);
                    temp = null;
                    continue;
                } else {
                    parent = findParentInPath(temp.getParent());
                    parent = path.get(path.size() - 1);
                    if (parent.insertToInternal(temp.getMinKey())) {
                        parent.insertToInternal(temp);
                        temp.deleteMinAndMove();

                        writeIntoFile(parent, false);
                        writeIntoFile(temp, false);
                        temp = null;
                    } else {
                        BPlusTreeComponent temp2 = null;
                        temp2 = parent.divideInternal(temp);

                        writeIntoFile(temp2, false);
                        writeIntoFile(parent, false);

                        temp = temp2;
                    }
                }
            } else {
                if (temp.getParent() == -1) {
                    parent = temp.divideLeaf(key, data);
                    root = new BPlusTreeComponent(genericInstance, internalComponentNumber, -1, parent.getMinKey(), null, temp.getAddress(), structureFileCommander, dataByteArrayLength, serializedLength, true, keyLength);
                    writeIntoFileAndSetAddress(root, true);
                    temp.setParent(root.getAddress());
                    writeIntoFileAndSetAddress(parent, false);
                    root.insertToInternal(parent);

                    if (temp.getRightChaining() != -1) {
                        parent.setChainingRight(temp.getRightChaining());
                    }
                    temp.setChainingRight(parent);
                    writeIntoFile(parent, false);
                    writeIntoFile(root, true);
                    writeIntoFile(temp, false);
                    temp = null;
                    continue;
                } else {
                    BPlusTreeComponent temp2 = null;
                    parent = findParentInPath(temp.getParent());
                    temp2 = temp.divideLeaf(key, data);
                    writeIntoFileAndSetAddress(temp2, false);
                    if (temp.getRightChaining() != -1) {
                        temp2.setChainingRight(temp.getRightChaining());
                    }
                    temp.setChainingRight(temp2);
                    writeIntoFile(temp, false);
                    if (parent.insertToInternal(temp2.getMinKey())) {
                        parent.insertToInternal(temp2);

                        writeIntoFile(temp, false);
                        writeIntoFile(parent, false);
                        temp = null;
                    } else {
                        temp = parent.divideInternal(temp2);

                        writeIntoFile(temp, false);
                        writeIntoFile(parent, false);
                    }
                }
            }
        }
        return true;
    }

    private BPlusTreeComponent findParentInPath(long parentAddress) {
        for (int i = path.size() - 1; i >= 0; i--) {
            if (path.get(i).getAddress() == parentAddress) {
                for (int j = i + 1; j <= path.size() - 1; j++) {
                    path.remove(i + 1);
                }
                return path.get(i);
            }
        }
        return null;
    }

    public BPlusTreeComponent find(Key key) {
        path.clear();
        path.add(root);
        long address = root.find(key);
        BPlusTreeComponent temp = root;
        if (address == root.getAddress()) {
            return root;
        }
        while (temp.getInternal()) {
            temp = new BPlusTreeComponent(genericInstance, internalComponentNumber, structureFileCommander, false, serializedLength, dataByteArrayLength, keyLength);
            temp.deserialize(structureFileCommander.read(address), address);

            path.add(temp);
            address = temp.find(key);
        }
        return temp;
    }

    public byte[] findData(Key key) {
        BPlusTreeComponent temp = find(key);
        return temp.findData(key);
    }

    public boolean updateDataOfKey(Key key, byte[] data) {
        BPlusTreeComponent temp = find(key);
        if (temp.findData(key) != null) {
            temp.setDataAtIndex(key, data);
            writeIntoFile(temp, false);
            return true;
        }
        return false;
    }

    public int count() {
        int count = 0;
        BPlusTreeComponent temp = new BPlusTreeComponent(genericInstance, internalComponentNumber, structureFileCommander, false, serializedLength, dataByteArrayLength, keyLength);
        if (firstOfLinkedListLeaf == -1) {
            return count;
        }
        temp.deserialize(structureFileCommander.read(firstOfLinkedListLeaf), firstOfLinkedListLeaf);
        while (temp != null) {
            if (temp.getKeysCount() == 0) {
                return count;
            }
            count += temp.getKeysCount();
            if (temp.getRightChaining() != -1) {
                temp.deserialize(structureFileCommander.read(temp.getRightChaining()), temp.getRightChaining());
            } else {
                break;
            }
        }
        return count;
    }

    public LinkedList<IntegerKey> writeOutLeafs() {
        LinkedList<IntegerKey> listOfItems = new LinkedList<>();
        BPlusTreeComponent temp = new BPlusTreeComponent(genericInstance, internalComponentNumber, structureFileCommander, false, serializedLength, dataByteArrayLength, keyLength);
        if (firstOfLinkedListLeaf == -1) {
            return listOfItems;
        }
        temp.deserialize(structureFileCommander.read(firstOfLinkedListLeaf), firstOfLinkedListLeaf);
        while (temp != null) {
            listOfItems.addAll(temp.getItemsOfLeaf());
            if (temp.getRightChaining() > 0) {
                temp.deserialize(structureFileCommander.read(temp.getRightChaining()), temp.getRightChaining());
            } else {
                break;
            }
        }
        return listOfItems;
    }

    public boolean delete(Key key) {
        BPlusTreeComponent temp = find(key);
        if (temp.findKeyIndex(key) == -1) {
            return false;
        } else {
            BPlusTreeComponent neighbourLeft = new BPlusTreeComponent(genericInstance, internalComponentNumber, structureFileCommander, false, serializedLength, dataByteArrayLength, keyLength);
            if (temp.getParent() != -1) {
                if (getParentByPath(temp.getParent()).getFirstSon() == temp.getAddress() && temp.getMinKey().compareTo(key) == 0) {
                    Key newKey = (Key) temp.getKeyAtIndex(1);
                    if (newKey == null) {
                        neighbourLeft.deserialize(structureFileCommander.read(getParentByPath(temp.getParent()).getSonAtIndex(1)), getParentByPath(temp.getParent()).getSonAtIndex(1));
                        newKey = (Key) neighbourLeft.getMinKey();
                    }
                    BPlusTreeComponent changeKey = root;
                    int index = 0;
                    int keyIndex = 0;
                    while (changeKey != null) {
                        keyIndex = changeKey.findKeyIndex(key);
                        if (keyIndex != -1) {
                            changeKey.setKeyAtIndex(keyIndex, newKey);
                            writeIntoFile(changeKey, false);
                            break;
                        }
                        changeKey = path.get(index);
                        index++;
                        if (!changeKey.getInternal()) {
                            break;
                        }
                    }
                }
            }

            boolean[] ret = temp.delete(key, getParentByPath(temp.getParent()));
            if (ret[1]) {
                if (temp == root && temp.getKeysCount() == 0) {
                    deleteFromFile(root);
                    return true;
                }
                writeIntoFile(temp, false);
                if (ret[0] == true) {
                    writeIntoFile(getParentByPath(temp.getParent()), false);
                }
                return true;
            }

            BPlusTreeComponent neighbourRight = new BPlusTreeComponent(genericInstance, internalComponentNumber, structureFileCommander, false, serializedLength, dataByteArrayLength, keyLength);
            long[] addresses = getParentByPath(temp.getParent()).findNeighbours(temp.getAddress());

            if (addresses[0] != -1) {
                neighbourLeft.deserialize(structureFileCommander.read(addresses[0]), addresses[0]);
            }
            if (addresses[1] != -1) {
                neighbourRight.deserialize(structureFileCommander.read(addresses[1]), addresses[1]);
            }

            BPlusTreeComponent forChange = getParentByPath(temp.getParent()).deleteFromLeafs(temp, neighbourLeft, neighbourRight, key);
            if (forChange != null) {
                writeIntoFile(forChange, false);
                writeIntoFile(getParentByPath(temp.getParent()), false);
                writeIntoFile(temp, false);
                return true;
            }

            if (temp.getAddress() == firstOfLinkedListLeaf) {
                firstOfLinkedListLeaf = temp.getRightChaining();
                structureFileCommander.setFirstOfChainedList(firstOfLinkedListLeaf);
            }

            BPlusTreeComponent[] componentComplex = new BPlusTreeComponent[2];
            componentComplex = getParentByPath(temp.getParent()).deleteAndCollapseSons(temp, neighbourLeft, neighbourRight, key);

            forChange = new BPlusTreeComponent(genericInstance, internalComponentNumber, structureFileCommander, false, serializedLength, dataByteArrayLength, keyLength);
            if (componentComplex[0].getLeftChaining() == temp.getAddress()) {
                if (temp.getLeftChaining() == -1) {
                    componentComplex[0].setLeftChaining(-1);
                } else {
                    if (componentComplex[0] == neighbourRight && temp.getLeftChaining() == neighbourLeft.getAddress()) {
                        neighbourLeft.setChainingRight(componentComplex[0]);
                        writeIntoFile(neighbourLeft, false);
                    }
                    forChange.deserialize(structureFileCommander.read(temp.getLeftChaining()), temp.getLeftChaining());
                    forChange.setChainingRight(componentComplex[0]);
                    writeIntoFile(forChange, false);
                }
            }
            if (componentComplex[0].getRightChaining() == temp.getAddress()) {
                if (temp.getRightChaining() == -1) {
                    componentComplex[0].setRightChaining(-1);
                } else {
                    if (componentComplex[0] == neighbourLeft && temp.getRightChaining() == neighbourRight.getAddress()) {
                        neighbourRight.setChainingLeft(componentComplex[0]);
                        writeIntoFile(neighbourRight, false);
                    }
                    forChange.deserialize(structureFileCommander.read(temp.getRightChaining()), temp.getRightChaining());
                    forChange.setChainingLeft(componentComplex[0]);
                    writeIntoFile(forChange, false);
                }
            }

            if (componentComplex[1] != root) {
                writeIntoFile(getParentByPath(temp.getParent()), false);
            }
            deleteFromFile(temp);
            writeIntoFile(componentComplex[0], false);

            temp = componentComplex[1];

            while (temp != null) {
                findParentInPath(temp.getAddress());    //path.remove(path.size() - 1);
                if (getParentByPath(temp.getParent()) == null) {
                    deleteFromFile(root);
                    root.deserialize(structureFileCommander.read(temp.getLastSon()), temp.getLastSon());
                    writeIntoFile(root, true);
                    temp = null;
                    return true;
                }

                addresses = getParentByPath(temp.getParent()).findNeighbours(temp.getAddress());
                neighbourRight = new BPlusTreeComponent(genericInstance, internalComponentNumber, structureFileCommander, false, serializedLength, dataByteArrayLength, keyLength);
                neighbourLeft = new BPlusTreeComponent(genericInstance, internalComponentNumber, structureFileCommander, false, serializedLength, dataByteArrayLength, keyLength);

                if (addresses[0] != -1) {
                    neighbourLeft.deserialize(structureFileCommander.read(addresses[0]), addresses[0]);
                }
                if (addresses[1] != -1) {
                    neighbourRight.deserialize(structureFileCommander.read(addresses[1]), addresses[1]);
                }

                componentComplex = getParentByPath(temp.getParent()).collapseInternal(temp, neighbourLeft, neighbourRight);
                if (componentComplex[1] != root) {
                    writeIntoFile(getParentByPath(temp.getParent()), false);
                }
                if (componentComplex[2] == null) {
                    deleteFromFile(temp);
                } else {
                    writeIntoFile(temp, false);
                }

                temp = componentComplex[1];

                writeIntoFile(componentComplex[0], false);

                if (temp != null) {
                    if (getParentByPath(temp.getParent()) == null) {
                        deleteFromFile(root);
                        root.deserialize(structureFileCommander.read(temp.getLastSon()), temp.getLastSon());
                        writeIntoFile(root, true);
                        temp = null;
                    } else {
                        writeIntoFile(getParentByPath(temp.getParent()), false);
                    }
                }
            }
            return true;
        }
    }

    public boolean validateStructure() {
        if (root == null) {
            if (count() == 0) {
                return true;
            }
            throw new NullPointerException();
        }
        LinkedList<BPlusTreeComponent> allComponents = new LinkedList<BPlusTreeComponent>();
        LinkedList<Long> componentsAddress = new LinkedList<Long>();
        BPlusTreeComponent creator = null;
        BPlusTreeComponent temp = null;
        boolean leafPart = false;
        allComponents.add(root);
        while (!allComponents.isEmpty()) {
            temp = allComponents.get(0);
            allComponents.remove(0);
            if (temp.getInternal()) {
                componentsAddress.addAll(temp.getAllSons());
            }
            for (Long component : componentsAddress) {
                creator = new BPlusTreeComponent(genericInstance, internalComponentNumber, structureFileCommander, false, serializedLength, dataByteArrayLength, keyLength);
                creator.deserialize(structureFileCommander.read(component), component);
                allComponents.add(creator);
            }
            componentsAddress.clear();
            if (!temp.chackSonAndKeyValues()) {
                throw new NullPointerException();
            }
            if (leafPart) {
                if (temp.getInternal()) {
                    throw new NullPointerException();
                }
            } else {
                if (!temp.getInternal()) {
                    leafPart = true;
                }
            }
        }
        return true;
    }

    private void writeIntoFileAndSetAddress(BPlusTreeComponent node, boolean rootWrite) {
        node.setAddress(structureFileCommander.write(node.serialize(), rootWrite));
    }

    private void writeIntoFile(BPlusTreeComponent node, boolean rootWrite) {
        structureFileCommander.write(node.serialize(), node.getAddress(), rootWrite);
    }

    private void deleteFromFile(BPlusTreeComponent node) {
        structureFileCommander.delete(node.getAddress());
    }

    private int serializeSize(int keySerializedLength) {
        return 1 + 2 * 4 + 3 * 8 + leafComponentNumber * dataByteArrayLength + leafComponentNumber * keySerializedLength;
    }

    private BPlusTreeComponent getParentByPath(long address) {
        for (BPlusTreeComponent component : path) {
            if (component.getAddress() == address) {
                return component;
            }
        }
        return null;
    }
}
