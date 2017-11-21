#!/bin/bash

# convert \
#     -size 500x300 canvas:white \
#     -gravity Center \
#     -font NewCenturySchlbk-Roman \
#     -pointsize 100 \
#     -kerning 2 \
#     -fill "#333" \
#     -annotate +0+5 "$1" \
#     -fill none \
#     -stroke "#c3b7aa" \
#     -strokewidth 1 \
#     -draw "rectangle 0,0 499,299" \
#     $1.png

echo \
"    {
	\"when\": {
	    \"type\": \"$1\",
	    \"then\": {
		\"say\": {
		    \"phrase\": \"$1\"
		},
		\"show\": {
		    \"picture\": {
			\"file\": \"$1.png\"
		    }
		}
	    }
	}
    },"
    
    
