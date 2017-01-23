#! /bin/bash
if [ ! -x ./jre/bin/java ];then
chmod 777 ./jre/bin/java
fi
CLASSPATH=.
LIBDIR=`ls lib/*.jar`
for jar in $LIBDIR
  do
	CLASSPATH=$CLASSPATH:$jar
  done
./jre/bin/java -cp $CLASSPATH com.bhz.eps.EPSServer

