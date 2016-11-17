/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package BPlusTree;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * Trieda predstavujuca komponent/jeden blok B+ stromu. Tieto bloky su ulozene
 * na disku a deserializuju sa len pri ich prehladavani/ praci s nimi. Obsahuju
 * kluce, ktore sluzia na traverzovanie pri internych nodoch alebo na
 * identifikovanie konkretnej datovej casti pri leaf nodoch. Obsahuju informacie
 * identifikaciu jednotlivych casti v deserializovanom byte kode (dlzka
 * serializovaneho kluca, typ serializovaneho kluca, dlzka datovej casti, max
 * pocet vlozenych klucov, adresa rodica,...) Kazdy blok je identifikovany
 * svojou adresou na disku, ktora sa pouziva namiesto referencii v OP.
 *
 * @author korenciak.marek
 */
public class BPlusTreeComponent<Key extends IByteConverter> {

    private boolean internal = false;
    private int blockLength = 0;
    private int recordNumber = -1;
    private int recordInserted = 0;
    private Object[] keyList = null;
    private byte[][] leafData = null;
    private int dataLength = 0;
    private long address = -1;
    private long[] sons = null;
    private long parent = -1;
    private long rightChaining = -1;
    private long leftChaining = -1;
    private Class<Key> genericInstance = null;
    private FileCommander commander = null;
    private int keyLength = -1;

    private Key getKey(int i) {
        return (Key) keyList[i];
    }

    private Key createInstanceOfKey(Class<Key> clazz) {
        try {
            return clazz.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            return null;
        }
    }

    // deserialize
    public BPlusTreeComponent(Class initGenericInstance, int initRecordNumber, FileCommander initCommand,
            boolean initInternal, int initBlockLength, int initDataLength, int initKeyLength) {
        internal = initInternal;
        commander = initCommand;
        recordNumber = initRecordNumber;
        keyInit();
        sonInit();
        blockLength = initBlockLength;
        dataLength = initDataLength;
        genericInstance = initGenericInstance;
        keyLength = initKeyLength;
    }

    //leaf
    public BPlusTreeComponent(Class initGenericInstance, int initRecordNumber, long initParent, Key initKey, byte[] initData,
            long initSon, FileCommander initCommand, int initDataLength, int initBlockLength, boolean initInternal, int initKeyLength) {
        keyLength = initKeyLength;
        genericInstance = initGenericInstance;
        internal = initInternal;
        dataLength = initDataLength;
        commander = initCommand;
        recordNumber = initRecordNumber;
        leafData = new byte[recordNumber][initDataLength];
        parent = initParent;
        keyInit();
        sonInit();
        if (internal) {
            addSon(initSon);
        }
        if (initData != null) {
            addKey(initKey, initData);
        } else {
            if (initKey != null) {
                addKey(initKey);
            }
        }
        blockLength = initBlockLength;
    }

    public long find(Key key) {
        if (!internal) {
            return getAddress();
        }
        for (int i = 0; i < getKeysCount(); i++) {
            if (key.compareTo(keyList[i]) < 0) {
                return sons[i];
            }
            if (key.compareTo(keyList[i]) == 0) {
                return sons[i + 1];
            }
        }
        return getLastSon();
    }

    public byte[] findData(Key key) {
        for (int i = 0; i < recordInserted; i++) {
            if (key.compareTo(keyList[i]) == 0) {
                return leafData[i];
            }
        }
        return null;
    }

    public long[] findNeighbours(long midSon) {
        long[] temp = new long[2];
        int index = 0;
        for (int i = 0; i < getSonsCount(); i++) {
            if (sons[i] == midSon) {
                index = i;
                break;
            }
        }
        if (index - 1 >= 0) {
            temp[0] = sons[index - 1];
        } else {
            temp[0] = -1;
        }
        if (index + 1 <= getLastSonIndex()) {
            temp[1] = sons[index + 1];
        } else {
            temp[1] = -1;
        }

        return temp;
    }

    public boolean insertToInternal(BPlusTreeComponent initSon) {
        return addSon(initSon);
    }

    public boolean insertToInternal(long initSon) {
        return addSon(initSon);
    }

    public boolean insertToInternal(Key initKey) {
        return addKey(initKey);
    }

    public boolean insertToLeaf(Key initKey, byte[] initData) {
        if (getFull()) {
            return false;
        }
        keyList[recordInserted] = initKey;
        leafData[recordInserted] = initData;
        recordInserted++;
        sortKeys();
        return true;
    }

    public BPlusTreeComponent divideLeaf(Key intKey, byte[] initData) {
        BPlusTreeComponent temp = new BPlusTreeComponent(genericInstance, recordNumber, parent, getLastKey(), getLastData(), 0, commander, dataLength, blockLength, false, keyLength);
        keyList[getLastKeyIndex()] = intKey;
        leafData[getLastKeyIndex()] = initData;
        sortKeys();
        int fullCount = recordInserted + 1;
        int divideCount = (int) (Math.ceil(fullCount / 2) - 1);
        for (int i = divideCount + 1; i < fullCount - 1; i++) {
            temp.insertToLeaf(getKey(i), getDataAtIndex(i));
            keyList[i] = null;
            recordInserted--;
        }
        return temp;
    }

    public BPlusTreeComponent divideInternal(BPlusTreeComponent initSon) {
        BPlusTreeComponent temp = new BPlusTreeComponent(genericInstance, recordNumber, commander, true, blockLength, dataLength, keyLength);
        temp.setParent(parent);
        temp.insertToInternal(getLastKey());
        temp.setAddress(commander.write(temp.serialize(), false));
        temp.insertToInternal(getLastSon());
        sons[getLastSonIndex()] = -1;
        keyList[getLastKeyIndex()] = (Key) initSon.getMinKey();
        if (initSon.getInternal()) {
            initSon.deleteMinAndMove();
        }
        sortKeys();
        addSon(initSon);
        int fullCount = recordInserted + 1;
        int divideCount = (int) (Math.ceil(fullCount / 2) - 1);
        for (int i = divideCount + 1; i < fullCount - 1; i++) {
            temp.insertToInternal(sons[i + 1]);
            sons[i + 1] = -1;
            temp.insertToInternal(getKey(i));
            keyList[i] = null;
            recordInserted--;
        }
        temp.sortKeys();
        return temp;
    }

    private void sortKeys() {
        Key tempKey = null;
        byte[] tempByte = null;
        for (int i = 0; i < recordInserted; i++) {
            for (int j = i + 1; j < recordInserted; j++) {
                if (getKey(i).compareTo(getKey(j)) > 0) {
                    tempKey = getKey(i);
                    keyList[i] = keyList[j];
                    keyList[j] = tempKey;

                    if (!internal) {
                        tempByte = leafData[i];
                        leafData[i] = leafData[j];
                        leafData[j] = tempByte;
                    }
                }
            }
        }
    }

    private void setAsSonAtIndex(long son, int index) {
        for (int i = getSonsCount() - 1; i >= index; i--) {
            sons[i + 1] = sons[i];
        }
        sons[index] = son;
    }

    public void deleteMinAndMove() {
        for (int i = 0; i < getKeysCount() - 1; i++) {
            keyList[i] = keyList[i + 1];
        }
        keyList[getLastKeyIndex()] = null;
        recordInserted--;
    }

    public boolean[] delete(Key key, BPlusTreeComponent parent) {
        boolean[] temp = new boolean[2];
        temp[0] = false;
        if (deleteAble(recordInserted)) {
            int keyIndex;
            if (parent != null && !internal) {
                keyIndex = parent.findKeyIndex(key);
                if (keyIndex != -1) {
                    parent.setKeyAtIndex(keyIndex, getKey(1));
                    temp[0] = true;
                }
            }
            keyIndex = findKeyIndex(key);
            keyList[keyIndex] = keyList[getLastKeyIndex()];
            keyList[getLastKeyIndex()] = null;
            if (leafData != null) {
                leafData[keyIndex] = leafData[getLastKeyIndex()];
                leafData[getLastKeyIndex()] = new byte[dataLength];
            }
            recordInserted--;
            allTogetherKey();
            temp[1] = true;
            return temp;
        }
        temp[1] = false;
        return temp;
    }

    public boolean deleteAble(int initRecordInserted) {
        if (initRecordInserted - 1 >= Math.ceil(recordNumber / 2) || parent == -1) {
            return true;
        }
        return false;
    }

    public boolean collapseAble(int recordsToInsert) {
        if (recordsToInsert - 1 + recordInserted <= recordNumber) {
            return true;
        }
        return false;
    }

    public BPlusTreeComponent deleteFromLeafs(BPlusTreeComponent deleteTarget, BPlusTreeComponent neighbourLeft, BPlusTreeComponent neighbourRight, Key key) {
        int sonIndex = 0;
        int keyIndex = 0;
        BPlusTreeComponent forChange = null;
        for (int i = 0; i < getSonsCount(); i++) {
            if (deleteTarget.getAddress() == sons[i]) {
                sonIndex = i;
                break;
            }
        }
        if (sonIndex - 1 >= 0) {
            if (neighbourLeft.deleteAble(neighbourLeft.getKeysCount())) {
                forChange = neighbourLeft;
                keyIndex = sonIndex - 1;
                moveSonsLeaf(deleteTarget, forChange, keyIndex, key, false);
                return forChange;
            }
        }
        if (forChange == null && sonIndex + 1 <= getLastSonIndex()) {
            if (neighbourRight.deleteAble(neighbourRight.getKeysCount())) {
                forChange = neighbourRight;
                keyIndex = sonIndex;
                moveSonsLeaf(deleteTarget, forChange, keyIndex, key, true);
                return forChange;
            }
        }

        return null;
    }

    private void moveSonsLeaf(BPlusTreeComponent deleteTarget, BPlusTreeComponent forChange, int keyIndex, Key key, boolean deleteLeft) {
        boolean indexChange = false;
        if (deleteLeft) {
            if (deleteTarget.getMinKey().compareTo(key) == 0) {
                indexChange = true;
            }
            deleteTarget.addKey(forChange.getKeyAtIndex(0), forChange.getDataAtIndex(0));
            forChange.delete(forChange.getKeyAtIndex(0), this);
            keyList[keyIndex] = (Key) forChange.getKeyAtIndex(0);
            deleteTarget.delete(key, this);
            deleteTarget.allTogetherKey();
            forChange.allTogetherKey();
            deleteTarget.sortKeys();
            forChange.sortKeys();
            if (indexChange && findKeyIndex(key) != -1) {
                setKeyAtIndex(findKeyIndex(key), (Key) deleteTarget.getKeyAtIndex(0));
            }
        } else {
            if (deleteTarget.getMinKey() == key) {
                indexChange = true;
            }
            deleteTarget.addKey(forChange.getKeyAtIndex(forChange.getLastKeyIndex()), forChange.getDataAtIndex(forChange.getLastKeyIndex()));
            forChange.delete(forChange.getLastKey(), this);
            deleteTarget.delete(key, this);
            keyList[keyIndex] = (Key) deleteTarget.getKeyAtIndex(0);
            forChange.sortKeys();
            if (indexChange && findKeyIndex(key) != -1) {
                setKeyAtIndex(findKeyIndex(key), (Key) deleteTarget.getKeyAtIndex(0));
            }
        }
    }

    public BPlusTreeComponent[] deleteAndCollapseSons(BPlusTreeComponent deleteTarget, BPlusTreeComponent neighbourLeft, BPlusTreeComponent neighbourRight, Key key) {
        BPlusTreeComponent[] componentComplex = new BPlusTreeComponent[2];
        int sonIndex = 0;
        int keyIndex = 0;
        BPlusTreeComponent forChange = null;
        for (int i = 0; i < getSonsCount(); i++) {
            if (deleteTarget.getAddress() == sons[i]) {
                sonIndex = i;
                break;
            }
        }
        if (sonIndex - 1 >= 0) {
            if (neighbourLeft.collapseAble(deleteTarget.getKeysCount() - 1)) {
                forChange = neighbourLeft;
                keyIndex = sonIndex - 1;
            }
        }
        if (forChange == null && sonIndex + 1 <= getLastSonIndex()) {
            if (neighbourRight.collapseAble(deleteTarget.getKeysCount() - 1)) {
                forChange = neighbourRight;
                keyIndex = sonIndex;
            }
        }
        componentComplex[0] = forChange;
        componentComplex[1] = collapseSonsOfLeafs(deleteTarget, forChange, sonIndex, keyIndex, key);
        return componentComplex;
    }

    public BPlusTreeComponent collapseSonsOfLeafs(BPlusTreeComponent deleteTarget, BPlusTreeComponent forChange, int sonIndex, int keyIndex, Key key) {
        for (int i = 0; i < deleteTarget.getKeysCount(); i++) {
            if (deleteTarget.getKeyAtIndex(i).compareTo(key) == 0) {
                continue;
            }
            forChange.addKey(deleteTarget.getKeyAtIndex(i), deleteTarget.getDataAtIndex(i));
        }
        keyList[keyIndex] = null;
        sons[sonIndex] = -1;
        recordInserted--;

        allTogetherKey();
        allTogetherSons();

        if (!deleteAble(recordInserted + 1)) {
            return this;
        }

        if (parent == -1 && recordInserted == 0) {
            forChange.setParent(-1);
            return this;
        }

        return null;
    }

    public BPlusTreeComponent[] collapseInternal(BPlusTreeComponent deleteTarget, BPlusTreeComponent neighbourLeft, BPlusTreeComponent neighbourRight) {
        BPlusTreeComponent[] componentComplex = new BPlusTreeComponent[3];
        int sonIndex = 0;
        int keyIndex = 0;
        BPlusTreeComponent forCollapse = null;
        BPlusTreeComponent forChange = null;
        for (int i = 0; i < getSonsCount(); i++) {
            if (deleteTarget.getAddress() == sons[i]) {
                sonIndex = i;
                break;
            }
        }
        if (sonIndex - 1 >= 0) {
            if (neighbourLeft.collapseAble(deleteTarget.getKeysCount())) {
                forCollapse = neighbourLeft;
                keyIndex = sonIndex - 1;
            }
            if (neighbourLeft.deleteAble(neighbourLeft.getKeysCount())) {
                forChange = neighbourLeft;
                keyIndex = sonIndex - 1;
                if (forChange != null) {
                    moveSonsInternal(deleteTarget, forChange, keyIndex, false);
                    componentComplex[0] = forChange;
                    componentComplex[1] = null;
                    componentComplex[2] = this;
                    return componentComplex;
                }
            }
        }
        if (sonIndex + 1 <= getLastSonIndex()) {
            if (neighbourRight.collapseAble(deleteTarget.getKeysCount())) {
                forCollapse = neighbourRight;
                keyIndex = sonIndex;
            }
            if (neighbourRight.deleteAble(neighbourRight.getKeysCount())) {
                forChange = neighbourRight;
                keyIndex = sonIndex;
                if (forChange != null) {
                    moveSonsInternal(deleteTarget, forChange, keyIndex, true);
                    componentComplex[0] = forChange;
                    componentComplex[1] = null;
                    componentComplex[2] = this;
                    return componentComplex;
                }
            }
        }

        componentComplex[0] = forCollapse;
        componentComplex[1] = collapseSonsOfInternal(deleteTarget, forCollapse, sonIndex, keyIndex);
        componentComplex[2] = null;

        return componentComplex;
    }

    private BPlusTreeComponent collapseSonsOfInternal(BPlusTreeComponent deleteTarget, BPlusTreeComponent forCollapse, int sonIndex, int keyIndex) {
        forCollapse.addKey(getKey(keyIndex));
        int keysInsertedOfDeleteTarget = deleteTarget.getKeysCount();
        for (int i = 0; i < keysInsertedOfDeleteTarget; i++) {
            forCollapse.addKey(deleteTarget.getKeyAtIndex(i));
            forCollapse.addSon(deleteTarget.getSonAtIndex(i));
        }
        forCollapse.addSon(deleteTarget.getLastSon());
        sons[sonIndex] = -1;
        keyList[keyIndex] = null;
        recordInserted--;
        allTogetherKey();
        allTogetherSons();

        if (!deleteAble(recordInserted + 1)) {
            return this;
        }

        if (parent == -1 && recordInserted == 0) {
            forCollapse.setParent(-1);
            return this;
        }

        return null;
    }

    private void moveSonsInternal(BPlusTreeComponent deleteTarget, BPlusTreeComponent forChange, int keyIndex, boolean deletingLeft) {
        if (deletingLeft) {
            deleteTarget.addKey(getKey(keyIndex));
            deleteTarget.addSon(forChange.getSonAtIndex(0));
            keyList[keyIndex] = (Key) forChange.getMinKey();
            forChange.delete(forChange.getMinKey(), this);
            forChange.removeSonAtIndex(0);
        } else {
            deleteTarget.addKey(getKey(keyIndex));
            deleteTarget.addSon(forChange.getLastSon());
            keyList[keyIndex] = (Key) forChange.getLastKey();
            forChange.removeSonAtIndex(forChange.getLastSonIndex());
            forChange.delete(forChange.getLastKey(), this);
        }
    }

    private void allTogetherKey() {
        int move = 0;
        for (int i = 0; i < recordNumber; i++) {
            if (keyList[i] == null) {
                move++;
            }
            if (keyList[i] != null) {
                keyList[i - move] = keyList[i];
                if (move != 0) {
                    keyList[i] = null;
                }
            }
        }
        sortKeys();
    }

    private void allTogetherSons() {
        int move = 0;
        for (int i = 0; i < recordNumber + 1; i++) {
            if (sons[i] == -1) {
                move++;
            }
            if (sons[i] != -1) {
                sons[i - move] = sons[i];
                if (move != 0) {
                    sons[i] = -1;
                }
            }
        }
    }

    public long getParent() {
        return parent;
    }

    public boolean getFull() {
        if (recordInserted == recordNumber) {
            return true;
        }
        return false;
    }

    public void setKeyAtIndex(int index, Key newKey) {
        keyList[index] = newKey;
    }

    public boolean getInternal() {
        return internal;
    }

    public Key getKeyAtIndex(int index) {
        return getKey(index);
    }

    public long getSonAtIndex(int index) {
        return sons[index];
    }

    public void setParent(long initParent) {
        parent = initParent;
    }

    public void setRightChaining(long nextLeaf) {
        rightChaining = nextLeaf;
    }

    public void setChainingRight(long nextLeaf) {
        BPlusTreeComponent temp = new BPlusTreeComponent(genericInstance, recordNumber, commander, false, blockLength, dataLength, keyLength);
        if (nextLeaf != -1) {
            temp.deserialize(commander.read(nextLeaf), nextLeaf);
        }
        setChainingRight(temp);
        commander.write(temp.serialize(), temp.getAddress(), false);
    }

    public void setChainingRight(BPlusTreeComponent nextLeaf) {
        rightChaining = nextLeaf.getAddress();
        nextLeaf.setLeftChaining(this.getAddress());
    }

    public void setLeftChaining(long prevLeaf) {
        leftChaining = prevLeaf;
    }

    public void setChainingLeft(long prevLeaf) {
        leftChaining = prevLeaf;
        if (prevLeaf != -1) {
            BPlusTreeComponent temp = new BPlusTreeComponent(genericInstance, recordNumber, commander, false, blockLength, dataLength, keyLength);
            temp.deserialize(commander.read(prevLeaf), prevLeaf);
            temp.setLeftChaining(this.getAddress());
        }
    }

    public void setChainingLeft(BPlusTreeComponent prevLeaf) {
        leftChaining = prevLeaf.getAddress();
        prevLeaf.setRightChaining(this.getAddress());
    }

    public long getLeftChaining() {
        return leftChaining;
    }

    public long getRightChaining() {
        return rightChaining;
    }

    public Key getMinKey() {
        return getKey(0);
    }

    public int findKeyIndex(Key key) {
        for (int i = 0; i < recordInserted; i++) {
            if (key.compareTo(keyList[i]) == 0) {
                return i;
            }
        }
        return -1;
    }

    public boolean chackSonAndKeyValues() {
        if (getInternal()) {
            if (getKeysCount() + 1 != getSonsCount()) {
                return false;
            }

            BPlusTreeComponent temp = new BPlusTreeComponent(genericInstance, recordNumber, commander, false, blockLength, dataLength, keyLength);

            LinkedList<Key> values = null;
            for (int i = 0; i < getKeysCount(); i++) {
                temp.deserialize(commander.read(sons[i]), sons[i]);
                values = temp.getItemsOfLeaf();
                for (Key value : values) {
                    if (value.compareTo(keyList[i]) > 0) {
                        return false;
                    }
                }
            }
            temp.deserialize(commander.read(getLastSon()), getLastSon());
            values = temp.getItemsOfLeaf();
            for (Key value : values) {
                if (value.compareTo(getLastKey()) < 0) {
                    return false;
                }
            }
        }
        if (getParent() != -1 && getKeysCount() < Math.ceil(recordNumber / 2)) {
            return false;
        }
        if (getKeysCount() > recordNumber) {
            return false;
        }

        for (int i = 1; i < getKeysCount(); i++) {
            if (getKey(i).compareTo(getKey(i - 1)) <= 0) {
                return false;
            }
        }
        return true;
    }

    private int getSonsCount() {
        for (int i = 0; i < sons.length; i++) {
            if (sons[i] == -1) {
                return i;
            }
        }
        return recordNumber + 1;
    }

    public LinkedList<Long> getAllSons() {
        LinkedList<Long> listOfSons = new LinkedList<Long>();
        for (int i = 0; i < getSonsCount(); i++) {
            listOfSons.add(sons[i]);
        }
        return listOfSons;
    }

    public long getLastSon() {
        return sons[recordInserted];
    }

    public long getFirstSon() {
        return sons[0];
    }

    private int getLastSonIndex() {
        return recordInserted;
    }

    private boolean addSon(long nextSon) {
        BPlusTreeComponent temp = new BPlusTreeComponent(genericInstance, recordNumber, commander, false, blockLength, dataLength, keyLength);
        temp.deserialize(commander.read(nextSon), nextSon);

        return addSon(temp);
    }

    private boolean addSon(BPlusTreeComponent nextSon) {
        if (getSonsCount() > recordNumber) {
            return false;
        }

        nextSon.setParent(this.getAddress());
        if (this.getMinKey() == null) {
            sons[getSonsCount()] = nextSon.getAddress();
            return true;
        }

        Key minKey = (Key) nextSon.getMinKey();
        int index = getSonsCount();
        for (int i = 0; i < getKeysCount(); i++) {
            if (minKey.compareTo(keyList[i]) < 0) {
                index = i;
                break;
            }
        }
        setAsSonAtIndex(nextSon.getAddress(), index);

        commander.write(nextSon.serialize(), nextSon.getAddress(), false);

        return true;
    }

    private void removeSonAtIndex(int index) {
        sons[index] = -1;
        allTogetherSons();
    }

    public int getKeysCount() {
        return recordInserted;
    }

    private Key getLastKey() {
        return getKey(getLastKeyIndex());
    }

    private int getLastKeyIndex() {
        return recordInserted - 1;
    }

    private boolean addKey(Key key) {
        if (recordInserted >= recordNumber) {
            return false;
        }
        keyList[recordInserted] = key;
        recordInserted++;
        sortKeys();
        return true;
    }

    private boolean addKey(Key key, byte[] data) {
        if (recordInserted >= recordNumber) {
            return false;
        }
        keyList[recordInserted] = key;
        leafData[recordInserted] = data;
        recordInserted++;
        sortKeys();
        return true;
    }

    private void keyInit() {
        keyList = new Object[recordNumber];
        for (int i = 0; i < keyList.length; i++) {
            keyList[i] = null;
        }
    }

    private void sonInit() {
        sons = new long[recordNumber + 1];
        for (int i = 0; i < recordNumber + 1; i++) {
            sons[i] = -1;
        }
    }

    public int getRecordNumber() {
        return recordNumber;
    }

    public LinkedList<Key> getItemsOfLeaf() {
        LinkedList<Key> temp = new LinkedList<>();
        for (int i = 0; i < recordInserted; i++) {
            temp.add(getKey(i));
        }
        return temp;
    }

    public long getAddress() {
        return address;
    }

    public void setAddress(long initAddress) {
        address = initAddress;
    }

    private byte[] keySerialize(int i) {
        return getKey(i).toByte();
    }

    private void keyDeserialize(byte[] inputData, int initRecordInserted) {
        Key pattern = createInstanceOfKey(genericInstance);
        int address = 0;

        for (int i = 0; i < initRecordInserted; i++) {
            byte[] n = Arrays.copyOfRange(inputData, address, address + keyLength);
            addKey((Key) pattern.toKey(Arrays.copyOfRange(inputData, address, address + keyLength)));
            address += keyLength;
        }
    }

    public byte[] serialize() {
        byte[] ret = new byte[blockLength];
        int position = 1;

        ByteBuffer buf = null;

        if (internal) {
            ret[0]=1;
        } else {
            ret[0]=0;
        }

        buf = ByteBuffer.allocate(4).putInt(recordNumber);
        System.arraycopy(buf.array(), 0, ret, position, 4);
        position += 4;

        buf = ByteBuffer.allocate(4).putInt(recordInserted);
        System.arraycopy(buf.array(), 0, ret, position, 4);
        position += 4;

        byte[] temp = null;
        for (int i = 0; i < recordNumber; i++) {
            if (keyList[i] == null) {
                temp = ByteBuffer.allocate(keyLength).array();
            } else {
                temp = keySerialize(i);
            }
            System.arraycopy(temp, 0, ret, position, keyLength);
            position += temp.length;
        }
        if (internal) {
            for (int i = 0; i < recordNumber + 1; i++) {
                if (sons[i] != -1) {
                    System.arraycopy(ByteBuffer.allocate(8).putLong(sons[i]).array(), 0, ret, position, 8);
                    position += 8;
                } else {
                    System.arraycopy(ByteBuffer.allocate(8).putLong(-1).array(), 0, ret, position, 8);
                    position += 8;
                }
            }
            position = ret.length - 24;
        } else {
            for (int i = 0; i < recordNumber; i++) {
                System.arraycopy(leafData[i], 0, ret, position, leafData[i].length);
                position += dataLength;
            }
        }
        System.arraycopy(ByteBuffer.allocate(8).putLong(parent).array(), 0, ret, position, 8);
        position += 8;

        System.arraycopy(ByteBuffer.allocate(8).putLong(leftChaining).array(), 0, ret, position, 8);
        position += 8;

        System.arraycopy(ByteBuffer.allocate(8).putLong(rightChaining).array(), 0, ret, position, 8);
        position += 8;
        return ret;
    }

    public void deserialize(byte[] byteArray, long localAddress) {
        address = localAddress;

        keyInit();
        sonInit();
        recordInserted = 0;
        parent = -1;
        ByteBuffer buf = null;
        buf = ByteBuffer.wrap(byteArray);
        int position = 1;
        int count = 0;
        long temp = 0;
        
        if (byteArray[0] == 0) {
            internal = false;
        }
        if (byteArray[0] == 1) {
            internal = true;
        }

        count = buf.getInt(position);
        recordNumber = count;
        if (internal) {
            keyInit();
            sonInit();
        } else {
            keyInit();
            leafData = new byte[recordNumber][dataLength];
        }
        position += 4;

        count = buf.getInt(position);
        position += 4;

        keyDeserialize(Arrays.copyOfRange(byteArray, position, position + recordNumber * keyLength), count);
        position += recordNumber * keyLength;

        if (internal) {
            for (int i = 0; i < count + 1; i++) {
                temp = buf.getLong(position);
                sons[i] = temp;
                position += 8;
            }
            position = byteArray.length - 24;
        } else {
            for (int i = 0; i < recordNumber; i++) {
                leafData[i] = Arrays.copyOfRange(byteArray, position, position + dataLength);
                position += dataLength;
            }
        }

        temp = buf.getLong(position);
        parent = temp;
        position += 8;
        temp = buf.getLong(position);
        leftChaining = temp;
        position += 8;
        temp = buf.getLong(position);
        rightChaining = temp;
        position += 8;
    }

    private byte[] getLastData() {
        return leafData[getLastKeyIndex()];
    }

    private byte[] getDataAtIndex(int index) {
        return leafData[index];
    }

    public void setDataAtIndex(Key key, byte[] data) {
        leafData[findKeyIndex(key)] = data;
    }
}
