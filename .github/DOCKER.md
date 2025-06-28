# Docker-based Deployment (Alternative)

If you prefer to use Docker for deployment instead of direct Tomcat installation, this directory contains Docker configurations for BIMserver.

## Quick Start with Docker

1. **Build the Docker image:**
   ```bash
   docker build -t bimserver:latest .
   ```

2. **Run the container:**
   ```bash
   docker run -d \
     --name bimserver \
     -p 8080:8080 \
     -v bimserver-data:/opt/bimserver/home \
     bimserver:latest
   ```

## Docker Compose Deployment

For production deployments, use Docker Compose:

```yaml
version: '3.8'
services:
  bimserver:
    image: bimserver:latest
    ports:
      - "8080:8080"
    volumes:
      - bimserver-data:/opt/bimserver/home
      - bimserver-logs:/opt/tomcat/logs
    restart: unless-stopped
    environment:
      - JAVA_OPTS=-Xmx2g -Xms1g

volumes:
  bimserver-data:
  bimserver-logs:
```

## CI/CD with Docker

To modify the GitHub Actions workflow for Docker deployment, replace the deployment step with:

```yaml
- name: Build Docker image
  run: |
    docker build -t bimserver:${{ steps.get_version.outputs.version }} .
    docker tag bimserver:${{ steps.get_version.outputs.version }} bimserver:latest

- name: Deploy with Docker
  env:
    DEPLOY_HOST: ${{ secrets.DEPLOY_HOST }}
    DEPLOY_USER: ${{ secrets.DEPLOY_USER }}
    DEPLOY_KEY: ${{ secrets.DEPLOY_KEY }}
  run: |
    # Save Docker image
    docker save bimserver:latest | gzip > bimserver.tar.gz
    
    # Create SSH key file
    echo "$DEPLOY_KEY" > deploy_key
    chmod 600 deploy_key
    
    # Copy image to server
    scp -i deploy_key -o StrictHostKeyChecking=no \
      bimserver.tar.gz \
      "$DEPLOY_USER@$DEPLOY_HOST:/tmp/bimserver.tar.gz"
    
    # Deploy on server
    ssh -i deploy_key -o StrictHostKeyChecking=no \
      "$DEPLOY_USER@$DEPLOY_HOST" \
      "gunzip -c /tmp/bimserver.tar.gz | docker load && \
       docker stop bimserver || true && \
       docker rm bimserver || true && \
       docker run -d --name bimserver -p 8080:8080 \
         -v bimserver-data:/opt/bimserver/home bimserver:latest && \
       rm /tmp/bimserver.tar.gz"
    
    # Clean up
    rm deploy_key bimserver.tar.gz
```