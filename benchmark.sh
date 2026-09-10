#!/bin/bash

# Configuration
FORMAT="xml" 
INPUT_DIR="./data_files"
OUTPUT_DIR="./output_results"

# Ensure output directory exists
mkdir -p "$OUTPUT_DIR"

echo "======================================================="
echo "Starting Bulk Processing Benchmark Analysis"
echo "======================================================="

for filepath in "$INPUT_DIR"/*; do
    [ -f "$filepath" ] || continue 
    
    filename=$(basename -- "$filepath")
    filename_no_ext="${filename%.*}"
    output_file="$OUTPUT_DIR/${filename_no_ext}_processed.$FORMAT"
    
    echo -e "\n[PROCESSING] File: $filename"
    
    # FIXED: Swapped argument positions to match what Spring expects 
    # Passing the source filepath directly instead of via stdin redirection (<)
    java -jar target/text-processing-service-0.0.1-SNAPSHOT.jar "$FORMAT" "$filepath" "$output_file"
    
done

echo -e "\n======================================================="
echo "Benchmark Analysis Completed Successfully!"
echo "======================================================="
