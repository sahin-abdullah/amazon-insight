#!/bin/bash

## Wait for the database to be ready
#while ! </dev/tcp/db/5432; do
#  echo "Waiting for database to be ready..."
#  sleep 1
#done

# Install Playwright within the Conda environment
conda run -n insight playwright install-deps
conda run -n insight playwright install



# Start the Flask application
exec conda run --no-capture-output -n insight flask run --host=0.0.0.0
