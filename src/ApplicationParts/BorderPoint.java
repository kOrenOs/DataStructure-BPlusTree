/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ApplicationParts;

import java.nio.ByteBuffer;

/**
 * Predstavuje jednu suradnicu GPS, ktora obsahuje suradnicu zemepisnej sirky,
 * dlzky a boolean hodnoty urcujuce konkratnu lokalitu na zemi (pologula a
 * vychod/zapad). Taktiez obsahuje metody na serializaciu a deserializaciu pre
 * umoznenie vkladania do suboru a jeho nacitanie.
 *
 * @author korenciak.marek
 */
class BorderPoint {

    private boolean longitudeWest = false;
    private boolean latitudeNorth = false;
    private double longitudeAxis = -1;
    private double latitudeAxis = -1;

    public BorderPoint() {
    }

    public BorderPoint(boolean initLongitudeWest, double initLongitudeAxis, boolean initLatitudeNorth, double initLatitudeAxis) {
        longitudeAxis = initLongitudeAxis;
        latitudeAxis = initLatitudeAxis;
        longitudeWest = initLongitudeWest;
        latitudeNorth = initLatitudeNorth;
    }

    public double getLongitudeAxis() {
        return longitudeAxis;
    }

    public double getLatitudeAxis() {
        return latitudeAxis;
    }

    public boolean getLongitudeWest() {
        return longitudeWest;
    }

    public boolean getLatitudeNorth() {
        return latitudeNorth;
    }

    public String toString() {
        String ret = String.format("%.4f", latitudeAxis) + " ";
        if (latitudeNorth) {
            ret += "N ";
        } else {
            ret += "S ";
        }
        ret = String.format("%.4f", longitudeAxis) + " ";
        if (longitudeWest) {
            ret += "W ";
        } else {
            ret += "E ";
        }
        return ret;
    }

    public int serializeLength() {
        return 2 * 8 + 2 * 1 + 1;
    }

    public byte[] serialize() {
        int position = 1;
        byte[] ret = new byte[serializeLength()];
        byte bool = 0;
        ret[0] = 1;

        ByteBuffer buf = null;

        buf = ByteBuffer.allocate(8).putDouble(latitudeAxis);
        System.arraycopy(buf.array(), 0, ret, position, 8);
        position += 8;

        bool = (byte) (latitudeNorth ? 1 : 0);
        ret[position] = bool;
        position++;

        buf = ByteBuffer.allocate(8).putDouble(longitudeAxis);
        System.arraycopy(buf.array(), 0, ret, position, 8);
        position += 8;

        bool = (byte) (longitudeWest ? 1 : 0);
        ret[position] = bool;

        return ret;
    }

    public void deserialize(byte[] data) {
        ByteBuffer buf = ByteBuffer.wrap(data);
        int position = 1;
        latitudeAxis = buf.getDouble(position);
        position += 8;
        if (buf.get(position) == 1) {
            latitudeNorth = true;
        } else {
            latitudeNorth = false;
        }
        position++;
        longitudeAxis = buf.getDouble(position);
        position += 8;
        if (buf.get(position) == 1) {
            longitudeWest = true;
        } else {
            longitudeWest = false;
        }
    }
}
