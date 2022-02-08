TARGET = exampleapp
INCLUDEPATH += .
QMAKE_CXXFLAGS += $$(JENKINS_CXXFLAGS)
LIBS += $$(JENKINS_LIBS)

# Input
HEADERS += Addition.h Multiply.h
SOURCES += Addition.cpp ExampleApp.cpp Multiply.cpp