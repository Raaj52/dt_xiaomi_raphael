#!/vendor/bin/sh
#
# A simple script that prepare ANX Camera required files

ANX="/sdcard/.ANXCamera"

while true; do
	test -w /sdcard && break || sleep 1
done

mkdir -p $ANX/cheatcodes{,_reference} $ANX/features{,_reference}
echo -n $ANX/cheatcodes{,_reference}/ | xargs -n 1 cp -f /system/etc/ANXCamera/cheatcodes/.
echo -n $ANX/features{,_reference}/ | xargs -n 1 cp -f /system/etc/device_features/.
