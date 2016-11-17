/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ConvertTypes;

import BPlusTree.IByteConverter;
import java.nio.ByteBuffer;

/**
 * Trieda kluca obalujuca integer. Je potrebne zadefinovat zakladne operacie
 * potrebne na serializaciu a deserializaciu kluca (dlzka serializovaneho kluca,
 * serializacia/deserializacia). Tieto metody implementuje interface
 * IByteConverter.
 *
 * @author korenciak.marek
 */
public class IntegerKey implements IByteConverter {

    private int number = -1;

    public IntegerKey() {
    }

    public IntegerKey(int initNumber) {
        number = initNumber;
    }

    public int getInteger() {
        return number;
    }

    public void setInteger(int initNumber) {
        number = initNumber;
    }

    public int convertedLength() {
        return 4;
    }

    public byte[] toByte() {
        return ByteBuffer.allocate(4).putInt((int) number).array();
    }

    public IByteConverter toKey(byte[] byteArray) {
        return new IntegerKey(ByteBuffer.wrap(byteArray).getInt());
    }

    public int compareTo(Object o) {
        return ((Integer) number).compareTo(((IntegerKey) o).getInteger());
    }

    public String toString() {
        return getInteger() + "";
    }
}
