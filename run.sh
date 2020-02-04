#!/bin/sh
#
# replacing username with ${TARGET_USERNAME}
sed -i -e 's@\${target_username}@'${TARGET_USERNAME}'@g' configuration.json
# replacing password with ${TARGET_PASSWORD}
sed -i -e 's@\${target_password}@'${TARGET_PASSWORD}'@g' configuration.json

# replacing source server with ${SOURCE_SERVER}
sed -i -e 's@\${source_server}@'${SOURCE_SERVER}'@g' configuration.json
# replacing target server with ${TARGET_SERVER}
sed -i -e 's@\${target_server}@'${TARGET_SERVER}'@g' configuration.json

while :
do
  exec java -Djava.net.useSystemProxies=true -jar SensorThingsCopier.jar
done
