# set binary name
set(BINARY ${CMAKE_PROJECT_NAME})

# get all .h and .cpp files and add them to the project
file(GLOB_RECURSE SOURCES LIST_DIRECTORIES true *.h *.cpp)
set(SOURCES ${SOURCES})

# set compiler flags for execution
set(CMAKE_CXX_FLAGS "$ENV{JENKINS_CXXFLAGS} -g -I ./")

# add all files to executable
add_executable(${BINARY} ${SOURCES})

# create static library with binaries
add_library(${BINARY}_lib STATIC ${SOURCES})