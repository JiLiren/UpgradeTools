#ifndef VERIFY_FILES_LIB_H
#define VERIFY_FILES_LIB_H

#include <string>
#include "jni.h"
#if defined(__cplusplus)
extern "C" {
#endif

typedef int (* VFL_Progress_Callback)( void * user_ptr, int percent );

typedef enum VFL_STATUS
{
    VFL_STATUS_OK,
    VFL_STATUS_HASH_LIST_PARSE_FAILED,
    VFL_STATUS_READ_FILE_FAILED,
    VFL_STATUS_MISSING_FILES,
    VFL_STATUS_HASH_WRONG,
}VFL_STATUS;

typedef struct VFL_Parameters 
{
    const char *            map_data_path;
    const char *            map_data_hash_file_path;
    VFL_Progress_Callback   progress_cb;
    void *                  user_ptr;
    VFL_STATUS              status;
} VFL_Parameters;

typedef int VFL_BOOLEAN;

#define VFL_TRUE    (1)
#define VFL_FALSE   (0)

VFL_BOOLEAN VFL_ValidateFiles( VFL_Parameters * parameters );
std::string GetFileHash(JNIEnv* env, std::string& strFileName);

#if defined(__cplusplus)
};
#endif

#endif /* VERIFY_FILES_LIB_H */
