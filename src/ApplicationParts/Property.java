/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ApplicationParts;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * Trieda predstavujuca samotnu nehnutelnost. Obsahuje meno aktastra a cislo
 * ktastra ako identifikacia nehnutelnosti, nasledne popis a zoznam hraniznych
 * bodov. Taktiez obsahuje serializaciu a deserializaciu, pomocou ktorej sa
 * vklada/vybera struktura z B+ stromu.
 *
 * @author korenciak.marek
 */
public class Property implements Comparable {

    private int propertyNumber = -1;
    private String cadasterName = null;
    private String description = null;
    private LinkedList<BorderPoint> border = null;
    private int maxBorderPoints = 90;

    public Property() {
        border = new LinkedList<>();
    }

    public Property(String initCadasterName, int initPropertyNumber, String initDescription) {
        setDescription(initDescription);
        setCadasterName(initCadasterName);
        propertyNumber = initPropertyNumber;
        border = new LinkedList<>();
    }

    public boolean addBorderPoint(BorderPoint aditionalBorderPoint) {
        if (border.size() < maxBorderPoints) {
            border.add(aditionalBorderPoint);
            return true;
        } else {
            return false;
        }
    }

    public boolean removeBorderPoint(int index) {
        if (border.remove(index) != null) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        String ret = "Property: \n";
        ret += "Cadaster name: " + cadasterName + "\n";
        ret += "Property number: " + propertyNumber + "\n";
        ret += "Property description: " + description + "\n";
        ret += "Border: \n";
        for (BorderPoint point : border) {
            ret += point.toString() + "\n";
        }
        return ret;
    }

    public void setCadasterName(String name) {
        name = trimEnd(name);
        if (name.length() > 20) {
            name = name.substring(0, 20);
        }
        cadasterName = name;
    }

    public void setDescription(String desc) {
        desc = trimEnd(desc);
        if (desc.length() > 60) {
            desc = desc.substring(0, 60);
        }
        description = desc;
    }

    public void setPropertyNumber(int number) {
        propertyNumber = number;
    }

    public String getCadasterName() {
        return cadasterName;
    }

    public int getPropertyNumber() {
        return propertyNumber;
    }

    public String getDescription() {
        return description;
    }

    public String trimEnd(String param) {
        int index = param.length() - 1;
        if (param.length() == 0) {
            return param;
        }
        while (param.charAt(index) == ' ') {
            index--;
        }
        return param.substring(0, index + 1);
    }

    public void updateBorderPoint(int index, boolean initLongitudeWest, double initLongitudeAxis, boolean initLatitudeNorth, double initLatitudeAxis) {
        BorderPoint temp = new BorderPoint(initLongitudeWest, initLongitudeAxis, initLatitudeNorth, initLatitudeAxis);
        border.set(index, temp);
    }

    public boolean deleteBorderPoint(int index) {
        if (border.remove(index) != null) {
            return true;
        }
        return false;
    }

    public Object[] getBorder(int index) {
        BorderPoint BP = border.get(index);
        Object[] temp = new Object[4];
        temp[0] = BP.getLatitudeAxis();
        if (BP.getLatitudeNorth()) {
            temp[1] = "true";
        } else {
            temp[1] = "false";
        }
        temp[2] = BP.getLongitudeAxis();
        if (BP.getLongitudeWest()) {
            temp[3] = "true";
        } else {
            temp[3] = "false";
        }
        return temp;
    }

    public Object[][] getBordesrs() {
        BorderPoint BP = null;
        Object[][] temp = new Object[border.size()][5];
        for (int i = 0; i < border.size(); i++) {
            BP = border.get(i);
            temp[i][0] = i;
            temp[i][1] = BP.getLatitudeAxis();
            if (BP.getLatitudeNorth()) {
                temp[i][2] = "true";
            } else {
                temp[i][2] = "false";
            }
            temp[i][3] = BP.getLongitudeAxis();
            if (BP.getLongitudeWest()) {
                temp[i][4] = "true";
            } else {
                temp[i][4] = "false";
            }
        }
        return temp;
    }

    @Override
    public int compareTo(Object o) {
        String Ocadaster = trimEnd(((Property) o).getCadasterName());
        if (Ocadaster.compareTo(cadasterName) > 0) {
            return -1;
        }
        if (Ocadaster.compareTo(cadasterName) < 0) {
            return 1;
        }
        if (((Property) o).getPropertyNumber() > propertyNumber) {
            return -1;
        }
        if (((Property) o).getPropertyNumber() < propertyNumber) {
            return 1;
        }
        return 0;
    }

    public int serializeLength() {
        return 20 + 60 + 4 + maxBorderPoints * 19;
    }

    public byte[] serialize() {
        int position = 0;
        byte[] ret = new byte[serializeLength()];

        ByteBuffer buf = null;

        byte[] temp = null;

        try {
            temp = cadasterName.getBytes("UTF-8");
            System.arraycopy(temp, 0, ret, position, temp.length);
            position += 20;

            buf = ByteBuffer.allocate(4).putInt(propertyNumber);
            System.arraycopy(buf.array(), 0, ret, position, 4);
            position += 4;

            temp = description.getBytes("UTF-8");
            System.arraycopy(temp, 0, ret, position, temp.length);
            position += 60;
        } catch (UnsupportedEncodingException e) {
        }

        for (BorderPoint bord : border) {
            System.arraycopy(bord.serialize(), 0, ret, position, bord.serializeLength());
            position += bord.serializeLength();
        }
        return ret;
    }

    public void deserialize(byte[] data) {
        border = new LinkedList<>();
        ByteBuffer buf = ByteBuffer.wrap(data);
        int position = 0;

        cadasterName = new String(data, position, 20);
        cadasterName = trimEnd(cadasterName);
        position += 20;

        propertyNumber = buf.getInt(position);
        position += 4;

        description = new String(data, position, 60);
        description = trimEnd(description);
        position += 60;

        BorderPoint point = null;
        byte[] temp = Arrays.copyOfRange(data, position, position + 19);

        while (temp[0] != 0) {
            point = new BorderPoint();
            point.deserialize(temp);
            border.add(point);
            position += 19;
            temp = Arrays.copyOfRange(data, position, position + 19);
        }
    }
}
