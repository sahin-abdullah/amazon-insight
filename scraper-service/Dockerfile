# Use Miniforge base image
FROM condaforge/mambaforge:latest

# Set the working directory
WORKDIR /app

# Set environment variable to avoid interactive prompts
ENV DEBIAN_FRONTEND=noninteractive

# Set the timezone environment variable
ENV TZ=America/New_York

# Install system dependencies
RUN apt-get update && apt-get install -y \
    gcc \
    g++ \
    make \
    libfreetype6-dev \
    libjpeg-dev \
    libpng-dev \
    libopenjp2-7-dev \
    zlib1g-dev \
    libtiff-dev \
    libglib2.0-0 \
    libnss3 \
    libnspr4 \
    libdbus-1-3 \
    libatk1.0-0 \
    libatk-bridge2.0-0 \
    libcups2 \
    libdrm2 \
    libxcb1 \
    libxkbcommon0 \
    libatspi2.0-0 \
    libx11-6 \
    libxcomposite1 \
    libxdamage1 \
    libxext6 \
    libxfixes3 \
    libxrandr2 \
    libgbm1 \
    libpango-1.0-0 \
    libcairo2 \
    libasound2 \
    tzdata \
    libx11-xcb1 \
    libxcursor1 \
    libxi6 \
    libgtk-3-0 \
    libpangocairo-1.0-0 \
    libcairo-gobject2 \
    libgdk-pixbuf2.0-0 \
    && rm -rf /var/lib/apt/lists/*

# Update Conda to the latest version available in the conda-forge channel
RUN mamba update -n base -c conda-forge conda -y

# Create a new user "appuser" and group, switch to it
RUN groupadd -r appuser && useradd -m -r -g appuser appuser

USER appuser

# Initialize Conda for shell interaction
RUN conda init bash

# Copy the Conda environment file
COPY --chown=appuser:appuser environment.yml /app/

# Install dependencies with Mamba
RUN mamba env create -f environment.yml

# Activate the environment
SHELL ["conda", "run", "-n", "insight", "/bin/bash", "-c"]

# Install Playwright dependencies
RUN playwright install

# Copy the rest of your application's code
COPY --chown=appuser:appuser . /app

# Expose the port the app runs on
EXPOSE 5000

# Set the Flask app to run when the container starts
CMD ["conda", "run", "--no-capture-output", "-n", "insight", "python", "run.py", "--host=0.0.0.0"]