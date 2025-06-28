# CI/CD Deployment Setup

This repository includes a GitHub Actions workflow that automatically builds and deploys the BIMserver WAR file to a target server when code is pushed to the main/master branch.

## Prerequisites

### Server Requirements

1. **Target Server Setup:**
   - Ubuntu/Debian Linux server with SSH access
   - Tomcat 9 installed at `/opt/tomcat9/`
   - User with sudo privileges for Tomcat management

2. **Tomcat 9 Installation:**
   ```bash
   # Install Tomcat 9
   sudo apt update
   sudo apt install tomcat9 tomcat9-admin
   
   # Ensure Tomcat service is enabled
   sudo systemctl enable tomcat
   sudo systemctl start tomcat
   
   # Verify installation
   sudo systemctl status tomcat
   ```

### GitHub Repository Secrets

The deployment workflow requires the following secrets to be configured in your GitHub repository:

1. **Navigate to your repository on GitHub**
2. **Go to Settings → Secrets and variables → Actions**
3. **Add the following Repository secrets:**

| Secret Name | Description | Example |
|-------------|-------------|---------|
| `DEPLOY_HOST` | IP address or hostname of target server | `192.168.1.100` or `myserver.com` |
| `DEPLOY_USER` | Username for SSH connection | `ubuntu` or `deploy` |
| `DEPLOY_KEY` | Private SSH key for authentication | Content of your private key file |

### SSH Key Setup

1. **Generate SSH key pair (if you don't have one):**
   ```bash
   ssh-keygen -t rsa -b 4096 -f ~/.ssh/bimserver_deploy
   ```

2. **Copy public key to target server:**
   ```bash
   ssh-copy-id -i ~/.ssh/bimserver_deploy.pub user@your-server-ip
   ```

3. **Add private key content to GitHub secret:**
   ```bash
   cat ~/.ssh/bimserver_deploy
   ```
   Copy the entire content (including `-----BEGIN OPENSSH PRIVATE KEY-----` and `-----END OPENSSH PRIVATE KEY-----`) and add it as the `DEPLOY_KEY` secret.

### Server User Permissions

The deployment user needs sudo privileges to manage Tomcat. Add the following to `/etc/sudoers` or create a file in `/etc/sudoers.d/`:

```bash
# Allow deployment user to manage Tomcat without password
deploy_user ALL=(ALL) NOPASSWD: /bin/mv /tmp/ROOT.war /opt/tomcat9/webapps/ROOT.war
deploy_user ALL=(ALL) NOPASSWD: /bin/chown tomcat:tomcat /opt/tomcat9/webapps/ROOT.war
deploy_user ALL=(ALL) NOPASSWD: /bin/systemctl restart tomcat
deploy_user ALL=(ALL) NOPASSWD: /bin/systemctl stop tomcat
deploy_user ALL=(ALL) NOPASSWD: /bin/systemctl start tomcat
```

Replace `deploy_user` with your actual username.

## Workflow Features

- **Automatic Triggering:** Runs on every push to main/master branch
- **Manual Triggering:** Can be triggered manually from GitHub Actions tab
- **Build Optimization:** Uses Maven dependency caching for faster builds
- **Artifact Storage:** Stores the built WAR file as a downloadable artifact
- **Version Extraction:** Automatically detects the project version from Maven
- **Safe Deployment:** Verifies WAR file exists before attempting deployment
- **Security:** Uses SSH key authentication and removes temporary files

## Deployment Process

When triggered, the workflow performs these steps:

1. **Checkout** the latest code
2. **Setup** Java 8 and Maven
3. **Build** the project using `mvn clean package -DskipTests`
4. **Extract** version and locate the generated WAR file
5. **Deploy** the WAR file to `/opt/tomcat9/webapps/ROOT.war`
6. **Restart** Tomcat service
7. **Upload** artifacts for download

## Monitoring Deployments

- View deployment status in the GitHub Actions tab
- Check Tomcat logs: `sudo journalctl -u tomcat -f`
- Verify deployment: `curl http://your-server-ip:8080`

## Troubleshooting

### Common Issues

1. **SSH Connection Failed:**
   - Verify `DEPLOY_HOST`, `DEPLOY_USER`, and `DEPLOY_KEY` secrets
   - Ensure SSH key has proper permissions (600)
   - Check if target server allows SSH connections

2. **Permission Denied:**
   - Verify sudoers configuration
   - Check that deployment user has necessary permissions

3. **Tomcat Restart Failed:**
   - Check Tomcat service status: `sudo systemctl status tomcat`
   - Review Tomcat logs: `sudo journalctl -u tomcat -f`
   - Ensure sufficient disk space and memory

4. **Build Failed:**
   - Check Maven dependencies and Java version
   - Review build logs in GitHub Actions

### Manual Deployment

If automatic deployment fails, you can manually deploy:

```bash
# Download the artifact from GitHub Actions
# Then copy to server:
scp bimserverwar-*.war user@server:/tmp/ROOT.war
ssh user@server "sudo mv /tmp/ROOT.war /opt/tomcat9/webapps/ROOT.war && sudo systemctl restart tomcat"
```

## Security Considerations

- SSH keys should be unique for this deployment
- Regularly rotate SSH keys
- Monitor server access logs
- Consider using a dedicated deployment user with minimal privileges
- Keep the target server updated with security patches