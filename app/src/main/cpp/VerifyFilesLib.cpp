#if defined(UPF_OS_IS_WINNT) || defined(UPF_OS_IS_WINCE)
#include <windows.h>
#else
#include <sys/types.h>
#include <sys/stat.h>
//#include <unistd.h>
#include <errno.h>
#include <time.h>
#endif

#include <memory.h>
#include <string.h>
#include <map>
#include <string>
#include <vector>
#include <ctype.h>

#include "VerifyFilesLib.h"
#include "VerifyFilesLibPrivate.h"
#include "GetFileMD5.h"

namespace VFL_NS {

/*  The following tests optimise behaviour on little-endian
    machines, where there is no need to reverse the byte order
    of 32 bit words in the MD5 computation.  By default,
    HIGHFIRST is defined, which indicates we're running on a
    big-endian (most significant byte first) machine, on which
    the byteReverse function in md5.c must be invoked. However,
    byteReverse is coded in such a way that it is an identity
    function when run on a little-endian machine, so calling it
    on such a platform causes no harm apart from wasting time. 
    If the platform is known to be little-endian, we speed
    things up by undefining HIGHFIRST, which defines
    byteReverse as a null macro.  Doing things in this manner
    insures we work on new platforms regardless of their byte
    order.  */

#define HIGHFIRST

#ifdef __i386__
#undef HIGHFIRST
#endif

/*  On machines where "long" is 64 bits, we need to declare
    MD5_UINT32 as something guaranteed to be 32 bits.  */

#ifdef __alpha
typedef VFL_UINT MD5_UINT32;
#else
typedef VFL_UINT MD5_UINT32;
#endif

typedef struct MD5Context {
        MD5_UINT32 buf[4];
        MD5_UINT32 bits[2];
        unsigned char in[64];
}tag_MD5Context;

extern void MD5Init(struct MD5Context *ctx);
extern void MD5Update(struct MD5Context *ctx, unsigned char *buf, unsigned len);
extern void MD5Final(unsigned char digest[16], struct MD5Context *ctx);
extern void MD5Transform(MD5_UINT32 buf[4], MD5_UINT32 in[16]);

/*
 * This is needed to make RSAREF happy on some MS-DOS compilers.
 */
typedef struct MD5Context MD5_CTX;

/*  Define CHECK_HARDWARE_PROPERTIES to have main,c verify
    byte order and MD5_UINT32 settings.  */
#define CHECK_HARDWARE_PROPERTIES



/* for memcpy() */

/*
 * This code implements the MD5 message-digest algorithm.
 * The algorithm is due to Ron Rivest.	This code was
 * written by Colin Plumb in 1993, no copyright is claimed.
 * This code is in the public domain; do with it what you wish.
 *
 * Equivalent code is available from RSA Data Security, Inc.
 * This code has been tested against that, and is equivalent,
 * except that you don't need to include two pages of legalese
 * with every copy.
 *
 * To compute the message digest of a chunk of bytes, declare an
 * MD5Context structure, pass it to MD5Init, call MD5Update as
 * needed on buffers full of bytes, and then call MD5Final, which
 * will fill a supplied 16-byte array with the digest.
 */

/* Brutally hacked by John Walker back from ANSI C to K&R (no
   prototypes) to maintain the tradition that Netfone will compile
   with Sun's original "cc". */


#ifndef HIGHFIRST
#define byteReverse(buf, len)	/* Nothing */
#else
/*
 * Note: this code is harmless on little-endian machines.
 */
void byteReverse( unsigned char *buf, unsigned longs )
{
    MD5_UINT32 t;
    do {
	t = (MD5_UINT32) ((unsigned) buf[3] << 8 | buf[2]) << 16 |
	    ((unsigned) buf[1] << 8 | buf[0]);
	*(MD5_UINT32 *) buf = t;
	buf += 4;
    } while (--longs);
}
#endif

/*
 * Start MD5 accumulation.  Set bit count to 0 and buffer to mysterious
 * initialization constants.
 */
void MD5Init( struct MD5Context *ctx )
{
    ctx->buf[0] = 0x67452301;
    ctx->buf[1] = 0xefcdab89;
    ctx->buf[2] = 0x98badcfe;
    ctx->buf[3] = 0x10325476;

    ctx->bits[0] = 0;
    ctx->bits[1] = 0;
}

/*
 * Update context to reflect the concatenation of another buffer full
 * of bytes.
 */
void MD5Update( struct MD5Context *ctx, unsigned char *buf, unsigned len )
{
    MD5_UINT32 t;

    /* Update bitcount */

    t = ctx->bits[0];
    if ((ctx->bits[0] = t + ((MD5_UINT32) len << 3)) < t)
	ctx->bits[1]++; 	/* Carry from low to high */
    ctx->bits[1] += len >> 29;

    t = (t >> 3) & 0x3f;	/* Bytes already in shsInfo->data */

    /* Handle any leading odd-sized chunks */

    if (t) {
	unsigned char *p = (unsigned char *) ctx->in + t;

	t = 64 - t;
	if (len < t) {
	    memcpy(p, buf, len);
	    return;
	}
	memcpy(p, buf, t);
	byteReverse(ctx->in, 16);
	MD5Transform(ctx->buf, (MD5_UINT32 *) ctx->in);
	buf += t;
	len -= t;
    }
    /* Process data in 64-byte chunks */

    while (len >= 64) {
	memcpy(ctx->in, buf, 64);
	byteReverse(ctx->in, 16);
	MD5Transform(ctx->buf, (MD5_UINT32 *) ctx->in);
	buf += 64;
	len -= 64;
    }

    /* Handle any remaining bytes of data. */

    memcpy(ctx->in, buf, len);
}

/*
 * Final wrapup - pad to 64-byte boundary with the bit pattern 
 * 1 0* (64-bit count of bits processed, MSB-first)
 */
void MD5Final( unsigned char digest[16], struct MD5Context *ctx )
{
    unsigned count;
    unsigned char *p;

    /* Compute number of bytes mod 64 */
    count = (ctx->bits[0] >> 3) & 0x3F;

    /* Set the first char of padding to 0x80.  This is safe since there is
       always at least one byte free */
    p = ctx->in + count;
    *p++ = 0x80;

    /* Bytes of padding needed to make 64 bytes */
    count = 64 - 1 - count;

    /* Pad out to 56 mod 64 */
    if (count < 8) {
	/* Two lots of padding:  Pad the first block to 64 bytes */
	memset(p, 0, count);
	byteReverse(ctx->in, 16);
	MD5Transform(ctx->buf, (MD5_UINT32 *) ctx->in);

	/* Now fill the next block with 56 bytes */
	memset(ctx->in, 0, 56);
    } else {
	/* Pad block to 56 bytes */
	memset(p, 0, count - 8);
    }
    byteReverse(ctx->in, 14);

    /* Append length in bits and transform */
    ((MD5_UINT32 *) ctx->in)[14] = ctx->bits[0];
    ((MD5_UINT32 *) ctx->in)[15] = ctx->bits[1];

    MD5Transform(ctx->buf, (MD5_UINT32 *) ctx->in);
    byteReverse((unsigned char *) ctx->buf, 4);
    memcpy(digest, ctx->buf, 16);
    memset(ctx, 0, sizeof(ctx));        /* In case it's sensitive */
}


/* The four core functions - F1 is optimized somewhat */

/* #define F1(x, y, z) (x & y | ~x & z) */
#define F1(x, y, z) (z ^ (x & (y ^ z)))
#define F2(x, y, z) F1(z, x, y)
#define F3(x, y, z) (x ^ y ^ z)
#define F4(x, y, z) (y ^ (x | ~z))

/* This is the central step in the MD5 algorithm. */
#define MD5STEP(f, w, x, y, z, data, s) \
	( w += f(x, y, z) + data,  w = w<<s | w>>(32-s),  w += x )

/*
 * The core of the MD5 algorithm, this alters an existing MD5 hash to
 * reflect the addition of 16 longwords of new data.  MD5Update blocks
 * the data and converts bytes into longwords for this routine.
 */
void MD5Transform( MD5_UINT32 buf[4], MD5_UINT32 in[16] )
{
    register MD5_UINT32 a, b, c, d;

    a = buf[0];
    b = buf[1];
    c = buf[2];
    d = buf[3];

    MD5STEP(F1, a, b, c, d, in[0] + 0xd76aa478, 7);
    MD5STEP(F1, d, a, b, c, in[1] + 0xe8c7b756, 12);
    MD5STEP(F1, c, d, a, b, in[2] + 0x242070db, 17);
    MD5STEP(F1, b, c, d, a, in[3] + 0xc1bdceee, 22);
    MD5STEP(F1, a, b, c, d, in[4] + 0xf57c0faf, 7);
    MD5STEP(F1, d, a, b, c, in[5] + 0x4787c62a, 12);
    MD5STEP(F1, c, d, a, b, in[6] + 0xa8304613, 17);
    MD5STEP(F1, b, c, d, a, in[7] + 0xfd469501, 22);
    MD5STEP(F1, a, b, c, d, in[8] + 0x698098d8, 7);
    MD5STEP(F1, d, a, b, c, in[9] + 0x8b44f7af, 12);
    MD5STEP(F1, c, d, a, b, in[10] + 0xffff5bb1, 17);
    MD5STEP(F1, b, c, d, a, in[11] + 0x895cd7be, 22);
    MD5STEP(F1, a, b, c, d, in[12] + 0x6b901122, 7);
    MD5STEP(F1, d, a, b, c, in[13] + 0xfd987193, 12);
    MD5STEP(F1, c, d, a, b, in[14] + 0xa679438e, 17);
    MD5STEP(F1, b, c, d, a, in[15] + 0x49b40821, 22);

    MD5STEP(F2, a, b, c, d, in[1] + 0xf61e2562, 5);
    MD5STEP(F2, d, a, b, c, in[6] + 0xc040b340, 9);
    MD5STEP(F2, c, d, a, b, in[11] + 0x265e5a51, 14);
    MD5STEP(F2, b, c, d, a, in[0] + 0xe9b6c7aa, 20);
    MD5STEP(F2, a, b, c, d, in[5] + 0xd62f105d, 5);
    MD5STEP(F2, d, a, b, c, in[10] + 0x02441453, 9);
    MD5STEP(F2, c, d, a, b, in[15] + 0xd8a1e681, 14);
    MD5STEP(F2, b, c, d, a, in[4] + 0xe7d3fbc8, 20);
    MD5STEP(F2, a, b, c, d, in[9] + 0x21e1cde6, 5);
    MD5STEP(F2, d, a, b, c, in[14] + 0xc33707d6, 9);
    MD5STEP(F2, c, d, a, b, in[3] + 0xf4d50d87, 14);
    MD5STEP(F2, b, c, d, a, in[8] + 0x455a14ed, 20);
    MD5STEP(F2, a, b, c, d, in[13] + 0xa9e3e905, 5);
    MD5STEP(F2, d, a, b, c, in[2] + 0xfcefa3f8, 9);
    MD5STEP(F2, c, d, a, b, in[7] + 0x676f02d9, 14);
    MD5STEP(F2, b, c, d, a, in[12] + 0x8d2a4c8a, 20);

    MD5STEP(F3, a, b, c, d, in[5] + 0xfffa3942, 4);
    MD5STEP(F3, d, a, b, c, in[8] + 0x8771f681, 11);
    MD5STEP(F3, c, d, a, b, in[11] + 0x6d9d6122, 16);
    MD5STEP(F3, b, c, d, a, in[14] + 0xfde5380c, 23);
    MD5STEP(F3, a, b, c, d, in[1] + 0xa4beea44, 4);
    MD5STEP(F3, d, a, b, c, in[4] + 0x4bdecfa9, 11);
    MD5STEP(F3, c, d, a, b, in[7] + 0xf6bb4b60, 16);
    MD5STEP(F3, b, c, d, a, in[10] + 0xbebfbc70, 23);
    MD5STEP(F3, a, b, c, d, in[13] + 0x289b7ec6, 4);
    MD5STEP(F3, d, a, b, c, in[0] + 0xeaa127fa, 11);
    MD5STEP(F3, c, d, a, b, in[3] + 0xd4ef3085, 16);
    MD5STEP(F3, b, c, d, a, in[6] + 0x04881d05, 23);
    MD5STEP(F3, a, b, c, d, in[9] + 0xd9d4d039, 4);
    MD5STEP(F3, d, a, b, c, in[12] + 0xe6db99e5, 11);
    MD5STEP(F3, c, d, a, b, in[15] + 0x1fa27cf8, 16);
    MD5STEP(F3, b, c, d, a, in[2] + 0xc4ac5665, 23);

    MD5STEP(F4, a, b, c, d, in[0] + 0xf4292244, 6);
    MD5STEP(F4, d, a, b, c, in[7] + 0x432aff97, 10);
    MD5STEP(F4, c, d, a, b, in[14] + 0xab9423a7, 15);
    MD5STEP(F4, b, c, d, a, in[5] + 0xfc93a039, 21);
    MD5STEP(F4, a, b, c, d, in[12] + 0x655b59c3, 6);
    MD5STEP(F4, d, a, b, c, in[3] + 0x8f0ccc92, 10);
    MD5STEP(F4, c, d, a, b, in[10] + 0xffeff47d, 15);
    MD5STEP(F4, b, c, d, a, in[1] + 0x85845dd1, 21);
    MD5STEP(F4, a, b, c, d, in[8] + 0x6fa87e4f, 6);
    MD5STEP(F4, d, a, b, c, in[15] + 0xfe2ce6e0, 10);
    MD5STEP(F4, c, d, a, b, in[6] + 0xa3014314, 15);
    MD5STEP(F4, b, c, d, a, in[13] + 0x4e0811a1, 21);
    MD5STEP(F4, a, b, c, d, in[4] + 0xf7537e82, 6);
    MD5STEP(F4, d, a, b, c, in[11] + 0xbd3af235, 10);
    MD5STEP(F4, c, d, a, b, in[2] + 0x2ad7d2bb, 15);
    MD5STEP(F4, b, c, d, a, in[9] + 0xeb86d391, 21);

    buf[0] += a;
    buf[1] += b;
    buf[2] += c;
    buf[3] += d;
}

}; // VFL_NS

class VFL_MD5Hash 
{
public:

    VFL_MD5Hash( void )
    {
        reset();
    }

    ~VFL_MD5Hash( void )
    {
    }

    void reset( void )
    {
        memset( &mContext, 0, sizeof(mContext) );
        memset( &mResult, 0, sizeof(mResult) );

        VFL_NS::MD5Init( &mContext );
    }

    void addData( const char * buffer, int size )
    {
        VFL_NS::MD5Update( &mContext, (unsigned char *)buffer, size );
    }

    const VFL_UCHAR * result( void )
    {
        VFL_NS::MD5Final( &mResult[0], &mContext );

        return mResult;
    }

    int resultSize( void )
    {
        return (int)sizeof(mResult);
    }

private:

    VFL_NS::MD5Context  mContext;
    VFL_UCHAR           mResult[16];
};

#if defined(UPF_OS_IS_WINNT) || defined(UPF_OS_IS_WINCE)

int VFL_GetLastError( void )
{
    return (int)::GetLastError();
}

#if defined(UNICODE) || defined(_UNICODE)
static std::wstring VFL_A2T( const std::string & str )
{
    std::wstring ret;

    int count = ::MultiByteToWideChar( CP_ACP, 0, str.c_str(), str.size(), NULL, 0 );

    if (count > 0)
    {
        ret.resize( count );

        ::MultiByteToWideChar( CP_ACP, 0, str.c_str(), str.size(), &ret[0], ret.size() );
    }
    else
    {
        VFL_ERROR("[VFL] MultiByteToWideChar failed errno = %d\n", VFL_GetLastError());
    }

    return ret;
}

static std::string VFL_T2A( const std::wstring & str )
{
    std::string ret;

    int count = ::WideCharToMultiByte( CP_ACP, 0, str.c_str(), str.size(), NULL, 0, NULL, NULL );

    if (count > 0)
    {
        ret.resize( count );

        ::WideCharToMultiByte( CP_ACP, 0, str.c_str(), str.size(), &ret[0], ret.size(), NULL, NULL );
    }
    else
    {
        VFL_ERROR("[VFL] WideCharToMultiByte failed errno = %d\n", VFL_GetLastError());
    }

    return ret;
}

#else

static std::string VFL_A2T( const std::string & str )
{
    return str;
}

static std::string VFL_T2A( const std::string & str )
{
    return str;
}

#endif

static 
int VFL_GetFileSize( const char * file_path, VFL_LONGLONG & file_size )
{
    int ret = -1;
    WIN32_FILE_ATTRIBUTE_DATA attr_data;
    memset( &attr_data, 0, sizeof(attr_data) );
    BOOL status = FALSE;

    file_size = 0;

    status = ::GetFileAttributesEx( VFL_A2T(file_path).c_str(), GetFileExInfoStandard, &attr_data );

    if (status)
    {
        file_size = attr_data.nFileSizeHigh * 0x100000000LL + attr_data.nFileSizeLow;
        ret = 0;
    }
    else
    {
        VFL_ERROR("[VFL] GetFileAttributesEx failed file %s errno = %d\n", file_path, VFL_GetLastError());
    }

    return ret;
}

#else

int VFL_GetLastError( void )
{
    return errno;
}

static 
int VFL_GetFileSize( const char * file_path, VFL_LONGLONG & file_size )
{
    int ret = -1;
    int status = 0;
    struct stat st_stat;
    memset( &st_stat, 0, sizeof(st_stat) );

    status = ::stat( file_path, &st_stat );

    if (0 == status)
    {
        file_size = st_stat.st_size;
        ret = 0;
    }
    else
    {
        VFL_ERROR("[VFL] stat failed file %s errno = %d\n", file_path, VFL_GetLastError());
    }

    return ret;
}

#endif

static 
int VFL_NormalPath( const std::string & path, std::string & normal_path )
{
    int ret = -1;
    size_t i;
    
    normal_path = path;

    for (i = 0; i < normal_path.size(); ++i )
    {
        char ch = normal_path[i];

        if ('\\' == ch)
        {
            normal_path[i] = '/';
        }
    }

    ret = 0;

    return ret;
}

static 
int VFL_JoinPath( const std::string & path1, const std::string & path2, std::string & result )
{
    int ret = -1;
    int status = 0;

    std::string tpath = path1 + path2;

    status = VFL_NormalPath( tpath, result );

    if (0 != status)
    {
        VFL_ERROR("[VFL][VFL_JoinPath] normalize path error path1 %s path2 %s\n", path1.c_str(), path2.c_str());
        goto LABEL_EXIT;
    }

    ret = 0;

LABEL_EXIT:;

    return ret;
}

static 
void VFL_TrimBuffer( char * buffer )
{
    int len = strlen( buffer );
    int first_pos = 0;
    int last_pos = len - 1;

    if (len <= 0)
    {
        return ;
    }

    for (; first_pos < len; ++first_pos )
    {
        if (!isspace( (VFL_UCHAR)buffer[first_pos] ))
        {
            break;
        }
    }

    for (; last_pos > first_pos; --last_pos )
    {
        if (!isspace( (VFL_UCHAR)buffer[last_pos] ))
        {
            break;
        }
    }

    int new_len = last_pos - first_pos + 1;

    memmove( buffer, buffer + first_pos, new_len );

    buffer[new_len] = '\0';

    return ;
}

static 
void VFL_SplitString( const std::string & in_str, char sep, std::vector<std::string> & string_array )
{
    size_t pos = 0, pos_2 = 0;

    std::string sub ;

    std::string well_in_str( in_str );

    size_t len = well_in_str.length();
    do 
    {
        if ( (well_in_str[len - 1] == '\r' ) || (well_in_str[len - 1] == '\n' ) )
        {
            --len;
        }
        else if ( well_in_str[len - 1] == sep)
        {
            --len;
        }
        else
        {
            break;
        }
    } while (1);

    well_in_str.resize( len );

    string_array.resize( 0 );

    do 
    {
        pos_2 = well_in_str.find( sep, pos );

        sub = well_in_str.substr( pos, pos_2 - pos );

        string_array.push_back( sub );

        pos = pos_2 + 1;

    } while ( pos != std::string::npos + 1 );

}

static
int VFL_DataToHex( const VFL_UCHAR * in_dat, int in_dat_size, std:: string & out_str )
{
    int i = 0, j = 0;
    int ret = 0;

    const char * HEXSYMSET = "0123456789abcdef";

    out_str.resize( in_dat_size * 2 );

    for (i = 0; i < in_dat_size; ++i)
    {
        j = (in_dat[i] >> 4) & 0xf;

        out_str[i * 2] = HEXSYMSET[j];

        j = in_dat[i] & 0xf;

        out_str[i * 2 + 1] = HEXSYMSET[j];
    }

    return ret;
}



class VFL_FileValidate
{
  friend std::string GetFileHash(JNIEnv* env, std::string& strFileName);
private:

    class VFL_Progress
    {
    private:

        enum CONSTANTS
        {
            FILE_INFO_SIZE = 512,
        };

    public:

        VFL_Progress( void )
        {
            mValidate = NULL;
            Reset();
        }

        void SetClassPtr( VFL_FileValidate * validate )
        {
            mValidate = validate;
        }

        void Reset( void )
        {
            mReadFileStat = 0;
            mReadByteStat = 0;
            memset( mPercents, 0, sizeof(mPercents) );
        }

        int OnFileUpdate( void )
        {
            int ret = -1;

            ++mReadFileStat;

            ret = UpdateProgress();

            return ret;
        }

        int OnReadUpdate( int read_bytes )
        {
            int ret = -1;

            mReadByteStat += read_bytes;

            ret = UpdateProgress();

            return ret;
        }

    private:

        int UpdateProgress( void )
        {
            int ret = -1;

            VFL_LONGLONG curr_size = GetCurrSize();

            if (curr_size < 0)
            {
                VFL_ERROR("[VFL] error GetCurrSize() %lld\n", curr_size);
                return ret;
            }

            VFL_LONGLONG all_size = GetAllSize();

            if (all_size <= 0)
            {
                VFL_ERROR("[VFL] error GetAllSize() %lld\n", all_size);
                return ret;
            }

            int percent = (int)(100.0 * curr_size / all_size);

            ++(mPercents[percent]);

            if (1 == mPercents[percent])
            {
                ret = DoProgressCall( percent );
            }
            else
            {
                ret = 0;
            }

            return ret;
        }

        VFL_LONGLONG GetCurrSize( void )
        {
            VFL_LONGLONG ret = 0;

            ret += mReadFileStat * FILE_INFO_SIZE;
            ret += mReadByteStat;

            return ret;
        }

        VFL_LONGLONG GetAllSize( void )
        {
            VFL_LONGLONG ret = 0;

            ret += mValidate->mHashFileContainer.size() * FILE_INFO_SIZE;
            ret += mValidate->mTotalFileSize;

            return ret;
        }

        int DoProgressCall( int percent )
        {
            int ret = -1;

            if (NULL == mValidate->mParameters->progress_cb)
            {
                VFL_ERROR("[VFL] error progress_cb NULL ptr\n");
                return ret;
            }

            int status = mValidate->mParameters->progress_cb( 
                mValidate->mParameters->user_ptr,
                percent );

            if (0 == status)
            {
                ret = 0;
            }
            else
            {
                VFL_ERROR("[VFL] error progress_cb status %d\n", status);
            }

            return ret;
        }

    private:

        int                 mReadFileStat;

        VFL_LONGLONG        mReadByteStat;

        VFL_FileValidate *  mValidate;

        int                 mPercents[101];
    };

public:

    VFL_FileValidate( void )
    {
        mParameters = NULL;
        mTotalFileSize = 0;
        mProgress.SetClassPtr( this );
    }

    int ValidateFiles( const std::string & map_data_path, const std::string & map_data_hash_file_path )
    {
        int ret = -1;
        int status = 0;

        if (NULL == mParameters)
        {
            VFL_ERROR("[VFL] error mParameters NULL\n");
            goto LABEL_EXIT;
        }

        status = ParseHashFile( map_data_hash_file_path );

        if (0 != status)
        {
            goto LABEL_EXIT;
        }

        status = CalculateFileSize( map_data_path, mTotalFileSize );

        if (0 != status)
        {
            goto LABEL_EXIT;
        }

        VFL_TRACE( "[VFL] mTotalFileSize = %lld\n", mTotalFileSize );

        status = CheckFiles( map_data_path );

        if (0 != status)
        {
            goto LABEL_EXIT;
        }

        ret = 0;

LABEL_EXIT:;

        return ret;
    }

    int CalculateFileSize( const std::string & map_data_path, VFL_LONGLONG & total_file_size )
    {
        int ret = -1;
        std::string full_file_path;
        VFL_LONGLONG file_size = 0;
        CONTAINER_TYPE::iterator i;
        int status = 0;

        if (mHashFileContainer.size() <= 0)
        {
            VFL_ERROR( "[VFL][CalculateFileSize] mHashFileContainer is empty\n" );
            goto LABEL_EXIT;
        }

        total_file_size = 0;

        for (i = mHashFileContainer.begin(); i != mHashFileContainer.end(); ++i )
        {
            status = VFL_JoinPath( map_data_path, i->first, full_file_path );

            if (0 != status)
            {
                goto LABEL_EXIT;
            }

            status = VFL_GetFileSize( full_file_path.c_str(), file_size );

            if (0 != status)
            {
                mParameters->status = VFL_STATUS_MISSING_FILES;
                goto LABEL_EXIT;
            }

            total_file_size += file_size;
        }

        ret = 0;

LABEL_EXIT:;

        return ret;
    }

    int ParseHashFile( const std::string & map_data_hash_file_path )
    {
        int ret = -1;
        const int MAX_LEN = 1024;
        char buffer[MAX_LEN + 1] = {0};
        FILE * fh = NULL;
        std::string buf;
        char * fg_status = NULL;
        std::vector<std::string> listArray;
        int skipLinesCount = 0;

        mHashFileContainer.clear();

        fh = fopen( map_data_hash_file_path.c_str(), "rb" );

        if (NULL == fh)
        {
            VFL_ERROR("[VFL] cannot open file %s errno = %d\n", map_data_hash_file_path.c_str(), VFL_GetLastError() );
            goto LABEL_EXIT;
        }

        for(;;)
        {
            fg_status = fgets( buffer, MAX_LEN, fh );

            if (NULL == fg_status)
            {
                break;
            }

            VFL_TrimBuffer( buffer );

            buf = buffer;

            if ((buf.size() > 0) && ('#' == buf[0]))
            {
                ++skipLinesCount;
                continue;
            }

            VFL_SplitString( buf, ',', listArray );

            if (listArray.size() < 2)
            {
                VFL_ERROR( "[VFL] found bad line %s\n", buf.c_str() );
                ++skipLinesCount;

                continue;
            }
            else
            {
                mHashFileContainer.insert( std::pair< std::string, std::string >(listArray[0], listArray[1]) );
            }

        }

        if ((mHashFileContainer.size() <= 0))
        {
            VFL_ERROR("[VFL] invalid hash list file %s\n", map_data_hash_file_path.c_str() );
            mParameters->status = VFL_STATUS_HASH_LIST_PARSE_FAILED;
            ret = -1;
        }
        else
        {
            ret = 0;
        }

LABEL_EXIT:;

        if (NULL != fh)
        {
            fclose( fh );
            fh = NULL;
        }

        return ret;
    }

    int CheckFiles( const std::string & map_data_path )
    {
        int ret = -1;
        std::string full_file_path;
        CONTAINER_TYPE::iterator i;
        int status = 0;
        std::string hash;

        if (mHashFileContainer.size() <= 0)
        {
            VFL_ERROR( "[VFL][CheckFiles] mHashFileContainer is empty\n" );
            goto LABEL_EXIT;
        }

        for (i = mHashFileContainer.begin(); i != mHashFileContainer.end(); ++i )
        {
            status = VFL_JoinPath( map_data_path, i->first, full_file_path );

            if (0 != status)
            {
                goto LABEL_EXIT;
            }

            status = CalculateFileHash( full_file_path, hash );

            if (0 != status)
            {
                goto LABEL_EXIT;
            }

            if (i->second != hash)
            {
                mParameters->status = VFL_STATUS_HASH_WRONG;
                VFL_ERROR("[VFL][CheckFiles] hash error file %s hash %s correct hash %s\n", 
                    full_file_path.c_str(), hash.c_str(), i->second.c_str());
                goto LABEL_EXIT;
            }
        }

        ret = 0;

LABEL_EXIT:;

        return ret;
    }

    void SetParam( VFL_Parameters * parameters )
    {
        mParameters = parameters;
    }

    void Reset( void )
    {
        mHashFileContainer.clear();
        mTotalFileSize = 0;
        mProgress.Reset();
    }

private:

    int CalculateFileHash( const std::string& file_path, std::string & hash )

    {
        int ret = -1;
        int status = 0;
        VFL_LONGLONG file_size = 0;
        const int READ_BLOCK_SIZE = 64;
        char buffer[READ_BLOCK_SIZE] = {0};
        int read_size = 0;
        int read_ret = 0;
        FILE * fh = NULL;
        VFL_MD5Hash hashObj;

        status = VFL_GetFileSize( file_path.c_str(), file_size );

        if (0 != status)
        {
            mParameters->status = VFL_STATUS_MISSING_FILES;
            goto LABEL_EXIT;
        }

        fh = fopen( file_path.c_str(), "rb" );

        if (NULL == fh)
        {
            VFL_ERROR("[VFL][CalculateFileHash] open file %s failed errno = %d\n", file_path.c_str(), VFL_GetLastError());
            goto LABEL_EXIT;
        }

//         status = mProgress.OnFileUpdate();
// 
//         if (0 != status)
//         {
//             goto LABEL_EXIT;
//         }

        while ( file_size > 0 )
        {
            if (file_size >= READ_BLOCK_SIZE)
            {
                read_size = READ_BLOCK_SIZE;
            }
            else
            {
                read_size = (int)file_size;
            }

            read_ret = fread( buffer, 1, read_size, fh );

            if (read_ret != read_size)
            {
                mParameters->status = VFL_STATUS_READ_FILE_FAILED;
                VFL_ERROR("[VFL][CalculateFileHash] read file %s failed errno = %d\n", file_path.c_str(), VFL_GetLastError());
                goto LABEL_EXIT;
            }

//             status = mProgress.OnReadUpdate( read_size );
// 
//             if (0 != status)
//             {
//                 goto LABEL_EXIT;
//             }

            hashObj.addData( buffer, read_size );

            file_size -= read_size;
        }

        VFL_DataToHex( hashObj.result(), hashObj.resultSize(), hash );

        VFL_TRACE("[VFL] file %s hash %s\n", file_path.c_str(), hash.c_str());

        ret = 0;

LABEL_EXIT:;

        if (NULL != fh)
        {
            fclose( fh );
            fh = NULL;
        }

        return ret;
    }

private:

    typedef std::map< std::string, std::string >    CONTAINER_TYPE;

    CONTAINER_TYPE                  mHashFileContainer;

    VFL_Parameters *                mParameters;
    VFL_LONGLONG                    mTotalFileSize;

    VFL_Progress                    mProgress;
};

VFL_BOOLEAN VFL_ValidateFiles( VFL_Parameters * parameters )
{
    VFL_BOOLEAN ret = VFL_FALSE;
    int status = 0;

    VFL_FileValidate fileValidateObj;
    std::string map_data_path, map_data_hash_file_path;

    fileValidateObj.SetParam( parameters );

    status = VFL_NormalPath(parameters->map_data_path, map_data_path);

    if (0 != status)
    {
        goto LABEL_EXIT;
    }

    status = VFL_NormalPath(parameters->map_data_hash_file_path, map_data_hash_file_path);

    if (0 != status)
    {
        goto LABEL_EXIT;
    }

    status = fileValidateObj.ValidateFiles( map_data_path, map_data_hash_file_path );

    if (0 != status)
    {
        goto LABEL_EXIT;
    }

    ret = VFL_TRUE;

LABEL_EXIT:;

    return ret;
}

std::string GetFileHash(JNIEnv* env, std::string& strFileName)
{
    LOGE("strFileName=%s",  env->NewStringUTF(strFileName.c_str()));
  VFL_FileValidate obj;
  std::string strHash;
  obj.CalculateFileHash(strFileName,strHash);
    LOGE("strFileName=%s",  env->NewStringUTF(strHash.c_str()));
  return strHash;
}