/*
 * standard_ptrs.h
 *
 * Creates standard shared pointers for common data types.
 *
 *  Created on: 3-Mar-2009
 *      Author: Del Myers
 */

#ifndef STANDARD_PTRS_H_
#define STANDARD_PTRS_H_

#include "integers.h"
#include <boost/shared_ptr.hpp>
#include <boost/shared_array.hpp>


/**
 * Usage: array_8 my_array(new __int8[size]);
 */
typedef boost::shared_array<__int8> array_8;
typedef boost::shared_array<__int16> array_16;
typedef boost::shared_array<char> array_char;
typedef boost::shared_array<int> array_int;

#endif /* STANDARD_PTRS_H_ */
