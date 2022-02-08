cmake_minimum_required(VERSION 2.8)

# CPack configurations
include(InstallRequiredSystemLibraries)
set(CPACK_PACKAGE_DESCRIPTION_SUMMARY "DOCPF project")
set(CPACK_PACKAGE_CONTACT "DOCPF")
set(CPACK_PACKAGE_VENDOR "DOCPF")
set(CPACK_PACKAGE_NAME "exampleapp")
set(CPACK_PACKAGE_VERSION "1.2.3")
set(CPACK_PACKAGE_VERSION_MAJOR "1")
set(CPACK_PACKAGE_VERSION_MINOR "2")
set(CPACK_PACKAGE_VERSION_PATCH "3")
set(CPACK_PACKAGE_INSTALL_DIRECTORY "${CPACK_PACKAGE_NAME}.${CPACK_PACKAGE_VERSION_MAJOR}.${CPACK_PACKAGE_VERSION_MINOR}.${CPACK_PACKAGE_VERSION_PATCH}")
set(CPACK_PACKAGE_EXECUTABLES "${CPACK_PACKAGE_NAME}-${CPACK_PACKAGE_VERSION}")
include(CPack)

# set the project name
project(exampleapp)

# add the project directories
include_directories(src)
add_subdirectory(src)
add_subdirectory(test/src)
