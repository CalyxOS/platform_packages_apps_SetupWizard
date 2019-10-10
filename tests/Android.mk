LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_PRIVILEGED_MODULE := true

LOCAL_STATIC_JAVA_LIBRARIES := \
    org.lineageos.platform.internal

# Include all test java files.
LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := CalyxSetupWizardTests
LOCAL_CERTIFICATE := platform

LOCAL_PRIVATE_PLATFORM_APIS := true

include $(BUILD_PACKAGE)
