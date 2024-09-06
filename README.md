# Amazon Insight

Welcome to **Amazon Insight**! This project allows users to gain deeper insights into their favorite Amazon products by scraping reviews in real-time and leveraging advanced language models to answer questions about the product.

## Watch Video

[![Watch the video](https://i.postimg.cc/Bn1rcbxM/Screenshot-2024-09-05-214015.png)](https://www.loom.com/share/7acc817ad20f49b6bdb7fd788c76a648?sid=7ace57d1-9699-44ab-a0e4-ce41959a387a)
## Overview

Amazon Insight is a multi-service application designed to provide a seamless user experience for extracting and analyzing Amazon product reviews. The application is composed of four main services:

1. **Spring Boot Service**: Hosts the user interface and handles user interactions.
2. **Flask Service**: Manages the scraping of Amazon product reviews and auxiliary tasks.
3. **Ollama Service**: Hosts the LLM model (currently using llama3:8b) for generating responses based on the scraped reviews.
4. **PostgreSQL Database**: Stores the scraped reviews and other relevant data.

All services are containerized using Docker, ensuring consistent and reproducible environments across development and production.

## Features

- **Real-Time Review Scraping**: Users can drop an Amazon product URL, and the application will scrape reviews in real-time.
- **Interactive Q&A**: After scraping, users can ask questions about the product, and the application will generate responses using the LLM model.
- **Customizable LLM Configuration**: The LLM model can be customized using Ollama's configuration, allowing for flexibility in model selection and tuning.

## Architecture

The project is structured as follows:

### Spring Boot Service

The Spring Boot service hosts the user interface and handles user interactions. Key components include:

- **Controllers**: Manage HTTP requests and responses.
  - `ChatController`
  - `ReviewController`
- **Services**: Handle business logic and communication with other services.
  - `ScraperService`
  - `OllamaClient`

### Flask Service

The Flask service manages the scraping of Amazon product reviews and auxiliary tasks. Key components include:

- **Routes**: Define API endpoints for scraping and other tasks.
  - `scraper.py`
- **Services**: Implement the scraping logic and other functionalities.
  - `scraper.py`
- **Schemas**: Define data validation and serialization schemas.
  - `amazon_product.py`
- **Models**: Define the database models for storing scraped data.
  - `amazon.py`

### Ollama Service

The Ollama service hosts the LLM model, which is used to generate responses based on the scraped reviews. The current model in use is llama3:8b, but this can be customized through Ollama's configuration.

### PostgreSQL Database

The PostgreSQL database stores the scraped reviews and other relevant data. Initialization scripts and Docker health checks ensure reliable database connectivity and performance.

## Getting Started

### Prerequisites

- Docker
- Docker Compose

### Installation

1. Clone the repository:
    ```sh
    git clone https://github.com/yourusername/amazon-insight.git
    cd amazon-insight
    ```

2. Create a `.env` file with the necessary environment variables.

3. Build and start the services using Docker Compose:
    ```sh
    docker-compose up --build
    ```
### Usage

1. Open your browser and navigate to `http://localhost:8080`.
2. Drop an Amazon product URL to start scraping reviews in real-time.
3. Once the reviews are scraped, you will be redirected to a landing page where you can ask questions about the product.
4. The application will generate responses using the LLM model based on the scraped reviews.

## Customization

The LLM model can be customized using Ollama's configuration. To change the model, update the configuration in the `ollama_models` directory and restart the services.

## Contributing

We welcome contributions! Please read our [contributing guidelines](CONTRIBUTING.md) for more information.

## License

This project is licensed under the GPL 3.0 License. See the `LICENSE` file for details.

## Acknowledgements

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Flask](https://flask.palletsprojects.com/)
- [Ollama](https://ollama.com/)
- [Docker](https://www.docker.com/)
- [PostgreSQL](https://www.postgresql.org/)

---

Thank you for using Amazon Insight! We hope this project helps you gain valuable insights into your favorite Amazon products. If you have any questions or feedback, please feel free to open an issue or contact us.