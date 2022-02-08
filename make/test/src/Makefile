# Determine O/S
ifeq '$(findstring ;,$(PATH))' ';'
    detected_OS := Windows
else
    detected_OS := $(shell cat /etc/os-release)
    detected_OS := $(patsubst CYGWIN%,Cygwin,$(detected_OS))
    detected_OS := $(patsubst MSYS%,MSYS,$(detected_OS))
    detected_OS := $(patsubst MINGW%,MSYS,$(detected_OS))
endif

ifneq (,$(findstring Windows,$(detected_OS)))
	detected_OS := Windows
    CXXFLAGS += -D WIN32
endif
ifneq (,$(findstring Ubuntu,$(detected_OS)))
	detected_OS := Ubuntu
    CXXFLAGS += -D LINUX
    GTEST_LIB = /usr/lib/libgtest.a
endif
ifneq (,$(findstring Debian,$(detected_OS)))
	detected_OS := Debian
    CXXFLAGS += -D LINUX
    GTEST_LIB = /usr/lib/libgtest.a
endif
ifneq (,$(findstring Red Hat,$(detected_OS)))
	detected_OS := Red Hat
    CXXFLAGS += -D LINUX
    GTEST_LIB = /usr/lib64/libgtest.so
endif
ifneq (,$(findstring CentOS,$(detected_OS)))
	detected_OS := CentOS
    CXXFLAGS += -D LINUX
    GTEST_LIB = /usr/lib64/libgtest.so
endif
ifneq (,$(findstring Darwin,$(detected_OS)))        # Mac OS X
	detected_OS := Darwin
    CXXFLAGS += -D OSX
endif

$(info "Detected OS $(detected_OS)")

CXX = g++
CXXFLAGS += $(JENKINS_CXXFLAGS) -g -lgtest -pthread
INCS = -I./ -I../../src
OBJS = ../../src/Addition.o Addition_Test.o ../../src/Multiply.o Multiply_Test.o

testAll: $(OBJS)
	$(CXX) $(CXXFLAGS) $(INCS) -o testAll  Main_TestAll.cpp $(OBJS) $(GTEST_LIB)

.cpp.o:
	$(CXX) $(CXXFLAGS) -c $< -o $@ $(INCS)

clean:
	rm -f testAll *.o testAll.xml
	