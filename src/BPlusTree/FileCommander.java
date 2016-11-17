/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package BPlusTree;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Trieda pouzivana na pracu so suborom. Nacitavat sa da uz existujuci
 * (naplneny) subor, kedy sa zo zaciatku suboru nacitaju vsetky potrebne
 * informacie pre rekonstrukciu riadiacich casti datovej struktury (adresa
 * korena, prvy volny blok, prvy blok zretazenia, dlzka bloku, dlzka datovej
 * casti, pocet zaznamov v bloku). Ak sa otvori novy subor, tak sa vytvori
 * hlavicka suboru a mozu sa vkladat nove data. Tato trieda zabezpecuje najdenie
 * vhodneho miesta v subore, jeho ulozenie, aktualizovanie, vymazanie, alebo
 * precitanie.
 *
 * @author korenciak.marek
 */
public class FileCommander {

    private String fileName = null;
    private int bytesPerBlock = 0;
    private ArrayList<Long> emptyBlocksAddress = null;
    private RandomAccessFile openFile = null;
    private boolean deleteWholeBlock = false;

    public FileCommander(String initFileName, int initBytesPerBlock, boolean newFile, boolean initDeleteWholeBlock) {
        deleteWholeBlock = initDeleteWholeBlock;
        fileName = initFileName;
        bytesPerBlock = initBytesPerBlock;
        emptyBlocksAddress = new ArrayList<>();
        try {
            File file = new File(fileName);
            if (!file.exists()) {
                newFile = true;
            }
            openFile = new RandomAccessFile(file, "rws");
            if (newFile) {
                openFile.setLength(36);
                saveAddressLong(-1, 0, false);
                saveAddressLong(-1, 8, false);
                saveAddressLong(-1, 16, false);
                saveInt(-1, 24);
                saveInt(-1, 28);
                saveInt(-1, 32);
            } else {
                scanEmptyBlocks();
                openFile.seek(36);
            }
        } catch (IOException e) {
        }
    }

    public long write(byte[] data, boolean root) {
        long address = -1;
        try {
            if (emptyBlocksAddress.size() == 0) {
                address = openFile.length();
                openFile.setLength(address + bytesPerBlock);
            } else {
                address = emptyBlocksAddress.get(0);
                emptyBlocksAddress.remove(0);
                if (emptyBlocksAddress.size() == 0) {
                    saveAddressLong(-1, 8, false);
                } else {
                    saveAddressLong(emptyBlocksAddress.get(0), 8, false);
                }
            }
            openFile.seek(address);
            openFile.write(data);
            if (root) {
                saveAddressLong(address, 0, false);
            }
        } catch (IOException e) {
            return -1;
        }
        return address;
    }

    public long write(byte[] data, long address, boolean root) {
        try {
            openFile.seek(address);
            openFile.write(data);
            if (root) {
                saveAddressLong(address, 0, false);
            }
        } catch (IOException e) {
            return -1;
        }
        return address;
    }

    public byte[] read(long address) {
        byte[] output = null;
        try {
            output = new byte[bytesPerBlock];
            openFile.seek(address);
            openFile.read(output);
        } catch (IOException e) {
            return null;
        }
        return output;
    }

    public void delete(long address) {
        addFreeSpace(address);
        int index = 0;
        try {
            if (emptyBlocksAddress.get(emptyBlocksAddress.size() - 1) == (openFile.length() - bytesPerBlock)) {
                index++;
                for (int i = emptyBlocksAddress.size() - 2; i >= 0; i--) {
                    if (emptyBlocksAddress.get(i) != (openFile.length() - (index + 1) * bytesPerBlock)) {
                        break;
                    }
                    index++;
                }
                openFile.setLength(openFile.length() - index * bytesPerBlock);
            }
            for (int i = 0; i < index; i++) {
                emptyBlocksAddress.remove(emptyBlocksAddress.size() - 1);
            }

            if (emptyBlocksAddress.size() == 0) {
                saveAddressLong(-1, 0, false);
                saveAddressLong(-1, 8, false);
                saveAddressLong(-1, 16, false);
            } else {
                if (index != 0) {
                    saveAddressLong(-1, emptyBlocksAddress.get(emptyBlocksAddress.size() - 1), deleteWholeBlock);
                }
            }
        } catch (IOException e) {
        }
    }

    private void addFreeSpace(long address) {
        emptyBlocksAddress.add(address);
        if (emptyBlocksAddress.size() == 1) {
            saveAddressLong(emptyBlocksAddress.get(0), 8, false);
            saveAddressLong(-1, emptyBlocksAddress.get(emptyBlocksAddress.size() - 1), deleteWholeBlock);
            return;
        }
        boolean stopFlag = false;
        long temp = 0;
        int i = emptyBlocksAddress.size() - 1;
        int index = 0;
        while (!stopFlag && i >= 1) {
            if (emptyBlocksAddress.get(i) > emptyBlocksAddress.get(i - 1)) {
                stopFlag = true;
                index = i;
            } else {
                temp = emptyBlocksAddress.get(i);
                emptyBlocksAddress.set(i, emptyBlocksAddress.get(i - 1));
                emptyBlocksAddress.set(i - 1, temp);
                i--;
            }
        }
        getChaining(index);

    }

    private void getChaining(int index) {
        if (index == 0) {
            saveAddressLong(emptyBlocksAddress.get(0), 8, false);
            saveAddressLong(emptyBlocksAddress.get(1), emptyBlocksAddress.get(0), deleteWholeBlock);
            return;
        }
        if (index == emptyBlocksAddress.size() - 1) {
            saveAddressLong(emptyBlocksAddress.get(emptyBlocksAddress.size() - 1), emptyBlocksAddress.get(emptyBlocksAddress.size() - 2), deleteWholeBlock);
            saveAddressLong(-1, emptyBlocksAddress.get(emptyBlocksAddress.size() - 1), deleteWholeBlock);
            return;
        }
        saveAddressLong(emptyBlocksAddress.get(index), emptyBlocksAddress.get(index - 1), deleteWholeBlock);
        saveAddressLong(emptyBlocksAddress.get(index + 1), emptyBlocksAddress.get(index), deleteWholeBlock);
    }

    public boolean hasNext() {
        try {
            if (openFile.getFilePointer() + bytesPerBlock <= openFile.length()) {
                return true;
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }

    private void scanEmptyBlocks() {
        long temp = getAddressLong(8);
        if (temp != -1) {
            emptyBlocksAddress.add(temp);
        } else {
            return;
        }
        temp = getAddressLong(emptyBlocksAddress.get(emptyBlocksAddress.size() - 1));
        while (temp != -1) {
            emptyBlocksAddress.add(temp);
            temp = getAddressLong(emptyBlocksAddress.get(emptyBlocksAddress.size() - 1));
        }
    }

    public ArrayList getEmpty() {
        return emptyBlocksAddress;
    }

    public long getRootAddress() {
        byte[] output = null;
        try {
            output = new byte[8];
            openFile.seek(0);
            openFile.read(output);
        } catch (IOException e) {
            return -1;
        }
        return ByteBuffer.wrap(output).getLong();
    }

    private void saveAddressLong(long longNumber, long possitionAddress, boolean deleteWholeBlock) {
        try {
            if (deleteWholeBlock) {
                write(ByteBuffer.allocate(bytesPerBlock).array(), possitionAddress, false);
            }
            openFile.seek(possitionAddress);
            openFile.write(ByteBuffer.allocate(8).putLong(longNumber).array());
        } catch (IOException e) {
        }
    }

    public long getAddressLong(long address) {
        byte[] temp = new byte[8];
        try {
            openFile.seek(address);
            openFile.read(temp);
        } catch (IOException e) {
        }
        return ByteBuffer.wrap(temp).getLong();
    }

    private void saveInt(int number, long possition) {
        try {
            openFile.seek(possition);
            openFile.write(ByteBuffer.allocate(4).putInt(number).array());
        } catch (IOException e) {
        }
    }

    public long fileSize() {
        try {
            return openFile.length();
        } catch (IOException e) {
        }
        return -1;
    }

    public void setFirstOfChainedList(long address) {
        saveAddressLong(address, 16, false);
    }

    public long getFirstOfChainedList() {
        byte[] output = null;
        try {
            output = new byte[8];
            openFile.seek(16);
            openFile.read(output);
        } catch (IOException e) {
            return -1;
        }
        return ByteBuffer.wrap(output).getLong();
    }

    public void setDataByteArrayLength(int number) {
        saveInt(number, 24);
    }

    public int getDataByteArrayLength() {
        byte[] output = null;
        try {
            output = new byte[4];
            openFile.seek(24);
            openFile.read(output);
        } catch (IOException e) {
            return -1;
        }
        return ByteBuffer.wrap(output).getInt();
    }

    public void setSerializedLength(int number) {
        saveInt(number, 28);
    }

    public int getSerializedLength() {
        byte[] output = null;
        try {
            output = new byte[4];
            openFile.seek(28);
            openFile.read(output);
        } catch (IOException e) {
            return -1;
        }
        return ByteBuffer.wrap(output).getInt();
    }

    public void setLeafComponentNumber(int number) {
        saveInt(number, 32);
    }

    public int getLeafComponentNumber() {
        byte[] output = null;
        try {
            output = new byte[4];
            openFile.seek(32);
            openFile.read(output);
        } catch (IOException e) {
            return -1;
        }
        return ByteBuffer.wrap(output).getInt();
    }

}
