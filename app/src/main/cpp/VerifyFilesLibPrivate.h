#ifndef VERIFY_FILES_LIB_PRIVATE_H
#define VERIFY_FILES_LIB_PRIVATE_H

#include <string>
#include <stdio.h>

#define VFL_PRINTF          printf
#define VFL_TRACE           VFL_PRINTF
#define VFL_ERROR           VFL_PRINTF

typedef unsigned char       VFL_UCHAR;
typedef signed char         VFL_SCHAR;
typedef unsigned int        VFL_UINT;
typedef signed int          VFL_INT;
typedef unsigned long long  VFL_ULONGLONG;
typedef long long           VFL_LONGLONG;

#endif /* VERIFY_FILES_LIB_PRIVATE_H */
