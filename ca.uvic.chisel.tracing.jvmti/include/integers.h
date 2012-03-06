/*
 * Header for defining integer sizes
 * integers.h
 *
 *  Created on: Nov 26, 2009
 *      Author: delmyers
 */

#ifndef INTEGERS_H_
#define INTEGERS_H_

#include <stdint.h>
#ifndef WIN32
//these are included in mingw.h
typedef char __int8;
typedef int32_t __int32;
typedef  int16_t __int16;
typedef int64_t __int64;
#else
//just to make sure
#include <_mingw.h>
#endif

typedef int8_t __uint8;
typedef uint16_t __uint16;
typedef uint32_t __uint32;
typedef uint64_t __uint64;

#endif /* INTEGERS_H_ */
