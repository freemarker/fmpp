REM   Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
REM   reserved.
REM
REM   This file is a modified version of the file comes with Ant 1.5.
REM   The original version is copyrighted by The Apache Software Foundation.
REM   See the license in the LICENSE.txt in the same directory as this file.

if "%FMPP_BAT_DEBUG%" == "on" echo [DEBUG] lcp.bat invoked with: %1

set _CLASSPATHCOMPONENT=%1
if ""%1""=="""" goto gotAllArgs
shift

:argCheck
if ""%1""=="""" goto gotAllArgs
set _CLASSPATHCOMPONENT=%_CLASSPATHCOMPONENT% %1
shift
goto argCheck

:gotAllArgs
set LOCALCLASSPATH=%_CLASSPATHCOMPONENT%;%LOCALCLASSPATH%

