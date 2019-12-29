#!/bin/bash
echo "NOTE: You can provide parameters for connection data etc."

pg_dump -f status.sql --schema=status --no-owner --schema-only --no-privileges $@
