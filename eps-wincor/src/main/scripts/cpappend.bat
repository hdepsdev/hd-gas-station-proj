@echo off
if ""%1"" == """" goto end
set CLASSPATH=%CLASSPATH%;lib/%1
shift
rem Process the remaining arguments
:setArgs
if ""%1"" == """" goto doneSetArgs
set CLASSPATH=%CLASSPATH% %1
shift
goto setArgs
:doneSetArgs
:end