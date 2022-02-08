QT += testlib
TEMPLATE = app
TARGET = testAll
INCLUDEPATH += .
INCLUDEPATH += ../../../src

QMAKE_LFLAGS += $$(JENKINS_CXXFLAGS)

LIBS += ../../src/Addition.o ../../src/Multiply.o

# Input
SOURCES += Main_TestAll.cpp
HEADERS += Main_TestAll.h