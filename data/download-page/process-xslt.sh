#!/bin/bash


out_dir=/home/kinok/www/helloapp

if [ "$cat" = '' ] ; then
    echo "Usage: process-templ.sh [category name (ex: house or bbad...)] "
    exit 0
fi

echo "Processing template..."

xsltproc        \
    --output "$out_dir"/index.html      \
    index.xsl   \
    index.xml
