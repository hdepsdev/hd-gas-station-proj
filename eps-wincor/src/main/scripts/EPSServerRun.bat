@echo off
set CLASSPATH=.
for %%i in (lib/*.jar) do call cpappend.bat %%i
set CLASSPATH=%CLASSPATH%
java -cp "%CLASSPATH%" com.bhz.eps.EPSServer
