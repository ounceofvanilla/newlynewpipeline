# set binary name
set(BINARY testAll)

#Enables testing for this directory and below
enable_testing()

# get all .h and .cpp files and add them to the project
file(GLOB_RECURSE TEST_SOURCES LIST_DIRECTORIES false *.h *.cpp)
set(SOURCES ${TEST_SOURCES})

# set compiler flags for execution
set(CMAKE_CXX_FLAGS "$ENV{JENKINS_CXXFLAGS} -g -lgtest -pthread")

# add all files to executable
add_executable(${BINARY} ${TEST_SOURCES})

# add test
add_test(success_test testAll)
set_tests_properties(success_test PROPERTIES PASS_REGULAR_EXPRESSION "PASSED")

# add directory where source files are
link_directories(../../src)

target_link_libraries(${BINARY} exampleapp_lib)

# check which OS is being used
string(REGEX MATCH ";" VERSION "${PATH}")

if(VERSION STREQUAL ";")
	set("${DETECTED_OS}" "Windows")
else()
	execute_process(COMMAND cat /etc/os-release OUTPUT_VARIABLE DETECTED_OS)
	string(REGEX REPLACE "CYGWIN*" "Cygwin" DETECTED_OS "${DETECTED_OS}")
	string(REGEX REPLACE "MSYS*" "MSYS" DETECTED_OS "${DETECTED_OS}")
	string(REGEX REPLACE "MINGW*" "MSYS" DETECTED_OS "${DETECTED_OS}")
endif()

# check if using Windows
if(DETECTED_OS STREQUAL "Windows")
   set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -D WIN32")
endif()

if(DETECTED_OS MATCHES "Debian|Ubuntu")
   target_link_libraries(${BINARY} /usr/lib/libgtest.a)
   set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -D Linux")
endif()

if(DETECTED_OS MATCHES "CentOS|RHEL|Fedora")
    target_link_libraries(${BINARY} /usr/lib64/libgtest.so)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -D Linux")
endif()

if(DETECTED_OS MATCHES "MacOS")
   set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -D OSX")
endif()