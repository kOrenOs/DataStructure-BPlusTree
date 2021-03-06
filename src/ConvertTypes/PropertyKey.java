/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ConvertTypes;

import BPlusTree.IByteConverter;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

/**
 * Obalovacia trieda, na vytvorenie klucu nehnutelnosti (meno katastra, cislo
 * nehnutelnosti). Implementuje interface IByteConverter, ktory poskytuje vsetky
 * metody na serializaciu/deserializaciu.
 *
 * @author korenciak.marek
 */
public class PropertyKey implements IByteConverter {

    private int propertyNumber = 0;
    private String cadasterName = null;

    public PropertyKey() {
    }

    public PropertyKey(int initPropertyNumber, String initCadasterName) {
        initCadasterName = trimEnd(initCadasterName);
        if (initCadasterName.length() > 20) {
            cadasterName = initCadasterName.substring(0, 20);
        } else {
            cadasterName = initCadasterName;
        }
        propertyNumber = initPropertyNumber;
    }

    public String getCadasterName() {
        return cadasterName;
    }

    public String trimEnd(String param) {
        int index = param.length() - 1;
        while (param.charAt(index) == ' ') {
            index--;
        }
        return param.substring(0, index + 1);
    }

    public int getPropertyNumber() {
        return propertyNumber;
    }

    @Override
    public int convertedLength() {
        return 24;
    }

    @Override
    public byte[] toByte() {
        int position = 0;
        byte[] ret = new byte[convertedLength()];

        ByteBuffer buf = null;
        try {
            byte[] temp = cadasterName.getBytes("UTF-8");
            System.arraycopy(temp, 0, ret, position, temp.length);
            position += 20;

            buf = ByteBuffer.allocate(4).putInt(propertyNumber);
            System.arraycopy(buf.array(), 0, ret, position, 4);
            position += 4;

        } catch (UnsupportedEncodingException e) {
        }
        return ret;
    }

    @Override
    public IByteConverter toKey(byte[] byteArray) {
        ByteBuffer buf = ByteBuffer.wrap(byteArray);
        int position = 0;

        cadasterName = new String(byteArray, position, 20);
        position += 20;

        propertyNumber = buf.getInt(position);
        position += 4;
        return new PropertyKey(propertyNumber, cadasterName);
    }

    @Override
    public int compareTo(Object o) {
        String Ocadaster = trimEnd(((PropertyKey) o).getCadasterName());
        if (Ocadaster.compareTo(cadasterName) > 0) {
            return -1;
        }
        if (Ocadaster.compareTo(cadasterName) < 0) {
            return 1;
        }
        if (((PropertyKey) o).getPropertyNumber() > propertyNumber) {
            return -1;
        }
        if (((PropertyKey) o).getPropertyNumber() < propertyNumber) {
            return 1;
        }
        return 0;
    }

    public String toString() {
        return cadasterName + " " + propertyNumber;
    }

}
