# Additional clean files
cmake_minimum_required(VERSION 3.16)

if("${CONFIG}" STREQUAL "" OR "${CONFIG}" STREQUAL "")
  file(REMOVE_RECURSE
  "CMakeFiles/dropin_autogen.dir/AutogenUsed.txt"
  "CMakeFiles/dropin_autogen.dir/ParseCache.txt"
  "dropin_autogen"
  )
endif()
