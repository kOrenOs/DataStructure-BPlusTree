/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package BPlusTree;

/**
 * Interface zarucujuci pritomnost konverznych metod v datovom type kluca.
 *
 * @author korenciak.marek
 */
public interface IByteConverter extends Comparable {

    int convertedLength();

    byte[] toByte();

    Comparable toKey(byte[] byteArray);

    String toString();
}
