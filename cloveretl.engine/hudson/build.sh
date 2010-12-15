#!/bin/sh

set -e
set -u

export ANT_OPTS="-Xmx500m"

CLOVER_VERSION_X_X=`echo $JOB_NAME | sed 's/^\(.*\)-\([^-]*\)$/\2/'`

set

cd cloveretl.engine

/opt/apache-ant/bin/ant clean reports-hudson \
	-Dadditional.plugin.list=cloveretl.component.commercial,cloveretl.lookup.commercial,cloveretl.compiler.commercial,cloveretl.quickbase.commercial,cloveretl.tlfunction.commercial,cloveretl.ctlfunction.commercial\
	-Dcte.environment.config=engine-${CLOVER_VERSION_X_X}_java-1.6-Sun \
	-Dcte.logpath=/data/cte-logs \
	-Dcte.hudson.link=job/$JOB_NAME/$BUILD_NUMBER \
	-Dtest.exclude=org/jetel/graph/ResetTest.java \
	-Ddir.examples=../cloveretl.examples
	
if  [ "$(hostname)" != "klara" ] ; then
	rsync -rv --remove-source-files /data/cte-logs/ klara:/data/cte-logs
fi
