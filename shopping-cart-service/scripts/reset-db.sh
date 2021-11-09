#! /usr/bin/env bash

psql -h localhost -p 5432 -Ushopping-cart shopping-cart -f ../ddl-scripts/drop_tables.sql  
