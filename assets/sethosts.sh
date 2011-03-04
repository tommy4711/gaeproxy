#!/system/bin/sh

DIR=/data/data/org.gaeproxy

case $1 in
    start)

        mount -o rw,remount -t yaffs2 \
            /dev/block/mtdblock3 \
            /system

        mv /etc/hosts $DIR/hosts

        echo "$2 $3" > /etc/hosts
        echo "" >> /etc/hosts

        mount -o ro,remount -t yaffs2 \
            /dev/block/mtdblock3 \
            /system

        ;;

    stop)

        mount -o rw,remount -t yaffs2 \
            /dev/block/mtdblock3 \
            /system

        mv $DIR/hosts /etc/hosts

        mount -o ro,remount -t yaffs2 \
            /dev/block/mtdblock3 \
            /system

        ;;
esac

