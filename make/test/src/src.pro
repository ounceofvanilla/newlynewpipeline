TARGET = testAll
INCLUDEPATH += . ../../src
DEPENDPATH += ../../src
LIBS += -lgtest -L/usr/include/gtest $$(JENKINS_LIBS)

OBJECTS += ../../src/Multiply.o ../../src/Addition.o

# Input
SOURCES += Addition_Test.cpp Main_TestAll.cpp Multiply_Test.cpp
