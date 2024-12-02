# AI-Dial App Controller

AI-Dial App Controller is a Java-based web service application that orchestrates the building and deployment of Python applications as Knative services on Kubernetes. The application is built using Spring WebFlux and leverages Kubernetes and Knative APIs to manage the lifecycle of applications.

## Features

- Builds a Docker image of a Python application from source code downloaded from [DIAL](https://github.com/epam/ai-dial).
- Deploys the Python application as a [Knative](https://knative.dev/docs/) service on [Kubernetes](https://kubernetes.io/).
- Provides RESTful APIs to manage the lifecycle of applications.

## Prerequisites

- Java 21
- [Docker](https://www.docker.com/)
- [Kubernetes](https://kubernetes.io/) cluster with [Knative](https://knative.dev/docs/) installed
- Access to a Docker registry
- The builder image should be deployed to the registry manually prior to starting the application.

## Setup

1. **Clone the repository:**

   ```bash
   git clone https://github.com/yourusername/ai-dial-app-controller.git
   cd ai-dial-app-controller
   ```

2. **Build the Docker image:**

   ```bash
   docker build -t ai-dial-app-controller .
   ```

3. **Run the application with environment variables:**

   ```bash
   docker run -p 8080:8080 \
     -e APP_BUILD_NAMESPACE=your-build-namespace \
     -e APP_DEPLOY_NAMESPACE=your-deploy-namespace \
     -e APP_DOCKER_REGISTRY=your-docker-registry \
     -e APP_DIAL_BASE_URL=https://your-dial-base-url \
     ai-dial-app-controller
   ```

## Configuration

The application can be configured using environment variables or by modifying the `application.yaml` file located in `src/main/resources`.

### Environment Variables

| Setting                          | Default            | Required | Description                                           |
|----------------------------------|--------------------|----------|-------------------------------------------------------|
| `APP_BUILD_NAMESPACE`            |                    | Yes      | The Kubernetes namespace used for building images.    |
| `APP_DEPLOY_NAMESPACE`           |                    | Yes      | The Kubernetes namespace used for deploying services. |
| `APP_DOCKER_REGISTRY`            |                    | Yes      | The Docker registry where images are stored.          |
| `APP_DIAL_BASE_URL`              |                    | Yes      | The base URL for the DIAL service.                    |
| `APP_HEARTBEAT_PERIOD_SEC`       | `30`               | No       | The interval in seconds for sending heartbeat events. |
| `APP_IMAGE_LABEL`                | `latest`           | No       | The label used for Docker images.                     |
| `APP_IMAGE_BUILD_TIMEOUT_SEC`    | `300`              | No       | Timeout in seconds for building Docker images.        |
| `APP_SERVICE_SETUP_TIMEOUT_SEC`  | `300`              | No       | Timeout in seconds for setting up Knative services.   |
| `APP_MAX_ERROR_LOG_LINES`        | `20`               | No       | Maximum number of error log lines to display.         |
| `APP_MAX_ERROR_LOG_CHARS`        | `1000`             | No       | Maximum number of error log characters to display.    |
| `APP_BUILDER_TEMPLATE_CONTAINER` | `builder-template` | No       | Name of the puller container.                         |
| `APP_BUILDER_CONTAINER`          | `builder`          | No       | Name of the builder container.                        |
| `APP_SERVICE_CONTAINER`          | `app-container`    | No       | Name of the service container.                        |
| `APP_DEFAULT_RUNTIME`            | `python3.11`       | No       | Default runtime for Python applications.              |

## Usage

The application exposes RESTful APIs to manage the lifecycle of applications. Below are some of the key endpoints with usage examples:

### Create Image

Builds a Docker image from the specified source code using Server-Sent Events (SSE).

**Request:**

```bash
curl -N -X POST http://localhost:8080/v1/image/my-python-app \
     -H "Content-Type: application/json" \
     -d '{
           "sources": "files/dial bucket/sources folder",
           "runtime": "python3.11"
         }'
```

**Response:**

The response is streamed as SSE, providing real-time updates on the image creation process.

### Delete Image

Deletes the Docker image for the specified application using SSE.

**Request:**

```bash
curl -N -X DELETE http://localhost:8080/v1/image/my-python-app
```

**Response:**

The response is streamed as SSE, providing real-time updates on the image deletion process.

### Create Deployment

Deploys the application as a Knative service using SSE.

**Request:**

```bash
curl -N -X POST http://localhost:8080/v1/deployment/my-python-app \
     -H "Content-Type: application/json" \
     -d '{
           "env": {
             "ENV_VAR_NAME": "value"
           }
         }'
```

**Response:**

The response is streamed as SSE, providing real-time updates on the deployment process.

### Delete Deployment

Deletes the Knative service for the specified application using SSE.

**Request:**

```bash
curl -N -X DELETE http://localhost:8080/v1/deployment/my-python-app
```

**Response:**

The response is streamed as SSE, providing real-time updates on the deployment deletion process.

### Get Logs

Retrieves logs for the specified application.

**Request:**

```bash
curl -X GET http://localhost:8080/v1/deployment/my-python-app/logs
```

**Response:**

```json
{
   "logs": [
      {
         "instance": "pod-name",
         "content": "log content"
      }
   ]
}
```

## Sequence Diagram

Below is a detailed sequence diagram illustrating the workflow of building and deploying a Python application:

```mermaid
sequenceDiagram
    participant User
    participant AI-Dial App Controller
    participant Kubernetes
    participant Knative
    participant Docker Registry

    User->>AI-Dial App Controller: POST /v1/image/{name}
    AI-Dial App Controller->>Kubernetes: Create Kubernetes Job
    Kubernetes->>Docker Registry: Build and Push Image
    Docker Registry-->>Kubernetes: Image Pushed
    Kubernetes-->>AI-Dial App Controller: Job Completion Status
    AI-Dial App Controller-->>User: Image Created

    User->>AI-Dial App Controller: POST /v1/deployment/{name}
    AI-Dial App Controller->>Knative: Deploy Knative Service
    Knative-->>AI-Dial App Controller: Service URL
    AI-Dial App Controller-->>User: Deployment Created

    User->>AI-Dial App Controller: GET /v1/deployment/{name}/logs
    AI-Dial App Controller->>Kubernetes: Retrieve Logs
    Kubernetes-->>AI-Dial App Controller: Logs
    AI-Dial App Controller-->>User: Application Logs
```

## License

This project is licensed under the Apache 2.0 License - see the [LICENSE](LICENSE) file for details.