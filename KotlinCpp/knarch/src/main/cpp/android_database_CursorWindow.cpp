/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#undef LOG_TAG
#define LOG_TAG "CursorWindow"

#include "Assert.h"
#include "Memory.h"
#include "Natives.h"
#include "KString.h"
#include "KonanHelper.h"
#include "Porting.h"
#include "Types.h"

#include "utf8.h"

#include "UtilsErrors.h"

#include <stdio.h>
#include <string.h>
#include <unistd.h>

#include "AndroidfwCursorWindow.h"

#include "android_database_SQLiteCommon.h"


namespace android {

    //TODO: We're using KLong to point to alloced memory, but maybe we want to use a pointer type?

/*
static struct {
    jfieldID data;
    jfieldID sizeCopied;
} gCharArrayBufferClassInfo;
*/

//static jstring gEmptyString;

static void throwExceptionWithRowCol(KInt row, KInt column) {
    char exceptionMessage[150];
    snprintf(exceptionMessage, sizeof(exceptionMessage),
             "Couldn't read row %d, col %d from CursorWindow.  Make sure the Cursor is initialized correctly before accessing data from it.",
             row, column);
    ObjHolder messageHold;
    CreateStringFromCString(exceptionMessage, messageHold.slot());
    ThrowSql_IllegalStateException((KString)messageHold.obj());
}

static void throwUnknownTypeException(KInt type) {
    char exceptionMessage[50];
    snprintf(exceptionMessage, sizeof(exceptionMessage), "UNKNOWN type %d", type);
    ObjHolder messageHold;
    CreateStringFromCString(exceptionMessage, messageHold.slot());
    ThrowSql_IllegalStateException((KString)messageHold.obj());
}

static KLong nativeCreate(KInt cursorWindowSize, KRef dataArray) {

    CursorWindow *window;
    status_t status = CursorWindow::create(cursorWindowSize, (void*)ArrayAddressOfElementAt(dataArray->array(), 0), &window);
    if (status || !window) {
        ALOGE("Could not allocate CursorWindow of size %d due to error %d.", cursorWindowSize, status);
        return 0;
    }

    LOG_WINDOW("nativeInitializeEmpty: window = %p", window);
    return reinterpret_cast<KLong>(window);
}

/*
static jlong nativeCreateFromParcel(JNIEnv* env, jclass clazz, jobject parcelObj) {
    Parcel* parcel = parcelForJavaObject(env, parcelObj);

    CursorWindow* window;
    status_t status = CursorWindow::createFromParcel(parcel, &window);
    if (status || !window) {
        ALOGE("Could not create CursorWindow from Parcel due to error %d, process fd count=%d",
                status, getFdCount());
        return 0;
    }

    LOG_WINDOW("nativeInitializeFromBinder: numRows = %d, numColumns = %d, window = %p",
            window->getNumRows(), window->getNumColumns(), window);
    return reinterpret_cast<jlong>(window);
}
 */

static void nativeDispose(KLong windowPtr) {
    CursorWindow *window = reinterpret_cast<CursorWindow *>(windowPtr);
    if (window) {
        LOG_WINDOW("Closing window %p", window);
        delete window;
    }
}

/*
static void nativeWriteToParcel(JNIEnv * env, jclass clazz, jlong windowPtr,
        jobject parcelObj) {
    CursorWindow* window = reinterpret_cast<CursorWindow*>(windowPtr);
    Parcel* parcel = parcelForJavaObject(env, parcelObj);

    status_t status = window->writeToParcel(parcel);
    if (status) {
        String8 msg;
        msg.appendFormat("Could not write CursorWindow to Parcel due to error %d.", status);
        jniThrowRuntimeException(env, msg.string());
    }
}
*/

static void nativeClear(KLong windowPtr) {
    CursorWindow *window = reinterpret_cast<CursorWindow *>(windowPtr);
    LOG_WINDOW("Clearing window %p", window);
    status_t status = window->clear();
    if (status) {
        LOG_WINDOW("Could not clear window. error=%d", status);
    }
}

static KInt nativeGetNumRows(KLong windowPtr) {
    CursorWindow *window = reinterpret_cast<CursorWindow *>(windowPtr);
    return window->getNumRows();
}

static KBoolean nativeSetNumColumns(KLong windowPtr, KInt columnNum) {
    CursorWindow *window = reinterpret_cast<CursorWindow *>(windowPtr);
    status_t status = window->setNumColumns(columnNum);
    return status == OK;
}

static KBoolean nativeAllocRow(KLong windowPtr) {
    CursorWindow *window = reinterpret_cast<CursorWindow *>(windowPtr);
    status_t status = window->allocRow();
    return status == OK;
}

static void nativeFreeLastRow(KLong windowPtr) {
    CursorWindow *window = reinterpret_cast<CursorWindow *>(windowPtr);
    window->freeLastRow();
}

static KInt nativeGetType(KLong windowPtr, KInt row, KInt column) {
    CursorWindow *window = reinterpret_cast<CursorWindow *>(windowPtr);
    LOG_WINDOW("returning column type affinity for %d,%d from %p", row, column, window);

    CursorWindow::FieldSlot *fieldSlot = window->getFieldSlot(row, column);
    if (!fieldSlot) {
        // FIXME: This is really broken but we have CTS tests that depend
        // on this legacy behavior.
        //throwExceptionWithRowCol(env, row, column);
        return CursorWindow::FIELD_TYPE_NULL;
    }
    return window->getFieldSlotType(fieldSlot);
}

extern "C" {

OBJ_GETTER(Android_Database_CursorWindow_nativeGetBlob, KRef thiz, KLong windowPtr, KInt row, KInt column) {
    CursorWindow *window = reinterpret_cast<CursorWindow *>(windowPtr);
    LOG_WINDOW("Getting blob for %d,%d from %p", row, column, window);

    CursorWindow::FieldSlot *fieldSlot = window->getFieldSlot(row, column);
    if (!fieldSlot) {
        throwExceptionWithRowCol(row, column);
        RETURN_OBJ(nullptr);
    }

    KInt type = window->getFieldSlotType(fieldSlot);
    if (type == CursorWindow::FIELD_TYPE_BLOB || type == CursorWindow::FIELD_TYPE_STRING) {
        size_t size;
        const void *value = window->getFieldSlotValueBlob(fieldSlot, &size);
        if (!value) {
            throw_sqlite3_exception("Native could not read blob slot");
            return NULL;
        }
        ArrayHeader *result = AllocArrayInstance(
                theByteArrayTypeInfo, size, OBJ_RESULT)->array();

        //TODO: How to check if array properly created?
        /*if (!byteArray) {
        env->ExceptionClear();
        throw_sqlite3_exception(env, "Native could not create new byte[]");
        RETURN_OBJ(nullptr);
    }*/
    memcpy(PrimitiveArrayAddressOfElementAt<KByte>(result, 0),
           value,
           size);

    RETURN_OBJ(result->obj());
} else if (type == CursorWindow::FIELD_TYPE_INTEGER) {
    throw_sqlite3_exception("INTEGER data in nativeGetBlob ");
} else if (type == CursorWindow::FIELD_TYPE_FLOAT) {
    throw_sqlite3_exception("FLOAT data in nativeGetBlob ");
} else if (type == CursorWindow::FIELD_TYPE_NULL) {
    // do nothing
} else {
    throwUnknownTypeException(type);
}
    RETURN_OBJ(nullptr);
}

OBJ_GETTER(Android_Database_CursorWindow_nativeGetString, KRef thiz, KLong windowPtr, KInt row, KInt column) {
    CursorWindow *window = reinterpret_cast<CursorWindow *>(windowPtr);
    LOG_WINDOW("Getting string for %d,%d from %p", row, column, window);

    CursorWindow::FieldSlot *fieldSlot = window->getFieldSlot(row, column);
    if (!fieldSlot) {
        throwExceptionWithRowCol(row, column);
        return NULL;
    }

    int32_t type = window->getFieldSlotType(fieldSlot);
    if (type == CursorWindow::FIELD_TYPE_STRING) {
        size_t sizeIncludingNull;
        const char *value = window->getFieldSlotValueString(fieldSlot, &sizeIncludingNull);
        //TODO: Figure this out
        if (sizeIncludingNull <= 1) {
            RETURN_RESULT_OF(CreateStringFromUtf8, "", 0);
        }
        // Convert to UTF-16 here instead of calling NewStringUTF.  NewStringUTF
        // doesn't like UTF-8 strings with high codepoints.  It actually expects
        // Modified UTF-8 with encoded surrogate pairs.
        RETURN_RESULT_OF(CreateStringFromUtf8, value, sizeIncludingNull - 1);
    } else if (type == CursorWindow::FIELD_TYPE_INTEGER) {
        int64_t value = window->getFieldSlotValueLong(fieldSlot);
        char buf[32];
        int size = snprintf(buf, sizeof(buf), "%" PRId64, value);
        RETURN_RESULT_OF(CreateStringFromUtf8, buf, size);
    } else if (type == CursorWindow::FIELD_TYPE_FLOAT) {
        double value = window->getFieldSlotValueDouble(fieldSlot);
        char buf[32];
        int size = snprintf(buf, sizeof(buf), "%g", value);
        RETURN_RESULT_OF(CreateStringFromUtf8, buf, size);
    } else if (type == CursorWindow::FIELD_TYPE_NULL) {
        return NULL;
    } else if (type == CursorWindow::FIELD_TYPE_BLOB) {
        throw_sqlite3_exception("Unable to convert BLOB to string");
        return NULL;
    } else {
        throwUnknownTypeException(type);
        return NULL;
    }
}

}

/*
static jcharArray allocCharArrayBuffer(JNIEnv* env, jobject bufferObj, size_t size) {
    jcharArray dataObj = jcharArray(env->GetObjectField(bufferObj,
            gCharArrayBufferClassInfo.data));
    if (dataObj && size) {
        jsize capacity = env->GetArrayLength(dataObj);
        if (size_t(capacity) < size) {
            env->DeleteLocalRef(dataObj);
            dataObj = NULL;
        }
    }
    if (!dataObj) {
        jsize capacity = size;
        if (capacity < 64) {
            capacity = 64;
        }
        dataObj = env->NewCharArray(capacity); // might throw OOM
        if (dataObj) {
            env->SetObjectField(bufferObj, gCharArrayBufferClassInfo.data, dataObj);
        }
    }
    return dataObj;
}

static void fillCharArrayBufferUTF(JNIEnv* env, jobject bufferObj,
        const char* str, size_t len) {
    ssize_t size = utf8_to_utf16_length(reinterpret_cast<const uint8_t*>(str), len);
    if (size < 0) {
        size = 0; // invalid UTF8 string
    }
    jcharArray dataObj = allocCharArrayBuffer(env, bufferObj, size);
    if (dataObj) {
        if (size) {
            jchar* data = static_cast<jchar*>(env->GetPrimitiveArrayCritical(dataObj, NULL));
            utf8_to_utf16_no_null_terminator(reinterpret_cast<const uint8_t*>(str), len,
                    reinterpret_cast<char16_t*>(data), (size_t) size);
            env->ReleasePrimitiveArrayCritical(dataObj, data, 0);
        }
        env->SetIntField(bufferObj, gCharArrayBufferClassInfo.sizeCopied, size);
    }
}

static void clearCharArrayBuffer(JNIEnv* env, jobject bufferObj) {
    jcharArray dataObj = allocCharArrayBuffer(env, bufferObj, 0);
    if (dataObj) {
        env->SetIntField(bufferObj, gCharArrayBufferClassInfo.sizeCopied, 0);
    }
}

static void nativeCopyStringToBuffer(JNIEnv* env, jclass clazz, jlong windowPtr,
        jint row, jint column, jobject bufferObj) {
    CursorWindow* window = reinterpret_cast<CursorWindow*>(windowPtr);
    LOG_WINDOW("Copying string for %d,%d from %p", row, column, window);

    CursorWindow::FieldSlot* fieldSlot = window->getFieldSlot(row, column);
    if (!fieldSlot) {
        throwExceptionWithRowCol(env, row, column);
        return;
    }

    int32_t type = window->getFieldSlotType(fieldSlot);
    if (type == CursorWindow::FIELD_TYPE_STRING) {
        size_t sizeIncludingNull;
        const char* value = window->getFieldSlotValueString(fieldSlot, &sizeIncludingNull);
        if (sizeIncludingNull > 1) {
            fillCharArrayBufferUTF(env, bufferObj, value, sizeIncludingNull - 1);
        } else {
            clearCharArrayBuffer(env, bufferObj);
        }
    } else if (type == CursorWindow::FIELD_TYPE_INTEGER) {
        int64_t value = window->getFieldSlotValueLong(fieldSlot);
        char buf[32];
        snprintf(buf, sizeof(buf), "%" PRId64, value);
        fillCharArrayBufferUTF(env, bufferObj, buf, strlen(buf));
    } else if (type == CursorWindow::FIELD_TYPE_FLOAT) {
        double value = window->getFieldSlotValueDouble(fieldSlot);
        char buf[32];
        snprintf(buf, sizeof(buf), "%g", value);
        fillCharArrayBufferUTF(env, bufferObj, buf, strlen(buf));
    } else if (type == CursorWindow::FIELD_TYPE_NULL) {
        clearCharArrayBuffer(env, bufferObj);
    } else if (type == CursorWindow::FIELD_TYPE_BLOB) {
        throw_sqlite3_exception(env, "Unable to convert BLOB to string");
    } else {
        throwUnknownTypeException(env, type);
    }
}
 */

static KLong nativeGetLong(KLong windowPtr, KInt row, KInt column) {
    CursorWindow *window = reinterpret_cast<CursorWindow *>(windowPtr);
    LOG_WINDOW("Getting long for %d,%d from %p", row, column, window);

    CursorWindow::FieldSlot *fieldSlot = window->getFieldSlot(row, column);
    if (!fieldSlot) {
        throwExceptionWithRowCol(row, column);
        return 0;
    }

    int32_t type = window->getFieldSlotType(fieldSlot);
    if (type == CursorWindow::FIELD_TYPE_INTEGER) {
        return window->getFieldSlotValueLong(fieldSlot);
    } else if (type == CursorWindow::FIELD_TYPE_STRING) {
        size_t sizeIncludingNull;
        const char* value = window->getFieldSlotValueString(fieldSlot, &sizeIncludingNull);
        return sizeIncludingNull > 1 ? strtoll(value, NULL, 0) : 0L;
    } else if (type == CursorWindow::FIELD_TYPE_FLOAT) {
        return (KLong)window->getFieldSlotValueDouble(fieldSlot);
    } else if (type == CursorWindow::FIELD_TYPE_NULL) {
        return 0;
    } else if (type == CursorWindow::FIELD_TYPE_BLOB) {
        throw_sqlite3_exception("Unable to convert BLOB to long");
        return 0;
    } else {
        throwUnknownTypeException(type);
        return 0;
    }
}

static KDouble nativeGetDouble(KLong windowPtr, KInt row, KInt column) {
    CursorWindow *window = reinterpret_cast<CursorWindow *>(windowPtr);
    LOG_WINDOW("Getting double for %d,%d from %p", row, column, window);

    CursorWindow::FieldSlot *fieldSlot = window->getFieldSlot(row, column);
    if (!fieldSlot) {
        throwExceptionWithRowCol(row, column);
        return 0.0;
    }

    int32_t type = window->getFieldSlotType(fieldSlot);
    if (type == CursorWindow::FIELD_TYPE_FLOAT) {
        return window->getFieldSlotValueDouble(fieldSlot);
    } else if (type == CursorWindow::FIELD_TYPE_STRING) {
        size_t sizeIncludingNull;
        const char* value = window->getFieldSlotValueString(fieldSlot, &sizeIncludingNull);
        return sizeIncludingNull > 1 ? strtod(value, NULL) : 0.0;
    } else if (type == CursorWindow::FIELD_TYPE_INTEGER) {
        return (KDouble)window->getFieldSlotValueLong(fieldSlot);
    } else if (type == CursorWindow::FIELD_TYPE_NULL) {
        return 0.0;
    } else if (type == CursorWindow::FIELD_TYPE_BLOB) {
        throw_sqlite3_exception("Unable to convert BLOB to double");
        return 0.0;
    } else {
        throwUnknownTypeException(type);
        return 0.0;
    }
}

static KBoolean nativePutBlob(KLong windowPtr, KConstRef valueObj, KInt row, KInt column) {
    CursorWindow *window = reinterpret_cast<CursorWindow *>(windowPtr);

    const ArrayHeader *array = valueObj->array();

    KInt len = array->count_;

    const KByte *value = ByteArrayAddressOfElementAt(array, 0);
    status_t status = window->putBlob(row, column, value, len);

    if (status) {
        LOG_WINDOW("Failed to put blob. error=%d", status);
        return false;
    }

    LOG_WINDOW("%d,%d is BLOB with %u bytes", row, column, len);
    return true;
}

static KBoolean nativePutString(KLong windowPtr, KString valueObj, KInt row, KInt column) {
    CursorWindow *window = reinterpret_cast<CursorWindow *>(windowPtr);

    size_t sizeIncludingNull;

    char *strBytes = CreateCStringFromStringWithSize(valueObj, &sizeIncludingNull);
    //Size from method doesn't have null
    sizeIncludingNull++;

    status_t status = window->putString(row, column, (const char *) strBytes, sizeIncludingNull);

    DisposeCStringHelper(strBytes);

    if (status) {
        LOG_WINDOW("Failed to put string. error=%d", status);
        return false;
    }

    LOG_WINDOW("%d,%d is TEXT with %u bytes", row, column, sizeIncludingNull);
    return true;
}


static KBoolean nativePutLong(KLong windowPtr, KLong value, KInt row, KInt column) {
    CursorWindow *window = reinterpret_cast<CursorWindow *>(windowPtr);
    status_t status = window->putLong(row, column, value);

    if (status) {
        LOG_WINDOW("Failed to put long. error=%d", status);
        return false;
    }

    LOG_WINDOW("%d,%d is INTEGER 0x%016llx", row, column, value);
    return true;
}

static KBoolean nativePutDouble(KLong windowPtr, KDouble value, KInt row, KInt column) {
    CursorWindow *window = reinterpret_cast<CursorWindow *>(windowPtr);
    status_t status = window->putDouble(row, column, value);

    if (status) {
        LOG_WINDOW("Failed to put double. error=%d", status);
        return false;
    }

    LOG_WINDOW("%d,%d is FLOAT %lf", row, column, value);
    return true;
}

static KBoolean nativePutNull(KLong windowPtr, KInt row, KInt column) {
    CursorWindow *window = reinterpret_cast<CursorWindow *>(windowPtr);
    status_t status = window->putNull(row, column);

    if (status) {
        LOG_WINDOW("Failed to put null. error=%d", status);
        return false;
    }

    LOG_WINDOW("%d,%d is NULL", row, column);
    return true;
}

extern "C" {

KLong Android_Database_CursorWindow_nativeCreate(KRef thiz, KInt cursorWindowSize, KRef dataArray) {
    return nativeCreate(cursorWindowSize, dataArray);
}

void Android_Database_CursorWindow_nativeDispose(KRef thiz, KLong windowPtr) {
    nativeDispose(windowPtr);
}

void Android_Database_CursorWindow_nativeClear(KRef thiz, KLong windowPtr) {
    nativeClear(windowPtr);
}

KInt Android_Database_CursorWindow_nativeGetNumRows(KRef thiz, KLong windowPtr) {
    return nativeGetNumRows(windowPtr);
}

KBoolean Android_Database_CursorWindow_nativeSetNumColumns(KRef thiz, KLong windowPtr, KInt columnNum) {
    return nativeSetNumColumns(windowPtr, columnNum);
}

KBoolean Android_Database_CursorWindow_nativeAllocRow(KRef thiz, KLong windowPtr) {
    return nativeAllocRow(windowPtr);
}

void Android_Database_CursorWindow_nativeFreeLastRow(KRef thiz, KLong windowPtr) {
    nativeFreeLastRow(windowPtr);
}

KInt Android_Database_CursorWindow_nativeGetType(KRef thiz, KLong windowPtr, KInt row, KInt column) {
    return nativeGetType(windowPtr, row, column);
}

KLong Android_Database_CursorWindow_nativeGetLong(KRef thiz, KLong windowPtr, KInt row, KInt column) {
    return nativeGetLong(windowPtr, row, column);
}

KDouble Android_Database_CursorWindow_nativeGetDouble(KRef thiz, KLong windowPtr, KInt row, KInt column) {
    return nativeGetDouble(windowPtr, row, column);
}

KBoolean
Android_Database_CursorWindow_nativePutBlob(KRef thiz, KLong windowPtr, KConstRef valueObj, KInt row, KInt column) {
    return nativePutBlob(windowPtr, valueObj, row, column);
}

KBoolean
Android_Database_CursorWindow_nativePutString(KRef thiz, KLong windowPtr, KString valueObj, KInt row, KInt column) {
    return nativePutString(windowPtr, valueObj, row, column);
}

KBoolean
Android_Database_CursorWindow_nativePutLong(KRef thiz, KLong windowPtr, KLong value, KInt row, KInt column) {
    return nativePutLong(windowPtr, value, row, column);
}

KBoolean
Android_Database_CursorWindow_nativePutDouble(KRef thiz, KLong windowPtr, KDouble value, KInt row, KInt column) {
    return nativePutDouble(windowPtr, value, row, column);
}

KBoolean Android_Database_CursorWindow_nativePutNull(KRef thiz, KLong windowPtr, KInt row, KInt column) {
    return nativePutNull(windowPtr, row, column);
}
}
} // namespace android