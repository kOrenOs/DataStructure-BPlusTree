# DataStructure-BPlusTree

This project was one of the schoolworks for the course Data structure. The main point was to implement data structure- B+ Tree, validate it and demonstrate implemented code. Fake data are:
•	Cadaster
•	Property
•	Borders

In the Cadaste can be more properties. It is possible to add to property also borders points. Geographical correctness of borders is not controlled.

This structure demonstrates usage of data from the disc. All data are still stored in the disc. To the RAM memory is uploaded just small part of data for data usage. This structure is designed for devices with small RAM memory.

Program is possible to use with GUI mode (ApplicationGUI). It is possible to auto generate, insert, edit, find, delete, save data. Moreover, it is able to check blocks on the disc and see structure of used data structure.

Validation of the structure is in the StructureValidator class. It will generate random data and try to insert them, save, again reload and delete this data. After every action is data structure checked.

In autumn 2015
